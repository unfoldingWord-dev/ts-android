package com.door43.translationstudio.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;

import com.door43.tools.reporting.Logger;

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
public class BroadcastService extends Service {
    private static final Timer sTimer = new Timer();
    public static final String PARAM_BROADCAST_PORT = "param_broadcast_udp_port";
    public static final String PARAM_SERVICE_PORT = "param_service_tcp_port";
    public static final String PARAM_FREQUENCY = "param_broadcast_frequency";
    public static final String PARAM_SERVICE_NAME = "param_service_name";
    private static DatagramSocket sSocket;
    private static boolean sRunning = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Logger.i(this.getClass().getName(), "Starting broadcaster");
        try {
            sSocket = new DatagramSocket();
            sSocket.setBroadcast(true);
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
                sRunning = true;
                final int udpPort = args.getInt(PARAM_BROADCAST_PORT);
                final int serviceTCPPort = args.getInt(PARAM_SERVICE_PORT);
                final int broadcastFrequency = args.getInt(PARAM_FREQUENCY);
                final String serviceName = args.getString(PARAM_SERVICE_NAME);
                PackageInfo pInfo;
                try {
                    pInfo = getApplication().getPackageManager().getPackageInfo(getApplication().getPackageName(), 0);
                } catch (PackageManager.NameNotFoundException e) {
                    // TODO: notify app
                    Logger.e(this.getClass().getName(), "Failed to load the app version", e);
                    stopService();
                    return START_NOT_STICKY;
                }
                String data = serviceName + ":" + pInfo.versionCode + ":" + serviceTCPPort;

                // prepare packet
                if (data.length() > 1024) {
                    // TODO: notify app
                    Logger.w(this.getClass().getName(), "The broadcast data cannot be longer than 1024 bytes");
                    stopService();
                    return START_NOT_STICKY;
                }
                WifiManager wifi = (WifiManager) getApplication().getSystemService(Context.WIFI_SERVICE);
                final InetAddress ipAddress;
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
                sTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            sSocket.send(packet);
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
        if(sTimer != null) {
            sTimer.cancel();
        }
        if(sSocket != null) {
            sSocket.close();
        }
        sRunning = false;
        Logger.i(this.getClass().getName(), "Stopping broadcaster");
    }

    /**
     * Checks if the service is running
     * @return
     */
    public static boolean isRunning() {
        return sRunning;
    }

    private InetAddress getBroadcastAddress() throws UnknownHostException {
        WifiManager wifi = (WifiManager)getApplication().getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        if(dhcp == null) {
            throw new UnknownHostException();
        }

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++) {
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        }
        return InetAddress.getByAddress(quads);
    }
}