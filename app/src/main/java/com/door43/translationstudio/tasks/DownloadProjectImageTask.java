package com.door43.translationstudio.tasks;

import com.door43.translationstudio.core.Project;
import com.door43.util.tasks.ManagedTask;

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
        publishProgress(-1, "");
        mImagPath =  null;//AppContext.projectManager().downloadProjectImage(mProject, false);
    }

    @Override
    public int maxProgress() {
        return 1;
    }

    /**
     * Returns the path to the downloaded image
     * @return
     */
    public String getImagePath() {
        return mImagPath;
    }
}
