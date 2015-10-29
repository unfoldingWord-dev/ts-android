package com.door43.translationstudio.tasks;

import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.LibraryUpdates;
import com.door43.translationstudio.AppContext;
import com.door43.util.tasks.ManagedTask;

/**
 * Gets a list of projects that are available for download from the server
 */
public class GetLibraryUpdatesTask extends ManagedTask {

    public static final String TASK_ID  = "get_available_source_translations_task";
    private static final long CACHE_TTL = 1000 * 60 * 60 * 5; // the cache will last 5 hours
    private final boolean ignoreCache;
    private int mMaxProgress = 100;
    private LibraryUpdates mUpdates = null;

    public GetLibraryUpdatesTask() {
        this.ignoreCache = false;
    }

    /**
     *
     * @param ignoreCache if true we will always check again.
     */
    public GetLibraryUpdatesTask(boolean ignoreCache) {
        this.ignoreCache = ignoreCache;
    }

    @Override
    public void start() {
        publishProgress(-1, "");

        Library library = AppContext.getLibrary();
        if(library != null) {
            if (ignoreCache || System.currentTimeMillis() - AppContext.getLastCheckedForUpdates() > CACHE_TTL) {
                mUpdates = library.checkServerForUpdates(new Library.OnProgressListener() {
                    @Override
                    public boolean onProgress(int progress, int max) {
                        mMaxProgress = max;
                        publishProgress(progress, "");
                        return !isCanceled();
                    }

                    @Override
                    public boolean onIndeterminate() {
                        publishProgress(-1, "");
                        return !isCanceled();
                    }
                });
                if(!isCanceled()) {
                    AppContext.setLastCheckedForUpdates(System.currentTimeMillis());
                }
            } else {
                mUpdates = library.getAvailableUpdates();
            }
        }
    }

    @Override
    public int maxProgress() {
        return mMaxProgress;
    }

    /**
     * Returns the library updates
     * @return
     */
    public LibraryUpdates getUpdates() {
        return mUpdates;
    }
}
