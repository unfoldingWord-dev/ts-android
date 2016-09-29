package com.door43.translationstudio.core;

import android.content.Context;
import android.support.annotation.Nullable;

import org.unfoldingword.tools.logger.Logger;

import org.unfoldingword.door43client.models.TargetLanguage;

import com.door43.util.Zip;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


/**
 * Created by joel on 8/29/2015.
 */
@Deprecated
public class Library {
    private static final int MIN_CHECKING_LEVEL = 3; // the minimum level to be considered a source translation
    private static final String DEFAULT_RESOURCE_SLUG = "ulb";
    private static final String IMAGES_DIR = "images";
    private static final String TAG = "Library";//Library.class.toString();
//    private static final String IMAGES_DOWNLOADED_TAG = "images_downloaded_and_extracted";
//    private LibraryData libraryData;
//    public static String DATABASE_NAME = "app";
    private final Context mContext;
    private final File mAssetsDir;
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
//        libraryData = new LibraryData(context);
//        mDownloader = null; //new Downloader(rootApiUrl);
        mAssetsDir = assetsDir;
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
        return libraryData.getSourceTranslation(projectSlug, sourceLanguageSlug, resourceSlug);
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
        return getSourceTranslations( projectId, MIN_CHECKING_LEVEL);
    }

    /**
     * Returns an array of source translations in a project that have met the minimum checking level
     * @param projectId
     * @param checkingLevel - acceptable checking level
     */
    public SourceTranslation[] getSourceTranslations(String projectId, int checkingLevel) {
        // TODO: write a query for this
        List<SourceTranslation> sourceTranslations = new ArrayList<>();
        String[] sourceLanguageIds = libraryData.getSourceLanguageSlugs(projectId);
        for(String sourceLanguageId:sourceLanguageIds) {
            String[] resourceIds = libraryData.getResourceSlugs(projectId, sourceLanguageId);
            for(String resourceId:resourceIds) {
                SourceTranslation sourceTranslation = getSourceTranslation(projectId, sourceLanguageId, resourceId);
                if(sourceTranslation != null && sourceTranslation.getCheckingLevel() >= checkingLevel) {
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
        String[] sourceLanguageIds = libraryData.getSourceLanguageSlugs(projectId);
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
        String[] resourceIds = libraryData.getResourceSlugs(projectId, sourceLanguageId);
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
        return libraryData.getNote(sourceTranslation, chapterId, frameId, translationNoteId);
    }

    /**
     * Returns an array of translation words in the source translation
     * @param sourceTranslation
     * @return
     */
    public TranslationWord[] getTranslationWords(SourceTranslation sourceTranslation) {
        return libraryData.getWords(sourceTranslation);
    }

    /**
     * Returns an array of translation words in the frame
     * @param sourceTranslation
     * @param chapterId
     * @param frameId
     * @return
     */
    public TranslationWord[] getTranslationWords(SourceTranslation sourceTranslation, String chapterId, String frameId) {
        return libraryData.getWordsForFrame(sourceTranslation, chapterId, frameId);
    }

    /**
     * Returns a translation word from the source translation
     * @param sourceTranslation
     * @param translationWordId
     * @return
     */
    public TranslationWord getTranslationWord(SourceTranslation sourceTranslation, String translationWordId) {
        return libraryData.getWord(sourceTranslation, translationWordId);
    }

    /**
     * Returns a translation academy entry from the source translation
     * @param sourceTranslation
     * @param volume
     *@param manual @return
     */
    public TranslationArticle getTranslationArticle(SourceTranslation sourceTranslation, String volume, String manual, String articleId) {
        return libraryData.getTranslationArticle(sourceTranslation, volume, manual, articleId);
    }

    /**
     * Returns an array of checking questions in the frame
     * @param sourceTranslation
     * @param chapterId
     * @param frameId
     * @return
     */
    public CheckingQuestion[] getCheckingQuestions(SourceTranslation sourceTranslation, String chapterId, String frameId) {
        return libraryData.getCheckingQuestions(sourceTranslation, chapterId, frameId);
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
        return libraryData.getCheckingQuestion(sourceTranslation, chapterId, frameId, checkingQuestionId);
    }

    /**
     * Checks if the project has any source downloaded
     *
     * @param projectId
     * @return
     */
    public boolean projectHasSource(String projectId) {
        String[] sourceLanguageIds = libraryData.getSourceLanguageSlugs(projectId);
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
        String[] resourceIds = libraryData.getResourceSlugs(projectId, sourceLanguageId);
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
        return libraryData.getChapterSlugs(sourceTranslation).length > 0;
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
     * This is a temporary method so we can index tA.
     * tA is not currently available in the api so we bundle it and index it manually
     * @param sourceTranslation
     * @param catalog
     * @return
     * @deprecated you probably shouldn't use this method
     */
    public boolean manuallyIndexTranslationAcademy(SourceTranslation sourceTranslation, String catalog) {
        libraryData.beginTransaction();
        boolean success = libraryData.indexTranslationAcademy(sourceTranslation, catalog);
        libraryData.endTransaction(success);
        return success;
    }

    /**
     * This is a temporary method so we can index chunk markers
     * Chunk markers are not currently available in the api so we must inject the catalog urls manually
     * @return
     * @deprecated you probably shouldn't use this method
     */
    public boolean manuallyInjectChunkMarkerCatalogUrl() {
        libraryData.beginTransaction();
        boolean success = false;
        try {
            success = libraryData.manuallyInjectChunkMarkerUrls();
        } catch (Exception e) {
            e.printStackTrace();
        }
        libraryData.endTransaction(success);
        return success;
    }

    /**
     * Resets all the date_modified values
     */
    public void setExpired() {
        libraryData.setExpired();
    }

    /**
     * Returns a target language that has been approved from a temporary language code request
     * @param tempLanguageCode the temp language code to look up
     * @return
     */
    public TargetLanguage getApprovedTargetLanguage(String tempLanguageCode) {
        return libraryData.getApprovedTargetLanguage(tempLanguageCode);
    }

    /**
     * Retrieves a temp target language from database
     * This is a utility method for unit tests
     * @param code
     * @return
     */
    public TargetLanguage getTempTargetLanguage(String code) {
        return libraryData.getTempTargetLanguage(code);
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
