package com.door43.translationstudio.tasks;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.AppContext;
import com.door43.util.tasks.ManagedTask;

/**
 * This tasks extracts the packaged library index into the assets directory
 */
public class InitializeLibraryTask extends ManagedTask {
    public static final String TASK_ID = "deploy_library_task";

    @Override
    public void start() {
        try {
            AppContext.deployDefaultLibrary();
        } catch (Exception e) {
            Logger.e(this.getClass().getName(), "Failed to deploy the default index", e);
        }
    }
}
