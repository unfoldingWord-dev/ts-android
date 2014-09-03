package com.door43.delegate;

import java.util.HashMap;
import java.util.Map;

/**
 * The delegate sender manages all of the listener registrations and is responsible for issueing
 * messages to the registered listeners. This allows for asyncronous communication between objects.
 * Care shouild be taken to unregister an object before it is destroyed so that the garbage collector
 * will properly dispose of it's memory.
 */
public abstract class DelegateSender {
    protected Map<DelegateListener, String> delegateListeners = new HashMap<DelegateListener, String>();

    /**
     * Adds a new listener to the delegate
     * @param listener the delegate listener that will receive notifications from this delegate sender
     */
    public void registerDelegateListener(DelegateListener listener) {
        registerDelegateListener(listener, null);
    }

    /**
     * Adds a new listener to the delegate
     * @param listener the delegate listener that will receive notifications from this delegate sender
     * @param id an id so the listener can identify the delegate sender in the response
     */
    public void registerDelegateListener(DelegateListener listener, String id) {
        if (!delegateListeners.containsKey(listener)) {
            delegateListeners.put(listener, id);
            onRegisteredListener(listener, id);
        }
    }

    /**
     * Removes the delegate listeners
     * @param listener the delegate listener that will no longer receive notifications from this delegate sender
     */
    public void removeDelegateListener(DelegateListener listener) {
        if (delegateListeners.containsKey(listener)) {
            delegateListeners.remove(listener);
        }
    }

    /**
     * Sends the response to the delegate listeners
     * @param response the response to be sent
     */
    protected void issueDelegateResponse(DelegateResponse response) {
        for (Map.Entry<DelegateListener, String> entry : delegateListeners.entrySet()){
            entry.getKey().onDelegateResponse(entry.getValue(), response);
        }
    }

    /**
     * Triggered when a new listener has registered
     * @param listener the delegate listener that registered to receive notifications
     * @param id an id for the listener to identify delegates
     */
    protected void onRegisteredListener(DelegateListener listener, String id) {

    }
}
