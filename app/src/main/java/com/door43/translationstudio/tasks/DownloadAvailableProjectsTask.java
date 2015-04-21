package com.door43.translationstudio.tasks;

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
public class DownloadAvailableProjectsTask extends ManagedTask {
    private final boolean mIgnoreCache;
    private List<Project> mProjects = new ArrayList<>();

    public DownloadAvailableProjectsTask(boolean ignoreCache) {
        mIgnoreCache = ignoreCache;
    }

    @Override
    public void start() {
        // download (or load from cache) the project list
        List<Project> projects = AppContext.projectManager().downloadProjectList(mIgnoreCache);

        // download (or load from cache) the source languages
        int i = 0;
        for(Project p:projects) {
            if(interrupted()) return;
            boolean didUpdateProjectInfo = false;

            publishProgress(i / (double) projects.size(), p.getId());

            // update the project details
            Project oldProject = AppContext.projectManager().getProject(p.getId());
            if(oldProject != null && p.getDateModified() > oldProject.getDateModified()) {
                AppContext.projectManager().mergeProject(p.getId());
                didUpdateProjectInfo = true;
            }

            // download source language
            List<SourceLanguage> languages = AppContext.projectManager().downloadSourceLanguageList(p, mIgnoreCache);

            // download (or load from cache) the resources
            for(SourceLanguage l:languages) {
                if(interrupted()) return;

                // load resources
//                List<Resource> resources = AppContext.projectManager().downloadResourceList(p, l, false);
//                for(Resource r:resources) {
//                    if(interrupted()) return;
//                    l.addResource(r);
//                }

                if(l.checkingLevel() >= AppContext.minCheckingLevel()) {
                    // update the language details
                    if (oldProject != null) {
                        SourceLanguage oldLanguage = oldProject.getSourceLanguage(l.getId());
                        if (oldLanguage != null && l.getDateModified() > oldLanguage.getDateModified()) {
                            AppContext.projectManager().mergeSourceLanguage(p.getId(), l.getId());
                            didUpdateProjectInfo = true;
                        }
                    }
                }
            }

            if(!interrupted()) {
                // reload the project
                if (didUpdateProjectInfo) {
                    AppContext.projectManager().reloadProject(p.getId());
                }

                if (languages.size() > 0) {
                    mProjects.add(p);
                }
            } else {
                return;
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
}
