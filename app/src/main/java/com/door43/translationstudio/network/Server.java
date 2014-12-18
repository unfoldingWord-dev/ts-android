package com.door43.translationstudio.network;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
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
    private int mBroadcastFrequency = 5000;
    private Thread mServerThread = null;
    private Thread mBroadcastThread = null;
    private Map<String, Connection> mClientConnections = new HashMap<String, Connection>();

    public Server(Context context, int clientUDPPort, OnServerEventListener listener) {
        super(context);
        mClientUDPPort = clientUDPPort;
        mListener = listener;
    }

    /**
     * This will cause the server to begin listening on a port for connections and advertise it's services to the network
     */
    public void start() {
        if(mIsRunning) return;
        mIsRunning = true;

        mServerThread = new Thread(new ServerThread());
        mServerThread.start();
    }

    /**
     * Stops the server from advertising it's services and listening for responses.
     */
    public void stop() {
        mIsRunning = false;
        mBroadcastThread.interrupt();
        // TODO: we may need to manually close the server socket.
        mServerThread.interrupt();
        Connection[] clients = mClientConnections.values().toArray(new Connection[mClientConnections.size()]);
        for(Connection c:clients) {
            c.close();
        }
        mClientConnections.clear();
    }

    /**
     * Sends a message to the peer
     * @param client the client to which the message will be sent
     * @param message the message being sent to the client
     */
    public void writeTo(Peer client, String message) {
        if(mClientConnections.containsKey(client.getIpAddress())) {
             mClientConnections.get(client.getIpAddress()).write(message);
        }
    }

    /**
     * Manage the server instance on it's own thread
     */
    private class ServerThread implements Runnable {

        public void run() {
            Socket socket;
            ServerSocket serverSocket;

            // set up sockets
            try {
                serverSocket = new ServerSocket(0);
            } catch (IOException e) {
                mListener.onError(e);
                return;
            }
            int serverTCPPort = serverSocket.getLocalPort();
            InetAddress broadcastAddress;
            try {
                broadcastAddress = getBroadcastAddress();
            } catch (UnknownHostException e) {
                mListener.onError(e);
                return;
            }
            PackageInfo pInfo;
            try {
                pInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
            } catch (PackageManager.NameNotFoundException e) {
                mListener.onError(e);
                return;
            }

            // we send the client the app:version:port. e.g. tS:47:5653
            mBroadcastThread = new Thread(new BroadcastRunnable("tS:" + pInfo.versionCode + ":" + serverTCPPort, broadcastAddress, mClientUDPPort, mBroadcastFrequency, new BroadcastRunnable.OnBroadcastEventListener() {
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
                    ClientThread clientThread = new ClientThread(socket);
                    new Thread(clientThread).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Manages a single client connection on it's own thread
     */
    private class ClientThread implements Runnable {
        private Connection mConnection;
        private Peer mClient;

        public ClientThread(Socket clientSocket) {
            // create a new peer
            mClient = new Peer(clientSocket.getInetAddress().toString().replace("/", ""), clientSocket.getPort());
            if(addPeer(mClient)) {
                mListener.onFoundClient(mClient);
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
            } catch (IOException e) {
                mListener.onError(e);
                Thread.currentThread().interrupt();
            }
        }

        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                String message = mConnection.readLine();
                if (message == null ){
                    Thread.currentThread().interrupt();
                } else {
                    mListener.onMessageReceived(mClient, message);
                }
            }
            // close the connection
            mConnection.close();
            // remove all instances of the peer
            if(mClientConnections.containsKey(mConnection.getIpAddress())) {
                mClientConnections.remove(mConnection.getIpAddress());
            }
            removePeer(mClient);
            mListener.onLostClient(mClient);
        }
    }

    public interface OnServerEventListener {
        public void onError(Exception e);
        public void onFoundClient(Peer client);
        public void onLostClient(Peer client);
        public void onMessageReceived(Peer client, String message);
    }

}
