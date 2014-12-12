package com.door43.translationstudio.network;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.HashMap;
import java.util.Map;

/**
 * This class operates as a client on the local network.
 * The client will listen for UDP broadcast messages from servers.
 * The client can connect to a server's TCP port as detailed in the UDP message.
 */
public class Client extends Service {
    private final int mClientUDPPort;
    private final OnClientEventListener mListener;
    private boolean mIsRunning = false;
    private final int mCleanupFrequency = 5000;
    private final long mServerTimeout = 5000; // servers will be considered as lost after this period of inactivity
    private Thread mBroadcastListenerThread;
    private Thread mCleanupThread;

    public Client(Context context, int clientUDPPort, OnClientEventListener listener) {
        super(context);
        mClientUDPPort = clientUDPPort;
        mListener = listener;
    }

    @Override
    public void start() {
        if(mIsRunning) return;
        mIsRunning = true;

        mBroadcastListenerThread = new Thread(new BroadcastListenerRunnable(mClientUDPPort, new BroadcastListenerRunnable.OnBroadcastListenerEventListener() {
            @Override
            public void onError(Exception e) {
                mListener.onError(e);
            }

            @Override
            public void onMessageReceived(String message, String senderIP) {
                // we received a broadcast from a server. expecting app:version:port
                String[] pieces = message.split(":");
                if(pieces != null && pieces.length == 3) {
                    String service = pieces[0];
                    int version = Integer.parseInt(pieces[1]);
                    int port = Integer.parseInt(pieces[2]);
                    Peer p = new Peer(senderIP, port, service, version);
                    if(addPeer(p)) {
                        mListener.onFoundServer(p);
                    }
                } else {
                    mListener.onError(new IndexOutOfBoundsException("The client expected three pieces of data from the server but received "+ message));
                }
            }
        }));
        mBroadcastListenerThread.start();

        // performs cleanup tasks such as checking for disconnected servers
        mCleanupThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        Map<String, Peer> connectedPeers = mPeers;
                        String[] keys = connectedPeers.keySet().toArray(new String[connectedPeers.keySet().size()]);
                        for(String key:keys) {
                            Peer p = mPeers.get(key);
                            if(System.currentTimeMillis() - p.getLastSeenAt() > mServerTimeout) {
                                mPeers.remove(key);
                                mListener.onLostServer(p);
                            }
                        }

                        try {
                            Thread.sleep(mCleanupFrequency);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    mListener.onError(e);
                }
            }
        });
        mCleanupThread.start();
    }

    @Override
    public void stop() {
        mIsRunning = false;
        mCleanupThread.interrupt();
        mBroadcastListenerThread.interrupt();
    }

    public interface OnClientEventListener {
        public void onError(Exception e);
        public void onFoundServer(Peer server);
        public void onLostServer(Peer server);
    }
}
