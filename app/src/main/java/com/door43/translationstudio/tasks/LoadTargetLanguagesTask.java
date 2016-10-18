package com.door43.translationstudio.tasks;

import com.door43.translationstudio.App;

import org.unfoldingword.tools.taskmanager.ManagedTask;

/**
 * Performs the initial loading of the target languages.
 *
 */
@Deprecated
public class LoadTargetLanguagesTask extends ManagedTask {
    public static final String TASK_ID = "load_target_languages";

    // TODO: 9/30/16 this task has no purpose anymore
    @Override
    public void start() {
        App.getLibrary().index().getTargetLanguages();
    }
}
