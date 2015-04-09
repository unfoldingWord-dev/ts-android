package com.door43.translationstudio.tasks;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;

import com.door43.translationstudio.network.BroadcastRunnable;
import com.door43.translationstudio.network.Connection;
import com.door43.translationstudio.network.Peer;
import com.door43.util.Logger;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by joel on 4/8/2015.
 */
public class ServerTask extends NetworkServiceTask {
    private final int clientUDPPort; // the port on which the client listens for broadcast messages
    private final PackageInfo pInfo;
    private final String packageName;
    private final InetAddress broadcastAddress;
    private OnServerEventListener mListener;
//    private boolean mIsRunning = false;
    private int mBroadcastFrequency = 2000;
    private Thread mServerThread = null;
    private Thread mBroadcastThread = null;
    private Map<String, Connection> mClientConnections = new HashMap<String, Connection>();
//    private Handler mHandler;
    private final String mServiceName;

    public ServerTask(Context context, String serviceName, int clientUDPPort) {
        mServiceName = serviceName;
        this.clientUDPPort = clientUDPPort;

        // TRICKY: we use the context right away rather than keeping it around so activity deconstruction doesn't break things

        // collect the package info
        PackageInfo pi = null;
        packageName = context.getPackageName();
        try {
            pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        pInfo = pi;

        // get broadcast address
        InetAddress ba = null;
        try {
            ba = getBroadcastAddress(context);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        broadcastAddress = ba;
    }

    @Override
    public void start() {
        onBeforeStart();

        Socket socket;
        ServerSocket serverSocket;

        // set up sockets
        try {
            serverSocket = new ServerSocket(0);
        } catch (Exception e) {
            onError(e);
            return;
        }
        int serverTCPPort = serverSocket.getLocalPort();

        if(broadcastAddress == null) {
            onError(new UnknownHostException());
            return;
        }

        if(pInfo == null) {
            onError(new PackageManager.NameNotFoundException(packageName));
            return;
        }

        // we send the client the app:version:port. e.g. tS:47:5653
        mBroadcastThread = new Thread(new BroadcastRunnable(mServiceName + ":" + pInfo.versionCode + ":" + serverTCPPort, broadcastAddress, clientUDPPort, mBroadcastFrequency, new BroadcastRunnable.OnBroadcastEventListener() {
            @Override
            public void onError(Exception e) {
                mServerThread.interrupt();
            }
        }));
        mBroadcastThread.start();

        // begin listening for connections
        while (!Thread.currentThread().isInterrupted()) {
            try {
                socket = serverSocket.accept();
                ClientRunnable clientThread = new ClientRunnable(socket);
                new Thread(clientThread).start();
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

    private void onError(Exception e) {
        if(mListener != null) {
            mListener.onError(e);
        }
    }

    private void onBeforeStart() {
        if(mListener != null) {
            mListener.onBeforeStart();
        }
    }

    /**
     * Perform cleanup operations
     */
    public void onStop() {

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
                onFoundClient(mClient);
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
                onError(e);
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
            onLostClient(mClient);
        }
    }

    private void onLostClient(Peer mClient) {
        if(mListener != null) {
            mListener.onLostClient(mClient);
        }
    }

    private void onMessageReceived(Peer mClient, String message) {
        if(mListener != null) {
            mListener.onMessageReceived(mClient, message);
        }
    }

    private void onFoundClient(Peer mClient) {
        if(mListener != null) {
            mListener.onFoundClient(mClient);
        }
    }

    public interface OnServerEventListener {
        void onBeforeStart();
        void onError(Exception e);
        void onFoundClient(Peer client);
        void onLostClient(Peer client);
        void onMessageReceived(Peer client, String message);

        /**
         * Allows you to perform global operations on a message(such as encryption) based on the peer
         * @param client
         * @param message
         * @return
         */
        String onWriteMessage(Peer client, String message);
    }
}
