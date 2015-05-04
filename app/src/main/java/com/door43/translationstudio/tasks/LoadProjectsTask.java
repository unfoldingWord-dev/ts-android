package com.door43.translationstudio.tasks;

import com.door43.translationstudio.projects.ProjectManager;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.threads.ManagedTask;

/**
 * Created by joel on 5/4/2015.
 */
public class LoadProjectsTask extends ManagedTask {
    public static final String TASK_ID = "load_projects";

    @Override
    public void start() {
        AppContext.projectManager().loadProjects(new ProjectManager.OnProgressListener() {
            @Override
            public void onProgress(double progress, String message) {
                publishProgress(progress, message);
            }

            @Override
            public void onSuccess() {

            }
        });
    }

    @Override
    public int maxProgress() {
        return 100;
    }
}
