package com.door43.translationstudio.tasks;

import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Resource;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.threads.ManagedTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Gets a list of projects that are available for download from the server
 */
public class GetAvailableProjectsTask extends ManagedTask {
    private List<Project> mProjects = new ArrayList<>();
    private OnProgress mListener;

    @Override
    public void start() {
        // download (or load from cache) the project list
        List<Project> projects = AppContext.projectManager().downloadProjectList(false);

        // download (or load from cache) the source languages
        int i = 0;
        for(Project p:projects) {
            if(mListener != null) {
                mListener.onProgress(i / (double)projects.size(), p.getId());
            }
            // update the project details
            Project oldProject = AppContext.projectManager().getProject(p.getId());
            if(p.getDateModified() > oldProject.getDateModified()) {
                AppContext.projectManager().mergeProject(p.getId());
                AppContext.projectManager().reloadProject(p.getId());
            }

            // download source language
            List<SourceLanguage> languages = AppContext.projectManager().downloadSourceLanguageList(p, false);

            // download (or load from cache) the resources
            for(SourceLanguage l:languages) {
                List<Resource> resources = AppContext.projectManager().downloadResourceList(p, l, false);
                for(Resource r:resources) {
                    l.addResource(r);
                }
            }
            if(languages.size() > 0) {
                mProjects.add(p);
            }
            i++;
        }
    }

    /**
     * Returns the available projects
     * @return
     */
    public List<Project> getProjects() {
        return mProjects;
    }

    /**
     * Attaches a progress listener to the task
     * @param listener
     */
    public void setOnProgressListener(OnProgress listener) {
        mListener = listener;
        if(isFinished() && mListener != null) {
            mListener.onProgress(1, "");
        }
    }

    public static interface OnProgress {
        public void onProgress(double progress, String message);
    }
}
