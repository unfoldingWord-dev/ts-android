package com.door43.translationstudio.tasks;

import com.door43.translationstudio.AppContext;
import com.door43.util.tasks.ManagedTask;

import java.io.File;

/**
 * Created by blm on 5/17/16.
 */
public class ImportProjectsTask extends ManagedTask {

    public static final String TASK_ID = "imports_projects_task";

    private File projectsFolder;
    private boolean overwrite;
    public boolean mSuccess;

    public ImportProjectsTask(File projectsFolder, boolean overwrite) {
        this.projectsFolder = projectsFolder;
        this.overwrite = overwrite;
        mSuccess = false;
    }

    @Override
    public void start() {
        mSuccess = false;

        try {
            String[] importedSlugs = AppContext.getTranslator().importArchive(projectsFolder, overwrite);
            mSuccess = (importedSlugs.length > 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}