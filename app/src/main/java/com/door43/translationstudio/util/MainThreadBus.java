package com.door43.translationstudio.util;

import android.os.Handler;
import android.os.Looper;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

/**
 * This is a custom extension of the otto event bus that allows us to send events from non-main threads
 * http://stackoverflow.com/questions/15431768/how-to-send-event-from-service-to-activity-with-otto-event-bus
 */
public class MainThreadBus extends Bus {
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public MainThreadBus() {
        super();
    }

    public MainThreadBus(ThreadEnforcer enforcer) {
        super(enforcer);
    }

    public MainThreadBus(String identifier) {
        super(identifier);
    }

    public MainThreadBus(ThreadEnforcer enforcer, String identifier) {
        super(enforcer, identifier);
    }

    @Override
    public void post(final Object event) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            super.post(event);
        } else {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    MainThreadBus.super.post(event);
                }
            });
        }
    }
}