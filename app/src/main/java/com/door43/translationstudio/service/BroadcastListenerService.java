package com.door43.translationstudio.service;

import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.network.BroadcastListenerRunnable;
import com.door43.translationstudio.network.Peer;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This class listens for services being broadcasted on the local network.
 * Notifications are fired whenever a service becomes or is no longer available.
 */
public class BroadcastListenerService extends NetworkService {
    public static final String PARAM_SERVER_TTL = "param_server_ttl";
    public static final String PARAM_REFRESH_FREQUENCY = "param_refresh_frequency";
    public static final String PARAM_BROADCAST_PORT = "param_broadcast_udp_port";
    public static final String PARAM_SERVICE_NAME = "param_service_name";
    private final IBinder mBinder = new LocalBinder();
    private Thread mBroadcastListenerThread;
    private BroadcastListenerRunnable mBroadcastListenerRunnable;
    private Callbacks mListener;
    private Timer mCleanupTimer;
    private static Boolean mIsRunning = false;

    /**
     * Sets whether or not the service is running
     * @param running
     */
    protected void setRunning(Boolean running) {
        mIsRunning = running;
    }

    /**
     * Checks if the service is currently running
     * @return
     */
    public static boolean isRunning() {
        return mIsRunning;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void registerCallback(Callbacks callback) {
        mListener = callback;
    }

    @Override
    public void onCreate() {
        Logger.i(this.getClass().getName(), "Starting broadcast listener");
        mCleanupTimer = new Timer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        if(intent != null) {
            Bundle args = intent.getExtras();
            if (args != null && args.containsKey(PARAM_BROADCAST_PORT) && args.containsKey(PARAM_SERVICE_NAME)) {
                final int UDPport = args.getInt(PARAM_BROADCAST_PORT);
                final String serviceName = args.getString(PARAM_SERVICE_NAME);
                final int serverTTL = args.getInt(PARAM_SERVER_TTL, 10000);
                final int refreshFrequency = args.getInt(PARAM_REFRESH_FREQUENCY, 5000);
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
                                if(addPeer(p) && mListener != null) {
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
                mCleanupTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        ArrayList<Peer> connectedPeers = getPeers();
                        for (Peer p : connectedPeers) {
                            if (System.currentTimeMillis() - p.getLastSeenAt() > serverTTL) {
                                removePeer(p);
                                if(mListener != null) {
                                    mListener.onLostServer(p);
                                }
                            }
                        }
                    }
                }, 0, refreshFrequency);
                setRunning(true);
                return START_STICKY;
            }
        }
        Logger.e(this.getClass().getName(), "Broadcast listener service requires arguments");
        stopService();
        return START_NOT_STICKY;
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
        setRunning(false);
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
