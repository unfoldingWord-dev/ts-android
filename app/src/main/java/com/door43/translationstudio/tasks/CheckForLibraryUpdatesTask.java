package com.door43.translationstudio.tasks;

import android.support.annotation.Nullable;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.LibraryUpdates;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TargetTranslationMigrator;

import org.unfoldingword.tools.taskmanager.ManagedTask;

/**
 * Gets a list of projects that are available for download from the server
 */
public class CheckForLibraryUpdatesTask extends ManagedTask {
    public static final String TASK_ID  = "get_available_source_translations_task";
    private int mMaxProgress = 100;
    private LibraryUpdates mUpdates = null;

    @Override
    public void start() {
        if(AppContext.context().isNetworkAvailable()) {
            publishProgress(-1, "");

            Library library = AppContext.getLibrary();
            if (library != null) {
                mUpdates = library.checkServerForUpdates(new Library.OnProgressListener() {
                    @Override
                    public boolean onProgress(int progress, int max) {
                        mMaxProgress = max;
                        publishProgress((float)progress/(float)max, "");
                        return !isCanceled();
                    }

                    @Override
                    public boolean onIndeterminate() {
                        publishProgress(-1, "");
                        return !isCanceled();
                    }
                });
                if (!isCanceled()) {
                    AppContext.setLastCheckedForUpdates(System.currentTimeMillis());
                }
            }

            // make sure we have the most recent new target language questionnaire
            publishProgress(-1, AppContext.context().getResources().getString(R.string.loading));
            library.downloadNewLanguageQuestionnaire();

            // submit new language requests
            SubmitNewLanguageRequestsTask task = new SubmitNewLanguageRequestsTask();
            mMaxProgress = task.maxProgress();
            delegate(task);

            // make sure we have the most recent target languages
            publishProgress(-1, AppContext.context().getResources().getString(R.string.downloading_languages));
            library.downloadTargetLanguages();

            // download the temp target languages
            library.downloadTempTargetLanguages();

            // download the temp target languages
            library.downloadTempTargetLanguageAssignments();

            // perform target translation migrations due to updates to languages
            publishProgress(-1, AppContext.context().getResources().getString(R.string.updating_projects));
            TargetTranslation[] targetTranslations = AppContext.getTranslator().getTargetTranslations();
            mMaxProgress = targetTranslations.length;
            for(int i = 0; i < targetTranslations.length; i ++) {
                TargetTranslation t = targetTranslations[i];
                publishProgress(i + 1, AppContext.context().getResources().getString(R.string.updating_projects));
                TargetTranslationMigrator.migrate(t.getPath());
            }
        }
    }

    @Override
    public int maxProgress() {
        return mMaxProgress;
    }

    /**
     * Returns the library updates
     * @return
     */
    @Nullable
    public LibraryUpdates getUpdates() {
        return mUpdates;
    }
}
