package com.door43.translationstudio.service;

import com.door43.translationstudio.network.Peer;

/**
 * Represents a notice from a peer
 */
public class PeerNotice {

    public final Peer peer;
    public final Request request;

    /**
     * Creates a new notice
     * @param peer the peer that sent the request
     * @param request the request that initiated the notice
     */
    public PeerNotice(Peer peer, Request request) {
        this.peer = peer;
        this.request = request;
    }
}
