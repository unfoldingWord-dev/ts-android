package com.door43.translationstudio.tasks;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.projects.TranslationManager;
import com.door43.util.tasks.ManagedTask;

/**
 * Created by joel on 3/26/2015.
 */
public class ImportTranslationDraftTask extends ManagedTask {

    private final Project mProject;
    private int mMaxProgress = 100;
    public static final String TASK_ID = "import_translation_draft";

    public ImportTranslationDraftTask(Project p) {
        mProject = p;
    }

    @Override
    public void start() {
        // TODO: we need to determine what happens when there are multiple resources in a draft.
        // right now we are only loading the first one
        SourceLanguage draft = mProject.getSourceLanguageDraft(mProject.getSelectedTargetLanguage().getId());
        if(draft != null) {
            TranslationManager.importTranslationDraft(mProject, draft, new TranslationManager.OnProgressListener() {
                @Override
                public void onProgress(double progress, String message) {
                    ImportTranslationDraftTask.this.publishProgress(progress, message);
                }
            });
        } else {
            Logger.w(this.getClass().getName(), "The translation draft resources could not be found. Project: "+mProject.getId() + " Language: " + mProject.getSelectedTargetLanguage().getId());
        }
    }

    @Override
    public int maxProgress() {
        return mMaxProgress;
    }
}
