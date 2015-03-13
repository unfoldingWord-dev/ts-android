package com.door43.translationstudio.tasks;

import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.threads.ManagedTask;

/**
 * Downloads a project icon from the server or loads it from the cache
 */
public class DownloadProjectImageTask extends ManagedTask {
    private final Project mProject;
    private String mImagPath;

    public DownloadProjectImageTask(Project p) {
        mProject = p;
    }

    @Override
    public void start() {
        mImagPath = AppContext.projectManager().downloadProjectImage(mProject, false);
    }

    /**
     * Returns the path to the downloaded image
     * @return
     */
    public String getImagePath() {
        return mImagPath;
    }
}
