package com.door43.translationstudio.tasks;

import org.unfoldingword.tools.logger.Logger;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.AppContext;
import org.unfoldingword.tools.taskmanager.ManagedTask;

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

        Library library = AppContext.getLibrary();
        try {
            library.downloadAllProjects(new Library.OnProgressListener() {
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
            }, new Library.OnProgressListener() {
                @Override
                public boolean onProgress(int progress, int max) {
                    float relativeProgress = (float)progress / (float)max * (float)mMaxProgress;
                    publishProgress(relativeProgress, "", true);
                    return !isCanceled();
                }

                @Override
                public boolean onIndeterminate() {
                    return !isCanceled();
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
