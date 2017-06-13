package com.door43.translationstudio.tasks;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.DownloadImages;

import org.unfoldingword.tools.logger.Logger;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import java.io.File;

/**
 * Created by jshuma on 1/11/16.
 */
public class DownloadImagesTask extends ManagedTask {

    public static final String TASK_ID = "download_images_task";

    private int mMaxProgress = 100;
    private boolean mSuccess;
    private File mImagesDir;

    public DownloadImagesTask() {
        mSuccess = false;
        mImagesDir = null;
    }

    @Override
    public void start() {
        mSuccess = false;
        try {
            DownloadImages downloadImages = new DownloadImages();
            mSuccess = downloadImages.download(new DownloadImages.OnProgressListener() {

                @Override
                public boolean onProgress(int progress, int max, String message) {
                    mMaxProgress = max;
                    publishProgress((float) progress / (float) max, message);
                    return !isCanceled();
                }

                @Override
                public boolean onIndeterminate() {
                    publishProgress(-1, "");
                    return !isCanceled();
                }
            });
            mImagesDir = downloadImages.getImagesDir();
        } catch (Exception e) {
            Logger.e(this.getClass().getSimpleName(),"Download Failed", e);
        }

        publishProgress(-1, "");
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

    public File getImagesDir() {
        return mImagesDir;
    }
}
