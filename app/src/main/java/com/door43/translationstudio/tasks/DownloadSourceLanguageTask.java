package com.door43.translationstudio.tasks;

import com.door43.translationstudio.App;
import com.door43.translationstudio.core.Resource;
import com.door43.translationstudio.core.SourceTranslation;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.tools.taskmanager.ManagedTask;

/**
 * This task downloads the source language data from the server.
 */
@Deprecated
public class DownloadSourceLanguageTask extends ManagedTask {

    public static final String TASK_ID = "download_source_language_id";

    private final String mProjectId;
    private final String mSourceLanguageId;
    private final Door43Client mLibrary;
    private int mMaxProgress = 1;
    private boolean mSuccess;
    private int mTaskProgress = 0;

    /**
     * Creates a new task to download a source language
     * @param projectId
     * @param sourceLanguageId
     */
    public DownloadSourceLanguageTask(String projectId, String sourceLanguageId) {
        mLibrary = App.getLibrary();
        mProjectId = projectId;
        mSourceLanguageId = sourceLanguageId;
    }

    /**
     * NOTE: the project, source language, and resources catalogs will have been downloaded when the
     * user first opens the download manager. So we do not need to download them again here.
     */
    @Override
    public void start() {
        publishProgress(-1, "");
//        final Resource[] resources = mLibrary.getResources(mProjectId, mSourceLanguageId);
//
//        mSuccess = true;
//        for(int i = 0; i < resources.length; i ++) {
//            // TODO: hook up progress listener
//            boolean status = mLibrary.downloadSourceTranslation(SourceTranslation.simple(mProjectId, mSourceLanguageId, resources[i].getId()), new Library.OnProgressListener() {
//                @Override
//                public boolean onProgress(int progress, int max) {
//                    mMaxProgress = resources.length * max;
//                    mTaskProgress ++;
//                    publishProgress(mTaskProgress, "");
//                    return !isCanceled();
//                }
//
//                @Override
//                public boolean onIndeterminate() {
//                    publishProgress(-1, "");
//                    return !isCanceled();
//                }
//            });
//            if(!status) {
//                mSuccess = status;
//            }
//        }
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

    /**
     * Returns the id of the project for the source language
     * @return
     */
    public String getProjectId() {
        return mProjectId;
    }

    /**
     * Returns the id of the source language that was downloaded
     * @return
     */
    public String getSourceLanguageId() {
        return mSourceLanguageId;
    }
}
