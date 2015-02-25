package com.door43.translationstudio.util;

import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import com.door43.util.Logger;
import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Navigator;
import com.door43.translationstudio.projects.ProjectManager;
import com.door43.translationstudio.translations.TranslationManager;
import com.door43.util.root.Root;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import java.io.File;
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
     * It is very important to initialize the class before using it because it assumes
     * the context has already been set.
     * @param context The application context. This can only be set once.
     */
    public AppContext(MainApplication context) {
        if(mContext == null) {
            mContext = context;
            mTranslationManager = new TranslationManager(context);
            mProjectManager = new ProjectManager(context);
            mNavigator = new Navigator(context, mProjectManager, getEventBus());
            new Logger(context);
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
     * Returns the unique device id for this device
     * @return
     */
    public static String udid() {
        return Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
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
     * Returns the directory in which the ssh keys are stored
     * @return
     */
    public static File getKeysFolder() {
        File folder = new File(mContext.getFilesDir() + "/" + mContext.getResources().getString(R.string.keys_dir) + "/");
        if(!folder.exists()) {
            folder.mkdir();
        }
        return folder;
    }

    /**
     * Checks if the external media is mounted and writeable
     * @return
     */
    public static boolean isExternalMediaAvailable() {
        // TRICKY: KITKAT introduced changes to the external media that made sd cards read only
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) { // || Root.isDeviceRooted()
            StorageUtils.StorageInfo removeableMediaInfo = StorageUtils.getRemoveableMediaDevice();
            return removeableMediaInfo != null;
        } else {
            return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
        }
    }

    /**
     * Returns the file to the external public downloads directory
     * @return
     */
    public static File getPublicDownloadsDirectory() {
        File dir;
        // TRICKY: KITKAT introduced changes to the external media that made sd cards read only
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) { // || Root.isDeviceRooted()
            StorageUtils.StorageInfo removeableMediaInfo = StorageUtils.getRemoveableMediaDevice();
            dir = new File("/storage/" + removeableMediaInfo.getMountName() + "/Download/translationStudio");
        } else {
            dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "translationStudio");
        }
        dir.mkdirs();
        return dir;
    }
}
