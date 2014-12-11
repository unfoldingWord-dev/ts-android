package com.door43.translationstudio.device2device;

import android.content.Context;
import android.os.Handler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Created by joel on 12/10/2014.
 */
public class Server {
    private final Context mContext;
    private final int mPort;
    private boolean mIsRunning = false;
    private int mBroadcastPeriod = 5000;

    public Server(Context context, int port) {
        mPort = port;
        mContext = context;
    }

    /**
     * This will cause the server to begin listening on a port for connections and advertise it's services to the network
     * @param port the client port to which the server will broadcast it's services.
     */
    public boolean start(int port) {
        if(mIsRunning) return false;
        mIsRunning = true;
        // TODO: start the server
        String data = "Hello world";
        final DatagramSocket socket;
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
            return false;
        }
        try {
            socket.setBroadcast(true);
        } catch (SocketException e) {
            e.printStackTrace();
            return false;
        }
        final DatagramPacket sendPacket;
        try {
            sendPacket = new DatagramPacket(data.getBytes(), data.length(), NetworkService.getBroadcastAddress(mContext), port);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        Handler handle = new Handler();

        // begin sending UDP broadcasts
        new Thread() {
            public void run() {
                while(mIsRunning) {
                    try {
                        socket.send(sendPacket);
                    } catch (IOException e) {
                        mIsRunning = false;
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(mBroadcastPeriod);
                    } catch (InterruptedException e) {
                        mIsRunning = false;
                        e.printStackTrace();
                    }
                }
            }
        }.start();

        // begin listening for responses
        // TODO: set up a tcp socket to listen
        return true;
    }

    /**
     * Stops the server from advertising it's services and listening for responses.
     */
    public void stop() {
        mIsRunning = false;
    }
}
