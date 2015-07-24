package com.door43.translationstudio.service;

import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.device2device.PeerStatusKeys;
import com.door43.translationstudio.device2device.SocketMessages;
import com.door43.translationstudio.network.Connection;
import com.door43.translationstudio.network.Peer;
import com.door43.translationstudio.projects.Language;
import com.door43.util.RSAEncryption;
import com.door43.util.StringUtilities;

import org.json.JSONArray;

import java.net.InetAddress;
import java.net.Socket;
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
public class ImportingService extends NetworkService {
    private static final String PARAM_PUBLIC_KEY = "param_public_key";
    private static final String PARAM_PRIVATE_KEY = "param_private_key";
    private final IBinder mBinder = new LocalBinder();
    private Callbacks mListener;
    private static boolean sRunning = false;
    private Map<String, Connection> mServerConnections = new HashMap<>();
    private PrivateKey mPrivateKey;
    private String mPublicKey;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void registerCallback(Callbacks callback) {
        mListener = callback;
        if(sRunning && mListener != null) {
            mListener.onImportServiceReady();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        Bundle args = intent.getExtras();
        if(args != null) {
            mPrivateKey = (PrivateKey)args.get(PARAM_PRIVATE_KEY);
            mPublicKey = args.getString(PARAM_PUBLIC_KEY);
            return START_STICKY;
        } else {
            Logger.e(this.getClass().getName(), "Import service requires arguments");
            stopService();
            return START_NOT_STICKY;
        }
    }

    /**
     * Establishes a TCP connection with the server.
     * Once this connection has been made the cleanup thread won't identify the server as lost unless the tcp connection is also disconnected.
     * @param server the server we will connect to
     */
    public void connectToServer(Peer server) {
        if(!mServerConnections.containsKey(server.getIpAddress())) {
            ServerThread serverThread = new ServerThread(server);
            new Thread(serverThread).start();
        }
    }

    /**
     * Stops the service
     */
    public void stopService() {
        Logger.i(this.getClass().getName(), "Stopping import service");
        sRunning = false;
    }

    /**
     * Checks if the service is running
     * @return
     */
    public static boolean isRunning() {
        return sRunning;
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
        if (mServerConnections.containsKey(server.getIpAddress())) {
            if(server.isConnected()) {
                // encrypt message
                PublicKey key = RSAEncryption.getPublicKeyFromString(server.keyStore.getString(PeerStatusKeys.PUBLIC_KEY));
                if(key != null) {
                    message = encryptMessage(key, message);
                } else {
                    Logger.w(this.getClass().getName(), "Missing the server's public key");
                    message = SocketMessages.MSG_EXCEPTION;
                }
            }
            mServerConnections.get(server.getIpAddress()).write(message);
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

    /**
     * Handles the initial handshake and authorization
     * @param server
     * @param message
     */
    private void onMessageReceived(Peer server, String message) {
        if(server.isConnected()) {
            message = decryptMessage(mPrivateKey, message);
            if(message != null) {
                String[] data = StringUtilities.chunk(message, ":");
                Logger.i(this.getClass().getName(), "message from " + server.getIpAddress() + ": " + message);
                onCommandReceived(server, data[0], Arrays.copyOfRange(data, 1, data.length-1));
            } else if(mListener != null) {
                mListener.onImportServiceError(new Exception("Message descryption failed"));
            }
        } else {
            String[] data = StringUtilities.chunk(message, ":");
            switch(data[0]) {
                case SocketMessages.MSG_PUBLIC_KEY:
                    // receive the server's public key
                    server.keyStore.add(PeerStatusKeys.PUBLIC_KEY, data[1]);
                    server.keyStore.add(PeerStatusKeys.WAITING, false);
                    server.keyStore.add(PeerStatusKeys.CONTROL_TEXT, getResources().getString(R.string.import_project));
                    server.setIsConnected(true);
                    if(mListener != null) {
                        mListener.onServerConnectionChanged(server);
                    }
                    break;
                case SocketMessages.MSG_OK:
                    // we are authorized to access the server
                    // send public key to server
                    sendMessage(server, SocketMessages.MSG_PUBLIC_KEY + ":" + mPublicKey);
                    break;
                default:
                    Logger.w(this.getClass().getName(), "Invalid request: " + message);
                    sendMessage(server, SocketMessages.MSG_INVALID_REQUEST);
            }
        }
    }

    /**
     * Handles commands sent from the server
     * @param server
     * @param command
     * @param data
     */
    private void onCommandReceived(Peer server, String command, String[] data) {
        switch(command) {
            case SocketMessages.MSG_PROJECT_ARCHIVE:
                break;
            case SocketMessages.MSG_PROJECT_LIST:
                break;
            default:
                sendMessage(server, SocketMessages.MSG_INVALID_REQUEST);
        }
    }

    /**
     * Interface for communication with service clients.
     */
    public interface Callbacks {
        void onImportServiceReady();
        void onServerConnectionLost(Peer peer);
        void onServerConnectionChanged(Peer peer);
        void onImportServiceError(Throwable e);
    }
    /**
     * Class to retrieve instance of service
     */
    public class LocalBinder extends Binder {
        public ImportingService getServiceInstance() {
            return ImportingService.this;
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
                mServerConnections.put(mConnection.getIpAddress(), mConnection);
            } catch (Exception e) {
                mListener.onImportServiceError(e);
                Thread.currentThread().interrupt();
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
            if(mServerConnections.containsKey(mConnection.getIpAddress())) {
                mServerConnections.remove(mConnection.getIpAddress());
            }
            removePeer(mServer);
            mListener.onServerConnectionLost(mServer);
        }
    }
}
