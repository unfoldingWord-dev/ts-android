package com.door43.translationstudio.tasks;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Chapter;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Resource;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.projects.data.IndexStore;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.threads.ManagedTask;

/**
 * Created by joel on 5/1/2015.
 */
public class LoadFramesTask extends ManagedTask {

    private final Project mProject;
    private final SourceLanguage mLanguage;
    private final Resource mResource;
    private Chapter mChapter;
    private int mMaxProgress = 100;
    public static final String TASK_ID = "load_frames";

    public LoadFramesTask(Project p, SourceLanguage l, Resource r, Chapter c) {
        mProject = p;
        mLanguage = l;
        mResource = r;
        mChapter = c;
    }

    @Override
    public void start() {
        publishProgress(-1, AppContext.context().getResources().getString(R.string.title_frames));
        IndexStore.loadFrames(mProject, mLanguage, mResource, mChapter);
    }

    @Override
    public int maxProgress() {
        return mMaxProgress;
    }
}
