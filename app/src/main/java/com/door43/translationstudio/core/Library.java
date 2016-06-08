package com.door43.translationstudio.core;

import android.content.Context;
import android.support.annotation.Nullable;

import com.door43.tools.reporting.Logger;
import com.door43.util.Zip;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FilenameFilter;
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
    private static final String IMAGES_DIR = "images";
    public static final String TAG = Library.class.toString();
//    private static final String IMAGES_DOWNLOADED_TAG = "images_downloaded_and_extracted";
    private final LibraryData libraryData;
//    public static String DATABASE_NAME = "app";
    private final Context mContext;
    private final File mAssetsDir;
    private Downloader mDownloader;
//    private static LibrarySQLiteHelper appIndexHelper;

    /**
     *
     * @param context
     * @param rootApiUrl the api url
     * @param assetsDir the directory where binary assets are stored such as images from the api
     * @throws IOException
     */
    public Library(Context context, String rootApiUrl, File assetsDir) throws IOException {
//        initalizeHelpers(context);
        mContext = context;
        libraryData = new LibraryData(context);
        mDownloader = new Downloader(rootApiUrl);
        mAssetsDir = assetsDir;
    }
//
//    /**
//     * Initializes the static index sqlite helpers
//     * @param context
//     */
//    private synchronized static void initalizeHelpers(Context context) throws IOException {
//        if(appIndexHelper == null) {
//            appIndexHelper = new LibrarySQLiteHelper(context, "schema.sqlite", DATABASE_NAME);
//        }
//    }

    /**
     * Deletes the index and rebuilds it from scratch
     * This will result in a completely empty index.
     */
    public void delete() {
        libraryData.delete();
        libraryData.rebuild();
    }

    /**
     * Returns the directory where project images are stored.
     * Images are stored within project directories and named with the complex frame id
     * examples:
     * "obs/01-01.jpg" frame image
     * "obs/01.jpg" chapter image
     * "obs/icon.jpg" project icon
     * @return
     */
    public File getImagesDir() {
        File file = new File(mAssetsDir, IMAGES_DIR);
        file.mkdirs();
        return file;
    }

    /**
     * Imports the default index and languages into the library
     *
     * @param index the default application sqlite index
     *
     */
    public void deploy(File index) throws Exception {
        libraryData.deploy(index);
    }

    /**
     * Returns the active index.
     * This allows us to switch between the app index and the server index
     *
     * @return
     */
    private LibraryData getActiveIndex() {
        return libraryData;
    }

    /**
     * Checks if the library exists
     * @return
     */
    public boolean exists() {
        return libraryData.getProjectSlugs().length > 0;
    }

    /**
     * Downloads the chunk markers for this project from the server
     * @param projectSlug
     */
    private void downloadChunkMarkerList(String projectSlug) {
        mDownloader.downloadChunkMarkerList(projectSlug, libraryData);
    }

    /**
     * Downloads the source language catalog from the server
     * @param projectId
     */
    private void downloadSourceLanguageList(String projectId) {
        if(mDownloader.downloadSourceLanguageList(projectId, libraryData)) {
            for(String sourceLanguageId: libraryData.getSourceLanguageSlugs(projectId)) {
                mDownloader.downloadResourceList(projectId, sourceLanguageId, libraryData);
            }
        }
    }

    /**
     * Returns a list of updates that are available on the server
     * @param listener an optional progress listener
     * @return
     */
    public LibraryUpdates checkServerForUpdates(OnProgressListener listener) {
        libraryData.beginTransaction();
        if(mDownloader.downloadProjectList(libraryData)) {
            String[] projectIds = libraryData.getProjectSlugs();
            for (int i = 0; i < projectIds.length; i ++) {
                String projectId = projectIds[i];
                downloadChunkMarkerList(projectId);
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
        libraryData.endTransaction(true);
        return getAvailableUpdates();
    }

    /**
     * Generates the available library updates from the server and app index.
     * The network is not used durring this operation.
     * Only projects with downloaded source are considered eligible for updates.
     * @return
     */
    public LibraryUpdates getAvailableUpdates() {
        LibraryUpdates updates = new LibraryUpdates();
        SourceTranslation[] sourceTranslations = libraryData.getSourceTranslationsWithUpdates();
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
        libraryData.beginTransaction();
        boolean success = mDownloader.downloadTargetLanguages(libraryData);
        libraryData.endTransaction(success);
        return success;
    }

    /**
     * Adds a temporary target language to the library
     * @param tempTargetLanguage
     * @return
     */
    public boolean addTempTargetLanguage(TargetLanguage tempTargetLanguage) {
        libraryData.addTempTargetLanguage(tempTargetLanguage);
        return true;
    }

    /**
     * Downloads the new language questionnaire from the server
     * @return
     */
    public boolean downloadNewLanguageQuestionnaire() {
        libraryData.beginTransaction();
        libraryData.deleteNewLanguageQuestionnaires();
        boolean success = mDownloader.downloadNewLanguageQuestionnaire(libraryData);
        libraryData.endTransaction(success);
        return success;
    }

    /**
     * Returns the new target language questionnaire
     * @return
     */
    public Questionnaire getNewLanguageQuestionnaire() {
        Questionnaire[] questionnaires = libraryData.getNewLanguageQuestionnaire();
        // TRICKY: we will only ever have one questionnaire for now.
        if(questionnaires.length > 0) {
            return questionnaires[0];
        }
        return null;
    }

    /**
     * Returns an array of chunk markers for the project
     * @param projectSlug
     * @return
     */
    public ChunkMarker[] getChunkMarkers(String projectSlug) {
        return libraryData.getChunkMarkers(projectSlug);
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
        int numProjects = libraryData.getProjectSlugs().length;

        // download
        libraryData.beginTransaction();
        for (String projectId : libraryData.getProjectSlugs()) {
            for (String sourceLanguageId : libraryData.getSourceLanguageSlugs(projectId)) {
                for(String resourceId : libraryData.getResourceSlugs(projectId, sourceLanguageId)) {
                    SourceTranslation sourceTranslation = SourceTranslation.simple(projectId, sourceLanguageId, resourceId);

                    // only download resources that meet the minimum checking level or have been downloaded before
                    Resource resource = getResource(sourceTranslation);
                    if(resource != null && resource.getCheckingLevel() < MIN_CHECKING_LEVEL) {
                        continue;
                    }

                    boolean downloadSuccess = startSourceTranslationDownload(sourceTranslation, sourceTranslationListener);
                    if(!downloadSuccess) {
                        Logger.w(TAG, "Failed to download " + sourceTranslation.getId());
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
        libraryData.endTransaction(success);
        return success;
    }

    /**
     * Downloads a source translation from the server
     * @param translation
     * @param listener
     * @return
     */
    public Boolean downloadSourceTranslation(SourceTranslation translation, OnProgressListener listener) {
        libraryData.beginTransaction();
        boolean success = startSourceTranslationDownload(translation, listener);
        libraryData.endTransaction(success);
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
        if(mDownloader.downloadSource(sourceTranslation, libraryData)) {
            libraryData.markSourceCatalogUpToDate(sourceTranslation);
        } else {
            success = false;
        }
        if(listener != null) {
            listener.onProgress(1, 6);
        }

        // words
        if(mDownloader.downloadWords(sourceTranslation, libraryData)) {
            libraryData.markWordsCatalogUpToDate(sourceTranslation);
        }
        if(listener != null) {
            listener.onProgress(2, 6);
        }

        // word assignments
        if(mDownloader.downloadWordAssignments(sourceTranslation, libraryData)) {
            libraryData.markWordAssignmentsCatalogUpToDate(sourceTranslation);
        }
        if(listener != null) {
            listener.onProgress(3, 6);
        }

        // notes
        if(mDownloader.downloadNotes(sourceTranslation, libraryData)) {
            libraryData.markNotesCatalogUpToDate(sourceTranslation);
        }
        if(listener != null) {
            listener.onProgress(4, 6);
        }

        // questions
        if(mDownloader.downloadCheckingQuestions(sourceTranslation, libraryData)) {
            libraryData.markQuestionsCatalogUpToDate(sourceTranslation);
        }
        if(listener != null) {
            listener.onProgress(5, 6);
        }

        if(listener != null) {
            listener.onProgress(6, 6);
        }

        // TODO: 4/5/2016 eventually ta


        // Images are not downloaded here; rather, fetched on demand since they're large.

        return success;
    }

    /**
     * Downloads images from the server
     * @param listener
     * @return
     */
    public Boolean downloadImages(OnProgressListener listener) {
        boolean success = mDownloader.downloadImages(listener);
        if(!success) {
            FileUtils.deleteQuietly(getImagesDir());
        }
        return success;
    }

    /**
     * Indicates whether the imagery download is complete and extraction was successful.
     *
     * <p>If any part of the process was not complete, this returns false. This method makes no
     * statement as to the freshness of the information downloaded.</p>
     * @return true if the download is complete
     */
    public boolean hasImages() {
        String[] names =  getImagesDir().list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return !new File(dir, filename).isDirectory();
            }
        });
        return names != null && names.length > 0;
    }

    /**
     * Exports the library in a zip archive
     * @param destDir the directory where the library will be exported
     * @return the path to the exported file
     */
    @Nullable
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
            Logger.e(TAG, "Failed to export the library", e);
            return null;
        }
        return destFile;
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
        return libraryData.getProjects(sourceLanguageSlug);
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
        return libraryData.getCategoryBranch(parentCategory.sourcelanguageId, parentCategory.parentCategoryId);
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
    @Nullable
    public SourceLanguage getPreferredSourceLanguage(String projectId, String sourceLanguageSlug) {
        // preferred language
        SourceLanguage sourceLanguage = getActiveIndex().getSourceLanguage(projectId, sourceLanguageSlug);
        // try to use default (en)
        if (sourceLanguage == null || (!sourceLanguage.code.equals(sourceLanguageSlug) && !sourceLanguageSlug.equals("en"))) {
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
    public Resource getResource(SourceTranslation sourceTranslation) {
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
        int numFinishedItems = targetTranslation.numFinished();
        SourceLanguage sourceLanguage = getPreferredSourceLanguage(targetTranslation.getProjectId(), Locale.getDefault().getLanguage());
        if(sourceLanguage != null) {
            SourceTranslation sourceTranslation = getDefaultSourceTranslation(targetTranslation.getProjectId(), sourceLanguage.getId());
            int numAvailableTranslations = libraryData.numTranslatable(sourceTranslation);
            if (numAvailableTranslations > 0) {
                return (float) numFinishedItems / (float) numAvailableTranslations;
            } else {
                return 0;
            }
        } else {
            Logger.w(TAG, "Cannot get progress of " + targetTranslation.getId() + " because a source language does not exist");
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
    @Nullable
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
    @Nullable
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
        return getActiveIndex().getTargetLanguage(targetLanguageId);
    }

    /**
     * Returns the target language used in the target translation.
     * This target language may not exist in the db, in which case the information in the target
     * translation will be used to create the target language.
     * @param targetTranslation
     * @return
     */
    @Nullable
    public TargetLanguage getTargetLanguage(TargetTranslation targetTranslation) {
        TargetLanguage language = null;
        if(targetTranslation != null) {
            language = getTargetLanguage(targetTranslation.getTargetLanguageId());
            if (language == null && !targetTranslation.getTargetLanguageId().isEmpty()) {
                String name = targetTranslation.getTargetLanguageName().isEmpty() ? targetTranslation.getTargetLanguageId() : targetTranslation.getTargetLanguageName();
                LanguageDirection direction = targetTranslation.getTargetLanguageDirection() == null ? LanguageDirection.LeftToRight : targetTranslation.getTargetLanguageDirection();
                language = new TargetLanguage(targetTranslation.getTargetLanguageId(), name, "", direction);
            }
        }
        return language;
    }

    /**
     * Returns the source translation by it's id
     * @param sourceTranslationId
     * @return null if the source translation does not exist
     */
    @Nullable
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
    @Nullable
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
    @Nullable
    public SourceTranslation getDefaultSourceTranslation(String projectSlug, String sourceLanguageSlug) {
        SourceTranslation sourceTranslation = libraryData.getSourceTranslation(projectSlug, sourceLanguageSlug, DEFAULT_RESOURCE_SLUG);
        if(sourceTranslation == null) {
            String[] resourceSlugs = libraryData.getResourceSlugs(projectSlug, sourceLanguageSlug);
            if(resourceSlugs.length > 0) {
                return libraryData.getSourceTranslation(projectSlug, sourceLanguageSlug, resourceSlugs[0]);
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
     *
     * @param projectId
     */
    public SourceTranslation[] getDraftTranslations(String projectId) {
        List<SourceTranslation> draftTranslations = new ArrayList<>();
        String[] sourceLanguageIds = getActiveIndex().getSourceLanguageSlugs(projectId);
        for(String sourceLanguageId:sourceLanguageIds) {
            draftTranslations.addAll(getDraftTranslations(projectId, sourceLanguageId));
        }
        return draftTranslations.toArray(new SourceTranslation[draftTranslations.size()]);
    }

    /**
     * Returns an array of source translations in a project and source language that have not yet met the minimum checking level
     *
     * @param projectId
     * @param sourceLanguageId
     * @return
     */
    public List<SourceTranslation> getDraftTranslations(String projectId, String sourceLanguageId) {
        List<SourceTranslation> draftTranslations = new ArrayList<>();
        String[] resourceIds = getActiveIndex().getResourceSlugs(projectId, sourceLanguageId);
        for(String resourceId:resourceIds) {
            SourceTranslation sourceTranslation = getDraftTranslation(projectId, sourceLanguageId, resourceId);
            if(sourceTranslation != null) {
                draftTranslations.add(sourceTranslation);
            }
        }
        return draftTranslations;
    }

    /**
     * Returns a source translation that has not yet met the minimum checking level.
     *
     * @param projectId
     * @param sourceLanguageId
     * @param resourceId
     * @return
     */
    public SourceTranslation getDraftTranslation(String projectId, String sourceLanguageId, String resourceId) {
        SourceTranslation sourceTranslation = getSourceTranslation(projectId, sourceLanguageId, resourceId);
        if(sourceTranslation != null && sourceTranslation.getCheckingLevel() < MIN_CHECKING_LEVEL) {
            return sourceTranslation;
        } else {
            return null;
        }
    }

    /**
     * Returns the source translation that has not yet met the minimum checking level
     * @param sourceTranslationId
     * @return
     */
    public SourceTranslation getDraftTranslation(String sourceTranslationId) {
        SourceTranslation sourceTranslation = getSourceTranslation(sourceTranslationId);
        if(sourceTranslation != null && sourceTranslation.getCheckingLevel() < MIN_CHECKING_LEVEL) {
            return sourceTranslation;
        } else {
            return null;
        }
    }

    /**
     * Returns an array of notes in the frame
     * @param sourceTranslation
     * @param chapterSlug
     * @param frameSlug
     * @return
     */
    public TranslationNote[] getTranslationNotes(SourceTranslation sourceTranslation, String chapterSlug, String frameSlug) {
        return libraryData.getTranslationNotes(sourceTranslation, chapterSlug, frameSlug);
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
     * Returns an array of translation words in the source translation
     * @param sourceTranslation
     * @return
     */
    public TranslationWord[] getTranslationWords(SourceTranslation sourceTranslation) {
        return getActiveIndex().getWords(sourceTranslation);
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
     * Returns a translation academy entry from the source translation
     * @param sourceTranslation
     * @param volume
     *@param manual @return
     */
    public TranslationArticle getTranslationArticle(SourceTranslation sourceTranslation, String volume, String manual, String articleId) {
        return getActiveIndex().getTranslationArticle(sourceTranslation, volume, manual, articleId);
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
            if(sourceTranslationHasSource(SourceTranslation.simple(projectId, sourceLanguageId, resourceId))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the source translation has any source downloaded
     * @param sourceTranslation
     * @return
     */
    public boolean sourceTranslationHasSource(SourceTranslation sourceTranslation) {
        return getActiveIndex().getChapterSlugs(sourceTranslation).length > 0;
    }

    /**
     * Deletes a project from the library
     * @param projectId
     */
    public void deleteProject(String projectId) {
        String[] sourceLanguageIds = libraryData.getSourceLanguageSlugs(projectId);
        libraryData.beginTransaction();
        for(String sourceLanguageId:sourceLanguageIds) {
            Resource[] resources = libraryData.getResources(projectId, sourceLanguageId);
            for(Resource r:resources) {
                libraryData.deleteResource(r.getDBId());
                // restore resource after cascade delete
                libraryData.saveResource(r, r.getSourceLanguageDBId());
            }
//            libraryData.deleteSourceLanguage(projectId, sourceLanguageId);
        }
        libraryData.endTransaction(true);
//        libraryData.deleteProject(projectId);
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
        libraryData.beginTransaction();
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
        libraryData.endTransaction(true);
        return true;
    }

    /**
     * Returns the format of the chapter body
     * @param mSourceTranslation
     * @param chapterSlug
     * @return
     */
    public TranslationFormat getChapterBodyFormat(SourceTranslation mSourceTranslation, String chapterSlug) {
        return libraryData.getChapterBodyFormat(mSourceTranslation, chapterSlug);
    }

    /**
     * Returns the body of the entire chapter.
     * This concats all the frame body's together
     * @param mSourceTranslation
     * @param chapterSlug
     * @return
     */
    public String getChapterBody(SourceTranslation mSourceTranslation, String chapterSlug) {
        return libraryData.getChapterBody(mSourceTranslation, chapterSlug);
    }

    /**
     * Returns a target language by searching for it's human readable name
     * @param name
     * @return
     */
    public TargetLanguage findTargetLanguageByName(String name) {
        return getActiveIndex().getTargetLanguageByName(name);
    }

    /**
     * This is a temporary method so we can index tA.
     * tA is not currently available in the api so we bundle it and index it manually
     * @param sourceTranslation
     * @param catalog
     * @return
     * @deprecated you probably shouldn't use this method
     */
    public boolean manuallyIndexTranslationAcademy(SourceTranslation sourceTranslation, String catalog) {
        getActiveIndex().beginTransaction();
        boolean success = getActiveIndex().indexTranslationAcademy(sourceTranslation, catalog);
        getActiveIndex().endTransaction(success);
        return success;
    }

    /**
     * This is a temporary method so we can index chunk markers
     * Chunk markers are not currently available in the api so we must inject the catalog urls manually
     * @return
     * @deprecated you probably shouldn't use this method
     */
    public boolean manuallyInjectChunkMarkerCatalogUrl() {
        getActiveIndex().beginTransaction();
        boolean success = false;
        try {
            success = getActiveIndex().manuallyInjectChunkMarkerUrls();
        } catch (Exception e) {
            e.printStackTrace();
        }
        getActiveIndex().endTransaction(success);
        return success;
    }

    /**
     * Resets all the date_modified values
     */
    public void setExpired() {
        libraryData.setExpired();
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
