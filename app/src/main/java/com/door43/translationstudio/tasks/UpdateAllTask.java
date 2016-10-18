package com.door43.translationstudio.tasks;

import com.door43.translationstudio.App;

import org.unfoldingword.tools.taskmanager.ManagedTask;

/**
 * Indexes all of the content in the api
 */

public class UpdateAllTask extends ManagedTask {
    public static final String TASK_ID = "update-all-task";
    private int maxProgress = 0;
    private boolean success = false;

    @Override
    public void start() {
        publishProgress(-1, "");
        UpdateSourceTask sourceTask = new UpdateSourceTask();
        UpdateCatalogsTask catalogsTask = new UpdateCatalogsTask();

        delegate(sourceTask);
        if(!sourceTask.isSuccess()) return;
        delegate(catalogsTask);
        if(!catalogsTask.isSuccess()) return;

        try {
            App.getLibrary().updateChunks(new org.unfoldingword.door43client.OnProgressListener() {
                @Override
                public void onProgress(String tag, long max, long complete) {
                    maxProgress = (int)max;
                    publishProgress((float)complete/(float)max, tag);
                }
            });
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int maxProgress() {
        return maxProgress;
    }

    public boolean isSuccess() {
        return success;
    }
}
