package com.door43.translationstudio.tasks;

import android.os.Process;

import com.door43.translationstudio.App;
import com.door43.translationstudio.core.TargetTranslation;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import java.util.HashMap;
import java.util.Map;

/**
 * Do GIT Merge of target translation projects
 */
public class MergeTargetTranslationTask extends ManagedTask {

    public static final String TASK_ID = "merge_target_translation_task";
    public final TargetTranslation destinationTranslation;
    public final TargetTranslation sourceTranslation;
    private Status status = Status.UNKNOWN;
    private Map<String, int[][]> conflicts = new HashMap<>();
    private boolean deleteSource = false;

    /**
     * do a merge of translations
     * @param destinationTranslation
     * @param sourceTranslation
     */
    public MergeTargetTranslationTask(TargetTranslation destinationTranslation, TargetTranslation sourceTranslation, boolean deleteSource) {
        setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
        this.destinationTranslation = destinationTranslation;
        this.sourceTranslation = sourceTranslation;
        this.deleteSource = deleteSource;
    }

    @Override
    public void start() {
        boolean success = false;
        status = Status.UNKNOWN;
        try {
            boolean mergeConflict = !destinationTranslation.merge(sourceTranslation.getPath());
            success = true;
            status = Status.SUCCESS;
            if(mergeConflict) {
                status = Status.MERGE_CONFLICTS;
            }
        } catch (Exception e) {
            e.printStackTrace();
            status = Status.MERGE_ERROR;
        }

        if(this.deleteSource && success) {
            // delete original
            App.getTranslator().deleteTargetTranslation(sourceTranslation.getId());
            App.clearTargetTranslationSettings(sourceTranslation.getId());
        }
    }

    public Status getStatus() {
        return status;
    }

    public TargetTranslation getDestinationTranslation() {
        return destinationTranslation;
    }

    public TargetTranslation getSourceTranslation() {
        return sourceTranslation;
    }

    public enum Status {
        SUCCESS,
        MERGE_CONFLICTS,
        MERGE_ERROR,
        UNKNOWN
    }
}
