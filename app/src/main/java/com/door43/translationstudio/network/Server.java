package com.door43.translationstudio.network;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;

import com.door43.util.reporting.Logger;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * This class operats are a server on the local network.
 * The server will broadcast it's services to a UDP port.
 * Clients will then be able to connect to a tcp port as detailed in the UDP message.
 */
public class Server extends Service {
    private final int mClientUDPPort; // the port on which the client listens for broadcast messages
    private final OnServerEventListener mListener;
    private boolean mIsRunning = false;
    private int mBroadcastFrequency = 2000;
    private Thread mServerThread = null;
    private Thread mBroadcastThread = null;
    private Map<String, Connection> mClientConnections = new HashMap<String, Connection>();
    private Handler mHandler;

    public Server(Context context, int clientUDPPort, OnServerEventListener listener) {
        super(context);
        mClientUDPPort = clientUDPPort;
        mListener = listener;
    }

    @Override
    /**
     * This will cause the server to begin listening on a port for connections and advertise it's services to the network
     */
    public void start(String serviceName, Handler handle) {
        if(mIsRunning) return;
        mIsRunning = true;

        mHandler = handle;
        mServerThread = new Thread(new ServerRunnable(serviceName));
        mServerThread.start();
    }

    @Override
    /**
     * Stops the server from advertising it's services and listening for responses.
     */
    public void stop() {
        mIsRunning = false;
        if(mBroadcastThread != null) {
            mBroadcastThread.interrupt();
        }
        // TODO: we may need to manually close the server socket.
        if(mServerThread != null) {
            mServerThread.interrupt();
        }
        Connection[] clients = mClientConnections.values().toArray(new Connection[mClientConnections.size()]);
        for(Connection c:clients) {
            c.close();
        }
        mClientConnections.clear();
    }

    @Override
    public boolean isRunning() {
        return mIsRunning;
    }

    @Override
    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    @Override
    /**
     * Sends a message to the peer
     * @param client the client to which the message will be sent
     * @param message the message being sent to the client
     */
    public void writeTo(Peer client, String message) {
        message = mListener.onWriteMessage(mHandler, client, message);
        if(mClientConnections.containsKey(client.getIpAddress())) {
             mClientConnections.get(client.getIpAddress()).write(message);
        }
    }

    /**
     * Manage the server instance on it's own thread
     */
    private class ServerRunnable implements Runnable {
        private final String mServiceName;

        /**
         * The name of the service that will be broadcast on the network
         * @param serviceName
         */
        public ServerRunnable(String serviceName) {
            mServiceName = serviceName;
        }

        public void run() {
            mListener.onBeforeStart(mHandler);

            Socket socket;
            ServerSocket serverSocket;

            // set up sockets
            try {
                serverSocket = new ServerSocket(0);
            } catch (Exception e) {
                mListener.onError(mHandler, e);
                return;
            }
            int serverTCPPort = serverSocket.getLocalPort();
            InetAddress broadcastAddress;
            try {
                broadcastAddress = getBroadcastAddress();
            } catch (Exception e) {
                mListener.onError(mHandler, e);
                return;
            }
            PackageInfo pInfo;
            try {
                pInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
            } catch (PackageManager.NameNotFoundException e) {
                mListener.onError(mHandler, e);
                return;
            }

            // we send the client the app:version:port. e.g. tS:47:5653
            mBroadcastThread = new Thread(new BroadcastRunnable(mServiceName + ":" + pInfo.versionCode + ":" + serverTCPPort, broadcastAddress, mClientUDPPort, mBroadcastFrequency, new BroadcastRunnable.OnBroadcastEventListener() {
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
                mListener.onFoundClient(mHandler, mClient);
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
                mListener.onError(mHandler, e);
                Thread.currentThread().interrupt();
            }
        }

        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                String message = mConnection.readLine();
                if (message == null ){
                    Thread.currentThread().interrupt();
                } else {
                    mListener.onMessageReceived(mHandler, mClient, message);
                }
            }
            // close the connection
            mConnection.close();
            // remove all instances of the peer
            if(mClientConnections.containsKey(mConnection.getIpAddress())) {
                mClientConnections.remove(mConnection.getIpAddress());
            }
            removePeer(mClient);
            mListener.onLostClient(mHandler, mClient);
        }
    }

    public interface OnServerEventListener {
        void onBeforeStart(Handler handle);
        void onError(Handler handle, Exception e);
        void onFoundClient(Handler handle, Peer client);
        void onLostClient(Handler handle, Peer client);
        void onMessageReceived(Handler handle, Peer client, String message);

        /**
         * Allows you to perform global operations on a message(such as encryption) based on the peer
         * @param client
         * @param message
         * @return
         */
        String onWriteMessage(Handler handle, Peer client, String message);
    }

}
