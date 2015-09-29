package com.door43.translationstudio.tasks;

import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.Resource;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.AppContext;
import com.door43.util.tasks.ManagedTask;

/**
 * This task downloads the source language data from the server.
 */
public class DownloadSourceLanguageTask extends ManagedTask {

    public static final String TASK_ID = "download_source_language_id";

    private final String mProjectId;
    private final String mSourceLanguageId;
    private final Library mLibrary;
    private final Library mServerLibrary;
    private int mMaxProgress = 1;
    private boolean mSuccess;
    private int mTaskProgress = 0;

    /**
     * Creates a new task to download a source language
     * @param projectId
     * @param sourceLanguageId
     */
    public DownloadSourceLanguageTask(String projectId, String sourceLanguageId) {
        mLibrary = AppContext.getLibrary();
        mServerLibrary = mLibrary.getServerLibrary();
        mProjectId = projectId;
        mSourceLanguageId = sourceLanguageId;
    }

    /**
     * NOTE: the project, source language, and resources catalogs will have been downloaded when the
     * user first opens the download manager. So we do not need to download them again here.
     */
    @Override
    public void start() {

        final Resource[] resources = mServerLibrary.getResources(mProjectId, mSourceLanguageId);
        mSuccess = true;
        for(int i = 0; i < resources.length; i ++) {
            // TODO: hook up progress listener
            boolean status = mLibrary.downloadSourceTranslation(SourceTranslation.simple(mProjectId, mSourceLanguageId, resources[i].getId()), new Library.OnProgressListener() {
                @Override
                public void onProgress(int progress, int max) {
                    mMaxProgress = resources.length * max;
                    mTaskProgress ++;
                    publishProgress(mTaskProgress, "");
                }

                @Override
                public void onIndeterminate() {
                    publishProgress(-1, "");
                }
            });
            if(!status) {
                mSuccess = status;
            }
        }

        // download resources
//        boolean ignoreCache = false;
//        publishProgress(-1, "");
//
//        // merge the new project and languages catalog
//        AppContext.projectManager().mergeProject(mProjectId.getId());
//        AppContext.projectManager().mergeSourceLanguage(mProjectId.getId(), mSourceLanguageId.getId());
//
//        // download resources
//        List<Resource> resources = AppContext.projectManager().downloadResourceList(mProjectId, mSourceLanguageId, ignoreCache);
//        mMaxProgress = resources.size();
//        for(int i = 0; i < resources.size(); i ++) {
//            if(interrupted()) return;
//            Resource r = resources.get(i);
//            AppContext.projectManager().mergeResource(mProjectId.getId(), mSourceLanguageId.getId(), r.getId());
//
//            // notes
//            publishProgress(((i + 1) / (double) resources.size()) * .25, "");
//            AppContext.projectManager().downloadNotes(mProjectId, mSourceLanguageId, r, ignoreCache);
//            AppContext.projectManager().mergeNotes(mProjectId.getId(), mSourceLanguageId.getId(), r);
//
//            // terms
//            publishProgress(((i + 1) / (double) resources.size()) * .50, "");
//            AppContext.projectManager().downloadTerms(mProjectId, mSourceLanguageId, r, ignoreCache);
//            AppContext.projectManager().mergeTerms(mProjectId.getId(), mSourceLanguageId.getId(), r);
//
//            // source
//            publishProgress(((i + 1) / (double) resources.size()) * .75, "");
//            AppContext.projectManager().downloadSource(mProjectId, mSourceLanguageId, r, ignoreCache);
//            AppContext.projectManager().mergeSource(mProjectId.getId(), mSourceLanguageId.getId(), r);
//
//            // questions
//            publishProgress(((i + 1) / (double) resources.size()) * .9, "");
//            AppContext.projectManager().downloadQuestions(mProjectId, mSourceLanguageId, r, ignoreCache);
//            AppContext.projectManager().mergeQuestions(mProjectId.getId(), mSourceLanguageId.getId(), r);
//
//            publishProgress((i + 1) / (double) resources.size(), "");
//            mSourceLanguageId.addResource(r);
//        }
//        publishProgress(-1, "");
//        // reload project
//        if(interrupted()) return;
//        // TODO: only delete the index if there were changes
//        publishProgress(-1, AppContext.context().getResources().getString(R.string.indexing));
//        IndexStore.destroy(mProjectId, mSourceLanguageId);
//        delegate(new IndexProjectsTask(mProjectId));
//        Project currentProject = AppContext.projectManager().getSelectedProject();
//        // index resources of current project
//        if(currentProject != null && currentProject.getId().equals(mProjectId.getId()) && currentProject.hasSelectedSourceLanguage() && currentProject.getSelectedSourceLanguage().getId().equals(mSourceLanguageId.getId())) {
//            delegate(new IndexResourceTask(currentProject, currentProject.getSelectedSourceLanguage(), currentProject.getSelectedSourceLanguage().getSelectedResource()));
//        }
//        AppContext.projectManager().reloadProject(mProjectId.getId());
    }

    @Override
    public int maxProgress() {
        return mMaxProgress;
    }

    /**
     * Checks if the download was a success
     * @return
     */
    public boolean getSuccess() {
        return mSuccess;
    }

    /**
     * Returns the id of the project for the source language
     * @return
     */
    public String getProjectId() {
        return mProjectId;
    }

    /**
     * Returns the id of the source language that was downloaded
     * @return
     */
    public String getSourceLanguageId() {
        return mSourceLanguageId;
    }
}
