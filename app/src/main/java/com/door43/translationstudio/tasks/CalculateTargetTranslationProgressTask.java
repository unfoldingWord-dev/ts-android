package com.door43.translationstudio.tasks;

import com.door43.translationstudio.App;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetTranslation;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.logger.Logger;
import org.unfoldingword.tools.taskmanager.ManagedTask;

/**
 * Created by joel on 5/20/16.
 */
public class CalculateTargetTranslationProgressTask extends ManagedTask {

    public static final String TASK_ID = "calculate_target_translation_progress";
    public final TargetTranslation targetTranslation;
    private final Door43Client library;
    public int translationProgress = 0;

    public CalculateTargetTranslationProgressTask(Door43Client library, TargetTranslation targetTranslation) {
        this.library = library;
        this.targetTranslation = targetTranslation;
    }

    @Override
    public void start() {
        String[] sourceTranslationIds = App.getSelectedSourceTranslations(targetTranslation.getId());
        if(sourceTranslationIds.length > 0 && library.exists(sourceTranslationIds[0])) {
            ResourceContainer container;
            try {
                container = library.open(sourceTranslationIds[0]);
            } catch (Exception e) {
                Logger.e("CalculateTranslationProgressTask", "Failed to load container " + sourceTranslationIds[0], e);
                return;
            }

            // count translatable items
            int numAvailable = 0;
            for(String chapterSlug:container.chapters()) {
                numAvailable += container.chunks(chapterSlug).length;
            }

            // count translated items
            int numFinished = targetTranslation.numFinished();

            translationProgress = Math.round((float)numAvailable / (float)numFinished * 100);
            if(translationProgress > 100) translationProgress = 100;
            if(translationProgress < 0) translationProgress = 0;
        }
    }
}
