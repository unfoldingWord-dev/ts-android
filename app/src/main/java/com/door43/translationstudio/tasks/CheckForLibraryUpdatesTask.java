package com.door43.translationstudio.tasks;

import android.support.annotation.Nullable;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.LibraryUpdates;
import com.door43.translationstudio.core.NewLanguageRequest;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TargetTranslationMigrator;

import org.unfoldingword.tools.logger.Logger;
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
        if(App.isNetworkAvailable()) {
            publishProgress(-1, "");

            Library library = App.getLibrary();
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
                    App.setLastCheckedForUpdates(System.currentTimeMillis());
                }
            }

            // make sure we have the most recent new target language questionnaire
            publishProgress(-1, App.context().getResources().getString(R.string.loading));
            library.downloadNewLanguageQuestionnaire();

            // submit new language requests
            SubmitNewLanguageRequestsTask task = new SubmitNewLanguageRequestsTask();
            mMaxProgress = task.maxProgress();
            delegate(task);

            // make sure we have the most recent target languages
            publishProgress(-1, App.context().getResources().getString(R.string.downloading_languages));
            library.downloadTargetLanguages();

            // download the temp target languages
            library.downloadTempTargetLanguages();

            // download the temp target languages assignments
            library.downloadTempTargetLanguageAssignments();

            // clean up language requests that have already been submitted
            NewLanguageRequest[] requests = App.getNewLanguageRequests();
            for(NewLanguageRequest request:requests) {
                if(library.getApprovedTargetLanguage(request.tempLanguageCode) != null) {
                    Logger.i(getClass().getName(), "Removing old language request " + request.tempLanguageCode);
                    App.removeNewLanguageRequest(request);
                }
            }

            // perform target translation migrations due to updates to languages
            publishProgress(-1, App.context().getResources().getString(R.string.updating_projects));
            TargetTranslation[] targetTranslations = App.getTranslator().getTargetTranslations();
            mMaxProgress = targetTranslations.length;
            for(int i = 0; i < targetTranslations.length; i ++) {
                TargetTranslation t = targetTranslations[i];
                publishProgress(i + 1, App.context().getResources().getString(R.string.updating_projects));
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
