package com.door43.translationstudio.newui.library;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.LibraryUpdates;
import com.door43.translationstudio.AppContext;
import com.door43.util.Security;

/**
 * This class handles the temporary list of available projects
 */
public class ServerLibraryCache {
    // TODO: this should be a setting in the user preferences
    private static final int CACHE_TTL = 60 * 60 * 1000; // the cache will expire every hour.
    private static LibraryUpdates mAvailableLibraryUpdates;
    private static int mCacheTimestamp = 0;
    private static String mToken;

    /**
     * Sets the available updates
     * @param availableLibraryUpdates
     */
    public static void setAvailableUpdates(LibraryUpdates availableLibraryUpdates) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AppContext.context());
        mToken = Security.md5(prefs.getString(SettingsActivity.KEY_PREF_MEDIA_SERVER, "_"));
        mAvailableLibraryUpdates = availableLibraryUpdates;
        mCacheTimestamp = (int)System.currentTimeMillis();
    }

    /**
     * Returns the available updates
     * @return
     */
    public static LibraryUpdates getAvailableUpdates() {
        return mAvailableLibraryUpdates;
    }

    /**
     * Checks if the library cache has expired
     * The cache expires if enough time passes or if the media server has changed in the user preferences.
     * @return
     */
    public static boolean isExpired() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AppContext.context());
        String token = Security.md5(prefs.getString(SettingsActivity.KEY_PREF_MEDIA_SERVER, "_"));
        boolean serverChanged = !token.equals(mToken);
        if(serverChanged) {
            // if we change the server url we must rebuild cached server index.
//            AppContext.getLibrary().destroyCache();
        }
        boolean expired = (mAvailableLibraryUpdates == null || Math.abs(mCacheTimestamp - (int)System.currentTimeMillis()) > CACHE_TTL);
        return serverChanged || expired;
    }

    /**
     * Clears the server cache
     */
    public static void clear() {
        mAvailableLibraryUpdates = null;
        setExpired();
    }

    /**
     * marks the cache as expired.
     */
    public static void setExpired() {
        mCacheTimestamp = 0;
    }
}
