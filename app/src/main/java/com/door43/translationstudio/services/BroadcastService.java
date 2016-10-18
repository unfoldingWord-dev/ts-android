package com.door43.translationstudio.services;

import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import org.unfoldingword.tools.logger.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Broadcasts services provided by this device on the network
 */
public class BroadcastService extends NetworkService {
    public static final String PARAM_BROADCAST_PORT = "param_broadcast_udp_port";
    public static final String PARAM_SERVICE_PORT = "param_service_tcp_port";
    public static final String PARAM_FREQUENCY = "param_broadcast_frequency";
    public static final int TS_PROTOCAL_VERSION = 2;
    private final IBinder mBinder = new LocalBinder();
    private DatagramSocket mSocket;
    private Timer mTimer;
    private static Boolean mIsRunning = false;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

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
    public void onCreate() {
        Logger.i(this.getClass().getName(), "Starting broadcaster");
        mTimer = new Timer();
        try {
            mSocket = new DatagramSocket();
            mSocket.setBroadcast(true);
        } catch (SocketException e) {
            // TODO: notify app
            Logger.e(this.getClass().getName(), "Failed to start the broadcaster", e);
            stopService();
            return;
        }
    }

    public int onStartCommand(Intent intent, int flags, int startid) {
        if(intent != null) {
            Bundle args = intent.getExtras();
            if (args != null) {
                setRunning(true);
                final int udpPort = args.getInt(PARAM_BROADCAST_PORT);
                final int serviceTCPPort = args.getInt(PARAM_SERVICE_PORT);
                final int broadcastFrequency = args.getInt(PARAM_FREQUENCY, 2000);

                JSONObject json = new JSONObject();
                try {
                    json.put("version", TS_PROTOCAL_VERSION);
                    json.put("port", serviceTCPPort);
                } catch (JSONException e) {
                    // TODO: 11/24/2015 notify app
                    Logger.e(this.getClass().getName(), "Failed to prepare the broadcast payload", e);
                    stopService();
                    return START_NOT_STICKY;
                }

                String data = json.toString();

                // prepare packet
                if (data.length() > 1024) {
                    // TODO: notify app
                    Logger.w(this.getClass().getName(), "The broadcast data cannot be longer than 1024 bytes");
                    stopService();
                    return START_NOT_STICKY;
                }

                InetAddress ipAddress;
                try {
                    ipAddress = getBroadcastAddress();
                } catch (UnknownHostException e) {
                    // TODO: notify app
                    Logger.e(this.getClass().getName(), "Failed to get the broadcast ip address", e);
                    stopService();
                    return START_NOT_STICKY;
                }
                final DatagramPacket packet = new DatagramPacket(data.getBytes(), data.length(), ipAddress, udpPort);

                // schedule broadcast
                mTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            mSocket.send(packet);
                        } catch (IOException e) {
                            Logger.e(this.getClass().getName(), "Failed to send the broadcast packet", e);
                        }
                    }
                }, 0, broadcastFrequency);
                return START_STICKY;
            }
        }
        Logger.w(this.getClass().getName(), "The broadcaster requires arguments to operate correctly");
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
        if(mTimer != null) {
            mTimer.cancel();
        }
        if(mSocket != null) {
            // TODO: 11/20/2015 notify network that we are shutting down

            // close
            mSocket.close();
        }
        setRunning(false);
        Logger.i(this.getClass().getName(), "Stopping broadcaster");
    }

    /**
     * Class to retrieve instance of service
     */
    public class LocalBinder extends Binder {
        public BroadcastService getServiceInstance() {
            return BroadcastService.this;
        }
    }
}