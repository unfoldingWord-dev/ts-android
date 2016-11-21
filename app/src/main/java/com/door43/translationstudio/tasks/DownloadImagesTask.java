package com.door43.translationstudio.tasks;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.tools.taskmanager.ManagedTask;

/**
 * Created by jshuma on 1/11/16.
 */
public class DownloadImagesTask extends ManagedTask {

    public static final String TASK_ID = "download_images_task";

    private final Door43Client mLibrary;
    private int mMaxProgress = 100;
    private boolean mSuccess;

    public DownloadImagesTask() {
        mLibrary = App.getLibrary();
    }

    @Override
    public void start() {
//        mSuccess = mLibrary.downloadImages(new Library.OnProgressListener() {
//
//            @Override
//            public boolean onProgress(int progress, int max) {
//                mMaxProgress = max;
//                String message = String.format("%2.2f %s %2.2f %s",
//                        progress / (1024f * 1024f),
//                        App.context().getResources().getString(R.string.out_of),
//                        max / (1024f * 1024f),
//                        App.context().getResources().getString(R.string.mb_downloaded));
//                publishProgress((float)progress / (float)max, message);
//                return !isCanceled();
//            }
//
//            @Override
//            public boolean onIndeterminate() {
//                publishProgress(-1, "");
//                return !isCanceled();
//            }
//        });

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
}
