package com.door43.translationstudio.network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Maintains information about a socket connection
 */
public class Connection {

    private final Socket mSocket;
    private final PrintWriter mWriter;
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
        mReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//        mWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        mWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

        mIpAddress = socket.getInetAddress().toString().replace("/", "");
    }

    /**
     * Writes a message to the socket
     * @param message
     * @throws IOException
     */
    public void write(String message) throws IOException {
        mWriter.println(message);
    }

    /**
     * Reads a message from the socket
     * @return
     * @throws IOException
     */
    public String readLine() throws IOException {
        return mReader.readLine();
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
            e.printStackTrace();
        }
        try {
            mReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mWriter.close();
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
