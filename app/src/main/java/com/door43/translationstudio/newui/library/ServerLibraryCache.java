package com.door43.translationstudio.newui.library;

import com.door43.translationstudio.core.LibraryUpdates;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.ProjectManager;
import com.door43.translationstudio.util.AppContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class handles the temporary list of available projects
 */
public class ServerLibraryCache {
    // TODO: this should be a setting in the user preferences
    private static final int CACHE_TTL = 10 * 60 * 1000; // the cache will expire every 10 minutes.
    private static LibraryUpdates mAvailableLibraryUpdates;
    private static int mCacheTimestamp = 0;

    /**
     * Sets the available updates
     * @param availableLibraryUpdates
     */
    public static void setAvailableUpdates(LibraryUpdates availableLibraryUpdates) {
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
     * @return
     */
    public static boolean isExpired() {
        return (mAvailableLibraryUpdates == null || Math.abs(mCacheTimestamp - (int)System.currentTimeMillis()) > CACHE_TTL);
    }

    /**
     * Clears the server cache
     */
    public static void clear() {
        mAvailableLibraryUpdates = null;
        mCacheTimestamp = 0;
    }
}
