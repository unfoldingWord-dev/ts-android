package com.door43.translationstudio.tasks;

import com.door43.translationstudio.AppContext;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import java.io.File;

/**
 * Created by blm on 5/17/16.
 */
public class ImportProjectsTask extends ManagedTask {

    public static final String TASK_ID = "imports_projects_task";

    private File projectsFolder;
    private boolean overwrite;
    private String[] importedSlugs;

    public ImportProjectsTask(File projectsFolder, boolean overwrite) {
        this.projectsFolder = projectsFolder;
        this.overwrite = overwrite;
    }

    @Override
    public void start() {
        try {
            importedSlugs = AppContext.getTranslator().importArchive(projectsFolder, overwrite);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String[] getImportedSlugs() {
        return importedSlugs;
    }
}