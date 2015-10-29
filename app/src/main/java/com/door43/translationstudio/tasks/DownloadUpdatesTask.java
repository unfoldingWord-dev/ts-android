package com.door43.translationstudio.tasks;

import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.LibraryUpdates;
import com.door43.util.tasks.ManagedTask;

/**
 * Created by joel on 10/28/2015.
 */
public class DownloadUpdatesTask extends ManagedTask {
    public static String TASK_ID = "download_all_updates";
    private final LibraryUpdates availableUpdates;
    private int mMaxProgress = 100;

    public DownloadUpdatesTask(LibraryUpdates availableUpdates) {

        this.availableUpdates = availableUpdates;
    }

    @Override
    public void start() {
        publishProgress(-1, "");

        if(availableUpdates != null) {
            Library library = AppContext.getLibrary();
            library.downloadUpdates(availableUpdates, new Library.OnProgressListener() {
                @Override
                public boolean onProgress(int progress, int max) {
                    mMaxProgress = max;
                    publishProgress(progress, "");
                    return !isCanceled();
                }

                @Override
                public boolean onIndeterminate() {
                    return !isCanceled();
                }
            });
        }
    }

    @Override
    public int maxProgress() {
        return mMaxProgress;
    }
}
