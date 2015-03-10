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
    private String mCatalog;

    @Override
    public void start() {
        mCatalog = AppContext.projectManager().getDataStore().fetchProjectCatalog(false);
    }

    /**
     * Sets the listener to be called on progress updates.
     * @param primaryListener
     */
    public void setProgressListener(OnProgressListener primaryListener, OnProgressListener secondaryListener) {
        mPrimaryListener = primaryListener;
        mSecondaryListener = secondaryListener;
    }

    /**
     * Returns the downloaded catalog contents.
     * @return
     */
    public String getCatalog() {
        return mCatalog;
    }

    public static interface OnProgressListener {
        public void onProgress(double progress, String message);
    }
}
