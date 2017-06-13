package com.door43.translationstudio.services;

import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.resourcecontainer.Project;
import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.network.Connection;
import com.door43.translationstudio.network.Peer;
import com.door43.util.RSAEncryption;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This class provides an exporting service (effectively a server) from which
 * other devices may browse and retreive translations
 */
public class ServerService extends NetworkService {
    public static final String PARAM_PRIVATE_KEY = "param_private_key";
    public static final String PARAM_PUBLIC_KEY = "param_public_key";
    public static final String PARAM_DEVICE_ALIAS = "param_device_alias";
    private static Boolean mIsRunning = false;
    private final IBinder mBinder = new LocalBinder();
    private OnServerEventListener listener;
    private int mPort = 0;
    private Thread mServerThread;
    private Map<String, Connection> mClientConnections = new HashMap<>();
    private PrivateKey privateKey;
    private String mPublicKey;
    private ServerSocket mServerSocket;
    private String deviceAlias;
    private Map<UUID, Request> requests = new HashMap<>();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Sets whether or not the service is running
     * @param running
     */
    protected void setRunning(Boolean running) {
        mIsRunning = running;
    }

    /**
     * Checks if the service is currently running
     * @return
     */
    public static boolean isRunning() {
        return mIsRunning;
    }

    public void setOnServerEventListener(OnServerEventListener callback) {
        listener = callback;
        if(isRunning() && listener != null) {
            listener.onServerServiceReady(mPort);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        if(intent != null) {
            Bundle args = intent.getExtras();
            if (args != null && args.containsKey(PARAM_PRIVATE_KEY) && args.containsKey(PARAM_PUBLIC_KEY) && args.containsKey(PARAM_DEVICE_ALIAS)) {
                privateKey = (PrivateKey) args.get(PARAM_PRIVATE_KEY);
                mPublicKey = args.getString(PARAM_PUBLIC_KEY);
                deviceAlias = args.getString(PARAM_DEVICE_ALIAS);
                mServerThread = new Thread(new ServerRunnable());
                mServerThread.start();
                return START_STICKY;
            }
        }
        Logger.e(this.getClass().getName(), "Export service requires arguments");
        stopService();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        stopService();
    }

    /**
     * Stops the service
     */
    public void stopService() {
        Logger.i(this.getClass().getName(), "Stopping export service");
        if(mServerThread != null) {
            mServerThread.interrupt();
        }
        if(mServerSocket != null) {
            try {
                mServerSocket.close();
            } catch (IOException e) {
                Logger.e(this.getClass().getName(), "Failed to close server socket", e);
            }
        }
        Connection[] clients = mClientConnections.values().toArray(new Connection[mClientConnections.size()]);
        for(Connection c:clients) {
            c.close();
        }
        mClientConnections.clear();
        setRunning(false);
    }

    /**
     * Sends a message to the peer
     * @param client the client to which the message will be sent
     * @param message the message being sent to the client
     */
    private void sendMessage(Peer client, String message) {
        if (mClientConnections.containsKey(client.getIpAddress())) {
            if(client.isSecure()) {
                // encrypt message
                PublicKey key = RSAEncryption.getPublicKeyFromString(client.keyStore.getString(PeerStatusKeys.PUBLIC_KEY));
                if(key != null) {
                    message = encryptMessage(key, message);
                } else {
                    Logger.w(this.getClass().getName(), "Missing the client's public key");
                    message = SocketMessages.MSG_EXCEPTION;
                }
            }
            mClientConnections.get(client.getIpAddress()).write(message);
        }
    }

    /**
     * Sends a request to a peer.
     * Requests are stored for reference when the client responds to the request
     * @param client
     * @param request
     */
    private void sendRequest(Peer client, Request request) {
        if(mClientConnections.containsKey(client.getIpAddress()) && client.isSecure()) {
            // remember request
            this.requests.put(request.uuid, request);
            // send request
            sendMessage(client, request.toString());
        }
    }

    /**
     * Accepts a client connection
     * @param peer
     */
    public void acceptConnection(Peer peer) {
        peer.setIsAuthorized(true);

        // send public key
        try {
            JSONObject json = new JSONObject();
            json.put("key", mPublicKey);
            // TRICKY: we manually write to peer so we don't encrypt it
            if(mClientConnections.containsKey(peer.getIpAddress())) {
                mClientConnections.get(peer.getIpAddress()).write(json.toString());
            }
        } catch (JSONException e) {
            Logger.w(this.getClass().getName(), "Failed to prepare response ", e);
            if(listener != null) {
                listener.onServerServiceError(e);
            }
        }
    }

    /**
     * Handles the initial handshake and authorization
     * @param client
     * @param message
     */
    private void onMessageReceived(Peer client, String message) {
        if(client.isAuthorized()) {
            if(client.isSecure() && client.hasIdentity()) {
                message = decryptMessage(privateKey, message);
                if(message != null) {
                    try {
                        Request request = Request.parse(message);
                        onRequestReceived(client, request);
                    } catch (JSONException e) {
                        if(listener != null) {
                            listener.onServerServiceError(e);
                        } else {
                            Logger.e(this.getClass().getName(), "Failed to parse request", e);
                        }
                    }
                } else if(listener != null) {
                    listener.onServerServiceError(new Exception("Message descryption failed"));
                }
            } else if(!client.isSecure()){
                // receive the key
                try {
                    JSONObject json = new JSONObject(message);
                    client.keyStore.add(PeerStatusKeys.PUBLIC_KEY, json.getString("key"));
                    client.setIsSecure(true);
                } catch (JSONException e) {
                    Logger.w(this.getClass().getName(), "Invalid request: " + message, e);
                }

                // send identity
                if(client.isSecure()) {
                    try {
                        JSONObject json = new JSONObject();
                        json.put("name", deviceAlias);
                        if(App.isTablet()) {
                            json.put("device", "tablet");
                        } else {
                            json.put("device", "phone");
                        }
                        MessageDigest md = MessageDigest.getInstance("SHA-256");
                        md.update(App.udid().getBytes("UTF-8"));
                        byte[] digest = md.digest();
                        BigInteger bigInt = new BigInteger(1, digest);
                        String hash = bigInt.toString();
                        json.put("id", hash);
                        sendMessage(client, json.toString());
                    } catch (Exception e){
                        Logger.w(this.getClass().getName(), "Failed to prepare response ", e);
                        if(listener != null) {
                            listener.onServerServiceError(e);
                        }
                    }
                }
            } else if(!client.hasIdentity()) {
                // receive identity
                message = decryptMessage(privateKey, message);
                try {
                    JSONObject json = new JSONObject(message);
                    client.setName(json.getString("name"));
                    client.setDevice(json.getString("device"));
                    client.setId(json.getString("id"));
                    client.setHasIdentity(true);
                    if(listener != null) {
                        listener.onClientChanged(client);
                    }
                } catch (JSONException e) {
                    Logger.w(this.getClass().getName(), "Invalid request: " + message, e);
                }
            }
        } else {
            Logger.w(this.getClass().getName(), "The client is not authorized");
            sendMessage(client, SocketMessages.MSG_AUTHORIZATION_ERROR);
        }
    }

    /**
     * Handles commands sent from the client
     * @param client
     * @param request
     */
    private void onRequestReceived(Peer client, Request request) {
        JSONObject contextJson = request.context;

        switch(request.type) {
            case TargetTranslation:
                String targetTranslationSlug = null;
                try {
                    targetTranslationSlug = contextJson.getString("target_translation_id");
                } catch (JSONException e) {
                    Logger.e(this.getClass().getName(), "invalid context", e);
                    break;
                }
                final File exportFile;
                try {
                    exportFile = File.createTempFile(targetTranslationSlug, "." + Translator.ARCHIVE_EXTENSION);
                } catch (IOException e) {
                    Logger.e(this.getClass().getName(), "Could not create a temp file", e);
                    break;
                }

                Translator translator = App.getTranslator();
                TargetTranslation targetTranslation = translator.getTargetTranslation(targetTranslationSlug);
                if(targetTranslation != null) {
                    try {
                        targetTranslation.setDefaultContributor(App.getProfile().getNativeSpeaker());
                        translator.exportArchive(targetTranslation, exportFile);
                        if(exportFile.exists()) {
                            ServerSocket fileSocket = openWriteSocket(new OnSocketEventListener() {
                                @Override
                                public void onOpen(Connection connection) {
                                    try {
                                        DataOutputStream out = new DataOutputStream(connection.getSocket().getOutputStream());
                                        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(exportFile)));
                                        byte[] buffer = new byte[8 * 1024];
                                        int count;
                                        while ((count = in.read(buffer)) > 0) {
                                            out.write(buffer, 0, count);
                                        }
                                        out.close();
                                        in.close();
                                    } catch (IOException e) {
                                        Logger.e(ServerService.class.getName(), "Failed to send the target translation", e);
                                    }
                                }
                            });

                            // send file details
                            JSONObject targetTranslationContext = new JSONObject();
                            targetTranslationContext.put("port", fileSocket.getLocalPort());
                            targetTranslationContext.put("name", exportFile.getName());
                            targetTranslationContext.put("size", exportFile.length());
                            Request reply = request.makeReply(targetTranslationContext);
                            sendRequest(client, reply);
                        }
                    } catch (Exception e) {
                        // export failed
                        Logger.e(this.getClass().getName(), "Failed to export the archive", e);
                    }
                } else {
                    // we don't have it
                }
                break;
            case TargetTranslationList:
                Logger.i(this.getClass().getName(), "received project list request from " + client.getIpAddress());
                // send the project list to the client
                // TODO: we shouldn't use the project manager here because this may be running in the background (eventually)

                // read preferred source languages (for better readability on the client)
////                List<Language> preferredLanguages = new ArrayList<>();
//                try {
////                    JSONArray preferredLanguagesJson = contextJson.getJSONArray("preferred_source_language_ids");
////                    for(int i = 0; i < preferredLanguagesJson.length(); i ++) {
////                        Language lang =  null;//App.projectManager().getLanguage(preferredLanguagesJson.getString(i));
////                        if(lang != null) {
////                            preferredLanguages.add(lang);
////                        }
////                    }
//                } catch (JSONException e) {
//                    Logger.e(this.getClass().getName(), "failed to parse preferred language list", e);
//                }

                // generate project library
                // TODO: identifying the projects that have changes could be expensive if there are lots of clients and lots of projects. We might want to cache this
                String library =  null;//Sharing.generateLibrary(App.projectManager().getProjectSlugs(), preferredLanguages);

                sendMessage(client, SocketMessages.MSG_PROJECT_LIST + ":" + library);
                break;
            default:
                Logger.i(this.getClass().getName(), "received invalid request from " + client.getIpAddress() + ": " + request.toString());
        }
    }

    /**
     * Offers a target translation to the peer
     * @param client
     * @param targetTranslationSlug
     */
    public void offerTargetTranslation(Peer client, String sourceLanguageSlug, String targetTranslationSlug) {
        Door43Client library = App.getLibrary();
        TargetTranslation targetTranslation = App.getTranslator().getTargetTranslation(targetTranslationSlug);
        if(targetTranslation != null) {
            try {
                Project p = App.getLibrary().index.getProject(sourceLanguageSlug, targetTranslation.getProjectId(), true);
                JSONObject json = new JSONObject();
                json.put("target_translation_id", targetTranslation.getId());
                json.put("package_version", TargetTranslation.PACKAGE_VERSION);
                json.put("project_name", p.name);
                json.put("target_language_name", targetTranslation.getTargetLanguageName());
                json.put("progress", 0); // we don't use this right now
                Request request = new Request(Request.Type.AlertTargetTranslation, json);
                sendRequest(client, request);
            } catch (Exception e) {
                if (listener != null) {
                    listener.onServerServiceError(e);
                }
            }
        } else {
            // invalid target translation
            if (listener != null) {
                listener.onServerServiceError(new Exception("Invalid target translation: " + targetTranslationSlug));
            }
        }
    }

    /**
     * Class to retrieve instance of service
     */
    public class LocalBinder extends Binder {
        public ServerService getServiceInstance() {
            return ServerService.this;
        }
    }

    /**
     * Interface for communication with service clients.
     */
    public interface OnServerEventListener {
        void onServerServiceReady(int port);
        void onClientConnected(Peer peer);
        void onClientLost(Peer peer);
        void onClientChanged(Peer peer);
        void onServerServiceError(Throwable e);
    }

    /**
     * Manage the server instance on it's own thread
     */
    private class ServerRunnable implements Runnable {

        public void run() {
            Socket socket;

            // set up sockets
            try {
                mServerSocket = new ServerSocket(0);
            } catch (Exception e) {
                if(listener != null) {
                    listener.onServerServiceError(e);
                }
                return;
            }
            mPort = mServerSocket.getLocalPort();

            if(listener != null) {
                listener.onServerServiceReady(mPort);
            }

            setRunning(true);

            // begin listening for connections
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    socket = mServerSocket.accept();
                    ClientRunnable clientRunnable = new ClientRunnable(socket);
                    new Thread(clientRunnable).start();
                } catch (Exception e) {
                    if(!Thread.currentThread().isInterrupted()) {
                        Logger.e(this.getClass().getName(), "failed to accept socket", e);
                    }
                }
            }
            try {
                mServerSocket.close();
            } catch (Exception e) {
                Logger.e(this.getClass().getName(), "failed to shutdown the server socket", e);
            }
        }
    }

    /**
     * Manages a single client connection on it's own thread
     */
    private class ClientRunnable implements Runnable {
        private Connection mConnection;
        private Peer mClient;

        public ClientRunnable(Socket clientSocket) {
            // set up socket
            try {
                mConnection = new Connection(clientSocket);
                mConnection.setOnCloseListener(new Connection.OnCloseListener() {
                    @Override
                    public void onClose() {
                        Thread.currentThread().interrupt();
                    }
                });
                // we store a reference to all connections so we can access them later
                mClientConnections.put(mConnection.getIpAddress(), mConnection);
            } catch (Exception e) {
                if(listener != null) {
                    listener.onServerServiceError(e);
                }
                Thread.currentThread().interrupt();
            }
            // create a new peer
            mClient = new Peer(clientSocket.getInetAddress().toString().replace("/", ""), clientSocket.getPort());
            if(addPeer(mClient)) {
                if(listener != null) {
                    listener.onClientConnected(mClient);
                }
            }
        }

        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                String message = mConnection.readLine();
                if (message == null ){
                    Thread.currentThread().interrupt();
                } else {
                    onMessageReceived(mClient, message);
                }
            }
            // close the connection
            mConnection.close();
            // remove all instances of the peer
            if(mClientConnections.containsKey(mConnection.getIpAddress())) {
                mClientConnections.remove(mConnection.getIpAddress());
            }
            removePeer(mClient);
            if(listener != null) {
                listener.onClientLost(mClient);
            }
        }
    }
}
