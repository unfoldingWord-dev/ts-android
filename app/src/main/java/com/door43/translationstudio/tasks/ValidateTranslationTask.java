package com.door43.translationstudio.tasks;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Chapter;
import com.door43.translationstudio.projects.Frame;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.uploadwizard.steps.validate.UploadValidationItem;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.tasks.ManagedTask;

import java.util.ArrayList;
import java.util.List;

/**
 * This task checks if there are any errors in the translation
 */
public class ValidateTranslationTask extends ManagedTask {

    public static final String TASK_ID = "validate_translation";
    private ArrayList<UploadValidationItem> mValidationItems = new ArrayList<>();
    private boolean mHasErrors = false;
    private boolean mHasWarnings = false;

    @Override
    public void start() {
        publishProgress(-1, AppContext.context().getResources().getString(R.string.loading));

        // make sure the project is configured correctly
        Project p = AppContext.projectManager().getSelectedProject();
        if(p.getSelectedSourceLanguage().equals(p.getSelectedTargetLanguage())) {
            // source and target language should not be the same
            mValidationItems.add(new UploadValidationItem(AppContext.context().getResources().getString(R.string.title_project_settings), AppContext.context().getResources().getString(R.string.error_target_and_source_are_same), UploadValidationItem.Status.ERROR));
            mHasErrors = true;
        } else {
//            mValidationItems.add(new UploadValidationItem(AppContext.context().getResources().getString(R.string.title_project_settings), UploadValidationItem.Status.SUCCESS));
        }

        // make sure all the chapter titles and references have been set
        int numChaptersTranslated = 0;
        boolean chapterHasWarnings = false;
        for(int i = 0; i < p.numChapters(); i ++) {
            Chapter c = p.getChapter(i);
            String description = "";
            UploadValidationItem.Status status = UploadValidationItem.Status.SUCCESS;
            // check title
            if(c.getTitleTranslation().getText().isEmpty()) {
                description = AppContext.context().getResources().getString(R.string.error_chapter_title_missing);
                status = UploadValidationItem.Status.WARNING;
                chapterHasWarnings = true;
            }
            // check reference
            if(c.getReferenceTranslation().getText().isEmpty()) {
                description = description + "\n"+AppContext.context().getResources().getString(R.string.error_chapter_reference_missing);
                status = UploadValidationItem.Status.WARNING;
                chapterHasWarnings = true;
            }
            // check frames
            int numFramesNotTranslated = 0;
            for(int j = 0; j < c.numFrames(); j ++) {
                Frame f = c.getFrame(j);
                if(f.getTranslation().getText().isEmpty()) {
                    numFramesNotTranslated ++;
                }
            }
            if(numFramesNotTranslated > 0) {
                description += "\n"+String.format(AppContext.context().getResources().getString(R.string.error_frames_not_translated), numFramesNotTranslated);
                status = UploadValidationItem.Status.WARNING;
                chapterHasWarnings = true;
            }

            // only display warnings for chapters that have at least some frames translated
            if(numFramesNotTranslated != c.numFrames()) {
                numChaptersTranslated ++;
                mValidationItems.add(new UploadValidationItem(String.format(AppContext.context().getResources().getString(R.string.label_chapter_title_detailed), c.getTitle()), description, status));
            } else {
                // ignore empty or completed chapters
                chapterHasWarnings = false;
            }
            mHasWarnings = chapterHasWarnings || mHasWarnings;
        }

        // ensure at least one chapter has been translated
        if(numChaptersTranslated == 0) {
            mValidationItems.add(new UploadValidationItem(AppContext.context().getResources().getString(R.string.title_chapters), AppContext.context().getResources().getString(R.string.no_translated_chapters), UploadValidationItem.Status.ERROR));
            mHasErrors = true;
        }
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
        return mHasWarnings;
    }

    /**
     * Checks if the validation results contain any errors
     * @return
     */
    public boolean hasErrors() {
        return mHasErrors;
    }

    /**
     * Returns a list of validation items
     * @return
     */
    public List<UploadValidationItem> getValidationItems() {
        return mValidationItems;
    }
}
