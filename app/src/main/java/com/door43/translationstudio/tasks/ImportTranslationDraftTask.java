package com.door43.translationstudio.tasks;

import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.TranslationManager;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.threads.ManagedTask;

/**
 * Created by joel on 3/26/2015.
 */
public class ImportTranslationDraftTask extends ManagedTask {

    private final Project mProject;
    public ImportTranslationDraftTask(Project p) {
        mProject = p;
    }

    @Override
    public void start() {
        // TODO: we need to determine what happens when there are multiple resources in an a draft.
        // right now we are only loading the first one
        AppContext.translationManager().importTranslationDraft(mProject, mProject.getSourceLanguageDraft(mProject.getSelectedTargetLanguage().getId()), new TranslationManager.OnProgressListener() {
            @Override
            public void onProgress(double progress, String message) {
                ImportTranslationDraftTask.this.publishProgress(progress, message);
            }
        });
    }
}
