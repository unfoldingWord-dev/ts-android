package com.door43.translationstudio.services;

import android.app.Service;
import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Base64;

import org.unfoldingword.tools.logger.Logger;
import com.door43.translationstudio.network.Connection;
import com.door43.translationstudio.network.Peer;
import com.door43.util.RSAEncryption;
import com.tozny.crypto.android.AesCbcWithIntegrity;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tozny.crypto.android.AesCbcWithIntegrity.decryptString;
import static com.tozny.crypto.android.AesCbcWithIntegrity.encrypt;
import static com.tozny.crypto.android.AesCbcWithIntegrity.generateKey;
import static com.tozny.crypto.android.AesCbcWithIntegrity.keyString;
import static com.tozny.crypto.android.AesCbcWithIntegrity.keys;

/**
 * Created by joel on 7/23/2015.
 */
public abstract class NetworkService extends Service {
    private static int CONNECTION_TIMEOUT = 30000; // 30 seconds
    private Map<String, Peer> mPeers = new HashMap<String, Peer>();

    /**
     * Returns the broadcast ip address
     * @return
     * @throws IOException
     */
    public InetAddress getBroadcastAddress() throws UnknownHostException {
        WifiManager wifi = (WifiManager)getApplication().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        if(dhcp == null) {
            throw new UnknownHostException();
        }

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
                    Logger.i(NetworkService.class.getName(), host + " is reachable");
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
        } else if(mPeers.get(p.getIpAddress()).getPort() != p.getPort()) {
            // the port changed, replace peer
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
    public ServerSocket openWriteSocket(final OnSocketEventListener listener) {
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
    public void openReadSocket(final Peer peer, final int port, final OnSocketEventListener listener) {
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

    /**
     * Encrypts a message with a public key
     * @param publicKey the public key that will be used to encrypt the message
     * @param message the message to be encrypted
     * @return the encrypted message
     */
    public String encryptMessage(PublicKey publicKey, String message)  {
        // TRICKY: RSA is not good for encrypting large amounts of data.
        // So we first encrypt the data then encrypt the encryption key using the public key.
        // the encrypted key is then attached to the encrypted message.

        try {
            // encrypt message
            AesCbcWithIntegrity.SecretKeys key = generateKey();
            AesCbcWithIntegrity.CipherTextIvMac civ = encrypt(message, key);
            String encryptedMessage = civ.toString();

            // encrypt key
            byte[] encryptedKeyBytes = RSAEncryption.encryptData(keyString(key), publicKey);
            if(encryptedKeyBytes == null) {
                Logger.e(this.getClass().getName(), "Failed to encrypt the message");
                return null;
            }
            // encode key
            String encryptedKey = new String(Base64.encode(encryptedKeyBytes, Base64.NO_WRAP));
            return encryptedKey + "-key-" + encryptedMessage;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Decrypts a message using the private key
     * @param privateKey
     * @param message the message to be decrypted
     * @return the decrypted message
     */
    public String decryptMessage(PrivateKey privateKey, String message) {
        // extract encryption key
        try {
            String[] pieces = message.split("\\-key\\-");
            if (pieces.length == 2) {
                // decode key
                byte[] data = Base64.decode(pieces[0].getBytes(), Base64.NO_WRAP);
                // decrypt key
                AesCbcWithIntegrity.SecretKeys key = keys(RSAEncryption.decryptData(data, privateKey));

                // decrypt message
                AesCbcWithIntegrity.CipherTextIvMac civ = new AesCbcWithIntegrity.CipherTextIvMac(pieces[1]);
                return decryptString(civ, key);
            } else {
                Logger.w(this.getClass().getName(), "Invalid message to decrypt");
                return null;
            }
        } catch(Exception e) {
            Logger.e(this.getClass().getName(), "Invalid message to decrypt", e);
            return null;
        }
    }

    public interface OnSocketEventListener {
        void onOpen(Connection connection);
    }
}
