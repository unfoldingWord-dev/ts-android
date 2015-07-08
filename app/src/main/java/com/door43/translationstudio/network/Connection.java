package com.door43.translationstudio.network;

import com.door43.util.reporting.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

/**
 * Maintains information about a socket connection
 */
public class Connection {

    private final Socket mSocket;
    private final BufferedWriter mWriter;
    private final BufferedReader mReader;
    private final String mIpAddress;
    private OnCloseListener mListener;

    /**
     * Creates a new connection object
     * @param socket the new socket
     * @throws IOException
     */
    public Connection(Socket socket) throws IOException {
        mSocket = socket;
        mReader = new BufferedReader(new InputStreamReader(socket.getInputStream()), 1024);
        mWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()), 1024);
        mIpAddress = socket.getInetAddress().toString().replace("/", "");
    }

    /**
     * Writes a string message to the socket
     * @param message
     * @throws IOException
     */
    public void write(String message) {
        try {
            mWriter.write(message);
            mWriter.newLine();
            mWriter.flush();
        } catch (IOException e) {
            Logger.e(this.getClass().getName(), "failed to write the message to the socket", e);
            close();
        }
    }

    /**
     * Reads a message from the socket
     * @return
     * @throws IOException
     */
    public String readLine() {
        try {
            return mReader.readLine();
        } catch (IOException e) {
            Logger.e(this.getClass().getName(), "failed to read the message from the socket", e);
            close();
            return null;
        }
    }

    /**
     * Returns the IP address of the socket
     * @return
     */
    public String getIpAddress() {
        return mIpAddress;
    }

    /**
     * Returns the actual socket
     * @return
     */
    public Socket getSocket() {
        return mSocket;
    }

    /**
     * Closes the socket
     */
    public void close() {
        try {
            mSocket.close();
        } catch (IOException e) {
            Logger.e(this.getClass().getName(), "socket close exception", e);
        }
        try {
            mReader.close();
        } catch (IOException e) {
            Logger.e(this.getClass().getName(), "reader close exception", e);
        }
        try {
            mWriter.close();
        } catch (IOException e) {
            Logger.e(this.getClass().getName(), "writer close exception", e);
        }
        if(mListener != null) {
            mListener.onClose();
        }
    }

    /**
     * Sets the listener that will be called when the connection is closed.
     * @param listener
     */
    public void setOnCloseListener(OnCloseListener listener) {
        mListener = listener;
    }

    public interface OnCloseListener {
        public void onClose();
    }
}
