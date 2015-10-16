package com.door43.translationstudio.service;

import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.device2device.PeerStatusKeys;
import com.door43.translationstudio.device2device.SocketMessages;
import com.door43.translationstudio.network.Connection;
import com.door43.translationstudio.network.Peer;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Sharing;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.AppContext;
import com.door43.util.RSAEncryption;
import com.door43.util.StringUtilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class provides an exporting service (effectively a server) from which
 * other devices may browse and retreive translations
 */
public class ExportingService extends NetworkService {
    public static final String PARAM_PRIVATE_KEY = "param_private_key";
    public static final String PARAM_PUBLIC_KEY = "param_public_key";
    private static Boolean mIsRunning = false;
    private final IBinder mBinder = new LocalBinder();
    private Callbacks mListener;
    private int mPort = 0;
    private Thread mServerThread;
    private Map<String, Connection> mClientConnections = new HashMap<>();
    private PrivateKey mPrivateKey;
    private String mPublicKey;
    private ServerSocket mServerSocket;

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

    public void registerCallback(Callbacks callback) {
        mListener = callback;
        if(isRunning() && mListener != null) {
            mListener.onExportServiceReady(mPort);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        if(intent != null) {
            Bundle args = intent.getExtras();
            if (args != null && args.containsKey(PARAM_PRIVATE_KEY) && args.containsKey(PARAM_PUBLIC_KEY)) {
                mPrivateKey = (PrivateKey) args.get(PARAM_PRIVATE_KEY);
                mPublicKey = args.getString(PARAM_PUBLIC_KEY);
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
            if(client.isConnected()) {
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

    public void acceptConnection(Peer peer) {
        peer.setIsAuthorized(true);
        // let the client know it's connection has been authorized.
        sendMessage(peer, SocketMessages.MSG_OK);
    }

    /**
     * Handles the initial handshake and authorization
     * @param client
     * @param message
     */
    private void onMessageReceived(Peer client, String message) {
        if(client.isAuthorized()) {
            if(client.isConnected()) {
                message = decryptMessage(mPrivateKey, message);
                if(message != null) {
                    String[] data = StringUtilities.chunk(message, ":");
                    onCommandReceived(client, data[0], Arrays.copyOfRange(data, 1, data.length));
                } else if(mListener != null) {
                    mListener.onExportServiceError(new Exception("Message descryption failed"));
                }
            } else {
                String[] data = StringUtilities.chunk(message, ":");
                switch(data[0]) {
                    case SocketMessages.MSG_PUBLIC_KEY:
                        Logger.i(this.getClass().getName(), "connected to client "+client.getIpAddress());
                        // receive the client's public key
                        client.keyStore.add(PeerStatusKeys.PUBLIC_KEY, data[1]);

                        // send the client our public key
                        sendMessage(client, SocketMessages.MSG_PUBLIC_KEY + ":" + mPublicKey);
                        client.setIsConnected(true);

                        // reload the list
                        if(mListener != null) {
                            mListener.onClientConnectionChanged(client);
                        }
                        break;
                    default:
                        Logger.w(this.getClass().getName(), "Invalid request: " + message);
                        sendMessage(client, SocketMessages.MSG_INVALID_REQUEST);
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
     * @param command
     * @param data
     */
    private void onCommandReceived(Peer client, String command, String[] data) {
        switch(command) {
            case SocketMessages.MSG_PROJECT_LIST:
                Logger.i(this.getClass().getName(), "received project list request from " + client.getIpAddress());
                // send the project list to the client
                // TODO: we shouldn't use the project manager here because this may be running in the background (eventually)

                // read preferred source languages (for better readability on the client)
                List<Language> preferredLanguages = new ArrayList<>();
                try {
                    JSONArray preferredLanguagesJson = new JSONArray(data[0]);
                    for(int i = 0; i < preferredLanguagesJson.length(); i ++) {
                        Language lang =  null;//AppContext.projectManager().getLanguage(preferredLanguagesJson.getString(i));
                        if(lang != null) {
                            preferredLanguages.add(lang);
                        }
                    }
                } catch (JSONException e) {
                    Logger.e(this.getClass().getName(), "failed to parse preferred language list", e);
                }

                // generate project library
                // TODO: identifying the projects that have changes could be expensive if there are lots of clients and lots of projects. We might want to cache this
                String library =  null;//Sharing.generateLibrary(AppContext.projectManager().getProjectSlugs(), preferredLanguages);

                sendMessage(client, SocketMessages.MSG_PROJECT_LIST + ":" + library);
                break;
            case SocketMessages.MSG_PROJECT_ARCHIVE:
                Logger.i(this.getClass().getName(), "received project archive request from " + client.getIpAddress());
                // TODO: we shouldn't use the project manager here because this may be running in the background (eventually)
                // send the project archive to the client
                JSONObject json;
                try {
                    json = new JSONObject(data[0]);
                } catch (final JSONException e) {
                    Logger.e(this.getClass().getName(), "failed to parse project archive response", e);
                    sendMessage(client, SocketMessages.MSG_INVALID_REQUEST);
                    break;
                }

                // load data
                if(json.has("id") && json.has("target_languages")) {
                    try {
                        String projectId = json.getString("id");

                        final Project p =  null;//AppContext.projectManager().getProject(projectSlug);
                        // validate project
                        if(p != null) {
                            // validate requested source languages
                            List<SourceLanguage> requestedSourceLanguages = new ArrayList<>();
                            if(json.has("source_languages")) {
                                JSONArray sourceLanguagesJson = json.getJSONArray("source_languages");
                                for(int i = 0; i < sourceLanguagesJson.length(); i ++) {
                                    String languageId = sourceLanguagesJson.getString(i);
                                    SourceLanguage s = p.getSourceLanguage(languageId);
                                    if(s != null) {
                                        requestedSourceLanguages.add(s);
                                    }
                                }
                            }

                            // validate requested target languages
                            Language[] activeLanguages = p.getActiveTargetLanguages();
                            JSONArray targetLanguagesJson = json.getJSONArray("target_languages");
                            List<Language> requestedTranslations = new ArrayList<>();
                            for (int i = 0; i < targetLanguagesJson.length(); i++) {
                                String languageId = (String) targetLanguagesJson.get(i);
                                for(Language l:activeLanguages) {
                                    if(l.getId().equals(languageId)) {
                                        requestedTranslations.add(l);
                                        break;
                                    }
                                }
                            }
                            if(requestedTranslations.size() > 0) {
                                String path = Sharing.export(p, requestedSourceLanguages.toArray(new SourceLanguage[requestedSourceLanguages.size()]), requestedTranslations.toArray(new Language[requestedTranslations.size()]));
                                final File archive = new File(path);
                                if(archive.exists()) {
                                    // open a socket to send the project
                                    ServerSocket fileSocket = openWriteSocket(new OnSocketEventListener() {
                                        @Override
                                        public void onOpen(Connection connection) {
                                            // send an archive of the current project to the connection
                                            try {
                                                // send the file to the connection
                                                // TODO: display a progress bar when the files are being transferred (on each client list item)
                                                DataOutputStream out = new DataOutputStream(connection.getSocket().getOutputStream());
                                                DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(archive)));
                                                byte[] buffer = new byte[8 * 1024];
                                                int count;
                                                while ((count = in.read(buffer)) > 0) {
                                                    out.write(buffer, 0, count);
                                                }
                                                out.close();
                                                in.close();
                                            } catch (final IOException e) {
                                                Logger.e(ExportingService.class.getName(), "Failed to send project archive to client", e);
                                            }
                                        }
                                    });
                                    // send details to the client so they can download
                                    JSONObject infoJson = new JSONObject();
                                    infoJson.put("port", fileSocket.getLocalPort());
                                    infoJson.put("name", archive.getName());
                                    infoJson.put("size", archive.length());
                                    sendMessage(client, SocketMessages.MSG_PROJECT_ARCHIVE + ":" + infoJson.toString());
                                } else {
                                    // the archive could not be created
                                    sendMessage(client, SocketMessages.MSG_SERVER_ERROR);
                                }
                            } else {
                                // the client should have known better
                                sendMessage(client, SocketMessages.MSG_INVALID_REQUEST);
                            }
                        } else {
                            // the client should have known better
                            sendMessage(client, SocketMessages.MSG_INVALID_REQUEST);
                        }
                    } catch (JSONException e) {
                        Logger.e(this.getClass().getName(), "malformed or corrupt project archive response", e);
                        sendMessage(client, SocketMessages.MSG_INVALID_REQUEST);
                    } catch (IOException e) {
                        Logger.e(this.getClass().getName(), "unable to read project archive response", e);
                        sendMessage(client, SocketMessages.MSG_SERVER_ERROR);
                    }
                } else {
                    sendMessage(client, SocketMessages.MSG_INVALID_REQUEST);
                }
                break;
            case SocketMessages.MSG_INVALID_REQUEST:
                // TODO: do something about this.
                if(mListener != null) {
                    mListener.onExportServiceError(new Throwable("Invalid request"));
                }
                break;
            default:
                Logger.i(this.getClass().getName(), "received invalid request from " + client.getIpAddress() + ": " + command);
                sendMessage(client, SocketMessages.MSG_INVALID_REQUEST);
        }
    }

    /**
     * Class to retrieve instance of service
     */
    public class LocalBinder extends Binder {
        public ExportingService getServiceInstance() {
            return ExportingService.this;
        }
    }

    /**
     * Interface for communication with service clients.
     */
    public interface Callbacks {
        void onExportServiceReady(int port);
        void onClientConnectionRequest(Peer peer);
        void onClientConnectionLost(Peer peer);
        void onClientConnectionChanged(Peer peer);
        void onExportServiceError(Throwable e);
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
                if(mListener != null) {
                    mListener.onExportServiceError(e);
                }
                return;
            }
            mPort = mServerSocket.getLocalPort();

            if(mListener != null) {
                mListener.onExportServiceReady(mPort);
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
            // create a new peer
            mClient = new Peer(clientSocket.getInetAddress().toString().replace("/", ""), clientSocket.getPort());
            if(addPeer(mClient)) {
                if(mListener != null) {
                    mListener.onClientConnectionRequest(mClient);
                }
            }
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
                if(mListener != null) {
                    mListener.onExportServiceError(e);
                }
                Thread.currentThread().interrupt();
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
            if(mListener != null) {
                mListener.onClientConnectionLost(mClient);
            }
        }
    }
}
