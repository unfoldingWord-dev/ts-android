package com.door43.translationstudio.tasks;

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
        Library library = AppContext.getLibrary();
        library.deployDefaultLibrary();
        library.seedDownloadIndex();
    }
}
