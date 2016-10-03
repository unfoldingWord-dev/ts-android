package com.door43.translationstudio.tasks;

import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;

import org.unfoldingword.tools.taskmanager.ManagedTask;

/**
 * This tasks extracts the packaged library index into the assets directory
 */
@Deprecated
public class InitializeLibraryTask extends ManagedTask {
    public static final String TASK_ID = "deploy_library_task";

    @Override
    public void start() {
        try {
            App.deployDefaultLibrary();
        } catch (Exception e) {
            Logger.e(this.getClass().getName(), "Failed to deploy the default index", e);
        }
    }
}
