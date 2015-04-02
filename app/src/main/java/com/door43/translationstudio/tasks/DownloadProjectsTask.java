package com.door43.translationstudio.tasks;

import android.util.Log;

import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Resource;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.threads.ManagedTask;

import java.util.List;

/**
 * This task downloads a list of projects
 */
public class DownloadProjectsTask extends ManagedTask {

    private final List<Project> mProjects;
    private double mProgress = -1;

    public DownloadProjectsTask(List<Project> projects) {
        mProjects = projects;
    }

    /**
     * NOTE: the project, source language, and resources catalogs will have been downloaded when the
     * user first opens the download manager. So we do not need to download them again here.
     */
    @Override
    public void start() {
        // download projects
        boolean ignoreCache = false;
        publishProgress(-1, "");

        for(int i = 0; i < mProjects.size(); i ++) {
            if(interrupted()) return;
            Project p = mProjects.get(i);
            // import the project
            AppContext.projectManager().mergeProject(p.getId());

            List<SourceLanguage> languages = p.getSourceLanguages();
            for(int j = 0; j < languages.size(); j ++) {
                if(interrupted()) return;
                SourceLanguage l = languages.get(j);
                if(l.checkingLevel() >= AppContext.minCheckingLevel()) {
                    // import the language
                    AppContext.projectManager().mergeSourceLanguage(p.getId(), l.getId());

                    Resource[] resources = l.getResources();
                    for(int k = 0; k < resources.length; k ++) {
                        if(interrupted()) return;
                        Resource r = resources[k];
                        if(r.getCheckingLevel() >= AppContext.minCheckingLevel()) {
                            // import the resource
                            AppContext.projectManager().mergeResource(p.getId(), l.getId(), r.getId());

                            // download notes
                            publishProgress((i + (j + (k + .3)/(double)resources.length)/(double)languages.size())/(double)mProjects.size(), p.getId());
                            AppContext.projectManager().downloadNotes(p, l, r, ignoreCache);
                            AppContext.projectManager().mergeNotes(p.getId(), l.getId(), r);

                            // download terms
                            publishProgress((i + (j + (k + .6)/(double)resources.length)/(double)languages.size())/(double)mProjects.size(), p.getId());
                            AppContext.projectManager().downloadTerms(p, l, r, ignoreCache);
                            AppContext.projectManager().mergeTerms(p.getId(), l.getId(), r);

                            // download source
                            publishProgress((i + (j + (k + .9)/(double)resources.length)/(double)languages.size())/(double)mProjects.size(), p.getId());
                            AppContext.projectManager().downloadSource(p, l, r, ignoreCache);
                            AppContext.projectManager().mergeSource(p.getId(), l.getId(), r);

                            publishProgress((i + (j + (k + 1)/(double)resources.length)/(double)languages.size())/(double)mProjects.size(), p.getId());
                        }
                    }
                }
            }
            // reload project
            if(interrupted()) return;
            AppContext.projectManager().reloadProject(p.getId());
        }
        publishProgress(1, "");
    }
}
