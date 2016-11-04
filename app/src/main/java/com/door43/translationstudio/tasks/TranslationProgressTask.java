package com.door43.translationstudio.tasks;

import com.door43.translationstudio.App;
import com.door43.translationstudio.core.TargetTranslation;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.Index;
import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import java.util.List;

/**
 * Calculates the progress of a translation
 */
public class TranslationProgressTask extends ManagedTask {
    public static final String TASK_ID = "translation-progress";
    private double progress = 0.0;

    public final TargetTranslation targetTranslation;

    public TranslationProgressTask(TargetTranslation targetTranslation) {
        this.targetTranslation = targetTranslation;
    }

    @Override
    public void start() {
        Door43Client library = App.getLibrary();
        if(library == null) return;

        // find matching source
        Translation sourceTranslation = getSourceTranslation(library.index, this.targetTranslation);
        if(sourceTranslation == null) return;

        // load source
        ResourceContainer container;
        try {
            container = library.open(sourceTranslation.resourceContainerSlug);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // count chunks
        int numSourceChunks = countChunks(container);
        int numTargetChunks = countChunks(targetTranslation);

        if(numSourceChunks == 0) {
            this.progress = 0.0;
        } else {
            this.progress = (double)numTargetChunks / (double)numSourceChunks;
        }

        // correct invalid values
        if(this.progress > 1) this.progress = 1.0;
    }

    /**
     * Returns the progress of the target translation
     * @return the progress as a percent value between 0 and 1
     */
    public double getProgress() {
        return this.progress;
    }

    /**
     * Counts the number of chunks in a target translation.
     * TODO: once target translations become resource containers we can use the method below instead.
     * @param targetTranslation the target translation to count
     * @return the number of completed chunks in the target translation
     */
    private int countChunks(TargetTranslation targetTranslation) {
        return targetTranslation.numFinished();
    }

    /**
     * Counts how many chunks are in a resource container
     * @param container the resource container to be counted
     * @return the number of chunks in the resource container
     */
    private int countChunks(ResourceContainer container) {
        int count = 0;
        for(String chapterSlug:container.chapters()) {
            count += container.chunks(chapterSlug).length;
        }
        return count;
    }

    /**
     * Returns a single source translation that corresponds to the target translation
     * @param index the source index
     * @param targetTranslation the target translation to match against
     * @return a matching source translation or null
     */
    private Translation getSourceTranslation(Index index, TargetTranslation targetTranslation) {
        List<Translation> sourceTranslations = index.findTranslations(null, targetTranslation.getProjectId(), null, "book", null, App.MIN_CHECKING_LEVEL, -1);
        if(sourceTranslations.size() > 0) {
            return sourceTranslations.get(0);
        }
        return null;
    }
}
