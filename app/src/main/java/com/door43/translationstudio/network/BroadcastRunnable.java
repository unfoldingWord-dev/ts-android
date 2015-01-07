package com.door43.translationstudio.network;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * Sets up a broadcaster to send packets to everyone on the network.
 */
public class BroadcastRunnable implements Runnable {
    private final int mPort;
    private final String mData;
    private final OnBroadcastEventListener mListener;
    private final InetAddress mIpAddress;
    private final int mBroadcastFrequency;

    /**
     *
     * @param data the data to be sent. This should be no larger than 1024 bytes
     * @param address
     * @param port
     * @param broadcastFrequency
     * @param listener
     */
    public BroadcastRunnable(String data, InetAddress address, int port, int broadcastFrequency, OnBroadcastEventListener listener) {
        mPort = port;
        mData = data;
        mListener = listener;
        mIpAddress = address;
        mBroadcastFrequency = broadcastFrequency;
        if(data.length() > 1024) {
            listener.onError(new Exception("The data cannot be longer than 1024 bytes"));
            return;
        }
    }

    @Override
    public void run() {
        DatagramSocket socket;
        try {
            socket = new DatagramSocket();
            socket.setBroadcast(true);
        } catch (SocketException e) {
            mListener.onError(e);
            return;
        }

        DatagramPacket sendPacket;
        sendPacket = new DatagramPacket(mData.getBytes(), mData.length(), mIpAddress, mPort);

        try {
            while (!Thread.currentThread().isInterrupted()) {
               socket.send(sendPacket);
               try {
                   Thread.sleep(mBroadcastFrequency);
               } catch (InterruptedException e) {
                   break;
               }
            }
        } catch (Exception e) {
            mListener.onError(e);
        } finally {
            socket.close();
        }
    }

    public interface OnBroadcastEventListener {
        public void onError(Exception e);
    }
}