package com.door43.translationstudio.service;

import android.app.Activity;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.device2device.PeerStatusKeys;
import com.door43.translationstudio.device2device.SocketMessages;
import com.door43.translationstudio.network.Connection;
import com.door43.translationstudio.network.Peer;
import com.door43.util.StringUtilities;

import java.net.ServerSocket;
import java.net.Socket;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;

import static com.tozny.crypto.android.AesCbcWithIntegrity.decryptString;
import static com.tozny.crypto.android.AesCbcWithIntegrity.encrypt;

/**
 * Created by joel on 7/23/2015.
 */
public class ExportingService extends NetworkService {
    public static final String PARAM_PRIVATE_KEY = "param_private_key";
    public static final String PARAM_PUBLIC_KEY = "param_public_key";
    private final IBinder mBinder = new LocalBinder();
    private Callbacks mActivity;
    private static boolean sRunning = false;
    private static int mPort = 0;
    private static Thread mServerThread;
    private static Map<String, Connection> mClientConnections = new HashMap<>();
    private PrivateKey mPrivateKey;
    private String mPublicKey;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void registerCallback(Activity activity) {
        mActivity = (Callbacks) activity;
        if(sRunning && mActivity != null) {
            mActivity.onServiceReady(mPort);
        }
    }

    public int onStartCommand(Intent intent, int flags, int startid) {
        Bundle args = intent.getExtras();
        if(args != null) {
            mPrivateKey = (PrivateKey)args.get(PARAM_PRIVATE_KEY);
            mPublicKey = args.getString(PARAM_PUBLIC_KEY);
            mServerThread = new Thread(new ServerRunnable());
            mServerThread.start();
            return START_STICKY;
        } else {
            Logger.e(this.getClass().getName(), "Sharing service requires arguments");
            stopService();
            return START_NOT_STICKY;
        }
    }

    /**
     * Stops the service
     */
    public void stopService() {
        Logger.i(this.getClass().getName(), "Stopping sharing service");
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
            mClientConnections.get(client.getIpAddress()).write(message);
        }
    }

    public void acceptConnection(Peer peer) {
        peer.setIsAuthorized(true);
        // let the client know it's connection has been authorized.
        sendMessage(peer, "ok");
    }


    private void onMessageReceived(Peer client, String message) {
        if(client.isAuthorized()) {
            if(client.isConnected()) {
                message = decryptMessage(mPrivateKey, message);
                String[] data = StringUtilities.chunk(message, ":");
                // TODO: handle commands
                Logger.i(this.getClass().getName(), message);
            } else {
                String[] data = StringUtilities.chunk(message, ":");
                // authorized but has not finished connecting
                if(data[0].equals(SocketMessages.MSG_PUBLIC_KEY)) {
                    // receive the client's public key
                    client.keyStore.add(PeerStatusKeys.PUBLIC_KEY, data[1]);

                    // send the client our public key
                    sendMessage(client, SocketMessages.MSG_PUBLIC_KEY + ":" + mPublicKey);
                    client.setIsConnected(true);

                    // reload the list
                    if(mActivity != null) {
                        mActivity.onConnectionChanged(client);
                    }
                } else {
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
        void onServiceReady(int port);
        void onConnectionRequest(Peer peer);
        void onConnectionLost(Peer peer);
        void onConnectionChanged(Peer peer);
        void onServiceError(Throwable e);
    }

    /**
     * Manage the server instance on it's own thread
     */
    private class ServerRunnable implements Runnable {

        public void run() {
            Socket socket;
            ServerSocket serverSocket;

            // set up sockets
            try {
                serverSocket = new ServerSocket(0);
            } catch (Exception e) {
                if(mActivity != null) {
                    mActivity.onServiceError(e);
                }
                return;
            }
            mPort = serverSocket.getLocalPort();

            if(mActivity != null) {
                mActivity.onServiceReady(mPort);
            }

            sRunning = true;

            // begin listening for connections
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    socket = serverSocket.accept();
                    ClientRunnable clientRunnable = new ClientRunnable(socket);
                    new Thread(clientRunnable).start();
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "failed to accept socket", e);
                }
            }
            try {
                serverSocket.close();
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
                if(mActivity != null) {
                    mActivity.onConnectionRequest(mClient);
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
                if(mActivity != null) {
                    mActivity.onServiceError(e);
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
            if(mActivity != null) {
                mActivity.onConnectionLost(mClient);
            }
        }
    }
}
