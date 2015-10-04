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
    private int mMaxProgress = 100;
    private LibraryUpdates mUpdates = null;

    @Override
    public void start() {
        publishProgress(-1, "");

        Library library = AppContext.getLibrary();
        mUpdates = library.getAvailableLibraryUpdates(new Library.OnProgressListener() {
            @Override
            public void onProgress(int progress, int max) {
                mMaxProgress = max;
                publishProgress(progress, "");
            }

            @Override
            public void onIndeterminate() {
                publishProgress(-1, "");
            }
        });
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
