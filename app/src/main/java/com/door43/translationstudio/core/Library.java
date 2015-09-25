package com.door43.translationstudio.core;

import android.content.Context;

import com.door43.tools.reporting.Logger;
import com.door43.util.Zip;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by joel on 8/29/2015.
 */
public class Library {
    public static final String TARGET_LANGUAGES_FILE = "languages.json";
    public static final String DEFAULT_LIBRARY_ZIP = "library.zip";
    private static final int SOURCE_TRANSLATION_MIN_CHECKING_LEVEL = 3;
    private static final String DEFAULT_RESOURCE_ID = "ulb";
    private final Indexer mServerIndex;
    private final Indexer mAppIndex;
    private final File mLibraryDir;
    private final Indexer mDownloaderIndex;
    private final File mIndexDir;
    private final Context mContext;
    private final boolean mAsServerLibrary;
    private final String mRootApiUrl;
    private LibraryUpdates mLibraryUpdates = new LibraryUpdates();
    private static Map<String, TargetLanguage> mTargetLanguages;
    private Downloader mDownloader;

    /**
     *
     * @param context
     * @param libraryDir
     * @param rootApiUrl
     * @param server when true will cause the library to operate off of the server index (just for reading)
     */
    private Library(Context context, File libraryDir, String rootApiUrl, boolean server) {
        mContext = context;
        mLibraryDir = libraryDir;
        mIndexDir = new File(libraryDir, "index");
        mDownloaderIndex = new Indexer("downloads", mIndexDir);
        mServerIndex = new Indexer("server", mIndexDir);
        mAppIndex = new Indexer("app", mIndexDir);
        mRootApiUrl = rootApiUrl;
        mDownloader = new Downloader(mDownloaderIndex, rootApiUrl);
        mAsServerLibrary = server;
    }

    public Library(Context context, File libraryDir, String rootApiUrl) {
        mContext = context;
        mLibraryDir = libraryDir;
        mIndexDir = new File(libraryDir, "index");
        mDownloaderIndex = new Indexer("downloads", mIndexDir);
        mServerIndex = new Indexer("server", mIndexDir);
        mAppIndex = new Indexer("app", mIndexDir);
        mRootApiUrl = rootApiUrl;
        mDownloader = new Downloader(mDownloaderIndex, rootApiUrl);
        mAsServerLibrary = false;
    }

    /**
     * Returns a new library instance that represents the server
     * @return
     */
    public Library getServerLibrary() {
        return new Library(mContext, mLibraryDir, mRootApiUrl, true);
    }

    /**
     * Checks if this library is operating as the server library or the local library
     * @return
     */
    public boolean isServerLibrary() {
        return mAsServerLibrary;
    }

    /**
     * Extracts the default library replacing anything that already existed
     * This will remove any existing app index before copying the zipped library
     * into the assets dir and extracting it.
     *
     */
    public Boolean deployDefaultLibrary() {
        try {
            // languages
            File languagesFile = new File(mLibraryDir, TARGET_LANGUAGES_FILE);
            Util.writeStream(mContext.getAssets().open(TARGET_LANGUAGES_FILE), languagesFile);

            // library index
            File libraryArchive = new File(mLibraryDir, DEFAULT_LIBRARY_ZIP);
            Util.writeStream(mContext.getAssets().open(DEFAULT_LIBRARY_ZIP), libraryArchive);
            FileUtils.deleteQuietly(mAppIndex.getIndexDir());
            mAppIndex.getIndexDir().mkdirs();
            // TODO: we should update the library export to not include the root folder so we are not dependent on the name.
            Zip.unzip(libraryArchive, mAppIndex.getIndexDir().getParentFile());
            FileUtils.deleteQuietly(libraryArchive);
            mAppIndex.reload();
            return true;
        } catch (Exception e) {
            Logger.e(this.getClass().getName(), "Failed to deploy the library", e);
        }
        return false;
    }

    /**
     * Returns the active index.
     * This allows us to switch between the app index and the server index
     *
     * @return
     */
    private Indexer getActiveIndex() {
        if(mAsServerLibrary) {
            return mServerIndex;
        } else {
            return mAppIndex;
        }
    }

    /**
     * Checks if the library exists
     * The app index and the target languages file must exist for this to return true
     * @return
     */
    public boolean exists() {
        File languagesFile = new File(mLibraryDir, TARGET_LANGUAGES_FILE);
        return languagesFile.exists() && mAppIndex.getProjects().length > 0;
    }

    /**
     * Performs a shallow merge from the app index to the download index to make downloads more efficient
     * @return
     */
    public Boolean seedDownloadIndex() {
        try {
            mDownloaderIndex.mergeIndex(mAppIndex, true);
            return true;
        } catch (IOException e) {
            Logger.w(this.getClass().getName(), "Failed to seed the download index", e);
        }
        return false;
    }

    /**
     * Downloads the resource catalog from the server
     * @param projectId
     * @param sourceLanguageId
     */
    private void downloadResourceList(String projectId, String sourceLanguageId) {
        if(mDownloader.downloadResourceList(projectId, sourceLanguageId)) {
            for(String resourceId:mDownloader.getIndex().getResources(projectId, sourceLanguageId)) {
                SourceTranslation sourceTranslation = SourceTranslation.simple(projectId, sourceLanguageId, resourceId);
                try {
                    int latestResourceModified = mDownloader.getIndex().getResource(sourceTranslation).getInt("date_modified");
                    JSONObject localResource = mAppIndex.getResource(sourceTranslation);
                    int localResourceModified = -1;
                    if(localResource != null) {
                        localResourceModified = localResource.getInt("date_modified");
                    }
                    if(localResourceModified == -1 || localResourceModified < latestResourceModified) {
                        // build update list
                        mLibraryUpdates.addUpdate(SourceTranslation.simple(projectId, sourceLanguageId, resourceId));
                    }
                } catch (JSONException e) {
                    Logger.w(this.getClass().getName(), "Failed to process the downloaded resource in " + sourceTranslation.getId(), e);
                }
            }
        }
    }

    /**
     * Downloads the source language catalog from the server
     * @param projectId
     */
    private void downloadSourceLanguageList(String projectId) {
        if(mDownloader.downloadSourceLanguageList(projectId)) {
            for(String sourceLanguageId:mDownloader.getIndex().getSourceLanguages(projectId)) {
                try {
                    int latestSourceLanguageModified = mDownloader.getIndex().getSourceLanguage(projectId, sourceLanguageId).getInt("date_modified");
                    JSONObject lastSourceLanguage = mServerIndex.getSourceLanguage(projectId, sourceLanguageId);
                    int lastSourceLanguageModified = -1;
                    if(lastSourceLanguage != null) {
                        lastSourceLanguageModified = lastSourceLanguage.getInt("date_modified");
                    }
                    if(lastSourceLanguageModified == -1 || lastSourceLanguageModified < latestSourceLanguageModified) {
                        downloadResourceList(projectId, sourceLanguageId);
                    }
                } catch (JSONException e) {
                    Logger.w(this.getClass().getName(), "Failed to process the downloaded source language " + sourceLanguageId + " in project " + projectId, e);
                }
            }
        }
    }

    /**
     * Returns a list of updates that are available on the server
     * @param listener an optiona progress listener
     * @return
     */
    public LibraryUpdates getAvailableLibraryUpdates(OnProgressListener listener) {
        mLibraryUpdates = new LibraryUpdates();
        if(mDownloader.downloadProjectList()) {
            String[] projectIds = mDownloader.getIndex().getProjects();
            for (int i = 0; i < projectIds.length; i ++) {
                String projectId = projectIds[i];
                try {
                    int latestProjectModified = mDownloader.getIndex().getProject(projectId).getInt("date_modified");
                    JSONObject lastProject = mServerIndex.getProject(projectId);
                    int lastProjectModified = -1;
                    if(lastProject != null) {
                        lastProjectModified = lastProject.getInt("date_modified");
                    }
                    if(lastProjectModified == -1 || lastProjectModified < latestProjectModified) {
                        downloadSourceLanguageList(projectId);
                    }
                } catch (JSONException e) {
                    Logger.w(this.getClass().getName(), "Failed to process the downloaded project " + projectId, e);
                }
                if(listener != null) {
                    listener.onProgress((i + 1), projectIds.length);
                }
            }
        }
        try {
            if(listener != null) {
                listener.onIndeterminate();
            }
            mServerIndex.mergeIndex(mDownloader.getIndex(), true);
        } catch (IOException e) {
            Logger.e(this.getClass().getName(), "Failed to merge the download index into the sever index", e);
        }
        return mLibraryUpdates;
    }

    /**
     * Downloads updates from the server
     * @param updates
     */
    public Boolean downloadUpdates(LibraryUpdates updates) throws Exception {
        boolean success = true;
        if(updates != null) {
            for (String projectId : updates.getUpdatedProjects()) {
                boolean projectDownloadSuccess = true;
                for (String sourceLanguageId : updates.getUpdatedSourceLanguages(projectId)) {
                    for (String resourceId : updates.getUpdatedResources(projectId, sourceLanguageId)) {
                        projectDownloadSuccess = downloadSourceTranslationWithoutMerging(SourceTranslation.simple(projectId, sourceLanguageId, resourceId)) ? projectDownloadSuccess : false;
                        if (!projectDownloadSuccess) {
                            throw new Exception("Failed to download " + projectId + " " + sourceLanguageId + " " + resourceId);
                        }
                    }
                }
                success = projectDownloadSuccess ? success : false;
                if (projectDownloadSuccess) {
                    try {
                        mAppIndex.mergeProject(projectId, mDownloader.getIndex());
                    } catch (IOException e) {
                        Logger.e(this.getClass().getName(), "Failed to merge the project " + projectId + " from the download index into the app index", e);
                        success = false;
                    }
                }
            }
        }
        return success;
    }

    /**
     * Downloads a source translation from the server and merges it into the app index
     * @param translation
     * @return
     */
    public Boolean downloadSourceTranslation(SourceTranslation translation) {
        if(downloadSourceTranslationWithoutMerging(translation)) {
            try {
                mAppIndex.mergeSourceTranslation(translation, mDownloader.getIndex());
            } catch (IOException e) {
                Logger.e(this.getClass().getName(), "Failed to merge the source translation " + translation.getId() + " from the download index into the app index", e);
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Downloads a source translation from the server without merging into the app index
     * @param translation
     * @return
     */
    private Boolean downloadSourceTranslationWithoutMerging(SourceTranslation translation) {
        boolean success = true;
        success = mDownloader.downloadSource(translation) ? success : false;
        mDownloader.downloadTerms(translation); // optional to success of download
        mDownloader.downloadNotes(translation); // optional to success of download
        mDownloader.downloadCheckingQuestions(translation); // optional to success of download
        return success;
    }

    /**
     * Exports the library
     * @param destDir the directory where the library will be exported
     * @return the path to the exported file
     */
    public File export(File destDir) {
        SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String date = s.format(new Date());
        File destFile = new File(destDir, "library_" + date + ".zip");
        try {
            Zip.zip(getActiveIndex().getIndexDir().getPath(), destFile.getPath());
        } catch (IOException e) {
            Logger.e(this.getClass().getName(), "Failed to export the library", e);
            return null;
        }
        return destFile;
    }

    /**
     * Returns an array of target languages
     * @return
     */
    public TargetLanguage[] getTargetLanguages() {
        if(mTargetLanguages == null) {
            Map<String, TargetLanguage> languages = new HashMap<>();
            try {
                File languagesFile = new File(mLibraryDir, TARGET_LANGUAGES_FILE);
                String catalog = FileUtils.readFileToString(languagesFile);
                JSONArray json = new JSONArray(catalog);
                for (int i = 0; i < json.length(); i++) {
                    JSONObject item = json.getJSONObject(i);
                    try {
                        TargetLanguage lang = TargetLanguage.Generate(item);
                        if(lang != null && !languages.containsKey(lang.getId())) {
                            languages.put(lang.getId(), lang);
                        }
                    } catch (JSONException e) {
                        Logger.e(this.getClass().getName(), "Failed to parse the target language " + item.toString() , e);
                    }
                }
                mTargetLanguages = languages;
                return mTargetLanguages.values().toArray(new TargetLanguage[mTargetLanguages.size()]);
            } catch (JSONException e) {
                Logger.e(this.getClass().getName(), "Failed to parse the target languages file", e);
            } catch (IOException e) {
                Logger.e(this.getClass().getName(), "Failed to open the target languages file", e);
            }
        } else {
            return mTargetLanguages.values().toArray(new TargetLanguage[mTargetLanguages.size()]);
        }
        return null;
    }

    /**
     * Returns a list of all projects without nested categorization
     * @param languageId the preferred language in which the category names will be returned. The default is english
     * @return
     */
    public ProjectCategory[] getProjectCategoriesFlat(String languageId) {
        List<ProjectCategory> categories = new ArrayList<>();

        String[] projectIds = getActiveIndex().getProjects();
        for(String projectId:projectIds) {
            JSONObject projectJson = getActiveIndex().getProject(projectId);
            try {
                String categoryId = null;
                JSONObject sourceLanguageJson = getPreferredSourceLanguage(projectId, languageId);
                if(sourceLanguageJson != null) {
                    // TRICKY: getPreferredSourceLanguage can return a different language than what was requested
                    String categoryLanguageId = sourceLanguageJson.getString("slug");
                    JSONObject projectLanguageJson = sourceLanguageJson.getJSONObject("project");
                    String title = projectLanguageJson.getString("name");
                    String sort = projectJson.getString("sort");

                    // TODO: we need to provide the icon path
                    ProjectCategory cat = new ProjectCategory(title, categoryId, projectId, categoryLanguageId, sort, -1);
                    categories.add(cat);
                } else {
                    Logger.w(this.getClass().getName(), "Could not find any source languages for " + projectId);
                }
            } catch (JSONException e) {
                Logger.e(this.getClass().getName(), "Failed to parse project " + projectId, e);
            }
        }
        sortProjectCategories(categories);
        return categories.toArray(new ProjectCategory[categories.size()]);
    }

    /**
     * Returns a list of project categories
     *
     * @param languageId the preferred language in which the category names will be returned. The default is english
     * @return
     */
    public ProjectCategory[] getProjectCategories(String languageId) {
        return getProjectCategories(new ProjectCategory(null, null, null, languageId, null, -1));
    }

    /**
     * Returns a list of project categories beneath the given category
     * @param parentCategory
     * @return
     */
    public ProjectCategory[] getProjectCategories(ProjectCategory parentCategory) {
        Map<String, ProjectCategory> categoriesMap = new HashMap<>();

        String[] projectIds = getActiveIndex().getProjects();
        for(String projectId:projectIds) {
            JSONObject projectJson = getActiveIndex().getProject(projectId);
            try {
                JSONArray metaJson = projectJson.getJSONArray("meta");

                if(parentCategory.categoryId != null && (metaJson.length() <= parentCategory.categoryDepth || !metaJson.getString(parentCategory.categoryDepth).equals(parentCategory.categoryId))) {
                    continue;
                }

                String categoryId = null;
                JSONObject sourceLanguageJson = getPreferredSourceLanguage(projectId, parentCategory.sourcelanguageId);
                if(sourceLanguageJson != null) {

                    // TRICKY: getPreferredSourceLanguage can return a different language than what was requested
                    String categoryLanguageId = sourceLanguageJson.getString("slug");
                    JSONObject projectLanguageJson = sourceLanguageJson.getJSONObject("project");
                    String title = projectLanguageJson.getString("name");
                    String sort = projectJson.getString("sort");

                    if (metaJson.length() > parentCategory.categoryDepth + 1) {
                        categoryId = metaJson.getString(parentCategory.categoryDepth + 1);
                        JSONArray metaLanguageJson = projectLanguageJson.getJSONArray("meta");
                        title = metaLanguageJson.getString(parentCategory.categoryDepth + 1);
                    }

                    // TODO: we need to provide the icon path
                    ProjectCategory cat = new ProjectCategory(title, categoryId, projectId, categoryLanguageId, sort, parentCategory.categoryDepth + 1);
                    if (!categoriesMap.containsKey(cat.getId()) || cat.sourcelanguageId.equals(parentCategory.sourcelanguageId)) {
                        // insert new categories or those with better language matches
                        categoriesMap.put(cat.getId(), cat);
                    }
                } else {
                    Logger.w(this.getClass().getName(), "Could not find any source languages for " + projectId);
                }
            } catch (JSONException e) {
                Logger.e(this.getClass().getName(), "Failed to parse project " + projectId, e);
            }
        }
        List<ProjectCategory> categories = new ArrayList<>(categoriesMap.values());
        sortProjectCategories(categories);
        return categories.toArray(new ProjectCategory[categories.size()]);
    }

    private static void sortProjectCategories(List<ProjectCategory> categories) {
        Collections.sort(categories, new Comparator<ProjectCategory>() {
            @Override
            public int compare(ProjectCategory lhs, ProjectCategory rhs) {
                return Integer.parseInt(lhs.sort) - Integer.parseInt(rhs.sort);
            }
        });
    }

    /**
     * Returns the preferred source language if it exists
     *
     * If the source language does not exist it will default to english.
     * If english does not exist it will return the first available source language
     * If no source language is available it will return null.
     *
     * @param projectId
     * @param sourceLanguageId
     * @return null if no source langauges exist for the project
     */
    private JSONObject getPreferredSourceLanguage(String projectId, String sourceLanguageId) {
        // preferred language
        JSONObject sourceLanguageJson = getActiveIndex().getSourceLanguage(projectId, sourceLanguageId);
        // default (en)
        if(sourceLanguageJson == null) {
            sourceLanguageJson = getActiveIndex().getSourceLanguage(projectId, "en");
        }
        // first available
        if(sourceLanguageJson == null) {
            String[] sourceLanguageIds = getActiveIndex().getSourceLanguages(projectId);
            if(sourceLanguageIds.length > 0) {
                sourceLanguageJson = getActiveIndex().getSourceLanguage(projectId, sourceLanguageIds[0]);
            }
        }
        return sourceLanguageJson;
    }

    /**
     * Returns an array of source languages for the project
     * @param projectId the id of the project who's source languages will be returned
     * @return
     */
    public SourceLanguage[] getSourceLanguages(String projectId) {
        List<SourceLanguage> sourceLanguages = new ArrayList<>();
        String[] sourceLanguageIds = getActiveIndex().getSourceLanguages(projectId);
        for(String id:sourceLanguageIds) {
            SourceLanguage lang = getSourceLanguage(projectId, id);
            if(lang != null) {
                sourceLanguages.add(lang);
            }
        }

        return sourceLanguages.toArray(new SourceLanguage[sourceLanguages.size()]);
    }

    /**
     * Returns a source language in the project
     * @param projectId
     * @param sourceLanguageId
     * @return
     */
    public SourceLanguage getSourceLanguage(String projectId, String sourceLanguageId) {
        JSONObject sourceLanguageJson = getActiveIndex().getSourceLanguage(projectId, sourceLanguageId);
        try {
            return SourceLanguage.generate(sourceLanguageJson);
        } catch (JSONException e) {
            Logger.e(this.getClass().getName(), "Failed to parse source language " + sourceLanguageId + " for project " + projectId, e);
        }
        return null;
    }

    /**
     * Returns an array of resources for the source language
     * @param projectId
     * @param sourceLanguageId
     * @return
     */
    public Resource[] getResources(String projectId, String sourceLanguageId) {
        List<Resource> resources = new ArrayList<>();
        String[] resourceIds = getActiveIndex().getResources(projectId, sourceLanguageId);
        for(String id:resourceIds) {
            JSONObject resourceJson = getActiveIndex().getResource(SourceTranslation.simple(projectId, sourceLanguageId, id));
            try {
                Resource res = Resource.generate(resourceJson);
                if(res != null) {
                    resources.add(res);
                }
            } catch (JSONException e) {
                Logger.e(this.getClass().getName(), "Failed to parse the resource " + id + " in source language " + sourceLanguageId + " in project " + projectId, e);
            }
        }
        return resources.toArray(new Resource[resources.size()]);
    }

    /**
     * Returns the project withh the information provided in the preferred source language
     * If the source language does not exist it will use the default language
     *
     * @param projectId
     * @param languageId the language in which the project information should be returned
     * @return
     */
    public Project getProject(String projectId, String languageId) {
        return Project.generate(getActiveIndex().getProject(projectId), getPreferredSourceLanguage(projectId, languageId));
    }

    /**
     * Calculates the progress of a target translation
     * @param targetTranslation
     * @return
     */
    public float getTranslationProgress(TargetTranslation targetTranslation) {
        // TODO: calculate the progress
        return 0.60f;
    }

    /**
     * Returns an array of chapters for the source translation
     * @param sourceTranslation
     * @return
     */
    public Chapter[] getChapters(SourceTranslation sourceTranslation) {
        List<Chapter> chapters = new ArrayList<>();
        String[] chapterIds = getActiveIndex().getChapters(sourceTranslation);
        for(String chapterId:chapterIds) {
            Chapter chapter = getChapter(sourceTranslation, chapterId);
            if(chapter != null) {
                chapters.add(chapter);
            }
        }
        // TODO: sort by id
        return chapters.toArray(new Chapter[chapters.size()]);
    }

    /**
     * Returns a single chapter
     * @param sourceTranslation
     * @param chapterId
     * @return
     */
    public Chapter getChapter(SourceTranslation sourceTranslation, String chapterId) {
        JSONObject chapterJson = getActiveIndex().getChapter(sourceTranslation, chapterId);
        return Chapter.generate(chapterJson);
    }

    /**
     * Returns an array of frames in the chapter
     * @param sourceTranslation
     * @param chapterId
     * @return
     */
    public Frame[] getFrames(SourceTranslation sourceTranslation, String chapterId) {
        List<Frame> frames = new ArrayList<>();
        String[] frameIds = getActiveIndex().getFrames(sourceTranslation, chapterId);
        for(String frameId:frameIds) {
            Frame frame = Frame.generate(chapterId, getActiveIndex().getFrame(sourceTranslation, chapterId, frameId));
            if(frame != null) {
                frames.add(frame);
            }
        }
        // TODO: sort by id
        return frames.toArray(new Frame[frames.size()]);
    }

    /**
     * Returns a single target language
     * @param targetLanguageId
     * @return
     */
    public TargetLanguage getTargetLanguage(String targetLanguageId) {
        if(mTargetLanguages.containsKey(targetLanguageId)) {
            return mTargetLanguages.get(targetLanguageId);
        } else {
            return null;
        }
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
     * @param projectId
     * @param sourceLanguageId
     * @param resourceId
     * @return null if the source translation does not exist
     */
    public SourceTranslation getSourceTranslation(String projectId, String sourceLanguageId, String resourceId) {
        SourceTranslation simpleTranslation = SourceTranslation.simple(projectId, sourceLanguageId, resourceId);
        JSONObject resourceJson = getActiveIndex().getResource(simpleTranslation);
        if(resourceJson != null) {
            JSONObject sourceLanguageJson = getPreferredSourceLanguage(projectId, sourceLanguageId);
            try {
                return SourceTranslation.generate(projectId, sourceLanguageJson, resourceJson);
            } catch (JSONException e) {
                Logger.e(this.getClass().getName(), "Failed to parse source translation " + simpleTranslation.getId(), e);
            }
        }
        return null;
    }

    /**
     * Returns the source translation with the default resource.
     * If the default resource does not exist it will use the first available resource
     *
     * @param projectId
     * @param sourceLanguageId
     * @return null if the source translation does not exist
     */
    public SourceTranslation getDefaultSourceTranslation(String projectId, String sourceLanguageId) {
        Resource[] resources = getResources(projectId, sourceLanguageId);
        if(resources.length > 0) {
            // start with first resource
            Resource defaultResource = resources[0];
            // try default resource
            for (Resource resource : resources) {
                if (resource.getId().toLowerCase().equals(DEFAULT_RESOURCE_ID)) {
                    defaultResource = resource;
                }
            }
            // load source translation
            JSONObject sourceLanguageJson = getPreferredSourceLanguage(projectId, sourceLanguageId);
            try {
                return SourceTranslation.generate(projectId, sourceLanguageJson, defaultResource);
            } catch (JSONException e) {
                Logger.e(this.getClass().getName(), "Failed to parse the source translation " + SourceTranslation.simple(projectId, sourceLanguageId, defaultResource.getId()).getId(), e);
            }
        }
        return null;
    }

    /**
     * Returns an array of source translations in a project that have met the minimum checking level
     * @param projectId
     */
    public SourceTranslation[] getSourceTranslations(String projectId) {
        List<SourceTranslation> sourceTranslations = new ArrayList<>();
        String[] sourceLanguageIds = getActiveIndex().getSourceLanguages(projectId);
        for(String sourceLanguageId:sourceLanguageIds) {
            String[] resourceIds = getActiveIndex().getResources(projectId, sourceLanguageId);
            for(String resourceId:resourceIds) {
                SourceTranslation sourceTranslation = getSourceTranslation(projectId, sourceLanguageId, resourceId);
                if(sourceTranslation != null && sourceTranslation.getCheckingLevel() >= SOURCE_TRANSLATION_MIN_CHECKING_LEVEL) {
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
        List<SourceTranslation> draftTranslations = new ArrayList<>();
        String[] sourceLanguageIds = getActiveIndex().getSourceLanguages(projectId);
        for(String sourceLanguageId:sourceLanguageIds) {
            String[] resourceIds = getActiveIndex().getResources(projectId, sourceLanguageId);
            for(String resourceId:resourceIds) {
                SourceTranslation sourceTranslation = getSourceTranslation(projectId, sourceLanguageId, resourceId);
                if(sourceTranslation != null && sourceTranslation.getCheckingLevel() < SOURCE_TRANSLATION_MIN_CHECKING_LEVEL) {
                    draftTranslations.add(sourceTranslation);
                }
            }
        }
        return draftTranslations.toArray(new SourceTranslation[draftTranslations.size()]);
    }

    /**
     * Returns an array of notes in the frame
     * @param sourceTranslation
     * @param chapterId
     * @param frameId
     * @return
     */
    public TranslationNote[] getTranslationNotes(SourceTranslation sourceTranslation, String chapterId, String frameId) {
        List<TranslationNote> notes = new ArrayList<>();
        String[] noteIds = getActiveIndex().getNotes(sourceTranslation, chapterId, frameId);
        for(String noteId:noteIds) {
            TranslationNote note = TranslationNote.generate(chapterId, frameId, getActiveIndex().getNote(sourceTranslation, chapterId, frameId, noteId));
            if(note != null) {
                notes.add(note);
            }
        }
        return notes.toArray(new TranslationNote[notes.size()]);
    }

    /**
     * Returns an array of translation words in the frame
     * @param sourceTranslation
     * @param chapterId
     * @param frameId
     * @return
     */
    public TranslationWord[] getTranslationWords(SourceTranslation sourceTranslation, String chapterId, String frameId) {
        List<TranslationWord> words = new ArrayList<>();
        String[] wordIds = getActiveIndex().getWords(sourceTranslation, chapterId, frameId);
        for(String wordId:wordIds) {
            TranslationWord note = TranslationWord.generate(getActiveIndex().getWord(sourceTranslation, wordId));
            if(note != null) {
                words.add(note);
            }
        }
        return words.toArray(new TranslationWord[words.size()]);
    }

    public interface OnProgressListener {
        /**
         * Provides the progress on an operation between 0.0 and 1.0
         * @param progress
         */
        void onProgress(int progress, int max);

        /**
         * Identifes the current task as not quantifiable
         */
        void onIndeterminate();
    }
}
