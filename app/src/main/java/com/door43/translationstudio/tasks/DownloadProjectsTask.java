package com.door43.translationstudio.tasks;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Resource;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.projects.data.IndexStore;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.tasks.ManagedTask;

import java.util.List;

/**
 * This task downloads a list of projects
 */
public class DownloadProjectsTask extends ManagedTask {

    private final List<Project> mProjects;
    private final boolean ignoreCache;
    private double mProgress = -1;
    private int mMaxProgress = 100;

    public DownloadProjectsTask(List<Project> projects) {
        mProjects = projects;
        this.ignoreCache = false;
    }

    public DownloadProjectsTask(List<Project> projects, boolean ignoreCache) {
        mProjects = projects;
        this.ignoreCache = ignoreCache;
    }

    /**
     * NOTE: the project, source language, and resources catalogs will have been downloaded when the
     * user first opens the download manager. So we do not need to download them again here.
     */
    @Override
    public void start() {
        // download projects
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
                            publishProgress((i + (j + (k + .25)/(double)resources.length)/(double)languages.size())/(double)mProjects.size(), p.getId());
                            AppContext.projectManager().downloadNotes(p, l, r, ignoreCache);
                            AppContext.projectManager().mergeNotes(p.getId(), l.getId(), r);

                            // download terms
                            publishProgress((i + (j + (k + .50)/(double)resources.length)/(double)languages.size())/(double)mProjects.size(), p.getId());
                            AppContext.projectManager().downloadTerms(p, l, r, ignoreCache);
                            AppContext.projectManager().mergeTerms(p.getId(), l.getId(), r);

                            // download source
                            publishProgress((i + (j + (k + .75)/(double)resources.length)/(double)languages.size())/(double)mProjects.size(), p.getId());
                            AppContext.projectManager().downloadSource(p, l, r, ignoreCache);
                            AppContext.projectManager().mergeSource(p.getId(), l.getId(), r);

                            // download questions
                            publishProgress((i + (j + (k + .9)/(double)resources.length)/(double)languages.size())/(double)mProjects.size(), p.getId());
                            AppContext.projectManager().downloadQuestions(p, l, r, ignoreCache);
                            AppContext.projectManager().mergeQuestions(p.getId(), l.getId(), r);

                            publishProgress((i + (j + (k + 1)/(double)resources.length)/(double)languages.size())/(double)mProjects.size(), p.getId());
                        }
                    }
                }
            }
            // reload project
            if(interrupted()) return;
            // TODO: only delete the index if there were changes
            publishProgress(-1, AppContext.context().getResources().getString(R.string.indexing));
            IndexStore.destroy(p);
            delegate(new IndexProjectsTask(p));
            Project currentProject = AppContext.projectManager().getSelectedProject();
            // index resources of current project
            if(currentProject != null && currentProject.getId().equals(p.getId()) && currentProject.hasSelectedSourceLanguage()) {
                delegate(new IndexResourceTask(currentProject, currentProject.getSelectedSourceLanguage(), currentProject.getSelectedSourceLanguage().getSelectedResource()));
            }
            AppContext.projectManager().reloadProject(p.getId());
        }
        publishProgress(1, "");
    }

    @Override
    public int maxProgress() {
        return mMaxProgress;
    }
}
