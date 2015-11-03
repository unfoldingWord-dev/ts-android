package com.door43.translationstudio.core;

import android.content.Context;

import com.door43.tools.reporting.Logger;
import com.door43.util.Zip;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by joel on 8/29/2015.
 */
public class Library {
    private static final int MIN_CHECKING_LEVEL = 3; // the minimum level to be considered a source translation
    private static final String DEFAULT_RESOURCE_SLUG = "ulb";
    private final Indexer mAppIndex;
    public static String DATABASE_NAME = "app";
    private final Context mContext;
    private Downloader mDownloader;
    private static IndexerSQLiteHelper appIndexHelper;

    public Library(Context context, String rootApiUrl) throws IOException {
        initalizeHelpers(context);
        mContext = context;
        mAppIndex = new Indexer(context, DATABASE_NAME, appIndexHelper);
        mDownloader = new Downloader(rootApiUrl);
    }

    /**
     * Initializes the static index sqlite helpers
     * @param context
     */
    private synchronized static void initalizeHelpers(Context context) throws IOException {
        if(appIndexHelper == null) {
            appIndexHelper = new IndexerSQLiteHelper(context, DATABASE_NAME);
        }
    }

    /**
     * Deletes the index and rebuilds it from scratch
     * This will result in a completely empty index.
     */
    public void delete() {
        mAppIndex.delete();
        mAppIndex.rebuild();
    }

    /**
     * Imports the default index and languages into the library
     *
     * @param index the default application sqlite index
     *
     */
    public void deploy(File index) throws Exception {
        appIndexHelper.close();
        mContext.getDatabasePath(appIndexHelper.getDatabaseName()).delete();
        File indexDest = mContext.getDatabasePath(appIndexHelper.getDatabaseName());
        indexDest.getParentFile().mkdirs();
        FileUtils.moveFile(index, indexDest);
        appIndexHelper = null;
    }

    /**
     * Returns the active index.
     * This allows us to switch between the app index and the server index
     *
     * @return
     */
    private Indexer getActiveIndex() {
        return mAppIndex;
    }

    /**
     * Checks if the library exists
     * @return
     */
    public boolean exists() {
        return mAppIndex.getProjectSlugs().length > 0;
    }

    /**
     * Downloads the source language catalog from the server
     * @param projectId
     */
    private void downloadSourceLanguageList(String projectId) {
        if(mDownloader.downloadSourceLanguageList(projectId, mAppIndex)) {
            for(String sourceLanguageId:mAppIndex.getSourceLanguageSlugs(projectId)) {
                mDownloader.downloadResourceList(projectId, sourceLanguageId, mAppIndex);
            }
        }
    }

    /**
     * Returns a list of updates that are available on the server
     * @param listener an optional progress listener
     * @return
     */
    public LibraryUpdates checkServerForUpdates(OnProgressListener listener) {
        mAppIndex.beginTransaction();
        if(mDownloader.downloadProjectList(mAppIndex)) {
            String[] projectIds = mAppIndex.getProjectSlugs();
            for (int i = 0; i < projectIds.length; i ++) {
                String projectId = projectIds[i];
                downloadSourceLanguageList(projectId);
                if(listener != null) {
                    if(!listener.onProgress((i + 1), projectIds.length)) {
                        break;
                    }
                }
            }
        }
        if(listener != null) {
            listener.onIndeterminate();
        }
        mAppIndex.endTransaction(true);
        return getAvailableUpdates();
    }

    /**
     * Generates the available library updates from the server and app index.
     * The network is not used durring this operation.
     * @return
     */
    public LibraryUpdates getAvailableUpdates() {
        LibraryUpdates updates = new LibraryUpdates();
        SourceTranslation[] sourceTranslations = mAppIndex.getSourceTranslationsWithUpdates();
        for(SourceTranslation sourceTranslation:sourceTranslations) {
            updates.addUpdate(sourceTranslation);
        }
        return updates;
    }

    /**
     * Downloads the target languages from the server
     * @return
     */
    public boolean downloadTargetLanguages() {
        mAppIndex.beginTransaction();
        boolean success = mDownloader.downloadTargetLanguages(mAppIndex);
        mAppIndex.endTransaction(success);
        return success;
    }

    /**
     * Downloads all of the projects from the server
     *
     * @param projectProgressListener
     * @param sourceTranslationListener
     * @return
     */
    public Boolean downloadAllProjects(OnProgressListener projectProgressListener, OnProgressListener sourceTranslationListener) {
        boolean success = true;
        int currentProject = 1;
        int numProjects = mAppIndex.getProjectSlugs().length;

        // download
        mAppIndex.beginTransaction();
        for (String projectId : mAppIndex.getProjectSlugs()) {
            for (String sourceLanguageId : mAppIndex.getSourceLanguageSlugs(projectId)) {
                for(String resourceId : mAppIndex.getResourceSlugs(projectId, sourceLanguageId)) {
                    SourceTranslation sourceTranslation = SourceTranslation.simple(projectId, sourceLanguageId, resourceId);

                    // only download resources that meet the minimum checking level or have been downloaded before
                    Resource resource = getResource(sourceTranslation);
                    if(resource != null && resource.getCheckingLevel() < MIN_CHECKING_LEVEL) {
                        continue;
                    }

                    boolean downloadSuccess = startSourceTranslationDownload(sourceTranslation, sourceTranslationListener);
                    if(!downloadSuccess) {
                        Logger.w(this.getClass().getName(), "Failed to download " + sourceTranslation.getId());
                        success = false;
                    }
                }
            }
            if(projectProgressListener != null) {
                if(!projectProgressListener.onProgress(currentProject, numProjects)) {
                    break;
                }
                currentProject ++;
            }
        }
        mAppIndex.endTransaction(success);
        return success;
    }

    /**
     * Downloads a source translation from the server
     * @param translation
     * @param listener
     * @return
     */
    public Boolean downloadSourceTranslation(SourceTranslation translation, OnProgressListener listener) {
        mAppIndex.beginTransaction();
        boolean success = startSourceTranslationDownload(translation, listener);
        mAppIndex.endTransaction(success);
        return success;
    }

    /**
     * begins downloading a source translation.
     * This will not start any transactions so it is safe to place in a transaction along with other queries
     * This method cannot be interrupted by the thread in order to maintain data integrity
     * @param sourceTranslation
     * @return
     */
    private Boolean startSourceTranslationDownload(SourceTranslation sourceTranslation, OnProgressListener listener) {
        boolean success = true;
        // source
        if(mDownloader.downloadSource(sourceTranslation, mAppIndex)) {
            mAppIndex.markSourceCatalogUpToDate(sourceTranslation);
        } else {
            success = false;
        }
        if(listener != null) {
            listener.onProgress(1, 5);
        }

        // words
        if(mDownloader.downloadWords(sourceTranslation, mAppIndex)) {
            mAppIndex.markWordsCatalogUpToDate(sourceTranslation);
        }
        if(listener != null) {
            listener.onProgress(2, 5);
        }

        // word assignments
        // TODO: delete current term assignments
        if(mDownloader.downloadWordAssignments(sourceTranslation, mAppIndex)) {
            mAppIndex.markWordAssignmentsCatalogUpToDate(sourceTranslation);
        }
        if(listener != null) {
            listener.onProgress(3, 5);
        }

        // notes
        if(mDownloader.downloadNotes(sourceTranslation, mAppIndex)) {
            mAppIndex.markNotesCatalogUpToDate(sourceTranslation);
        }
        if(listener != null) {
            listener.onProgress(4, 5);
        }

        // questions
        if(mDownloader.downloadCheckingQuestions(sourceTranslation, mAppIndex)) {
            mAppIndex.markQuestionsCatalogUpToDate(sourceTranslation);
        }
        if(listener != null) {
            listener.onProgress(5, 5);
        }

        return success;
    }

    /**
     * Exports the library in a zip archive
     * @param destDir the directory where the library will be exported
     * @return the path to the exported file
     */
    public File export(File destDir) {
        SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String date = s.format(new Date());
        File destFile = new File(destDir, "library_" + date + ".zip");
        destDir.mkdirs();
        try {
            destFile.createNewFile();
//            Tar.tar(mContext.getDatabasePath(getActiveIndex().getIndexId()).getPath(), destFile.getPath());
            Zip.zip(mContext.getDatabasePath(getActiveIndex().getIndexId()).getPath(), destFile.getPath());
        } catch (IOException e) {
            Logger.e(this.getClass().getName(), "Failed to export the library", e);
            return null;
        }
        return destFile;
    }

    /**
     * Exports a json array of target translations on this device
     * @param targetTranslations the target translations to export as json
     * @param preferredSourceLanguages the preferred source languages to return the project information in
     * @return
     */
    public String exportTranslationLibrary(TargetTranslation[] targetTranslations, String[] preferredSourceLanguages) {
        // TODO: 10/23/2015 implement this follow structure... we might want to place this in an exporter class
        // and pass in the version requested so we can support backwards compatible request but still
        // upgrade the structure for newer devices.
//        [
//        {
//            "id":"obs",
//                "project":{
//            "name":"Open Bible Stories",
//                    "description":"The Bible in 50 stories",
//                    "meta":[]
//        },
//            "language":{
//            "slug":"en",
//                    "name":"English",
//                    "direction":"rtl"
//        },
//            "target_languages":[
//            {
//                "slug":"de",
//                    "name":"Deutsch",
//                    "direction":"rtl"
//            },
//            ...
//            ]
//        }
//        ]
        return "[]";
    }

    /**
     * Returns an array of target languages
     * @return
     */
    public TargetLanguage[] getTargetLanguages() {
        return getActiveIndex().getTargetLanguages();
    }

    /**
     * Returns an array of projects
     *
     * @param sourceLanguageSlug the preferred language in which the project names will be returned. The default is english
     * @return
     */
    public Project[] getProjects(String sourceLanguageSlug) {
        return mAppIndex.getProjects(sourceLanguageSlug);
    }

    /**
     * Returns a list of project categories
     * Only projects with source are included
     *
     * @param languageId the preferred language in which the category names will be returned. The default is english
     * @return
     */
    public ProjectCategory[] getProjectCategories(String languageId) {
        return getProjectCategories(new ProjectCategory(null, null, null, languageId, 0l));
    }

    /**
     * Returns a list of project categories beneath the given category
     * Only projects with source are included
     *
     * @param parentCategory
     * @return
     */
    public ProjectCategory[] getProjectCategories(ProjectCategory parentCategory) {
        return mAppIndex.getCategoryBranch(parentCategory.sourcelanguageId, parentCategory.parentCategoryId);
    }

    /**
     * Returns the preferred source language if it exists
     *
     * If the source language does not exist it will default to english.
     * If english does not exist it will return the first available source language (this happens in the query)
     *
     * @param projectId
     * @param sourceLanguageSlug
     * @return null if no source langauges exist for the project
     */
    public SourceLanguage getPreferredSourceLanguage(String projectId, String sourceLanguageSlug) {
        // preferred language
        SourceLanguage sourceLanguage = getActiveIndex().getSourceLanguage(projectId, sourceLanguageSlug);
        // try to use default (en)
        if(sourceLanguage == null || (!sourceLanguage.code.equals(sourceLanguageSlug) && !sourceLanguageSlug.equals("en"))) {
            sourceLanguage = getActiveIndex().getSourceLanguage(projectId, "en");
        }
        return sourceLanguage;
    }

    /**
     * Returns an array of source languages for the project
     * @param projectSlug the id of the project who's source languages will be returned
     * @return
     */
    public SourceLanguage[] getSourceLanguages(String projectSlug) {
        return getActiveIndex().getSourceLanguages(projectSlug);
    }

    /**
     * Returns a source language in the project
     * @param projectId
     * @param sourceLanguageId
     * @return
     */
    public SourceLanguage getSourceLanguage(String projectId, String sourceLanguageId) {
        return getActiveIndex().getSourceLanguage(projectId, sourceLanguageId);
    }

    /**
     * Returns an array of resources for the source language
     * @param projectSlug
     * @param sourceLanguageSlug
     * @return
     */
    public Resource[] getResources(String projectSlug, String sourceLanguageSlug) {
        return getActiveIndex().getResources(projectSlug, sourceLanguageSlug);
    }

    /**
     * Returns a resource
     * @param sourceTranslation
     * @return
     */
    private Resource getResource(SourceTranslation sourceTranslation) {
        return getActiveIndex().getResource(SourceTranslation.simple(sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug, sourceTranslation.resourceSlug));
    }

    /**
     * Returns the project with the information provided in the preferred source language
     * If the source language does not exist it will use the default language
     *
     * @param projectId
     * @param languageId the language in which the project information should be returned
     * @return
     */
    public Project getProject(String projectId, String languageId) {
        return getActiveIndex().getProject(projectId, languageId);
    }

    /**
     * Calculates the progress of a target translation.
     * This can take some time so don't run this on the main thread
     * @param targetTranslation
     * @return
     */
    public float getTranslationProgress(TargetTranslation targetTranslation) {
        int numTranslatedItems = targetTranslation.numTranslated();
        SourceLanguage sourceLanguage = getPreferredSourceLanguage(targetTranslation.getProjectId(), Locale.getDefault().getLanguage());
        SourceTranslation sourceTranslation = getDefaultSourceTranslation(targetTranslation.getProjectId(), sourceLanguage.getId());
        int numAvailableTranslations = mAppIndex.numTranslatable(sourceTranslation);
        if(numAvailableTranslations > 0) {
            return (float) numTranslatedItems / (float) numAvailableTranslations;
        } else {
            return 0;
        }
    }

    /**
     * Returns an array of chapters for the source translation
     * @param sourceTranslation
     * @return
     */
    public Chapter[] getChapters(SourceTranslation sourceTranslation) {
        return getActiveIndex().getChapters(sourceTranslation);
    }

    /**
     * Returns a single chapter
     * @param sourceTranslation
     * @param chapterId
     * @return
     */
    public Chapter getChapter(SourceTranslation sourceTranslation, String chapterId) {
        return getActiveIndex().getChapter(sourceTranslation, chapterId);
    }

    /**
     * Returns an array of frames in the chapter
     * @param sourceTranslation
     * @param chapterSlug
     * @return
     */
    public Frame[] getFrames(SourceTranslation sourceTranslation, String chapterSlug) {
        List<Frame> frames = new ArrayList<>(Arrays.asList(getActiveIndex().getFrames(sourceTranslation, chapterSlug)));

        if(frames.size() > 0) {
            // TRICKY: a bug in the v2 api gives the last frame in the last chapter and id of 00 which messes up the sorting
            Frame firstFrame = frames.get(0);
            if (Integer.parseInt(firstFrame.getId()) == 0) {
                frames.remove(0);
                frames.add(firstFrame);
            }
            return frames.toArray(new Frame[frames.size()]);
        } else {
            return new Frame[0];
        }
    }

    /**
     * Returns an array of frame slugs
     * @param sourceTranslation
     * @param chapterSlug
     * @return
     */
    public String[] getFrameSlugs(SourceTranslation sourceTranslation, String chapterSlug) {
        List<String> frameSlugs = new ArrayList<>(Arrays.asList(getActiveIndex().getFrameSlugs(sourceTranslation, chapterSlug)));

        if(frameSlugs.size() > 0) {
            // TRICKY: a bug in the v2 api gives the last frame in the last chapter and id of 00 which messes up the sorting
            String firstFrame = frameSlugs.get(0);
            if (Integer.parseInt(firstFrame) == 0) {
                frameSlugs.remove(0);
                frameSlugs.add(firstFrame);
            }
            return frameSlugs.toArray(new String[frameSlugs.size()]);
        } else {
            return new String[0];
        }
    }

    /**
     * Returns a single frame
     * @param sourceTranslation
     * @param chapterId
     * @param frameId
     * @return
     */
    public Frame getFrame(SourceTranslation sourceTranslation, String chapterId, String frameId) {
        if(frameId == null || chapterId == null) {
            return null;
        }
        return getActiveIndex().getFrame(sourceTranslation, chapterId, frameId);
    }

    /**
     * Returns a single target language
     * @param targetLanguageId
     * @return
     */
    public TargetLanguage getTargetLanguage(String targetLanguageId) {
        // TODO: cache for better performance
        return getActiveIndex().getTargetLanguage(targetLanguageId);
    }

    /**
     * Returns the source translation by it's id
     * @param sourceTranslationId
     * @return null if the source translation does not exist
     */
    public SourceTranslation getSourceTranslation(String sourceTranslationId) {
        if(sourceTranslationId != null) {
            String projectId = SourceTranslation.getProjectIdFromId(sourceTranslationId);
            String sourceLanguageId = SourceTranslation.getSourceLanguageIdFromId(sourceTranslationId);
            String resourceId = SourceTranslation.getResourceIdFromId(sourceTranslationId);
            return getSourceTranslation(projectId, sourceLanguageId, resourceId);
        }
        return null;
    }

    /**
     * Returns the source translation
     * @param projectSlug
     * @param sourceLanguageSlug
     * @param resourceSlug
     * @return null if the source translation does not exist
     */
    public SourceTranslation getSourceTranslation(String projectSlug, String sourceLanguageSlug, String resourceSlug) {
        return getActiveIndex().getSourceTranslation(projectSlug, sourceLanguageSlug, resourceSlug);
    }

    /**
     * Returns the source translation with the default resource.
     * If the default resource does not exist it will use the first available resource
     *
     * @param projectSlug
     * @param sourceLanguageSlug
     * @return null if the source translation does not exist
     */
    public SourceTranslation getDefaultSourceTranslation(String projectSlug, String sourceLanguageSlug) {
        SourceTranslation sourceTranslation = mAppIndex.getSourceTranslation(projectSlug, sourceLanguageSlug, DEFAULT_RESOURCE_SLUG);
        if(sourceTranslation == null) {
            String[] resourceSlugs = mAppIndex.getResourceSlugs(projectSlug, sourceLanguageSlug);
            if(resourceSlugs.length > 0) {
                return mAppIndex.getSourceTranslation(projectSlug, sourceLanguageSlug, resourceSlugs[0]);
            }
        } else {
            return sourceTranslation;
        }
        return null;
    }

    /**
     * Returns an array of source translations in a project that have met the minimum checking level
     * @param projectId
     */
    public SourceTranslation[] getSourceTranslations(String projectId) {
        // TODO: write a query for this
        List<SourceTranslation> sourceTranslations = new ArrayList<>();
        String[] sourceLanguageIds = getActiveIndex().getSourceLanguageSlugs(projectId);
        for(String sourceLanguageId:sourceLanguageIds) {
            String[] resourceIds = getActiveIndex().getResourceSlugs(projectId, sourceLanguageId);
            for(String resourceId:resourceIds) {
                SourceTranslation sourceTranslation = getSourceTranslation(projectId, sourceLanguageId, resourceId);
                if(sourceTranslation != null && sourceTranslation.getCheckingLevel() >= MIN_CHECKING_LEVEL) {
                    sourceTranslations.add(sourceTranslation);
                }
            }
        }
        return sourceTranslations.toArray(new SourceTranslation[sourceTranslations.size()]);
    }

    /**
     * Returns an array of source translations in a project that have not yet met the minimum checking level
     * @param projectId
     */
    public SourceTranslation[] getDraftTranslations(String projectId) {
        // TODO: 10/20/2015 write query for this
        List<SourceTranslation> draftTranslations = new ArrayList<>();
        String[] sourceLanguageIds = getActiveIndex().getSourceLanguageSlugs(projectId);
        for(String sourceLanguageId:sourceLanguageIds) {
            String[] resourceIds = getActiveIndex().getResourceSlugs(projectId, sourceLanguageId);
            for(String resourceId:resourceIds) {
                SourceTranslation sourceTranslation = getSourceTranslation(projectId, sourceLanguageId, resourceId);
                if(sourceTranslation != null && sourceTranslation.getCheckingLevel() < MIN_CHECKING_LEVEL) {
                    draftTranslations.add(sourceTranslation);
                }
            }
        }
        return draftTranslations.toArray(new SourceTranslation[draftTranslations.size()]);
    }

    /**
     * Returns an array of notes in the frame
     * @param sourceTranslation
     * @param chapterSlug
     * @param frameSlug
     * @return
     */
    public TranslationNote[] getTranslationNotes(SourceTranslation sourceTranslation, String chapterSlug, String frameSlug) {
        return mAppIndex.getTranslationNotes(sourceTranslation, chapterSlug, frameSlug);
    }

    /**
     * Returns a single translation note
     * @param sourceTranslation
     * @param chapterId
     * @param frameId
     * @param translationNoteId
     * @return
     */
    public TranslationNote getTranslationNote(SourceTranslation sourceTranslation, String chapterId, String frameId, String translationNoteId) {
        return getActiveIndex().getNote(sourceTranslation, chapterId, frameId, translationNoteId);
    }

    /**
     * Returns an array of translation words in the frame
     * @param sourceTranslation
     * @param chapterId
     * @param frameId
     * @return
     */
    public TranslationWord[] getTranslationWords(SourceTranslation sourceTranslation, String chapterId, String frameId) {
        return getActiveIndex().getWordsForFrame(sourceTranslation, chapterId, frameId);
    }

    /**
     * Returns a translation word from the source translation
     * @param sourceTranslation
     * @param translationWordId
     * @return
     */
    public TranslationWord getTranslationWord(SourceTranslation sourceTranslation, String translationWordId) {
        return getActiveIndex().getWord(sourceTranslation, translationWordId);
    }

    /**
     * Returns an array of checking questions in the frame
     * @param sourceTranslation
     * @param chapterId
     * @param frameId
     * @return
     */
    public CheckingQuestion[] getCheckingQuestions(SourceTranslation sourceTranslation, String chapterId, String frameId) {
        return getActiveIndex().getCheckingQuestions(sourceTranslation, chapterId, frameId);
    }

    /**
     * Returns a checking question in the frame
     * @param sourceTranslation
     * @param chapterId
     * @param frameId
     * @param checkingQuestionId
     * @return
     */
    public CheckingQuestion getCheckingQuestion(SourceTranslation sourceTranslation, String chapterId, String frameId, String checkingQuestionId) {
        return getActiveIndex().getCheckingQuestion(sourceTranslation, chapterId, frameId, checkingQuestionId);
    }

    /**
     * Checks if the project has any source downloaded
     *
     * @param projectId
     * @return
     */
    public boolean projectHasSource(String projectId) {
        String[] sourceLanguageIds = getActiveIndex().getSourceLanguageSlugs(projectId);
        for(String sourceLanguageId:sourceLanguageIds) {
            if(sourceLanguageHasSource(projectId, sourceLanguageId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the source language has any source downloaded
     * @param projectId
     * @param sourceLanguageId
     * @return
     */
    public boolean sourceLanguageHasSource(String projectId, String sourceLanguageId) {
        String[] resourceIds = getActiveIndex().getResourceSlugs(projectId, sourceLanguageId);
        for(String resourceId:resourceIds) {
            String[] chapterIds = getActiveIndex().getChapterSlugs(SourceTranslation.simple(projectId, sourceLanguageId, resourceId));
            if(chapterIds.length > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deletes a project from the library
     * @param projectId
     */
    public void deleteProject(String projectId) {
        // TODO: we can delete everything with one query if cascade on delete is set up.
        String[] sourceLanguageIds = mAppIndex.getSourceLanguageSlugs(projectId);
        for(String sourceLanguageId:sourceLanguageIds) {
            mAppIndex.deleteSourceLanguage(projectId, sourceLanguageId);
        }
        mAppIndex.deleteProject(projectId);
    }

    /**
     * Returns the number of target languages without loading them from the disk.
     * @return
     */
    public int getTargetLanguagesLength() {
        return getActiveIndex().getNumTargetLanguages();
    }

    /**
     * Downloads updates from the server
     * @param updates
     * @param listener
     * @return
     */
    public boolean downloadUpdates(LibraryUpdates updates, OnProgressListener listener) {
        mAppIndex.beginTransaction();
        int progress = 0;
        int numUpdates = updates.numSourceTranslationUpdates();
        String[] projectSlugs = updates.getUpdatedProjects();
        outerloop:
        for(String projectSlug:projectSlugs) {
            String[] sourceLanguageSlugs = updates.getUpdatedSourceLanguages(projectSlug);
            for(String sourceLanguageSlug:sourceLanguageSlugs) {
                String[] resourceSlugs = updates.getUpdatedResources(projectSlug, sourceLanguageSlug);
                for(String resourceSlug:resourceSlugs) {
                    if(startSourceTranslationDownload(SourceTranslation.simple(projectSlug, sourceLanguageSlug, resourceSlug), null)) {
                        updates.removeSourceLanguageUpdate(projectSlug, sourceLanguageSlug);
                    }
                    if(listener != null) {
                        progress ++;
                        if(!listener.onProgress(progress, numUpdates)) {
                            break outerloop;
                        }
                    }
                }
            }
        }
        mAppIndex.endTransaction(true);
        return true;
    }

    /**
     * Returns the format of the chapter body
     * @param mSourceTranslation
     * @param chapterSlug
     * @return
     */
    public TranslationFormat getChapterBodyFormat(SourceTranslation mSourceTranslation, String chapterSlug) {
        return mAppIndex.getChapterBodyFormat(mSourceTranslation, chapterSlug);
    }

    /**
     * Returns the body of the entire chapter.
     * This concats all the frame body's together
     * @param mSourceTranslation
     * @param chapterSlug
     * @return
     */
    public String getChapterBody(SourceTranslation mSourceTranslation, String chapterSlug) {
        return mAppIndex.getChapterBody(mSourceTranslation, chapterSlug);
    }

    /**
     * Returns a target language by searching for it's human readable name
     * @param name
     * @return
     */
    public TargetLanguage findTargetLanguageByName(String name) {
        return getActiveIndex().getTargetLanguageByName(name);
    }

    public interface OnProgressListener {
        /**
         * Progress the progress on an operation between 0 and max
         * @param progress
         * @param max
         * @return the process should stop if returns false
         */
        boolean onProgress(int progress, int max);

        /**
         * Identifes the current task as not quantifiable
         * @return the process should stop if returns false
         */
        boolean onIndeterminate();
    }
}
