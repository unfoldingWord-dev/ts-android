package com.door43.translationstudio.projects.data;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.util.FileUtilities;
import com.door43.translationstudio.util.Logger;
import com.door43.translationstudio.util.Security;
import com.door43.translationstudio.util.ServerUtilities;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * The data store handles all of the text and media resources within the app.
 * This class will look for data locally as well as on the server.
 * The api can be found at https://door43.org/en/dev/api/unfoldingword
 */
public class DataStore {
    private static MainApplication mContext;
    private static String SOURCE_TRANSLATIONS_DIR = "sourceTranslations/";
    private static final int API_VERSION = 2;
    // this is used so we can force a cache reset between versions of the app if we make changes to api implimentation
    private static final int API_VERSION_INTERNAL = 2;

    public DataStore(MainApplication context) {
        mContext = context;
    }

    /**
     * Adds a project entry into the project catalog
     * @param json
     */
    public void importProject(String json) {
        File file = new File(mContext.getCacheDir(), "assets/" + projectCatalogPath());
        String catJson = fetchProjectCatalog(false);
        try {
            JSONArray cat = new JSONArray();
            if(catJson != null) {
                cat = new JSONArray(catJson);
            }
            JSONObject proj = new JSONObject(json);
            cat.put(proj);
            FileUtils.writeStringToFile(file, cat.toString());
        } catch (Exception e) {
            Logger.e(this.getClass().getName(), "failed to add the project to the catalog", e);
        }
    }

    /**
     * Adds a source language entry into the languages catalog
     * @param projectId
     * @param json
     */
    public void importSourceLanguage(String projectId, String json) {
        File file = new File(mContext.getCacheDir(), "assets/" + sourceLanguageCatalogPath(projectId));
        String catJson = fetchSourceLanguageCatalog(projectId, false);
        try {
            JSONArray cat = new JSONArray();
            if(catJson != null) {
                cat = new JSONArray(catJson);
            }
            JSONObject lang = new JSONObject(json);
            cat.put(lang);
            FileUtils.writeStringToFile(file, cat.toString());
        } catch (Exception e) {
            Logger.e(this.getClass().getName(), "failed to add the source language to the catalog", e);
        }
    }

    /**
     * Adds a resource entry to the resource catalog
     * @param projectId
     * @param languageId
     * @param json
     */
    public void importResource(String projectId, String languageId, String json) {
        File file = new File(mContext.getCacheDir(), "assets/" + resourceCatalogPath(projectId, languageId));
        String catJson = fetchResourceCatalog(projectId, languageId, false);
        try {
            JSONArray cat = new JSONArray();
            if(catJson != null) {
                cat = new JSONArray(catJson);
            }
            JSONObject res = new JSONObject(json);
            cat.put(res);
            FileUtils.writeStringToFile(file, cat.toString());
        } catch (Exception e) {
            Logger.e(this.getClass().getName(), "failed to add the resource to the catalog", e);
        }
    }

    /**
     * Adds notes to the resources
     * @param projectId
     * @param languageId
     * @param resourceId
     * @param data
     */
    public void importNotes(String projectId, String languageId, String resourceId, String data) {
        File file = new File(mContext.getCacheDir(), "assets/" + notesPath(projectId, languageId, resourceId));
        try {
            FileUtils.writeStringToFile(file, data);
        } catch (IOException e) {
            Logger.e(this.getClass().getName(), "failed to add the notes to the resource", e);
        }
    }

    /**
     * Adds source to the resources
     * @param projectId
     * @param languageId
     * @param resourceId
     * @param data
     */
    public void importSource(String projectId, String languageId, String resourceId, String data) {
        File file = new File(mContext.getCacheDir(), "assets/" + sourcePath(projectId, languageId, resourceId));
        try {
            FileUtils.writeStringToFile(file, data);
        } catch (IOException e) {
            Logger.e(this.getClass().getName(), "failed to add the source to the resource", e);
        }
    }

    /**
     * Adds terms to the resources
     * @param projectId
     * @param languageId
     * @param resourceId
     * @param data
     */
    public void importTerms(String projectId, String languageId, String resourceId, String data) {
        File file = new File(mContext.getCacheDir(), "assets/" + termsPath(projectId, languageId, resourceId));
        try {
            FileUtils.writeStringToFile(file, data);
        } catch (IOException e) {
            Logger.e(this.getClass().getName(), "failed to add the terms to the resource", e);
        }
    }

    /**
     * Downloads a file and places it in the cached assets directory
     * @param path the relative path within the cached assets directory
     * @param urlString the url from which the file will be downloaded
     */
    private void downloadToAssets(String path, String urlString) {
        try {
            URL url = new URL(urlString);
            File file = new File(mContext.getCacheDir(), "assets/" + path);
            ServerUtilities.downloadFile(url, file);
        } catch (MalformedURLException e) {
            Logger.e(this.getClass().getName(), "malformed url", e);
        }
    }

    /**
     * Returns the projects
     * This should not be ran on the main thread when checking the server
     * @param checkServer indicates an updated list of projects should be downloaded from the server
     * @return
     */
    public String fetchProjectCatalog(boolean checkServer) {
        String path = projectCatalogPath();

        if(checkServer) {
            downloadToAssets(path, "https://api.unfoldingword.org/ts/txt/"+API_VERSION+"/catalog.json");
        }
        return loadJSONAsset(path);
    }

    /**
     * Returns the relative path to the project catalog
     * @return
     */
    public static String projectCatalogPath() {
        return SOURCE_TRANSLATIONS_DIR + "projects_catalog.json";
    }

    /**
     * Returns the source languages for a specific project
     * This should not be ran on the main thread when checking the server
     * @param projectId the slug of the project for which languages will be returned
     * @param checkServer indicates an updated list of projects should be downloaded from the server
     * @return
     */
    public String fetchSourceLanguageCatalog(String projectId, boolean checkServer) {
        String path = sourceLanguageCatalogPath(projectId);

        if(checkServer) {
            downloadToAssets(path, sourceLanguageCatalogUrl(projectId));
        }
        return loadJSONAsset(path);
    }

    /**
     * Returns the relative path to the source language catalog
     * @param projectId
     * @return
     */
    public static String sourceLanguageCatalogPath(String projectId) {
        return SOURCE_TRANSLATIONS_DIR + projectId + "/languages_catalog.json";
    }

    /**
     * generates the source language catalog url
     * @param projectId
     * @return
     */
    public String sourceLanguageCatalogUrl(String projectId) {
        return "https://api.unfoldingword.org/ts/txt/"+API_VERSION+"/"+projectId+"/languages.json";
    }

    /**
     * Returns the resources for a specific language
     * This should not be ran on the main thread when checking the server
     * @param projectId the id of the project that contains the language
     * @param languageId the id of the language for which resources will be returned
     * @param checkServer indicates an updated list of projects should be downloaded from the server
     * @return
     */
    public String fetchResourceCatalog(String projectId, String languageId, boolean checkServer) {
        String path = resourceCatalogPath(projectId, languageId);

        if(checkServer) {
            downloadToAssets(path, resourceCatalogUrl(projectId, languageId));
        }
        return loadJSONAsset(path);
    }

    /**
     * Returns the relative path to the resource catalog
     * @param projectId
     * @param languageId
     * @return
     */
    public static String resourceCatalogPath(String projectId, String languageId) {
        return SOURCE_TRANSLATIONS_DIR + projectId + "/" + languageId + "/resources_catalog.json";
    }

    /**
     * generates the resource catalog url
     * @param projectId
     * @param languageId
     * @return
     */
    public String resourceCatalogUrl(String projectId, String languageId) {
        return "https://api.unfoldingword.org/ts/txt/"+API_VERSION+"/"+projectId+"/"+languageId+"/resources.json";
    }

    /**
     * Returns the target languages
     */
    public String fetchTargetLanguageCatalog() {
        // TODO: check for updates on the server
        // https://api.unfoldingword.org/ts/txt/1/langnames.json
        String path = "target_languages.json";
        return loadJSONAsset(path);
    }

    /**
     * Returns the key terms.
     * @param projectId
     * @param languageId
     * @param resourceId
     * @param checkServer indicates an updated list of terms should be downloaded from the server
     * @return
     */
    public String fetchTerms(String projectId, String languageId, String resourceId, boolean checkServer) {
        String path = termsPath(projectId, languageId, resourceId);

        if(checkServer) {
            downloadToAssets(path, "https://api.unfoldingword.org/ts/txt/"+API_VERSION+"/"+projectId+"/"+languageId+"/"+resourceId+"/terms.json");
        }
        return loadJSONAsset(path);
    }

    /**
     * Downloads and returns the key terms.
     * Rather than using the standard download url this method allows you to specify from which url
     * to download the terms. This is especially helpful when cross referencing api's.
     * @param projectId
     * @param languageId
     * @param resourceId
     * @param urlString the url from which the terms will be downloaded
     * @return
     */
    public String fetchTerms(String projectId, String languageId, String resourceId, String urlString) {
        String path = termsPath(projectId, languageId, resourceId);

        downloadToAssets(path, urlString);

        return loadJSONAsset(path);
    }

    /**
     * Returns the path to the terms file
     * @param projectId
     * @param languageId
     * @param resourceId
     * @return
     */
    public String termsPath(String projectId, String languageId, String resourceId) {
        return SOURCE_TRANSLATIONS_DIR + projectId + "/" + languageId + "/" + resourceId + "/terms.json";
    }

    /**
     * Returns the notes
     * @param projectId
     * @param languageId
     * @param resourceId
     * @param checkServer indicates an updated list of notes should be downloaded from the server
     * @return
     */
    public String fetchNotes(String projectId, String languageId, String resourceId, boolean checkServer) {
        String path = notesPath(projectId, languageId, resourceId);

        if(checkServer) {
            downloadToAssets(path, "https://api.unfoldingword.org/ts/txt/"+API_VERSION+"/"+projectId+"/"+languageId+"/"+resourceId+"/notes.json");
        }
        return loadJSONAsset(path);
    }

    /**
     * Downloads and returns the notes.
     * Rather than using the standard download url this method allows you to specify from which url
     * to download the notes. This is especially helpful when cross referencing api's.
     * @param projectId
     * @param languageId
     * @param resourceId
     * @param urlString the url from which the notes will be downloaded
     * @return
     */
    public String fetchNotes(String projectId, String languageId, String resourceId, String urlString) {
        String path = notesPath(projectId, languageId, resourceId);

        // urls that include extra parameters (date_modified) provide better download caching and optimization.
//        if(urlString.split("\\?").length > 0) {
//            String key = Security.md5(urlString);
//            path = new File(new File(path).getParentFile(), "notes.cache").getAbsolutePath();
//        }

        downloadToAssets(path, urlString);

        return loadJSONAsset(path);
    }

    /**
     * Returns the path to the resource file
     * @param projectId
     * @param languageId
     * @param resourceId
     * @return
     */
    public String notesPath(String projectId, String languageId, String resourceId) {
        return SOURCE_TRANSLATIONS_DIR + projectId + "/" + languageId + "/" + resourceId + "/notes.json";
    }

    /**
     * Returns the source text
     * @param projectId
     * @param languageId
     * @param resourceId
     * @param checkServer indicates an updated version of the source should be downloaded from the server
     * @return
     */
    public String fetchSource(String projectId, String languageId, String resourceId, boolean checkServer) {
        String path = sourcePath(projectId, languageId, resourceId);

        if(checkServer) {
            downloadToAssets(path, "https://api.unfoldingword.org/ts/txt/"+API_VERSION+"/"+projectId+"/"+languageId+"/"+resourceId+"/source.json");
        }
        return loadJSONAsset(path);
    }

    /**
     * Downloads and returns the source.
     * Rather than using the standard download url this method allows you to specify from which url
     * to download the source. This is especially helpful when cross referencing api's.
     * @param projectId
     * @param languageId
     * @param resourceId
     * @param urlString the url from which the source will be downloaded
     * @return
     */
    public String fetchSource(String projectId, String languageId, String resourceId, String urlString) {
        String path = sourcePath(projectId, languageId, resourceId);

        downloadToAssets(path, urlString);

        return loadJSONAsset(path);
    }

    /**
     * Returns the path to the source file
     * @param projectId
     * @param languageId
     * @param resourceId
     * @return
     */
    public String sourcePath(String projectId, String languageId, String resourceId) {
        return SOURCE_TRANSLATIONS_DIR + projectId + "/" + languageId + "/" + resourceId + "/source.json";
    }


    /**
     * Load json from the local assets
     * @param path path to the json file within the assets directory
     * @return the string contents of the json file
     */
    private String loadJSONAsset(String path) {
        File cacheDir = new File(mContext.getCacheDir(), "assets");

        // verify the cached assets match the expected server api level
        File cacheVersionFile = new File(cacheDir, ".cache_api_version");
        if(cacheVersionFile.exists() && cacheVersionFile.isFile()) {
            try {
                // version is composed of API_VERSION.API_VERSION_INTERNAL e.g. "2.1"
                String[] version = FileUtils.readFileToString(cacheVersionFile).split("\\.");
                if(Integer.parseInt(version[0]) != API_VERSION || Integer.parseInt(version[1]) != API_VERSION_INTERNAL) {
                    cacheVersionFile.delete();
                }
            } catch (Exception e) {
                Logger.e(this.getClass().getName(), "failed to read the cached assets api version", e);
                cacheVersionFile.delete();
            }
        }
        if(!cacheVersionFile.exists() || !cacheVersionFile.isFile()) {
            Logger.i(this.getClass().getName(), "clearing the asset cache to support api version "+API_VERSION+"."+API_VERSION_INTERNAL);
            FileUtilities.deleteRecursive(cacheDir);
            // record cache version
            cacheDir.mkdirs();
            try {
                cacheVersionFile.createNewFile();
                FileUtils.write(cacheVersionFile, API_VERSION+"."+API_VERSION_INTERNAL);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // retrieve the asset
        File cacheAsset = new File(cacheDir, path);
        String name = cacheAsset.getName();
        File linkedAsset = new File(cacheAsset.getParentFile(), FilenameUtils.removeExtension(name) + ".link");

        if(cacheAsset.exists() && cacheAsset.isFile()) {
            // attempt to load from the cached assets first. These will be the most up to date
            try {
                return FileUtilities.getStringFromFile(cacheAsset);
            } catch (IOException e) {
                Logger.e(this.getClass().getName(), "failed to load cached asset", e);
            }
        } else if(linkedAsset.exists() && linkedAsset.isFile()){
            // attempt to load a linked asset (references some global asset that is shared among projects)
            try {
                String link = FileUtilities.getStringFromFile(linkedAsset);
                return loadLinkedJSONAsset(link);
            } catch (IOException e) {
                Logger.e(this.getClass().getName(), "failed to load linked asset", e);
            }
        }
        // load the packaged asset by default
        return loadPackagedJSONAsset(path);
    }

    /**
     * Loads the json from a linked asset (those shared between multiple projects)
     * NOTE: this is technically the new way assets are loaded, but we need to provide backwards compatability.
     * @param link
     * @return
     */
    private String loadLinkedJSONAsset(String link) {
        // TODO: do something here.
        return null;
    }

    /**
     * Loads json from the packaged assets (those distributed with the app)
     * @param path
     * @return
     */
    private String loadPackagedJSONAsset(String path) {
        try {
            File asset = mContext.getAssetAsFile(path);
            if(asset != null) {
                return FileUtilities.getStringFromFile(mContext.getAssetAsFile(path));
            } else {
                return null;
            }
        } catch (Exception e) {
            Logger.e(this.getClass().getName(), "failed to load the packaged asset "+path, e);
            return null;
        }
    }
}
