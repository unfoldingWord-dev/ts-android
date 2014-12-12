package com.door43.translationstudio.network;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * This class operats are a server on the local network.
 * The server will broadcast it's services to a UDP port.
 * Clients will then be able to connect to a tcp port as detailed in the UDP message.
 */
public class Server extends Service {
    private final int mClientUDPPort; // the port on which the client listens for broadcast messages
    private boolean mIsRunning = false;
    private int mBroadcastFrequency = 5000;
    Thread mServerThread = null;
    Thread mBroadcastThread = null;

    public Server(Context context, int clientUDPPort) {
        super(context);
        mClientUDPPort = clientUDPPort;
    }

    /**
     * This will cause the server to begin listening on a port for connections and advertise it's services to the network
     */
    public void start() {
        if(mIsRunning) return;
        mIsRunning = true;

        mServerThread = new Thread(new ServerThread());
        mServerThread.start();
    }

    /**
     * Stops the server from advertising it's services and listening for responses.
     */
    public void stop() {
        mIsRunning = false;
        mBroadcastThread.interrupt();
        mServerThread.interrupt();
    }

    private class ServerThread implements Runnable {

        public void run() {
            Socket socket;
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(0);
                // begin broadcasting the tcp port to the network.
                int serverTCPPort = serverSocket.getLocalPort();
                InetAddress broadcastAddress = getBroadcastAddress();
                PackageInfo pInfo = null;
                try {
                    pInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
                } catch (PackageManager.NameNotFoundException e) {
                }
                if(broadcastAddress != null && pInfo != null) {
                    // we send the client the app:version:port. e.g. tS:47:5653
                    mBroadcastThread = new Thread(new BroadcastRunnable("tS:" + pInfo.versionCode + ":" + serverTCPPort, broadcastAddress, mClientUDPPort, mBroadcastFrequency, new BroadcastRunnable.OnBroadcastEventListener() {
                        @Override
                        public void onError(Exception e) {
                            // The broadcast service failed to start.
                            mServerThread.interrupt();
                        }
                    }));
                    mBroadcastThread.start();
                } else {
                    // the broadcaster could not be started
                    mServerThread.interrupt();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    socket = serverSocket.accept();
                    CommunicationThread commThread = new CommunicationThread(socket);
                    new Thread(commThread).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class CommunicationThread implements Runnable {

        private Socket clientSocket;

        private BufferedReader input;

        public CommunicationThread(Socket clientSocket) {

            this.clientSocket = clientSocket;

            try {

                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {


            while (!Thread.currentThread().isInterrupted()) {

                try {

                    String read = input.readLine();

                    if (read == null ){
                        Thread.currentThread().interrupt();
                    }else{
                        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                        // send test data back to the client
                        out.write("TstMsg");
                        // TODO: we should fire a callback
//                        updateConversationHandler.post(new updateUIThread(read));

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
