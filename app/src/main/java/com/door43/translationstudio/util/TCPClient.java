package com.door43.translationstudio.util;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class TCPClient {

    private String serverMessage;
    private String mServer;
    private int mPort;
    private TcpListener mTcpListener = null;
    private boolean mRun = false;
    private final String TAG = "TCP Client";
    private TCPClient me = this;

    PrintWriter out;
    InputStream in;

    /**
     *  Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public TCPClient(String server, int port, TcpListener listener) {
        mServer = server;
        mPort = port;
        mTcpListener = listener;
    }

    /**
     * Begins an asyncronous task to send a message to the server
     * @param message message to send
     */
    public void sendMessage(String message){
          new SendTask().execute(message);
    }

    /**
     * Sends a message to the server
     * @param message the message to send
     */
    private void sendAsync(final String message) {
        if (out != null) {
            out.println(message);
        }
    }

    /**
     * Stops listening to the server and closes the TCP connection.
     */
    public void stop(){
        mRun = false;
    }

    /**
     * Begins an asyncronous task to open a TCP connection to the server
     */
    public void connect() {
        if(!mRun) {
            Thread t = new Thread(new ConnectTask());
            t.start();
        }
    }

    /**
     * Establish a TCP connection with the server.
     */
    public void connectAsync() {
        mRun = true;

        try {
            InetAddress serverAddr = InetAddress.getByName(mServer);
            Socket socket = new Socket(serverAddr, mPort);

            try {
                //send the message to the server
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                in = socket.getInputStream();

                // notify delegate listeners that the tcp connection has been established
                mTcpListener.onConnectionEstablished();
                while(mRun) {
                    byte[] buffer = new byte[4096];
                    int read = 0;
                    try {
                        read = in.read(buffer, 0, 4096);
                    } catch (IOException e) {
                        mTcpListener.onError(e);
                        break;
                    }

                    // receive data
                    // TODO: right now we just read the first buffer. We should probably create a way to determine how much data has actually been sent. Probably need to have the server specify how many bytes have been sent.
                    if (read != -1) {
                        byte[] tempdata = new byte[read];
                        System.arraycopy(buffer, 0, tempdata, 0, read);
                        serverMessage = new String(tempdata);
                    }
                    Log.d(TAG, serverMessage);

                    // handle response from the server
                    if (serverMessage != null && mTcpListener != null) {
                        mTcpListener.onMessageReceived(serverMessage);
                    }
                    serverMessage = null;
                }
            } catch (Exception e) {
                mTcpListener.onError(e);
            } finally {
                socket.close();
            }
        } catch (Exception e) {
            mTcpListener.onError(e);
        }

        // stop everything
        if(in != null) {
            try {
                in.close();
            } catch (IOException e) {
                mTcpListener.onError(e);
            }
            in = null;
        }
        if(out != null) {
            out.close();
            out = null;
        }
        mRun = false;
    }

    public interface TcpListener {
        public void onConnectionEstablished();
        public void onMessageReceived(String message);
        public void onError(Throwable t);
    }

    /**
     * Starts a new tcp connection on a new thread
     */
    private class ConnectTask implements Runnable {

        @Override
        public void run() {
            me.connectAsync();
        }
    }

    /**
     * Asyncronous class to send a message to the server
     */
    private class SendTask extends AsyncTask<String, String, TCPClient> {

        @Override
        protected TCPClient doInBackground(String... strings) {
            android.os.Debug.waitForDebugger();
            me.sendAsync(strings[0]);
            return null;
        }
    }
}