package com.door43.translationstudio.tasks;

import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.util.tasks.ManagedTask;

/**
 * Created by joel on 5/20/16.
 */
public class CalculateTargetTranslationProgressTask extends ManagedTask {

    public static final String TASK_ID = "calculate_target_translation_progress";
    public final TargetTranslation targetTranslation;
    private final Library library;
    public int translationProgress = 0;

    public CalculateTargetTranslationProgressTask(Library library, TargetTranslation targetTranslation) {
        this.library = library;
        this.targetTranslation = targetTranslation;
    }

    @Override
    public void start() {
        translationProgress = Math.round(library.getTranslationProgress(targetTranslation) * 100);
        translationProgress = translationProgress > 100 ? 100 : (translationProgress < 0 ? 0 : translationProgress);
    }
}
