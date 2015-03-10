package com.door43.translationstudio.tasks;

import com.door43.translationstudio.projects.ProjectManager;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.threads.ManagedTask;

/**
 * Created by joel on 3/9/2015.
 */
public class DownloadProjectCatalogTask extends ManagedTask {
    private OnProgressListener mPrimaryListener;
    private OnProgressListener mSecondaryListener;

    @Override
    public void start() {
        AppContext.projectManager().downloadNewProjects(new ProjectManager.OnProgressCallback() {
            @Override
            public void onProgress(final double progress, final String message) {
                if(mPrimaryListener != null) {
                    mPrimaryListener.onProgress(progress, message);
                }
            }

            @Override
            public void onSuccess() {

            }
        }, new ProjectManager.OnProgressCallback() {
            @Override
            public void onProgress(final double progress, final String message) {
                if(mSecondaryListener != null) {
                    mSecondaryListener.onProgress(progress, message);
                }
            }

            @Override
            public void onSuccess() {

            }
        });
    }

    /**
     * Sets the listener to be called on progress updates.
     * @param primaryListener
     */
    public void setProgressListener(OnProgressListener primaryListener, OnProgressListener secondaryListener) {
        mPrimaryListener = primaryListener;
        mSecondaryListener = secondaryListener;
    }

    public static interface OnProgressListener {
        public void onProgress(double progress, String message);
    }
}
