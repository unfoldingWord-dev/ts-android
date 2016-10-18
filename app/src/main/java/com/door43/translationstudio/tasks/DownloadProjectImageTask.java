package com.door43.translationstudio.tasks;

import org.unfoldingword.tools.taskmanager.ManagedTask;

import org.unfoldingword.resourcecontainer.Project;

/**
 * Downloads a project icon from the server or loads it from the cache
 */
@Deprecated
public class DownloadProjectImageTask extends ManagedTask {
    private final Project mProject;
    private String mImagPath;

    public DownloadProjectImageTask(Project p) {
        mProject = p;
    }

    @Override
    public void start() {
        publishProgress(-1, "");
        mImagPath =  null;//App.projectManager().downloadProjectImage(mProject, false);
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
