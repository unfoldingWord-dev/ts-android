package com.door43.delegate;

/*
 * Provides an interface for interacting with delegates.
 * Delegate listeners may register to receive messages from any number of delegate senders.
 */
public interface DelegateListener {

    /**
     * Receives a response from the delegate sender
     * @param id the id specified by the listener when it registered itself with the listener
     * @param response the delegate response sent by the delegate sender. You can determine which
     *                 message has been sent by comparing it's class with a known delegat response class
     *                 e.g. boolean match = response.getClass().equals(MyCustomDelegateResponse.class);.
     */
    void onDelegateResponse(String id, DelegateResponse response);
}
