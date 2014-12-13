package com.door43.translationstudio.network;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
    private final PrintWriter mStringWriter;
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
        mStringWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

        mIpAddress = socket.getInetAddress().toString().replace("/", "");
    }

    /**
     * Writes a string message to the socket
     * @param message
     * @throws IOException
     */
    public void write(String message) throws IOException {
        mStringWriter.println(message);
    }

    /**
     * Writes a file to the socket
     * @param file the file to be written
     * @return returns true if the file was sent successfully
     */
    public boolean write(File file) {
        // TODO: we want to keep the client and server simple, but we also want to be able to transfer files.
        if(file.exists()) {
            BufferedOutputStream out;
            try {
                out = new BufferedOutputStream(mSocket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            FileInputStream in;
            try {
                in = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return false;
            }

            byte[] buffer = new byte[(int)file.length()];
            int bytesRead = 0;

            try {
                while((bytesRead = in.read(buffer)) > 0) {
                    out.write(buffer, 0, bytesRead);
                }
                out.flush();
                out.close();
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
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
        mStringWriter.close();
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
