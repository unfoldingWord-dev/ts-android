package com.door43.translationstudio.device2device;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.DhcpInfo;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by joel on 12/9/2014.
 */
public class NetworkService {
    private final Context mContext;
    private boolean mIsRunning = false;

    public NetworkService(Context context) {
        mContext = context;
    }

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
                        System.out.println("ip address is: " + ipAddress);
                        return ipAddress;
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("Socket exception in GetIP Address of Utilities", ex.toString());
        }
        return null;
    }

    public static InetAddress getBroadcastAddress(Context context) throws IOException {
        WifiManager wifi = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        // handle null somehow

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }

    public static void broadcast(Context context) throws IOException {
        // TODO: the socket would need to be closed. But really we should create an instance of some network object that we can perform actions with. e.g. broadcast, listening, send, etc.
        String data = "Hello world";
        DatagramSocket socket = new DatagramSocket(9545);
        socket.setBroadcast(true);
        DatagramPacket sendPacket = new DatagramPacket(data.getBytes(), data.length(), getBroadcastAddress(context), 8546);
        socket.send(sendPacket);
//
//        byte[] buf = new byte[1024];
//        DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);
//        socket.receive(receivePacket);
    }

    /**
     * Sets up a new server on a given port that allows you to receive connections communidate with them and advertise services
     * @param port the port on which the server will listen
     */
    public static void registerServer(int port) {

    }

    /**
     * Sets up a new client on a given port that allows you to communicate with a server
     * @param port
     */
    public static void registerClient(int port) {

    }

    /**
     * Begins running as a server
     * @param port the port the server listens on
     */
    public void startServer(int port) {
        if(mIsRunning) return;
        mIsRunning = true;
        // TODO: start the server
    }

    /**
     * Begins running as a client
     * @param port the port on which the client listens on
     */
    public void startClient(int port) {
        if(mIsRunning) return;
        mIsRunning = false;
        // TODO: start the client
    }
}
