package com.door43.translationstudio.tasks;

import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Resource;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.projects.data.DataStore;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.tasks.ManagedTask;

/**
 * Created by joel on 5/4/2015.
 */
public class LoadTermsTask extends ManagedTask {

    private final Project mProject;
    private final Resource mResource;
    private final SourceLanguage mLanguage;
    private int mMaxProgress = 100;
    public static final String TASK_ID = "load_terms";

    public LoadTermsTask(Project p, SourceLanguage l, Resource r) {
        mProject = p;
        mLanguage = l;
        mResource = r;
    }

    @Override
    public void start() {
        publishProgress(-1, "");
        String terms = DataStore.pullTerms(mProject.getId(), mLanguage.getId(), mResource.getId(), false, false);
        AppContext.projectManager().loadTerms(terms, mProject);
    }

    @Override
    public int maxProgress() {
        return mMaxProgress;
    }
}
