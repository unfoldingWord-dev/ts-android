package com.door43.translationstudio.network;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * This class listens for broadcast messages from servers.
 */
public class BroadcastListenerRunnable implements Runnable {
    private final int mPort;
    private final OnBroadcastListenerEventListener mListener;

    /**
     * Creates a new runnable that listens for UDB broadcasts
     * @param port the port on which the client will listen
     */
    public BroadcastListenerRunnable(int port, OnBroadcastListenerEventListener listener) {
        mPort = port;
        mListener = listener;
    }

    @Override
    public void run() {
        DatagramSocket socket;
        try {
            socket = new DatagramSocket(mPort);
        } catch (SocketException e) {
            mListener.onError(e);
            return;
        }
        try {
            while (!Thread.currentThread().isInterrupted()) {
                byte[] recvBuf = new byte[15000];
                if (socket == null || socket.isClosed()) {
                    socket = new DatagramSocket(mPort);
                }
                DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);

                // receive broadcast message
                socket.receive(packet);
                String senderIP = packet.getAddress().getHostAddress();
                String message = new String(packet.getData()).trim();

                // notify listener of broadcast message
                mListener.onMessageReceived(message, senderIP);
            }
        } catch (Exception e) {
            mListener.onError(e);
        } finally {
            socket.close();
        }
    }

    /**
     * An interface to handle events from the broadcast listener
     */
    public interface OnBroadcastListenerEventListener {
        public void onError(Exception e);
        public void onMessageReceived(String message, String senderIP);
    }
}