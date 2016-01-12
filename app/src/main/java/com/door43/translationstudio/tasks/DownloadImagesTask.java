package com.door43.translationstudio.tasks;

import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.core.Library;
import com.door43.util.tasks.ManagedTask;

/**
 * Created by jshuma on 1/11/16.
 */
public class DownloadImagesTask extends ManagedTask {

    public static final String TASK_ID = "download_images_id";

    private final Library mLibrary;
    private int mMaxProgress = 1;
    private boolean mSuccess;

    public DownloadImagesTask() {
        mLibrary = AppContext.getLibrary();
    }

    @Override
    public void start() {
        mSuccess = mLibrary.downloadImages(new Library.OnProgressListener() {

            @Override
            public boolean onProgress(int progress, int max) {
                mMaxProgress = max;
                publishProgress((float)progress / max, "");
                return !isCanceled();
            }

            @Override
            public boolean onIndeterminate() {
                publishProgress(-1, "");
                return !isCanceled();
            }
        });

        publishProgress(1., "Finished.");
    }

    @Override
    public int maxProgress() {
        return mMaxProgress;
    }

    /**
     * Checks if the download was a success
     * @return
     */
    public boolean getSuccess() {
        return mSuccess;
    }
}
