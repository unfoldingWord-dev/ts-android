package com.door43.translationstudio.projects.data;

import android.content.SharedPreferences;
import android.net.Uri;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.util.AppContext;
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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This class manages json assets within the app.
 * Caching is accomlished using an advanced lazy-linking system which allows redundant data
 * to be reused efficiently.
 *
 * API specs can be found at https://door43.org/en/dev/api/translationstudio
 */
public class DataStore {
    private static MainApplication mContext;
    private static String SOURCE_TRANSLATIONS_DIR = "sourceTranslations/";
    private static final String PREFERENCES_TAG = "com.door43.translationstudio.assets";
    private SharedPreferences mSettings;
    private static final int API_VERSION = 2;
    // this is used so we can force a cache reset between versions of the app if we make changes to api implimentation
    private static final int API_VERSION_INTERNAL = 4;

    public DataStore(MainApplication context) {
        mContext = context;
        mSettings = mContext.getSharedPreferences(PREFERENCES_TAG, mContext.MODE_PRIVATE);
    }

    /**
     * Adds a project entry into the project catalog
     * @param json
     */
    public void importProject(String json) {
        File file = new File(cachedAssetsDir(),projectCatalogPath());
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
        File file = new File(cachedAssetsDir(), sourceLanguageCatalogPath(projectId));
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
        File file = new File(cachedAssetsDir(), resourceCatalogPath(projectId, languageId));
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
        File file = new File(cachedAssetsDir(), notesPath(projectId, languageId, resourceId));
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
        File file = new File(cachedAssetsDir(), sourcePath(projectId, languageId, resourceId));
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
        File file = new File(cachedAssetsDir(), termsPath(projectId, languageId, resourceId));
        try {
            FileUtils.writeStringToFile(file, data);
        } catch (IOException e) {
            Logger.e(this.getClass().getName(), "failed to add the terms to the resource", e);
        }
    }

    /**
     * Decrements the asset link count
     * when the count reaches 0 the asset is removed
     * @param key
     */
    private void decrementAssetLink(String key) {
        int numLinks = mSettings.getInt(key, 0) - 1;
        SharedPreferences.Editor editor = mSettings.edit();
        if(numLinks <=0 ) {
            // asset is orphaned
            editor.remove(key);
            editor.remove(key+"_modified");
            editor.remove(key + "_alias");
            File file = getLinkedAsset(key);
            file.delete();
        } else {
            // decrement link count
            editor.putInt(key, numLinks);
        }
        editor.apply();
    }

    /**
     * Increments the asset link count
     * @param key
     */
    private void incrementAssetLink(String key) {
        int numLinks = mSettings.getInt(key, 0) + 1;
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putInt(key, numLinks);
        editor.apply();
    }

    /**
     * Creates an alias for a deprecated asset.
     * This allows outdated links to be updated lazily
     * @param newKey key to new asset
     * @param oldKey key to old (deleted) asset
     */
    private void aliasAsset(String newKey, String oldKey) {
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putString(oldKey+"_alias", newKey);
        editor.apply();
    }

    /**
     * Resolves the key to the linked asset.
     * This will automatically update outdated links
     * @param link
     * @return the key to the linked asset
     */
    private String resolveLink(File link) {
        if(link != null) {
            try {
                // resolve key aliases
                String key;
                String alias = FileUtils.readFileToString(link);
                String originalKey = alias;
                do {
                    key = alias;
                    alias = mSettings.getString(alias+"_alias", null);
                } while(alias != null);

                // update link if nessesary
                if(!key.equals(originalKey)) {
                    linkAsset(key, link);
                }

                return key;
            } catch (IOException e) {
                Logger.e(this.getClass().getName(), "failed to read the key from the asset link " + link.getAbsolutePath(), e);
            }
        } else {
            Logger.e(this.getClass().getName(), "received a null asset link");
        }
        return null;
    }

    /**
     * Links an asset to a particular file path
     * @param key the asset key
     * @param path relative path to the link file
     */
    private void linkAsset(String key, String path) {
        File link = new File(cachedAssetsDir(), path);
        linkAsset(key, link);
    }

    /**
     * Links an asset to a particular file path
     * @param key the aset key
     * @param link the link file
     */
    private void linkAsset(String key, File link) {
        if(!FilenameUtils.getExtension(link.getName()).equals("link")) {
            // convert path to a link
            link = new File(link.getParentFile(), FilenameUtils.removeExtension(link.getName()) + ".link");
        }

        // resolve old link
        if(link.exists()) {
            try {
                String oldKey = FileUtils.readFileToString(link);
                if(oldKey != null && !oldKey.isEmpty()) {
                    // process old key
                    if(oldKey.equals(key)) {
                        // the link already exists
                        return;
                    } else {
                        // unlink from old asset
                        decrementAssetLink(oldKey);
                        // add alias for lazy updates to outdated links
                        aliasAsset(key, oldKey);
                    }
                }
            } catch (IOException e) {
                Logger.e(this.getClass().getName(), "failed to read link file "+link.getAbsolutePath(), e);
            }
        }

        // create new link
        try {
            FileUtils.writeStringToFile(link, key);
        } catch (IOException e) {
            Logger.e(this.getClass().getName(), "failed to link the asset "+key+ " to "+link.getAbsolutePath(), e);
        }
    }

    /**
     * String returns the file to an asset
     * @param key
     */
    private File getLinkedAsset(String key) {
        return new File(cachedAssetsDir(), "data/" + key);
    }

    /**
     * Downloads a file and places it in the cached assets directory
     * @param urlString the url from which the file will be downloaded
     * @return the asset key
     */
    private String downloadAsset(String urlString) {
        SharedPreferences.Editor editor = mSettings.edit();
        Uri uri = Uri.parse(urlString);
        String key = Security.md5(uri.getHost()+uri.getPath());
        File file = new File(cachedAssetsDir(), "data/" + key);
        String dateModifiedRaw = uri.getQueryParameter("date_modified");

        // identify existing data
        if(file.exists() && dateModifiedRaw != null) {
            int dateModified = Integer.parseInt(dateModifiedRaw);
            try {
                int oldDateModified = mSettings.getInt(key + "_modified", 0);
                if (dateModified <= oldDateModified) {
                    // current data is up to date
                    return key;
                }
            } catch (Exception e) {
                Logger.e(this.getClass().getName(), "invalid datetime value " +dateModifiedRaw, e);
            }
        }

        // perform download
        try {
            URL url = new URL(urlString);
            ServerUtilities.downloadFile(url, file);
            // record date modified if provided
            if(dateModifiedRaw != null) {
                try {
                    editor.putInt(key + "_modified", Integer.parseInt(dateModifiedRaw));
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "invalid datetime value " +dateModifiedRaw, e);
                }
            } else {
                editor.remove(key+"_modified");
            }
            editor.apply();
            Logger.i(this.getClass().getName(), "downloaded new/updated asset from "+urlString);
            return key;
        } catch (MalformedURLException e) {
            Logger.e(this.getClass().getName(), "malformed url", e);
        }
        return null;
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
            String key = downloadAsset("https://api.unfoldingword.org/ts/txt/" + API_VERSION + "/catalog.json");
            linkAsset(key, path);
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
            String key = downloadAsset(sourceLanguageCatalogUrl(projectId));
            linkAsset(key, path);
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
            String key = downloadAsset(resourceCatalogUrl(projectId, languageId));
            linkAsset(key, path);
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
            String key = downloadAsset("https://api.unfoldingword.org/ts/txt/" + API_VERSION + "/" + projectId + "/" + languageId + "/" + resourceId + "/terms.json");
            linkAsset(key, path);
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

        String key = downloadAsset(urlString);
        linkAsset(key, path);

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
            String key = downloadAsset("https://api.unfoldingword.org/ts/txt/" + API_VERSION + "/" + projectId + "/" + languageId + "/" + resourceId + "/notes.json");
            linkAsset(key, path);
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

        String key = downloadAsset(urlString);
        linkAsset(key, path);

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
            String key = downloadAsset("https://api.unfoldingword.org/ts/txt/" + API_VERSION + "/" + projectId + "/" + languageId + "/" + resourceId + "/source.json");
            linkAsset(key, path);
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

        String key = downloadAsset(urlString);
        linkAsset(key, path);

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
     * Returns the file for the cached assets directory
     * @return
     */
    public static File cachedAssetsDir() {
        return new File(mContext.getCacheDir(), "assets");
    }

    /**
     * Validates the asset cache version
     */
    private void validateAssetCache() {
        File cacheDir = cachedAssetsDir();
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
                Logger.e(this.getClass().getName(), "failed to create the asset version file", e);
            }
        }
    }

    /**
     * Load json from the local assets
     * @param path path to the json file within the assets directory
     * @return the string contents of the json file
     */
    private String loadJSONAsset(String path) {
        File cacheDir = cachedAssetsDir();

        validateAssetCache();

        File cachedAsset = new File(cacheDir, path);
        File link = new File(cachedAsset.getParentFile(), FilenameUtils.removeExtension(cachedAsset.getName()) + ".link");

        // resolve links
        if(link.exists()) {
            String key = resolveLink(link);
            if(key != null) {
                cachedAsset = getLinkedAsset(key);
            }
        }

        // load the asset
        if(cachedAsset.exists() && cachedAsset.isFile()) {
            try {
                return FileUtilities.getStringFromFile(cachedAsset);
            } catch (IOException e) {
                Logger.e(this.getClass().getName(), "failed to load cached asset", e);
            }
        }

        // load the packaged asset by default
        return loadPackagedJSONAsset(path);
    }

    /**
     * Loads json from the packaged assets (those distributed with the app)
     * @param path
     * @return
     */
    private String loadPackagedJSONAsset(String path) {
        try {
            // TRICKY: this method will actually move packaged assets into the cached assets dir.
//            File asset = mContext.getAssetAsFile(path);
//            if(asset != null) {
//                return FileUtilities.getStringFromFile(mContext.getAssetAsFile(path));
//            } else {
//                return null;
//            }
            if(AppContext.assetExists(path)) {
                InputStream is = mContext.getAssets().open(path);
                return FileUtilities.convertStreamToString(is);
            } else {
                return null;
            }
        } catch (Exception e) {
            Logger.e(this.getClass().getName(), "failed to load the packaged asset "+path, e);
            return null;
        }
    }
}
