package com.door43.translationstudio.util;

import com.squareup.otto.Bus;

/**
 * A helper class to provide decoubled syncrounous event communication
 */
public class EventBus {
    private static final Bus mBus = new Bus();

    /**
     * Returns an instance of the event bus.
     * @return
     */
    public static Bus getInstance() {
        return mBus;
    }
}
