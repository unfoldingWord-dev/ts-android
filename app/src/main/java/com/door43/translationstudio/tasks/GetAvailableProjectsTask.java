package com.door43.translationstudio.tasks;

import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.threads.ManagedTask;

import java.util.List;

/**
 * Gets a list of projects that are available for download from the server
 */
public class GetAvailableProjectsTask extends ManagedTask {
    private List<Model> mProjects;

    @Override
    public void start() {
        mProjects = AppContext.projectManager().fetchAvailableProjects();
    }

    /**
     * Returns the available projects
     * @return
     */
    public List<Model> getProjects() {
        return mProjects;
    }
}
