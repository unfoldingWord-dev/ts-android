package com.door43.translationstudio.tasks;

import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.LibraryUpdates;
import com.door43.translationstudio.AppContext;
import com.door43.util.tasks.ManagedTask;

/**
 * Gets a list of projects that are available for download from the server
 */
public class CheckForLibraryUpdatesTask extends ManagedTask {
    public static final String TASK_ID  = "get_available_source_translations_task";
    private int mMaxProgress = 100;
    private LibraryUpdates mUpdates = null;

    @Override
    public void start() {
        publishProgress(-1, "");

        Library library = AppContext.getLibrary();
        if(library != null) {
            mUpdates = library.checkServerForUpdates(new Library.OnProgressListener() {
                @Override
                public boolean onProgress(int progress, int max) {
                    mMaxProgress = max;
                    publishProgress(progress, "");
                    return !isCanceled();
                }

                @Override
                public boolean onIndeterminate() {
                    publishProgress(-1, "");
                    return !isCanceled();
                }
            });
            if(!isCanceled()) {
                AppContext.setLastCheckedForUpdates(System.currentTimeMillis());
            }
        }
        // make sure we have the most recent target languages
        library.downloadTargetLanguages();
        // make sure we have the most recent new target language questionnaire
        library.downloadNewLanguageQuestionnaire();
    }

    @Override
    public int maxProgress() {
        return mMaxProgress;
    }

    /**
     * Returns the library updates
     * @return
     */
    public LibraryUpdates getUpdates() {
        return mUpdates;
    }
}
