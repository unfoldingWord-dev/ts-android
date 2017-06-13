package com.door43.translationstudio.network;

import com.door43.translationstudio.services.Request;
import com.door43.util.KeyValueStore;

import java.util.ArrayList;
import java.util.List;

/**
 * A peer is any device that is connected to another device.
 * Clients can have peers in the form of servers and servers can have peers in the form of clients.
 */
public class Peer {
    private final String mIpAddress;
    private final int mPort;
    private String mServiceName = "";
    private int mVersion = 0;
    private long mLastSeenAt = 0;
    private boolean isSecure = false;
    public final KeyValueStore keyStore = new KeyValueStore();
    private boolean mIsAuthorized = false;
    private boolean hasIdentity = false;
    private String name = "unknown";
    private String deviceType = "unknown";
    private String id = "unknown";
    private List<Request> requests = new ArrayList<>();

    /**
     * Specifies a new peer (likely a client)
     * @param ipAddress
     * @param port
     */
    public Peer(String ipAddress, int port) {
        mIpAddress = ipAddress;
        mPort = port;
        touch();
    }

    /**
     * Specifies a new peer (likely a server)
     * @param ipAddress
     * @param port
     * @param serviceName the name of the service. This is deprecated
     * @param version
     */
    public Peer(String ipAddress, int port, String serviceName, int version) {
        mIpAddress = ipAddress;
        mPort = port;
        mServiceName = serviceName;
        mVersion = version;
        touch();
    }

    /**
     * Returns the ip address of the peer
     * @return
     */
    public String getIpAddress() {
        return mIpAddress;
    }

    /**
     * Returns the port number of the peer
     * @return
     */
    public int getPort() {
        return mPort;
    }

    /**
     * Returns the name of the service that was advertized by the peer.
     * This may be blank in the case of clients
     * @return
     */
    public String getServiceName() {
        return mServiceName;
    }

    /**
     * Returns the version of the service being offered (app version).
     * This may be 0 in the case of clients
     * @return
     */
    public int getVersion() {
        return mVersion;
    }

    /**
     * Returns the time when we last saw this peer
     * @return
     */
    public long getLastSeenAt() {
        return mLastSeenAt;
    }

    /**
     * Changes the timestamp on the peer so we know when we last saw it.
     */
    public void touch() {
        mLastSeenAt = System.currentTimeMillis();
    }

    /**
     * Checks if the connection to this peer is secured by an encryption key
     * @return
     */
    public boolean isSecure() {
        return isSecure;
    }

    /**
     * Checks if this peer has shared it's identity
     * @return
     */
    public boolean hasIdentity() {
        return hasIdentity;
    }

    /**
     * Checks if the peer has permission to connect
     * @return
     */
    public boolean isAuthorized() {
        return mIsAuthorized;
    }

    /**
     * Sets the authorization status of the peer
     * @param isAuthorized
     */
    public void setIsAuthorized(boolean isAuthorized) {
        mIsAuthorized = isAuthorized;
    }

    /**
     * Indicates whether or not the connection to this peer is secured
     * with an encryption key.
     * @param secure
     */
    public void setIsSecure(Boolean secure) {
        isSecure = secure;
    }

    /**
     * Indicates if this connection has shared it's identity
     * @param hasIdentity
     */
    public void setHasIdentity(boolean hasIdentity) {
        this.hasIdentity = hasIdentity;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDevice(String device) {
        this.deviceType = device;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getDevice() {
        return deviceType;
    }

    public String getId() {
        return id;
    }

    /**
     * Queues a request to be approved by the user
     * @param request
     */
    public void queueRequest(Request request) {
        this.requests.add(request);
    }

    /**
     * Removes a request from the approval queue
     * @param request
     */
    public void dismissRequest(Request request) {
        this.requests.remove(request);
    }

    /**
     * Returns an array of requests that are pending approval
     * @return
     */
    public Request[] getRequests() {
        return this.requests.toArray(new Request[this.requests.size()]);
    }
}
