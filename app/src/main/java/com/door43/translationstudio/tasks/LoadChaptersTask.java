package com.door43.translationstudio.tasks;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Resource;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.projects.data.IndexStore;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.threads.ManagedTask;

/**
 * Created by joel on 4/28/2015.
 */
public class LoadChaptersTask extends ManagedTask {

    private final Project mProject;
    private final SourceLanguage mLanguage;
    private final Resource mResource;
    private int mMaxProgress = 100;
    public String TASK_ID = "load_chapters";

    public LoadChaptersTask(Project p, SourceLanguage l, Resource r) {
        mProject = p;
        mLanguage = l;
        mResource = r;
    }

    @Override
    public void start() {
        publishProgress(-1, AppContext.context().getResources().getString(R.string.title_chapters));
        IndexStore.loadChapters(mProject, mLanguage, mResource);
    }

    @Override
    public int maxProgress() {
        return mMaxProgress;
    }
}
