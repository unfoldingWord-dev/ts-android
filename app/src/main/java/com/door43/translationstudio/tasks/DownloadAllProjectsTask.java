package com.door43.translationstudio.tasks;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.AppContext;
import com.door43.util.tasks.ManagedTask;

/**
 * This task downloads a all of the projects
 */
public class DownloadAllProjectsTask extends ManagedTask {
    public static final String TASK_ID = "download_all_projects task";
    private int mMaxProgress = 100;

    /**
     * NOTE: the project, source language, and resources catalogs will have been downloaded when the
     * user first opens the download manager. So we do not need to download them again here.
     */
    @Override
    public void start() {
        // download projects
        publishProgress(-1, "");

        // new download code
        Library library = AppContext.getLibrary();
        try {
            library.downloadAllProjects(new Library.OnProgressListener() {
                @Override
                public void onProgress(int progress, int max) {
                    mMaxProgress = max;
                    publishProgress(progress, "");
                }

                @Override
                public void onIndeterminate() {
                    publishProgress(-1, "");
                }
            }, new Library.OnProgressListener() {
                @Override
                public void onProgress(int progress, int max) {
                    float relativeProgress = (float)progress / (float)max * (float)mMaxProgress;
                    publishProgress(relativeProgress, "", true);
                }

                @Override
                public void onIndeterminate() {
                }
            });
        } catch (Exception e) {
            Logger.e(this.getClass().getName(), "Failed to download the updates", e);
        }
    }

    @Override
    public int maxProgress() {
        return mMaxProgress;
    }
}
