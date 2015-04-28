package com.door43.translationstudio.tasks;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Chapter;
import com.door43.translationstudio.projects.Frame;
import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Resource;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.projects.data.IndexStore;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.threads.ManagedTask;

import java.io.File;

/**
 * Indexes the the the notes, terms, and source of a resource.
 * NOTE: this does not index the resource object, just the raw sources
 */
public class IndexResourceTask extends ManagedTask {

    public static final String TASK_INDEX = "index_resource";
    private final Resource mResource;
    private final SourceLanguage mLanguage;
    private final Project mProject;

    public IndexResourceTask(Project p, SourceLanguage l, Resource r) {
        // TRICKY: we create a new project so we don't override anything the user is working on.
        mProject = p.softClone();
        mLanguage = l;
        mResource = r;
    }

    @Override
    public void start() {
        File resourceDir = IndexStore.getResourceDir(mProject, mLanguage, mResource);
        resourceDir.mkdirs();

        // load the source
        AppContext.projectManager().fetchProjectSource(mProject, mLanguage, mResource, false);

        int cIndex = -1;
        for(Chapter c: mProject.getChapters()) {
            cIndex ++;
            if(interrupted()) return;

            IndexStore.index(c);

            int fIndex = -1;
            for(Model m:c.getFrames()) {
                fIndex ++;
                if(interrupted()) return;

                Frame f = (Frame)m;
                publishProgress((cIndex + (fIndex + 1) / (double) c.getFrames().length) / (double) mProject.getChapters().length, AppContext.context().getResources().getString(R.string.indexing_projects));
                IndexStore.index(f);
            }
        }

        mProject.flush();
    }
}
