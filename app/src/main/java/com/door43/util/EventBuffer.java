package com.door43.util;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

/**
 * The event buffer provides a simple way to registers objects
 * to receive one-way events.
 *
 * A sample use case would be to register an activity
 * to receive events from a dialog
 */
public class EventBuffer {
    private List<OnEventListener> listeners = new ArrayList<>();

    /**
     * Adds a listener to the event buffer
     * @param listener the listener that will be added
     */
    public final void addOnEventListener(OnEventListener listener) {
        if(!listeners.contains(listener) && listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a listener from the event buffer
     * @param listener the listener that will be removed
     */
    public final void removeOnEventListener(OnEventListener listener) {
        if(listener != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Removes all listeners from the event buffer
     */
    public final void removeAllListeners() {
        listeners.clear();
    }

    /**
     * Writes a new event to the buffer
     * @param talker the class that is doing the talking
     * @param tag an identifier to keep track of the message
     * @param args arguments to send to the listener
     */
    public final void write(OnEventTalker talker, int tag, Bundle args) {
        for(OnEventListener listener:listeners) {
            listener.onEventBufferEvent(talker, tag, args);
        }
    }

    /**
     * The class doing the listener
     */
    public interface OnEventListener {
        /**
         * Called when the event talker issues a new event
         * @param talker the class doing the talking
         * @param tag an identifier to keep track of what message is being set.
         * @param args arguments sent from the dialog
         */
        void onEventBufferEvent(OnEventTalker talker, int tag, Bundle args);
    }

    /**
     * The class doing the talking
     */
    public interface OnEventTalker {

        /**
         * Returns the event buffer for the class
         * @return the event buffer used for communication
         */
        EventBuffer getEventBuffer();
    }
}
