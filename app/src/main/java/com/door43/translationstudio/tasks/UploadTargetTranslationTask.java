package com.door43.translationstudio.tasks;

import com.door43.translationstudio.core.TargetTranslation;
import com.door43.util.tasks.ManagedTask;

/**
 * Created by joel on 9/21/2015.
 */
public class UploadTargetTranslationTask extends ManagedTask {
    public static final String TASK_ID = "upload_target_translation";
    private final TargetTranslation mTargetTranslation;

    public UploadTargetTranslationTask(TargetTranslation targetTranslation) {
        mTargetTranslation = targetTranslation;
    }

    @Override
    public void start() {
        // TODO: begin upload
    }
}
