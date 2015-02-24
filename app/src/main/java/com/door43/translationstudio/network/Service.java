package com.door43.translationstudio.network;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;

import com.door43.util.Logger;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the base class for the client and server classes
 */
public abstract class Service {
    protected final Context mContext;
    private static int CONNECTION_TIMEOUT = 10000; // 10 seconds
    private Map<String, Peer> mPeers = new HashMap<String, Peer>();

    public Service(Context context) {
        mContext = context;
    }

    /**
     * Returns the broadcast ip address
     * @return
     * @throws IOException
     */
    public InetAddress getBroadcastAddress() throws UnknownHostException {
        WifiManager wifi = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        // handle null somehow

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++) {
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        }
       return InetAddress.getByAddress(quads);
    }

    /**
     * Returns the ip address of the device
     * @return
     */
    public static String getIpAddress() {
        try {
            for (Enumeration en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = (NetworkInterface)en.nextElement();
                for (Enumeration enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = (InetAddress)enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()&&inetAddress instanceof Inet4Address) {
                        String ipAddress=inetAddress.getHostAddress().toString();
                        return ipAddress;
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Returns a list of ip addresses on the local network
     * @param subnet the subnet address. This can be any ip address and the subnet will automatically be retreived.
     * @return
     */
    public static List<String> checkHosts(String subnet){

        List<String> hosts = new ArrayList<String>();
        int timeout=1000;

        // trim down to just the subnet
        String[] pieces = subnet.trim().split("\\.");
        if(pieces.length == 4) {
            subnet = pieces[0]+"."+pieces[1]+"."+pieces[2];
        } else if(pieces.length != 3) {
            return hosts;
        }

        for (int i=1;i<254;i++){
            String host=subnet + "." + i;
            try {
                if (InetAddress.getByName(host).isReachable(timeout)){
                    hosts.add(host);
                    System.out.println(host + " is reachable");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return hosts;
    }

    /**
     * Adds a network peer to the list of known peers for this service
     * @param p The peer to be added to the list of peers
     * @return returns true if the peer is new and was added
     */
    protected boolean addPeer(Peer p) {
        if(!mPeers.containsKey(p.getIpAddress())) {
            mPeers.put(p.getIpAddress(), p);
            return true;
        } else {
            mPeers.get(p.getIpAddress()).touch();
            return false;
        }
    }

    /**
     * Removes a peer from the list of peers
     * @param p the peer to be removed
     */
    protected void removePeer(Peer p) {
        if(mPeers.containsKey(p.getIpAddress())) {
            mPeers.remove(p.getIpAddress());
        }
    }

    /**
     * Returns a list of peers that are connected to this service
     * @return
     */
    public ArrayList<Peer> getPeers() {
        return new ArrayList<Peer>(mPeers.values());
    }

    /**
     * Opens a new temporary socket for transfering a file and lets the client know it should connect to it.
     * TODO: I don't think we should attempt to throw too much into the client and server classes.
     * They work well at establishing initial contact. We should place this elsewhere.
     */
    public ServerSocket createSenderSocket(final OnSocketEventListener listener) {
        final ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(0);
            serverSocket.setSoTimeout(CONNECTION_TIMEOUT);
        } catch (IOException e) {
            Logger.e(this.getClass().getName(), "failed to create a sender socket", e);
            return null;
        }
        // begin listening for the socket connection
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Socket socket;
                try {
                    socket = serverSocket.accept();
                    listener.onOpen(new Connection(socket));
                } catch (IOException e) {
                    Logger.e(this.getClass().getName(), "failed to accept the receiver socket", e);
                    return;
                }
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    Logger.e(this.getClass().getName(), "failed to close the sender socket", e);
                }
            }
        });
        t.start();
        return serverSocket;
    }

    /**
     * Connects to the end of a data socket
     * @param listener
     * @return
     */
    public void createReceiverSocket(final Peer peer, final int port, final OnSocketEventListener listener) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InetAddress serverAddr = InetAddress.getByName(peer.getIpAddress());
                    Socket socket = new Socket(serverAddr, port);
                    socket.setSoTimeout(CONNECTION_TIMEOUT);
                    listener.onOpen(new Connection(socket));
                } catch(UnknownHostException e) {
                    Thread.currentThread().interrupt();
                } catch (IOException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        t.start();
    }

    public abstract void writeTo(Peer peer, String message);

    /**
     * Starts the service
     * @param serviceName the name of the service being advertised or listened for on the network
     */
    public abstract void start(String serviceName, Handler handle);

    /**
     * Stops the service
     */
    public abstract void stop();

    /**
     * Checks if the service is currently running
     * @return
     */
    public abstract boolean isRunning();

    /**
     * Sets the handler for the service. This is handy when keeping the service alive across different activities.
     * @deprecated this doesn't help us with the fragment lifecycle at all.
     * @param handler
     */
    public abstract void setHandler(Handler handler);


    public interface OnSocketEventListener {
        public void onOpen(Connection connection);

    }
}
