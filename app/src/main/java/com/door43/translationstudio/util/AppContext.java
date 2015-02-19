package com.door43.translationstudio.util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Navigator;
import com.door43.translationstudio.projects.ProjectManager;
import com.door43.translationstudio.translations.TranslationManager;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import java.io.IOException;

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
     * Checks if the package asset exists
     * @param path
     * @return
     */
    public static boolean assetExists(String path) {
        try {
            mContext.getAssets().open(path);
            return true;
        } catch (IOException e) {
            return false;
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

    /**
     * Displays a generic progress dialog in the given activity
     * @param activity
     * @return
     */
    public static ProgressDialog showLoading(final Activity activity) {
        ProgressDialog dialog = new ProgressDialog(activity);
        dialog.setMax(R.string.loading);
        dialog.show();
        return dialog;
    }
}
