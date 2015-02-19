package com.door43.translationstudio.util;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.projects.Navigator;
import com.door43.translationstudio.projects.ProjectManager;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

/**
 * This is sort of a hack to provide the main application context to all classes
 * without having to manually pass the context to them.
 */
public class AppContext {
    private static MainThreadBus mEventBus;
    private static MainApplication mContext;
    private static Navigator mNavigator;
    private static ProjectManager mProjectManager;

    /**
     * Initializes the basic functions context.
     * @param context The application context. This can only be set once.
     */
    public AppContext(MainApplication context) {
        if(mContext == null) {
            mContext = context;
            mProjectManager = new ProjectManager(context);
            mNavigator = new Navigator(context, mProjectManager);
        }
    }

    /**
     * Returns the main application context
     * @return
     */
    public static MainApplication context() {
        return mContext;
    }

    /**
     * Returns the global event bus
     * @return
     */
    public static Bus getEventBus() {
        if(mEventBus == null) {
            mEventBus = new MainThreadBus(ThreadEnforcer.ANY);
        }
        return mEventBus;
    }

    /**
     * Returns the global project manager
     * @return
     */
    public static ProjectManager projectManager() {
        return mProjectManager;
    }
}
