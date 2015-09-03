package com.door43.translationstudio.tasks;

import com.door43.translationstudio.projects.ProjectManager;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.tasks.ManagedTask;

/**
 * Created by joel on 5/4/2015.
 */
public class LoadTargetLanguagesTask extends ManagedTask {
    public static final String TASK_ID = "load_target_languages";

    @Override
    public void start() {
        AppContext.getLibrary().getTargetLanguages();
    }
}
