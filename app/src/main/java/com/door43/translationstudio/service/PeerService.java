package com.door43.translationstudio.service;

import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.device2device.SocketMessages;
import com.door43.translationstudio.network.Connection;
import com.door43.translationstudio.network.Peer;
import com.door43.util.RSAEncryption;
import com.door43.util.StringUtilities;

import java.net.InetAddress;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles communication between peers on the network
 */
@Deprecated
public class PeerService extends NetworkService {
    private static final String PARAM_PUBLIC_KEY = "param_public_key";
    private static final String PARAM_PRIVATE_KEY = "param_private_key";
    private final IBinder binder = new LocalBinder();
    private OnServiceEventListener listener;
    private Map<String, Connection> peerConnections = new HashMap<>();
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * Notifies the listener about events with the service and peers
     * @param listener
     */
    public void setOnServiceEventListener(OnServiceEventListener listener) {
        this.listener = listener;
        if(isRunning() && this.listener != null) {
            this.listener.onReady();
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
                    listener.onReady();
                }
                setRunning(true);
                return START_STICKY;
            }
        }
        Logger.e(this.getClass().getName(), "Peer service requires arguments");
        stopService();
        return START_NOT_STICKY;
    }

    /**
     * Establishes a TCP connection with a peer.
     * Once this connection has been made the cleanup thread won't identify the peer as lost unless the tcp connection is also disconnected.
     * @param peer
     */
    public void connectToPeer(Peer peer) {
        if(!peerConnections.containsKey(peer.getIpAddress())) {
            PeerThread peerThread = new PeerThread(peer);
            new Thread(peerThread).start();
        }
    }

    /**
     * Stops the service
     */
    public void stopService() {
        Logger.i(this.getClass().getName(), "Stopping peer service");
        // close sockets
        for(String key: peerConnections.keySet()) {
            peerConnections.get(key).close();
        }
        setRunning(false);
    }

    @Override
    public void onDestroy() {
        stopService();
    }

    /**
     * Sends a message to the peer
     * @param peer
     * @param message the message being sent to the peer
     */
    private void sendMessage(Peer peer, String message) {
        if (peerConnections.containsKey(peer.getIpAddress())) {
            if(peer.isSecure()) {
                // encrypt message
                PublicKey key = RSAEncryption.getPublicKeyFromString(peer.keyStore.getString(PeerStatusKeys.PUBLIC_KEY));
                if(key != null) {
                    message = encryptMessage(key, message);
                } else {
                    Logger.w(this.getClass().getName(), "Missing the peer's public key");
                    message = SocketMessages.MSG_EXCEPTION;
                }
            }
            peerConnections.get(peer.getIpAddress()).write(message);
        }
    }

    /**
     * Handles the initial handshake and authorization with the peer.
     * @param peer
     * @param message
     */
    private void onMessageReceived(Peer peer, String message) {
        if(peer.isSecure()) {
            message = decryptMessage(privateKey, message);
            if(message != null) {
                String[] data = StringUtilities.chunk(message, ":");
                onCommandReceived(peer, data[0], Arrays.copyOfRange(data, 1, data.length));
            } else if(listener != null) {
                listener.onError(new Exception("Message descryption failed"));
            }
        } else {
            handshake(peer, message);
        }
    }

    /**
     * Performs the handshake with the peer
     * @param peer
     * @param message
     */
    private void handshake(Peer peer, String message) {
        String[] data = StringUtilities.chunk(message, ":");
        switch(data[0]) {
            case SocketMessages.MSG_PUBLIC_KEY:
                Logger.i(this.getClass().getName(), "connected to server " + peer.getIpAddress());
                // receive the server's public key
                peer.keyStore.add(PeerStatusKeys.PUBLIC_KEY, data[1]);
                peer.keyStore.add(PeerStatusKeys.WAITING, false);
                peer.keyStore.add(PeerStatusKeys.CONTROL_TEXT, getResources().getString(R.string.browse));
                peer.setIsSecure(true);
                if(listener != null) {
                    listener.onPeerChanged(peer);
                }
                break;
            case SocketMessages.MSG_OK:
                Logger.i(this.getClass().getName(), "accepted by server " + peer.getIpAddress());
                // we are authorized to access the server
                // send public key to server
                sendMessage(peer, SocketMessages.MSG_PUBLIC_KEY + ":" + publicKey);
                break;
            default:
                Logger.w(this.getClass().getName(), "Invalid request: " + message);
                sendMessage(peer, SocketMessages.MSG_INVALID_REQUEST);
        }
    }

    /**
     * Handles commands sent from the server
     * @param server
     * @param command
     * @param data
     */
    private void onCommandReceived(final Peer server, String command, String[] data) {
        // TODO: 11/20/2015 place command management in another class
        switch(command) {
            case SocketMessages.MSG_INVALID_REQUEST:
                // TODO: do something about this.
                if(listener != null) {
                    listener.onError(new Throwable("Invalid request"));
                }
                break;
            default:
                Logger.i(this.getClass().getName(), "received invalid request from " + server.getIpAddress() + ": " + command);
                sendMessage(server, SocketMessages.MSG_INVALID_REQUEST);
        }
    }

    public interface OnServiceEventListener {
        void onReady();
        void onPeerLost(Peer peer);
        void onPeerChanged(Peer peer);
        void onError(Throwable e);

    }

    /**
     * Class to retrieve instance of service
     */
    public class LocalBinder extends Binder {
        public PeerService getServiceInstance() {
            return PeerService.this;
        }
    }

    /**
     * Manages a single peer connection on it's own thread
     */
    private class PeerThread implements Runnable {
        private Connection connection;
        private Peer peer;

        public PeerThread(Peer peer) {
            this.peer = peer;
        }

        @Override
        public void run() {
            // set up sockets
            try {
                InetAddress serverAddr = InetAddress.getByName(peer.getIpAddress());
                connection = new Connection(new Socket(serverAddr, peer.getPort()));
                connection.setOnCloseListener(new Connection.OnCloseListener() {
                    @Override
                    public void onClose() {
                        Thread.currentThread().interrupt();
                    }
                });
                // we store references to all connections so we can access them later
                peerConnections.put(connection.getIpAddress(), connection);
            } catch (Exception e) {
                if(listener != null) {
                    listener.onError(e);
                }
                Thread.currentThread().interrupt();
                return;
            }

            // begin listening to peer
            while (!Thread.currentThread().isInterrupted()) {
                String message = connection.readLine();
                if(message == null) {
                    Thread.currentThread().interrupt();
                } else {
                    onMessageReceived(peer, message);
                }
            }
            // close the connection
            connection.close();
            // remove all instances of the peer
            if(peerConnections.containsKey(connection.getIpAddress())) {
                peerConnections.remove(connection.getIpAddress());
            }
            removePeer(peer);
            if(listener != null) {
                listener.onPeerLost(peer);
            }
        }
    }
}
