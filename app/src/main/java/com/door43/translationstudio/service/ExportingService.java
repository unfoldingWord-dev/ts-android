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
import com.door43.util.RSAEncryption;
import com.door43.util.StringUtilities;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * This class provides an exporting service (effectively a server) from which
 * other devices may browse and retreive translations
 */
public class ExportingService extends NetworkService {
    public static final String PARAM_PRIVATE_KEY = "param_private_key";
    public static final String PARAM_PUBLIC_KEY = "param_public_key";
    private final IBinder mBinder = new LocalBinder();
    private Callbacks mListener;
    private static boolean sRunning = false;
    private static int mPort = 0;
    private static Thread mServerThread;
    private static Map<String, Connection> mClientConnections = new HashMap<>();
    private PrivateKey mPrivateKey;
    private String mPublicKey;
    private ServerSocket mServerSocket;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void registerCallback(Callbacks callback) {
        mListener = callback;
        if(sRunning && mListener != null) {
            mListener.onExportServiceReady(mPort);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        Bundle args = intent.getExtras();
        if(args != null) {
            mPrivateKey = (PrivateKey)args.get(PARAM_PRIVATE_KEY);
            mPublicKey = args.getString(PARAM_PUBLIC_KEY);
            mServerThread = new Thread(new ServerRunnable());
            mServerThread.start();
            return START_STICKY;
        } else {
            Logger.e(this.getClass().getName(), "Export service requires arguments");
            stopService();
            return START_NOT_STICKY;
        }
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
        sRunning = false;
    }

    /**
     * Checks if the service is running
     * @return
     */
    public static boolean isRunning() {
        return sRunning;
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
                    Logger.i(this.getClass().getName(), "message from " + client.getIpAddress() + ": " + message);
                    onCommandReceived(client, data[0], Arrays.copyOfRange(data, 1, data.length - 1));
                } else if(mListener != null) {
                    mListener.onExportServiceError(new Exception("Message descryption failed"));
                }
            } else {
                String[] data = StringUtilities.chunk(message, ":");
                switch(data[0]) {
                    case SocketMessages.MSG_PUBLIC_KEY:
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
                break;
            case SocketMessages.MSG_PROJECT_ARCHIVE:

            default:
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

            sRunning = true;

            // begin listening for connections
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    socket = mServerSocket.accept();
                    ClientRunnable clientRunnable = new ClientRunnable(socket);
                    new Thread(clientRunnable).start();
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "failed to accept socket", e);
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
