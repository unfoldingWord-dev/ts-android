package com.door43.translationstudio.service;

import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.network.BroadcastListenerRunnable;
import com.door43.translationstudio.network.Peer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by joel on 7/24/2015.
 */
public class BroadcastListenerService extends NetworkService {
    private static final String PARAM_SERVER_TTL = "param_server_ttl";
    private static final String PARAM_REFRESH_FREQUENCY = "param_refresh_frequency";
    private Thread mBroadcastListenerThread;
    private BroadcastListenerRunnable mBroadcastListenerRunnable;
    public static final String PARAM_BROADCAST_PORT = "param_broadcast_udp_port";
    public static final String PARAM_SERVICE_NAME = "param_service_name";
    private Callbacks mListener;
    private static boolean sRunning = false;
    private final IBinder mBinder = new LocalBinder();
    private Timer sCleanupTimer = new Timer();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void registerCallback(Callbacks callback) {
        mListener = callback;
    }

    /**
     * Checks if the service is running
     * @return
     */
    public static boolean isRunning() {
        return sRunning;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        Bundle args = intent.getExtras();
        if(args != null) {
            final int UDPport = args.getInt(PARAM_BROADCAST_PORT);
            final String serviceName = args.getString(PARAM_SERVICE_NAME);
            final int serverTTL = args.getInt(PARAM_SERVER_TTL);
            final int refreshFrequency = args.getInt(PARAM_REFRESH_FREQUENCY);
            // listener thread
            mBroadcastListenerRunnable = new BroadcastListenerRunnable(UDPport, new BroadcastListenerRunnable.OnBroadcastListenerEventListener() {
                @Override
                public void onError(Exception e) {
                    Logger.e(BroadcastListenerService.class.getName(), "Broadcast listener encountered an exception", e);
                }

                @Override
                public void onMessageReceived(String message, String senderIP) {
                    String[] parts = message.split(":");
                    if (parts != null && parts.length == 3) {
                        String service = parts[0];
                        if (service.equals(serviceName)) {
                            int version = Integer.parseInt(parts[1]);
                            int port = Integer.parseInt(parts[2]);
                            Peer p = new Peer(senderIP, port, service, version);
                            addPeer(p);
                            if (mListener != null) {
                                mListener.onFoundServer(p);
                            }
                        }
                    } else {
                        Logger.w(BroadcastListenerService.class.getName(), "Expected three pieces of data from the server but rceived " + message);
                    }
                }
            });
            mBroadcastListenerThread = new Thread(mBroadcastListenerRunnable);
            mBroadcastListenerThread.start();
            // cleanup task
            sCleanupTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    ArrayList<Peer> connectedPeers = getPeers();
                    for(Peer p:connectedPeers) {
                        if(System.currentTimeMillis() - p.getLastSeenAt() > serverTTL) {
                            removePeer(p);
                            mListener.onLostServer(p);
                        }
                    }
                }
            }, 0, refreshFrequency);
            sRunning = true;
            return START_STICKY;
        } else {
            Logger.e(this.getClass().getName(), "Broadcast listener service requires arguments");
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
    private void stopService() {
        if(mBroadcastListenerThread != null) {
            mBroadcastListenerThread.interrupt();
        }
        if(mBroadcastListenerRunnable != null) {
            mBroadcastListenerRunnable.stop();
        }
        sRunning = false;
        Logger.i(this.getClass().getName(), "Stopping broadcast listener");
    }

    public interface Callbacks {
        void onFoundServer(Peer server);
        void onLostServer(Peer server);
    }

    /**
     * Class to retrieve instance of service
     */
    public class LocalBinder extends Binder {
        public BroadcastListenerService getServiceInstance() {
            return BroadcastListenerService.this;
        }
    }
}
