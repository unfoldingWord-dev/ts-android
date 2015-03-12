package com.door43.translationstudio.tasks;

import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.Project;
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
        for(Project p:projects) {
            if(mListener != null) {
                mListener.onProgress(projects.size() / 100, p.getId());
            }
            List<SourceLanguage> languages = AppContext.projectManager().downloadSourceLanguageList(p, false);
            if(languages.size() > 0) {
                mProjects.add(p);
            }
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
