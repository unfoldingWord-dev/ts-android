package com.door43.translationstudio.tasks;

import com.door43.util.threads.ManagedTask;

/**
 * Created by joel on 5/14/2015.
 */
public class ValidateTranslationTask extends ManagedTask {

    public static final String TASK_ID = "validate_translation";

    @Override
    public void start() {
        // TODO: validate the translation
    }

    @Override
    public int maxProgress() {
        return 100;
    }

    /**
     * Checks if the validation results contain any warnings
     * @return
     */
    public boolean hasWarnings() {
        // TODO: implement
        return false;
    }

    /**
     * Checks if the validation results contain any errors
     * @return
     */
    public boolean hasErrors() {
        // TODO: implement
        return false;
    }
}
