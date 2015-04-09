package com.door43.translationstudio.tasks;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;

import com.door43.translationstudio.network.Peer;
import com.door43.util.threads.ManagedTask;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by joel on 4/8/2015.
 */
public abstract class NetworkServiceTask extends ManagedTask {
    private Map<String, Peer> mPeers = new HashMap<>();

    /**
     * Returns the network broadcast ip address
     * @return
     * @throws IOException
     */
    protected static InetAddress getBroadcastAddress(Context context) throws UnknownHostException {
        WifiManager wifi = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
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
    protected static String getIpAddress() {
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
    protected static List<String> checkHosts(String subnet){

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
}
