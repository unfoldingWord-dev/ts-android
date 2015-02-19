package com.door43.translationstudio.util;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.projects.Navigator;
import com.door43.translationstudio.projects.ProjectManager;
import com.door43.translationstudio.translations.TranslationManager;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

/**
 * This class provides global access to the application context as well as other important tools
 */
public class AppContext {
    private static MainThreadBus mEventBus;
    private static MainApplication mContext;
    private static Navigator mNavigator;
    private static ProjectManager mProjectManager;
    private static TranslationManager mTranslationManager;

    /**
     * Initializes the basic functions context.
     * @param context The application context. This can only be set once.
     */
    public AppContext(MainApplication context) {
        if(mContext == null) {
            mContext = context;
            mTranslationManager = new TranslationManager(context);
            mProjectManager = new ProjectManager(context);
            mNavigator = new Navigator(context, mProjectManager, getEventBus());
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

    /**
     * Returns the global translation manager
     * @return
     */
    public static TranslationManager translationManager() {
        return mTranslationManager;
    }

    /**
     * Returns the global navigation system
     * @return
     */
    public static Navigator navigator() {
        return mNavigator;
    }
}
