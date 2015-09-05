package com.door43.translationstudio.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.door43.util.Zip;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
    private final Indexer mServerIndex;
    private final Indexer mAppIndex;
    private final File mLibraryDir;
    private final Indexer mDownloaderIndex;
    private final File mIndexDir;
    private final Context mContext;
    private LibraryUpdates mLibraryUpdates = new LibraryUpdates();
    private static TargetLanguage[] mTargetLanguages;
    private Downloader mDownloader;

    public Library(Context context, File libraryDir, String rootApiUrl) {
        mContext = context;
        mLibraryDir = libraryDir;
        mIndexDir = new File(libraryDir, "index");
        mDownloaderIndex = new Indexer("downloads", mIndexDir);
        mServerIndex = new Indexer("server", mIndexDir);
        mAppIndex = new Indexer("app", mIndexDir);
        mDownloader = new Downloader(mDownloaderIndex, rootApiUrl);
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
            e.printStackTrace();
        }
        return false;
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
            e.printStackTrace();
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
                SourceTranslation sourceTranslation = new SourceTranslation(projectId, sourceLanguageId, resourceId);
                try {
                    int latestResourceModified = mDownloader.getIndex().getResource(sourceTranslation).getInt("date_modified");
                    JSONObject localResource = mAppIndex.getResource(sourceTranslation);
                    int localResourceModified = -1;
                    if(localResource != null) {
                        localResourceModified = localResource.getInt("date_modified");
                    }
                    if(localResourceModified == -1 || localResourceModified < latestResourceModified) {
                        // build update list
                        mLibraryUpdates.addUpdate(new SourceTranslation(projectId, sourceLanguageId, resourceId));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
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
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Returns a list of updates that are available on the server
     * @return
     */
    public LibraryUpdates getAvailableLibraryUpdates() {
        mLibraryUpdates = new LibraryUpdates();
        if(mDownloader.downloadProjectList()) {
            for (String projectId : mDownloader.getIndex().getProjects()) {
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
                    e.printStackTrace();
                }
            }
        }
        try {
            mServerIndex.mergeIndex(mDownloader.getIndex(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mLibraryUpdates;
    }

    /**
     * Downloads updates from the server
     * @param updates
     */
    public Boolean downloadUpdates(LibraryUpdates updates) throws Exception {
        boolean success = true;
        for(String projectId:updates.getUpdatedProjects()) {
            boolean projectDownloadSuccess = true;
            for(String sourceLanguageId:updates.getUpdatedSourceLanguages(projectId)) {
                for(String resourceId:updates.getUpdatedResources(projectId, sourceLanguageId)) {
                    projectDownloadSuccess = downloadSourceTranslationWithoutMerging(new SourceTranslation(projectId, sourceLanguageId, resourceId)) ? projectDownloadSuccess : false;
                    if(!projectDownloadSuccess) {
                        throw new Exception("Failed to download " + projectId + " " + sourceLanguageId + " " + resourceId);
                    }
                }
            }
            success = projectDownloadSuccess ? success : false;
            if(projectDownloadSuccess) {
                try {
                    mAppIndex.mergeProject(projectId, mDownloader.getIndex());
                } catch (IOException e) {
                    e.printStackTrace();
                    success = false;
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
                e.printStackTrace();
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
            Zip.zip(mAppIndex.getIndexDir().getPath(), destFile.getPath());
        } catch (IOException e) {
            e.printStackTrace();
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
            List<TargetLanguage> languages = new ArrayList<>();
            try {
                File languagesFile = new File(mLibraryDir, TARGET_LANGUAGES_FILE);
                String catalog = FileUtils.readFileToString(languagesFile);
                JSONArray json = new JSONArray(catalog);
                for (int i = 0; i < json.length(); i++) {
                    JSONObject item = json.getJSONObject(i);
                    TargetLanguage lang = TargetLanguage.Generate(item);
                    if (lang != null) {
                        languages.add(lang);
                    }
                }
                mTargetLanguages = languages.toArray(new TargetLanguage[languages.size()]);
                return mTargetLanguages;
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            return mTargetLanguages;
        }
        return null;
    }

    /**
     * Returns a list of project categories
     *
     * @param languageId the preferred language in which the category names will be returned. The default is english
     * @return
     */
    public ProjectCategory[] getProjectCategories(String languageId) {
        // TODO: we need to update this to support multiple levels of categories. Right now it only suports ones.
        // we may want to create a second method for retrieving sub categories.

        Map<String, ProjectCategory> categories = new HashMap<>();

        // TODO: we may need the index to generate this info for us for better performance
        String[] projectIds = mAppIndex.getProjects();
        for(String projectId:projectIds) {
            JSONObject projectJson = mAppIndex.getProject(projectId);
            try {
                JSONArray metaJson = projectJson.getJSONArray("meta");
                String categoryId = null;
                JSONObject sourceLanguageJson = getPreferredSourceLanguage(projectId, languageId);
                JSONObject projectLanguageJson = sourceLanguageJson.getJSONObject("project");
                String title = projectLanguageJson.getString("name");
                if(metaJson.length() > 0) {
                    categoryId = metaJson.getString(0);
                    JSONArray metaLanguageJson = projectLanguageJson.getJSONArray("meta");
                    title = metaLanguageJson.getString(0);
                }
                // TODO: we need to provide the icon path
                ProjectCategory cat = new ProjectCategory(title, categoryId, projectId, languageId, 0);
                if(!categories.containsKey(cat.getId())) {
                    categories.put(cat.getId(), cat);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return categories.values().toArray(new ProjectCategory[categories.size()]);
    }

    /**
     * Returns a list of project categories beneath the given category
     * @param parentCategory
     * @return
     */
    public ProjectCategory[] getProjectCategories(ProjectCategory parentCategory) {
        Map<String, ProjectCategory> categories = new HashMap<>();

        // TODO: we may need the index to generate this info for us for better performance
        String[] projectIds = mAppIndex.getProjects();
        for(String projectId:projectIds) {
            JSONObject projectJson = mAppIndex.getProject(projectId);
            try {
                JSONArray metaJson = projectJson.getJSONArray("meta");

                if(metaJson.length() > parentCategory.categoryDepth && metaJson.getString(parentCategory.categoryDepth + 1).equals(parentCategory.categoryId)) {
                    String categoryId = null;
                    JSONObject sourceLanguageJson = getPreferredSourceLanguage(projectId, parentCategory.sourcelanguageId);
                    JSONObject projectLanguageJson = sourceLanguageJson.getJSONObject("project");
                    String title = projectLanguageJson.getString("name");

                    if(metaJson.length() > parentCategory.categoryDepth + 1) {
                        categoryId = metaJson.getString(parentCategory.categoryDepth + 1);
                        JSONArray metaLanguageJson = projectLanguageJson.getJSONArray("meta");
                        title = metaLanguageJson.getString(parentCategory.categoryDepth + 1);
                    }

                    // TODO: we need to provide the icon path
                    ProjectCategory cat = new ProjectCategory(title, categoryId, projectId, parentCategory.sourcelanguageId, parentCategory.categoryDepth + 1);
                    if(!categories.containsKey(cat.getId())) {
                        categories.put(cat.getId(), cat);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return categories.values().toArray(new ProjectCategory[categories.size()]);
    }

    /**
     * Returns the preferred source language if it exists
     *
     * If the source language does not exist it will defaul to english.
     * If english does not exist it will return the first available source language
     * If no source language is available it will return null.
     *
     * @param projectId
     * @param sourceLanguageId
     * @return
     */
    private JSONObject getPreferredSourceLanguage(String projectId, String sourceLanguageId) {
        // preferred language
        JSONObject sourceLanguageJson = mAppIndex.getSourceLanguage(projectId, sourceLanguageId);
        // default (en)
        if(sourceLanguageJson == null) {
            sourceLanguageJson = mAppIndex.getSourceLanguage(projectId, "en");
        }
        // first available
        if(sourceLanguageJson == null) {
            String[] sourceLanguageIds = mAppIndex.getSourceLanguages(projectId);
            if(sourceLanguageIds.length > 0) {
                sourceLanguageJson = mAppIndex.getSourceLanguage(projectId, sourceLanguageIds[0]);
            }
        }
        return sourceLanguageJson;
    }
}
