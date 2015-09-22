package com.door43.translationstudio.tasks;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Resource;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.projects.data.IndexStore;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.tasks.ManagedTask;

import java.util.List;

/**
 * This task downloads the source language data from the server.
 */
public class DownloadLanguageTask extends ManagedTask {

    private final Project mProject;
    private final SourceLanguage mLanguage;
    private int mMaxProgress = 1;

    /**
     * Creates a new task to download a source language
     * @param project
     * @param language
     */
    public DownloadLanguageTask(Project project, SourceLanguage language) {
        mProject = project;
        mLanguage = language;
    }

    /**
     * NOTE: the project, source language, and resources catalogs will have been downloaded when the
     * user first opens the download manager. So we do not need to download them again here.
     */
    @Override
    public void start() {
        // download resources
        boolean ignoreCache = false;
        publishProgress(-1, "");

        // merge the new project and languages catalog
        AppContext.projectManager().mergeProject(mProject.getId());
        AppContext.projectManager().mergeSourceLanguage(mProject.getId(), mLanguage.getId());

        // download resources
        List<Resource> resources = AppContext.projectManager().downloadResourceList(mProject, mLanguage, ignoreCache);
        mMaxProgress = resources.size();
        for(int i = 0; i < resources.size(); i ++) {
            if(interrupted()) return;
            Resource r = resources.get(i);
            AppContext.projectManager().mergeResource(mProject.getId(), mLanguage.getId(), r.getId());

            // notes
            publishProgress(((i + 1) / (double) resources.size()) * .25, "");
            AppContext.projectManager().downloadNotes(mProject, mLanguage, r, ignoreCache);
            AppContext.projectManager().mergeNotes(mProject.getId(), mLanguage.getId(), r);

            // terms
            publishProgress(((i + 1) / (double) resources.size()) * .50, "");
            AppContext.projectManager().downloadTerms(mProject, mLanguage, r, ignoreCache);
            AppContext.projectManager().mergeTerms(mProject.getId(), mLanguage.getId(), r);

            // source
            publishProgress(((i + 1) / (double) resources.size()) * .75, "");
            AppContext.projectManager().downloadSource(mProject, mLanguage, r, ignoreCache);
            AppContext.projectManager().mergeSource(mProject.getId(), mLanguage.getId(), r);

            // questions
            publishProgress(((i + 1) / (double) resources.size()) * .9, "");
            AppContext.projectManager().downloadQuestions(mProject, mLanguage, r, ignoreCache);
            AppContext.projectManager().mergeQuestions(mProject.getId(), mLanguage.getId(), r);

            publishProgress((i + 1) / (double) resources.size(), "");
            mLanguage.addResource(r);
        }
        publishProgress(-1, "");
        // reload project
        if(interrupted()) return;
        // TODO: only delete the index if there were changes
        publishProgress(-1, AppContext.context().getResources().getString(R.string.indexing));
        IndexStore.destroy(mProject, mLanguage);
        delegate(new IndexProjectsTask(mProject));
        Project currentProject = AppContext.projectManager().getSelectedProject();
        // index resources of current project
        if(currentProject != null && currentProject.getId().equals(mProject.getId()) && currentProject.hasSelectedSourceLanguage() && currentProject.getSelectedSourceLanguage().getId().equals(mLanguage.getId())) {
            delegate(new IndexResourceTask(currentProject, currentProject.getSelectedSourceLanguage(), currentProject.getSelectedSourceLanguage().getSelectedResource()));
        }
        AppContext.projectManager().reloadProject(mProject.getId());
    }

    @Override
    public int maxProgress() {
        return mMaxProgress;
    }

    /**
     * Returns the id of the project of the language
     * @return
     */
    public Project getProject() {
        return mProject;
    }

    /**
     * Returns the id of the language that was downloaded
     * @return
     */
    public SourceLanguage getLanguage() {
        return mLanguage;
    }
}
