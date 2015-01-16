package com.door43.translationstudio.events;

import com.door43.translationstudio.network.Peer;

/**
 * Created by joel on 1/16/2015.
 */
public class LostServerPeerEvent {

    private final Peer mServer;

    public LostServerPeerEvent(Peer server) {
        mServer = server;
    }

    /**
     * Returns the server peer
     * @return
     */
    public Peer getServer() {
        return mServer;
    }
}
