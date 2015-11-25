package com.door43.translationstudio.service;

import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.device2device.SocketMessages;
import com.door43.translationstudio.network.Connection;
import com.door43.translationstudio.network.Peer;
import com.door43.util.RSAEncryption;
import com.door43.util.StringUtilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class provides an importing service (effectively a client) that can
 * communicate with an exporting service (server) to browse and retrieve translations
 */
public class ClientService extends NetworkService {
    private static final String PARAM_PUBLIC_KEY = "param_public_key";
    private static final String PARAM_PRIVATE_KEY = "param_private_key";
    private final IBinder binder = new LocalBinder();
    private OnClientEventListener listener;
    private Map<String, Connection> serverConnections = new HashMap<>();
    private PrivateKey privateKey;
    private String publicKey;
    private static Boolean isRunning = false;

    /**
     * Sets whether or not the service is running
     * @param running
     */
    protected void setRunning(Boolean running) {
        isRunning = running;
    }

    /**
     * Checks if the service is currently running
     * @return
     */
    public static boolean isRunning() {
        return isRunning;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setOnClientEventListener(OnClientEventListener callback) {
        listener = callback;
        if(isRunning() && listener != null) {
            listener.onClientServiceReady();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        if(intent != null) {
            Bundle args = intent.getExtras();
            if (args != null && args.containsKey(PARAM_PRIVATE_KEY) && args.containsKey(PARAM_PUBLIC_KEY)) {
                privateKey = (PrivateKey) args.get(PARAM_PRIVATE_KEY);
                publicKey = args.getString(PARAM_PUBLIC_KEY);
                if (listener != null) {
                    listener.onClientServiceReady();
                }
                setRunning(true);
                return START_STICKY;
            }
        }
        Logger.e(this.getClass().getName(), "Import service requires arguments");
        stopService();
        return START_NOT_STICKY;
    }

    /**
     * Establishes a TCP connection with the server.
     * Once this connection has been made the cleanup thread won't identify the server as lost unless the tcp connection is also disconnected.
     * @param server the server we will connect to
     */
    public void connectToServer(Peer server) {
        if(!serverConnections.containsKey(server.getIpAddress())) {
            ServerThread serverThread = new ServerThread(server);
            new Thread(serverThread).start();
        }
    }

    /**
     * Stops the service
     */
    public void stopService() {
        Logger.i(this.getClass().getName(), "Stopping client service");
        // close sockets
        for(String key: serverConnections.keySet()) {
            serverConnections.get(key).close();
        }
        setRunning(false);
    }

    @Override
    public void onDestroy() {
        stopService();
    }

    /**
     * Sends a message to the peer
     * @param server the client to which the message will be sent
     * @param message the message being sent to the client
     */
    private void sendMessage(Peer server, String message) {
        if (serverConnections.containsKey(server.getIpAddress())) {
            if(server.isSecure()) {
                // encrypt message
                PublicKey key = RSAEncryption.getPublicKeyFromString(server.keyStore.getString(PeerStatusKeys.PUBLIC_KEY));
                if(key != null) {
                    message = encryptMessage(key, message);
                } else {
                    Logger.w(this.getClass().getName(), "Missing the server's public key");
                    message = SocketMessages.MSG_EXCEPTION;
                }
            }
            serverConnections.get(server.getIpAddress()).write(message);
        }
    }

    /**
     * Requests a list of projects from the server
     * @param server the server that will give the project list
     * @param preferredLanguages the languages preferred by the client
     */
    public void requestProjectList(Peer server, List<String> preferredLanguages) {
        JSONArray languagesJson = new JSONArray();
        for(String l:preferredLanguages) {
            languagesJson.put(l);
        }
        sendMessage(server, SocketMessages.MSG_PROJECT_LIST + ":" + languagesJson);
    }

    public void requestProjectArchive(Peer server, JSONObject languagesJson) {
        sendMessage(server, SocketMessages.MSG_PROJECT_ARCHIVE + ":" + languagesJson);
    }

    public void requestTargetTranslation(Peer server, String targetTranslationSlug) {
        sendMessage(server, PeerCommand.TargetTranslation + ":" + targetTranslationSlug);
    }

    /**
     * Handles the initial handshake and authorization
     * @param server
     * @param message
     */
    private void onMessageReceived(Peer server, String message) {
        if(server.isSecure() && server.hasIdentity()) {
            message = decryptMessage(privateKey, message);
            if(message != null) {
                String[] data = StringUtilities.chunk(message, ":");
                onCommandReceived(server, PeerCommand.get(data[0]), Arrays.copyOfRange(data, 1, data.length));
            } else if(listener != null) {
                listener.onClientServiceError(new Exception("Message descryption failed"));
            }
        } else if(!server.isSecure()){
            // receive the key
            try {
                JSONObject json = new JSONObject(message);
                server.keyStore.add(PeerStatusKeys.PUBLIC_KEY, json.getString("key"));
                server.setIsSecure(true);
            } catch (JSONException e) {
                Logger.w(this.getClass().getName(), "Invalid request: " + message, e);
//                sendMessage(server, SocketMessages.MSG_INVALID_REQUEST);
            }

            // send public key
            try {
                JSONObject json = new JSONObject();
                json.put("key", publicKey);
                // TRICKY: manually write to server so we don't encrypt it
                if(serverConnections.containsKey(server.getIpAddress())) {
                    serverConnections.get(server.getIpAddress()).write(json.toString());
                }
            } catch (JSONException e) {
                if(listener != null) {
                    listener.onClientServiceError(e);
                }
            }

            // send identity
            if(server.isSecure()) {
                try {
                    JSONObject json = new JSONObject();
                    json.put("name", "Jon Doe Client");
                    if(AppContext.isTablet()) {
                        json.put("device", "tablet");
                    } else {
                        json.put("device", "phone");
                    }
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    md.update(AppContext.udid().getBytes("UTF-8"));
                    byte[] digest = md.digest();
                    BigInteger bigInt = new BigInteger(1, digest);
                    String hash = bigInt.toString();
                    json.put("id", hash);
                    sendMessage(server, json.toString());
                } catch (Exception e){
                    Logger.w(this.getClass().getName(), "Failed to prepare response ", e);
                    if(listener != null) {
                        listener.onClientServiceError(e);
                    }
                }
            }
        } else if(!server.hasIdentity()) {
            // receive identity
            message = decryptMessage(privateKey, message);
            try {
                JSONObject json = new JSONObject(message);
                server.setName(json.getString("name"));
                server.setDevice(json.getString("device"));
                server.setId(json.getString("id"));
                server.setHasIdentity(true);
                if(listener != null) {
                    listener.onServerConnectionChanged(server);
                }
            } catch (JSONException e) {
                Logger.w(this.getClass().getName(), "Invalid request: " + message, e);
            }
        }
    }

    /**
     * Handles commands sent from the server
     * @param server
     * @param command
     * @param data
     */
    private void onCommandReceived(final Peer server, PeerCommand command, String[] data) {
        switch(command) {
            case TargetTranslation:
                Logger.i(this.getClass().getName(), "Received target translation archive from " + server.getIpAddress());

                // receive project archive from server
                JSONObject importJson;
                try {
                    importJson = new JSONObject(data[0]);
                } catch (JSONException e) {
                    if(listener != null) {
                        listener.onClientServiceError(e);
                    }
                    break;
                }

                if(importJson.has("port") && importJson.has("size") && importJson.has("name")) {
                    int port;
                    final long size;
                    final String name;
                    try {
                        port = importJson.getInt("port");
                        size = importJson.getLong("size");
                        name = importJson.getString("name");
                    } catch (JSONException e) {
                        if(listener != null) {
                            listener.onClientServiceError(e);
                        }
                        break;
                    }
                    // the server is sending a project archive
                    openReadSocket(server, port, new OnSocketEventListener() {
                        @Override
                        public void onOpen(Connection connection) {
                            connection.setOnCloseListener(new Connection.OnCloseListener() {
                                @Override
                                public void onClose() {
                                    if (listener != null) {
                                        listener.onClientServiceError(new Exception("Socket was closed before download completed"));
                                    }
                                }
                            });

                            File file = null;
                            try {
                                file = File.createTempFile("p2p", name);
                                // download archive
                                DataInputStream in = new DataInputStream(connection.getSocket().getInputStream());
                                file.getParentFile().mkdirs();
                                file.createNewFile();
                                OutputStream out = new FileOutputStream(file.getAbsolutePath());
                                byte[] buffer = new byte[8 * 1024];
                                int totalCount = 0;
                                int count;
                                while ((count = in.read(buffer)) > 0) {
                                    totalCount += count;
                                    server.keyStore.add(PeerStatusKeys.PROGRESS, totalCount / ((int) size) * 100);
                                    if (listener != null) {
                                        listener.onServerConnectionChanged(server);
                                    }
                                    out.write(buffer, 0, count);
                                }
                                server.keyStore.add(PeerStatusKeys.PROGRESS, 0);
                                if (listener != null) {
                                    listener.onServerConnectionChanged(server);
                                }
                                out.close();
                                in.close();

                                // import the target translation
                                Translator translator = AppContext.getTranslator();
                                // TODO: 11/23/2015 perform a diff first
                                try {
                                    String[] targetTranslationSlugs = translator.importArchive(file);
                                    if(listener != null) {
                                        listener.onReceivedTargetTranslations(server, targetTranslationSlugs);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                file.delete();
                            } catch (IOException e) {
                                Logger.e(this.getClass().getName(), "Failed to download the file", e);
                                if(file != null) {
                                    file.delete();
                                }
                                if (listener != null) {
                                    listener.onClientServiceError(e);
                                }
                            }
                        }
                    });
                } else {
                    Logger.w(this.getClass().getName(), "Invalid response from server: " + data.toString());
                    if(listener != null) {
                        listener.onClientServiceError(new Exception("Invalid response from server"));
                    }
                }
                break;
            case InvalidRequest:
                // TODO: do something about this.
                if(listener != null) {
                    listener.onClientServiceError(new Throwable("Invalid request"));
                }
                break;
            default:
                Logger.i(this.getClass().getName(), "received invalid request from " + server.getIpAddress() + ": " + command);
                sendMessage(server, SocketMessages.MSG_INVALID_REQUEST);
        }
    }

    /**
     * Interface for communication with service clients.
     */
    public interface OnClientEventListener {
        void onClientServiceReady();
        void onServerConnectionLost(Peer peer);
        void onServerConnectionChanged(Peer peer);
        void onClientServiceError(Throwable e);
//        void onReceivedProjectList(Peer server, Model[] models);
//        void onReceivedProject(Peer server, ProjectImport[] importStatuses);
        void onReceivedTargetTranslations(Peer server, String[] targetTranslations);
    }

    /**
     * Class to retrieve instance of service
     */
    public class LocalBinder extends Binder {
        public ClientService getServiceInstance() {
            return ClientService.this;
        }
    }

    /**
     * Manages a single server connection on it's own thread
     */
    private class ServerThread implements Runnable {
        private Connection mConnection;
        private Peer mServer;

        public ServerThread(Peer server) {
            mServer = server;
        }

        @Override
        public void run() {
            // set up sockets
            try {
                InetAddress serverAddr = InetAddress.getByName(mServer.getIpAddress());
                mConnection = new Connection(new Socket(serverAddr, mServer.getPort()));
                mConnection.setOnCloseListener(new Connection.OnCloseListener() {
                    @Override
                    public void onClose() {
                        Thread.currentThread().interrupt();
                    }
                });
                // we store references to all connections so we can access them later
                if(!serverConnections.containsKey(mConnection.getIpAddress())) {
                    serverConnections.put(mConnection.getIpAddress(), mConnection);
                } else {
                    // we already have a connection to this server
                    mConnection.close();
                    return;
                }
            } catch (Exception e) {
                // the connection could not be established
                if(mConnection != null) {
                    mConnection.close();
                }
                if(listener != null) {
                    listener.onClientServiceError(e);
                }
                return;
            }

            // begin listening to server
            while (!Thread.currentThread().isInterrupted()) {
                String message = mConnection.readLine();
                if(message == null) {
                    Thread.currentThread().interrupt();
                } else {
                    onMessageReceived(mServer, message);
                }
            }
            // close the connection
            mConnection.close();
            // remove all instances of the peer
            if(serverConnections.containsKey(mConnection.getIpAddress())) {
                serverConnections.remove(mConnection.getIpAddress());
            }
            removePeer(mServer);
            if(listener != null) {
                listener.onServerConnectionLost(mServer);
            }
        }
    }
}
