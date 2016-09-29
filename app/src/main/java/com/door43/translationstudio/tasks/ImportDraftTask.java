package com.door43.translationstudio.tasks;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetTranslation;

import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.taskmanager.ManagedTask;

/**
 * this task imports a draft translation as a new/updated target translation.
 * Draft translations are source translations that have not met the minimum checking level
 */
public class ImportDraftTask extends ManagedTask {

    public static final String TASK_ID = "import_draft";
    private final ResourceContainer draftTranslation;
    private TargetTranslation targetTranslation;

    public ImportDraftTask(ResourceContainer draftTranslation) {
        this.draftTranslation = draftTranslation;
    }

    @Override
    public void start() {
        publishProgress(-1, App.context().getResources().getString(R.string.importing_draft));
        if(draftTranslation != null) {
            this.targetTranslation = App.getTranslator().importDraftTranslation(App.getProfile().getNativeSpeaker(), draftTranslation, App.getLibrary());
        }
    }

    /**
     * Returns the new/updated target translation
     * @return
     */
    public TargetTranslation getTargetTranslation() {
        return this.targetTranslation;
    }
}
