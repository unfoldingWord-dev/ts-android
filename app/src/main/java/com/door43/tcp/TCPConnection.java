package com.door43.tcp;

import android.util.Log;
import android.util.SparseArray;

import com.door43.tcp.tasks.tcp.TCPTask;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

/**
 * This class allows you to connectAsync to a tcp port to send and receive data.
 * @deprecated
 */
public class TCPConnection {
    private static int numConnections;
    private static SparseArray<TCPTask> mTCPTasks = new SparseArray<TCPTask>();

    private int mId;
    private String mServerMessage;
    private String mServer;
    private int mPort;
    private OnMessageReceived mMessageListener = null;
    private boolean mRun = false;

    private PrintWriter out;
    private BufferedReader in;

    /**
     * Defines a new tcp connection
     * @param server
     * @param port
     */
    public TCPConnection(String server, int port) {
        // automatically generate connection ids
        mId = numConnections;
        numConnections++;

        mServer = server;
        mPort = port;
    }

    /**
     * Adds a new task to be peformed for this connection
     * @param tcpTask
     * @return
     */
    public boolean addTask(TCPTask tcpTask) {
        if (mTCPTasks.get(getID()) != null)
            return false;
        mTCPTasks.put(getID(), tcpTask);
        return true;
    }

    /**
     * Returns this repository's unique id
     * @return
     */
    public int getID() {
        return mId;
    }

    public void cancelTask() {
        TCPTask task = mTCPTasks.get(getID());
        if(task == null) {
            return;
        } else {
            task.cancelTask();
            removeTask(task);
        }
    }

    public void removeTask(TCPTask task) {
        TCPTask runningTask = mTCPTasks.get(getID());
        if (runningTask == null || runningTask != task)
            return;
        mTCPTasks.remove(getID());
    }

    public String getServer() {
        return mServer;
    }

    public int getPort() {
        return mPort;
    }

    public void setListener(OnMessageReceived onMessageReceived) {
        mMessageListener = onMessageReceived;
    }

    /**
     * Sends the message entered by client to the server
     * @param message text entered by client
     */
    public void sendMessage(String message){
        if (out != null && !out.checkError()) {
            out.println(message);
            out.flush();
        }
    }

    public void stopClient(){
        mRun = false;
    }

    public void run() {
        mRun = true;

        try {
            //here you must put your computer's IP address.
            InetAddress serverAddr = InetAddress.getByName(mServer);

            Log.e("TCP Client", "C: Connecting...");

            //create a socket to make the connection with the server
            Socket socket = new Socket(serverAddr, mPort);

            try {

                //send the message to the server
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                Log.e("TCP Client", "C: Sent.");

                Log.e("TCP Client", "C: Done.");

                //receive the message which the server sends back
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                //in this while the client listens for the messages sent by the server
                while (mRun) {
                    mServerMessage = in.readLine();

                    if (mServerMessage != null && mMessageListener != null) {
                        //call the method messageReceived from MyActivity class
                        mMessageListener.messageReceived(mServerMessage);
                    }
                    mServerMessage = null;

                }

                Log.e("RESPONSE FROM SERVER", "S: Received Message: '" + mServerMessage + "'");

            } catch (Exception e) {

                Log.e("TCP", "S: Error", e);

            } finally {
                //the socket must be closed. It is not possible to reconnect to this socket
                // after it is closed, which means a new socket instance has to be created.
                socket.close();
            }

        } catch (Exception e) {

            Log.e("TCP", "C: Error", e);

        }
    }

    public static interface OnMessageReceived {
        public void messageReceived(String message);
    }
}
