package com.door43.translationstudio.tasks;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Resource;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.threads.ManagedTask;

import java.util.List;

/**
 * This task downloads the source language data from the server.
 * Only source at the minimum checking level is downloaded.
 */
public class DownloadLanguageTask extends ManagedTask {

    private final Project mProject;
    private final SourceLanguage mLanguage;
    private OnProgressListener mListener;

    /**
     * Creates a new task to download a source language
     * @param project
     * @param language
     */
    public DownloadLanguageTask(Project project, SourceLanguage language) {
        mProject = project;
        mLanguage = language;
    }

    @Override
    public void start() {
        // download resources
        boolean ignoreCache = true;
        publishProgress(-1, "");
        List<Resource> resources = AppContext.projectManager().downloadResourceList(mProject, mLanguage, ignoreCache);

        for(int i = 0; i < resources.size(); i ++) {

            Resource r = resources.get(i);
            if(r.getCheckingLevel() >= AppContext.context().getResources().getInteger(R.integer.min_source_lang_checking_level)) {
                publishProgress((i/resources.size())*.3, "");
                AppContext.projectManager().downloadNotes(mProject, mLanguage, r, ignoreCache);

                publishProgress((i/resources.size())*.6, "");
                AppContext.projectManager().downloadTerms(mProject, mLanguage, r, ignoreCache);

                publishProgress((i/resources.size())*.9, "");
                AppContext.projectManager().downloadSource(mProject, mLanguage, r, ignoreCache);

            }
            publishProgress(i/resources.size(), "");
            mLanguage.addResource(r);
        }

    }

    /**
     * Publishes the progress
     * @param progress
     * @param message
     */
    private void publishProgress(double progress, String message) {
        if(mListener != null) {
            try {
                mListener.onProgress(progress, message);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setOnProgressListener(OnProgressListener listener) {
        mListener = listener;
    }

    /**
     * Returns the id of the project of the language
     * @return
     */
    public Project getProject() {
        return mProject;
    }

    /**
     * Returns the id of the language that was downloaded
     * @return
     */
    public SourceLanguage getLanguage() {
        return mLanguage;
    }

    public static interface OnProgressListener {
        public void onProgress(double progress, String message);
    }
}
