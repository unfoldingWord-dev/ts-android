package com.door43.translationstudio.projects.data;

import android.content.SharedPreferences;
import android.net.Uri;

import com.door43.translationstudio.MainApplication;
import com.door43.util.FileUtilities;
import com.door43.util.Logger;
import com.door43.util.Security;
import com.door43.util.ServerUtilities;

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
    private static final String TEMP_ASSET_PREFIX = "temp_";
    private SharedPreferences mSettings;
    private static final int API_VERSION = 2;
    // this is used so we can force a cache reset between versions of the app if we make changes to api implimentation
    private static final int API_VERSION_INTERNAL = 8;

    public DataStore(MainApplication context) {
        mContext = context;
        mSettings = mContext.getSharedPreferences(PREFERENCES_TAG, mContext.MODE_PRIVATE);
    }

    /**
     * Adds a project entry into the project catalog
     * @param newProjectString
     */
    public void importProject(String newProjectString) {
        String path = projectCatalogPath();
        String key = getKey(projectCatalogUri());
        File file = getAsset(key);

        String catalog = pullProjectCatalog(false, false);
        try {
            JSONArray json = new JSONArray();
            if(catalog != null) {
                json = new JSONArray(catalog);
            }
            JSONObject jsonNewProj = new JSONObject(newProjectString);
            // remove old project
            int oldProjIndex = -1;
            for(int i=0; i<json.length(); i++) {
                JSONObject jsonProj = json.getJSONObject(i);
                if(jsonProj.getString("slug").equals(jsonNewProj.getString("slug"))) {
                    oldProjIndex = i;
                    break;
                }
            }
            // add new project
            if(oldProjIndex != -1) {
                json.put(oldProjIndex, jsonNewProj);
            } else {
                json.put(jsonNewProj);
            }
            FileUtils.writeStringToFile(file, json.toString());
            linkAsset(key, path);
        } catch (Exception e) {
            Logger.e(this.getClass().getName(), "failed to add the project to the catalog", e);
        }
    }

    /**
     * Adds a source language entry into the languages catalog
     * @param projectId
     * @param newLanguageString
     */
    public void importSourceLanguage(String projectId, String newLanguageString) {
        String path = sourceLanguageCatalogPath(projectId);
        String key = getKey(sourceLanguageCatalogUri(projectId));
        File file = getAsset(key);
        String catalog = pullSourceLanguageCatalog(projectId, false, false);
        try {
            JSONArray json = new JSONArray();
            if(catalog != null) {
                json = new JSONArray(catalog);
            }
            JSONObject jsonNewLang = new JSONObject(newLanguageString);
            // remove old language
            int oldLangIndex = -1;
            for(int i=0; i<json.length(); i++) {
                JSONObject jsonLang = json.getJSONObject(i).getJSONObject("language");
                if(jsonLang.getString("slug").equals(jsonNewLang.getJSONObject("language").getString("slug"))) {
                    oldLangIndex = i;
                    break;
                }
            }
            // add new language
            if(oldLangIndex != -1) {
                json.put(oldLangIndex, jsonNewLang);
            } else {
                json.put(jsonNewLang);
            }
            FileUtils.writeStringToFile(file, json.toString());
            linkAsset(key, path);
        } catch (Exception e) {
            Logger.e(this.getClass().getName(), "failed to add the source language to the catalog", e);
        }
    }

    /**
     * Adds a resource entry to the resource catalog
     * @param projectId
     * @param languageId
     * @param newResString
     */
    public void importResource(String projectId, String languageId, String newResString) {
        String path = resourceCatalogPath(projectId, languageId);
        String key = getKey(resourceCatalogUri(projectId, languageId));
        File file = getAsset(key);
        String catalog = pullResourceCatalog(projectId, languageId, false, false);
        try {
            JSONArray json = new JSONArray();
            if(catalog != null) {
                json = new JSONArray(catalog);
            }
            JSONObject jsonNewRes = new JSONObject(newResString);
            // remove old resource
            int oldResIndex = -1;
            for(int i=0; i<json.length(); i++) {
                JSONObject jsonRes = json.getJSONObject(i);
                if(jsonRes.getString("slug").equals(jsonNewRes.getString("slug"))) {
                    oldResIndex = i;
                    break;
                }
            }
            // add new resource
            if(oldResIndex != -1) {
                json.put(oldResIndex, jsonNewRes);
            } else {
                json.put(jsonNewRes);
            }
            FileUtils.writeStringToFile(file, json.toString());
            linkAsset(key, path);
        } catch (Exception e) {
            Logger.e(this.getClass().getName(), "failed to add the resource to the catalog", e);
        }
    }

    /**
     * Adds notes to the resources
     * Notes may be located at an arbitrary location. If the uri is not null this custom location will
     * be used when generating the key
     * @param projectId
     * @param languageId
     * @param resourceId
     * @param notesUri The uri from which the notes originated
     * @param data
     */
    public void importNotes(String projectId, String languageId, String resourceId, Uri notesUri, String data) {
        String path = notesPath(projectId, languageId, resourceId);
        String key;
        if(notesUri != null) {
            key = getKey(notesUri);
        } else {
            key = getKey(notesUri(projectId, languageId, resourceId));
        }
        File file = getAsset(key);
        try {
            FileUtils.writeStringToFile(file, data);
            linkAsset(key, path);
        } catch (IOException e) {
            Logger.e(this.getClass().getName(), "failed to add the notes to the resource", e);
        }
    }

    /**
     * Adds source to the resources
     * Source may be located at an arbitrary location. If the uri is not null this custom location will
     * be used when generating the key
     * @param projectId
     * @param languageId
     * @param resourceId
     * @param sourceUri The uri from which the source originated
     * @param data
     */
    public void importSource(String projectId, String languageId, String resourceId, Uri sourceUri, String data) {
        String path = sourcePath(projectId, languageId, resourceId);
        String key;
        if(sourceUri != null) {
            key = getKey(sourceUri);
        } else {
            key = getKey(sourceUri(projectId, languageId, resourceId));
        }
        File file = getAsset(key);
        try {
            FileUtils.writeStringToFile(file, data);
            linkAsset(key, path);
        } catch (IOException e) {
            Logger.e(this.getClass().getName(), "failed to add the source to the resource", e);
        }
    }

    /**
     * Adds terms to the resources
     * Terms may be located at an arbitrary location. If the uri is not null this custom location will
     * be used when generating the key
     * @param projectId
     * @param languageId
     * @param resourceId
     * @param termsUri The uri from which the terms originated
     * @param data
     */
    public void importTerms(String projectId, String languageId, String resourceId, Uri termsUri, String data) {
        String path = termsPath(projectId, languageId, resourceId);
        String key;
        if(termsUri != null) {
            key = getKey(termsUri);
        } else {
            key = getKey(termsUri(projectId, languageId, resourceId));
        }
        File file = getAsset(key);
        try {
            FileUtils.writeStringToFile(file, data);
            linkAsset(key, path);
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
            editor.remove(key + "_modified");
            editor.remove(key + "_alias");
            File file = getAsset(key);
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
        editor.putString(oldKey + "_alias", newKey);
        editor.apply();
    }

    /**
     * Resolves the key to the linked cached asset.
     * This will automatically update outdated links
     * @param file
     * @return the key to the linked asset
     */
    private String resolveLink(File file) {
        if(file != null) {
            // ensure the file extension is correct
            String linkPath = FilenameUtils.removeExtension(file.getAbsolutePath()) + ".link";
            File link = new File(linkPath);

            if(link.exists() && link.isFile()) {
                try {
                    // resolve key aliases
                    String key;
                    String alias = FileUtils.readFileToString(link);
                    if (alias != null) alias = alias.trim();
                    String originalKey = alias;
                    do {
                        key = alias;
                        alias = mSettings.getString(alias + "_alias", null);
                    } while (alias != null);
                    if (key != null) key = key.trim();

                    // update link if nessesary
                    if (!key.equals(originalKey)) {
                        linkAsset(key, link);
                    }

                    return key;
                } catch (IOException e) {
                    Logger.e(this.getClass().getName(), "failed to read the key from the asset link " + link.getAbsolutePath(), e);
                }
            }
        } else {
            Logger.e(this.getClass().getName(), "received a null asset link");
        }
        return null;
    }

    /**
     * Resolves the key to the linked packaged asset.
     * Packaged assets are read only so nothing is updated.
     * They are also always direct links so no alias lookup is nessesary.
     * @param link
     * @return the key to the linked asset
     */
    private String resolvePackagedLink(String link) {
        if(link != null) {
            try {
                // resolve key
                InputStream is = mContext.getAssets().open(link);
                String key = FileUtilities.convertStreamToString(is);
                if(key != null) key = key.trim();
                return key;
            } catch (IOException e) {
                // This is common. The file was not found
            }
        } else {
            Logger.e(this.getClass().getName(), "received a null packaged asset link");
        }
        return null;
    }

    /**
     * Links an asset to a particular file path
     * @param key the asset key
     * @param path relative path to the link file
     */
    private void linkAsset(String key, String path) {
        File link = new File(assetsDir(), path);
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
     * Returns the file to a temp asset
     * @param key
     * @return
     */
    public File getTempAsset(String key) {
        return getLinkedAsset(key, tempAssetsDir(), TEMP_ASSET_PREFIX);
    }

    /**
     * returns the file to an asset
     * @param key
     */
    public File getAsset(String key) {
        return getLinkedAsset(key, assetsDir(), "");
    }

    /**
     * returns the file to an asset
     * @param key
     */
    private File getLinkedAsset(String key, File dir, String prefix) {
        return new File(dir, prefix + "data/" + key);
    }

    /**
     * Generates the data key from a uri
     * @param uri
     * @return
     */
    public static String getKey(Uri uri) {
        return Security.md5(uri.getHost()+uri.getPath());
    }

    /**
     * Downloads an asset to the device
     * @param uri the uri from which the file will be downloaded
     * @param ignoreCache indicates that the cache should be ignored when determining whether or not to download
     * @return the asset key
     */
    private String downloadAsset(Uri uri, boolean ignoreCache) {
        return downloadFile(uri, assetsDir(), "", ignoreCache);
    }

    /**
     * Downloads a temp asset to the device
     * @param uri the uri from which the file will be downloaded
     * @param ignoreCache indicates that the cache should be ignored when determining whether or not to download
     * @return the asset key
     */
    private String downloadTempAsset(Uri uri, boolean ignoreCache) {
        return downloadFile(uri, tempAssetsDir(), TEMP_ASSET_PREFIX, ignoreCache);
    }

    /**
     * Downloads an asset to the device
     * @param uri the uri from which the file will be downloaded
     * @param prefix the asset prefix. This allows you to download and track separate groups of assets
     * @param ignoreCache indicates that the cache should be ignored when determining whether or not to download
     * @return the asset key
     */
    private String downloadFile(Uri uri, File directory, String prefix, boolean ignoreCache) {
        SharedPreferences.Editor editor = mSettings.edit();
        String key = getKey(uri);
        File file = new File(directory, prefix + "data/" + key);
        String dateModifiedRaw = uri.getQueryParameter("date_modified");
        boolean fileExists = file.exists();
        // identify existing data
        if(!ignoreCache  && fileExists && dateModifiedRaw != null) {
            int dateModified = Integer.parseInt(dateModifiedRaw);
            try {
                int oldDateModified = mSettings.getInt(prefix + key + "_modified", 0);
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
            URL url = new URL(uri.toString());
            ServerUtilities.downloadFile(url, file);
            // record date modified if provided
            if(dateModifiedRaw != null) {
                try {
                    editor.putInt(prefix + key + "_modified", Integer.parseInt(dateModifiedRaw));
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "invalid datetime value " +dateModifiedRaw, e);
                }
            } else {
                editor.remove(prefix + key+"_modified");
            }
            editor.apply();
            if(fileExists) {
//                Logger.i(this.getClass().getName(), "downloaded updated asset from " + uri);
            } else {
//                Logger.i(this.getClass().getName(), "downloaded new asset from " + uri);
            }
            return key;
        } catch (MalformedURLException e) {
            Logger.e(this.getClass().getName(), "malformed url", e);
        }
        return null;
    }

    /**
     * Returns the projects
     * This should not be ran on the main thread when checking the server.
     * If downloaded this will replace the current project catalog
     * @param checkServer indicates an updated list of projects should be downloaded from the server
     * @param ignoreCache indicates that the cache should be ignored when determining whether or not to download
     * @return
     */
    public String pullProjectCatalog(boolean checkServer, boolean ignoreCache) {
        String path = projectCatalogPath();

        if(checkServer) {
            String key = downloadAsset(projectCatalogUri(), ignoreCache);
            linkAsset(key, path);
        }
        return loadJSONAsset(path);
    }

    /**
     * Downloads the project catalog from the server and stores it in the temp cache
     * @param ignoreCache
     * @return
     */
    public String fetchProjectCatalog(boolean ignoreCache) {
        String key = downloadTempAsset(projectCatalogUri(), ignoreCache);
        try {
            return FileUtils.readFileToString(getTempAsset(key));
        } catch (IOException e) {
            Logger.e(this.getClass().getName(), "Failed to read the downloaded project catalog", e);
            return "";
        }
    }

    /**
     * Returns the relative path to the project catalog
     * @return
     */
    public static String projectCatalogPath() {
        return SOURCE_TRANSLATIONS_DIR + "projects_catalog.json";
    }

    /**
     * Returns the project catalog uri
     * @return
     */
    public static Uri projectCatalogUri() {
        return Uri.parse("https://api.unfoldingword.org/ts/txt/" + API_VERSION + "/catalog.json");
    }

    /**
     * Returns the source languages for a specific project
     * This should not be ran on the main thread when checking the server
     * If downloaded this will replace the current source language catalog
     * @param projectId the slug of the project for which languages will be returned
     * @param checkServer indicates an updated list of projects should be downloaded from the server
     * @param ignoreCache indicates that the cache should be ignored when determining whether or not to download
     * @return
     */
    public String pullSourceLanguageCatalog(String projectId, boolean checkServer, boolean ignoreCache) {
        String path = sourceLanguageCatalogPath(projectId);

        if(checkServer) {
            String key = downloadAsset(sourceLanguageCatalogUri(projectId), ignoreCache);
            linkAsset(key, path);
        }
        return loadJSONAsset(path);
    }

    /**
     * Downloads the source language catalog from the server and stores it in the temp cache
     * @deprecated we can get this functionality by using fetchTempAsset and sourceLanguageCatalogUri
     * @param ignoreCache
     * @return
     */
    public String fetchSourceLanguageCatalog(String projectId, boolean ignoreCache) {
        String key = downloadTempAsset(sourceLanguageCatalogUri(projectId), ignoreCache);
        try {
            return FileUtils.readFileToString(getTempAsset(key));
        } catch (IOException e) {
            Logger.e(this.getClass().getName(), "Failed to read the downloaded source language catalog", e);
            return "";
        }
    }

    /**
     * Downloads a temporary asset and returns it's contents
     * @param uri
     * @param ignoreCache If set to true the asset will be downloaded every time
     * @return
     */
    public String fetchTempAsset(Uri uri, boolean ignoreCache) {
        String key = downloadTempAsset(uri, ignoreCache);
        try {
            return FileUtils.readFileToString(getTempAsset(key));
        } catch (IOException e) {
            Logger.w(this.getClass().getName(), "Failed to read the downloaded the temp asset from " + uri.toString(), e);
            return "";
        }
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
     * generates the source language catalog uri
     * @param projectId
     * @return
     */
    public Uri sourceLanguageCatalogUri(String projectId) {
        return Uri.parse("https://api.unfoldingword.org/ts/txt/"+API_VERSION+"/"+projectId+"/languages.json");
    }

    /**
     * Returns the resources for a specific language
     * This should not be ran on the main thread when checking the server
     * If downloaded this will replace the current resources catalog
     * @param projectId the id of the project that contains the language
     * @param languageId the id of the language for which resources will be returned
     * @param checkServer indicates an updated list of projects should be downloaded from the server
     * @param ignoreCache indicates that the cache should be ignored when determining whether or not to download
     * @return
     */
    public String pullResourceCatalog(String projectId, String languageId, boolean checkServer, boolean ignoreCache) {
        String path = resourceCatalogPath(projectId, languageId);

        if(checkServer) {
            String key = downloadAsset(resourceCatalogUri(projectId, languageId), ignoreCache);
            linkAsset(key, path);
        }
        return loadJSONAsset(path);
    }

    /**
     * Returns the resources for the specified language.
     * This should not be ranon the main thread.
     * If downloaded this will replace the current resources catalog
     * @param projectId the id of the project that contains the language
     * @param languageId the id of the language for which resources will be returned
     * @param uri the uri from which to download the resources
     * @param ignoreCache indicates that the cache should be ignored when determining whether or not to download
     * @return
     */
    public String pullResourceCatalog(String projectId, String languageId, Uri uri, boolean ignoreCache) {
        String path = resourceCatalogPath(projectId, languageId);
        String key = downloadAsset(uri, ignoreCache);
        linkAsset(key, path);

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
     * generates the resource catalog uri
     * @param projectId
     * @param languageId
     * @return
     */
    public Uri resourceCatalogUri(String projectId, String languageId) {
        return Uri.parse("https://api.unfoldingword.org/ts/txt/"+API_VERSION+"/"+projectId+"/"+languageId+"/resources.json");
    }

    /**
     * Returns the target languages
     */
    public String pullTargetLanguageCatalog() {
        // TODO: check for updates on the server
        // http://td.unfoldingword.org/exports/langnames.json
        String path = "target_languages.json";
        return loadJSONAsset(path);
    }

    /**
     * Returns the key terms.
     * If downloaded this will replace the current terms
     * @param projectId
     * @param languageId
     * @param resourceId
     * @param checkServer indicates an updated list of terms should be downloaded from the server
     * @param ignoreCache indicates that the cache should be ignored when determining whether or not to download
     * @return
     */
    public String pullTerms(String projectId, String languageId, String resourceId, boolean checkServer, boolean ignoreCache) {
        String path = termsPath(projectId, languageId, resourceId);

        if(checkServer) {
            String key = downloadAsset(termsUri(projectId, languageId, resourceId), ignoreCache);
            linkAsset(key, path);
        }
        return loadJSONAsset(path);
    }

    /**
     * Downloads and returns the key terms.
     * Rather than using the standard download uri this method allows you to specify from which uri
     * to download the terms. This is especially helpful when cross referencing api's.
     * If downloaded this will replace the current terms
     * @param projectId
     * @param languageId
     * @param resourceId
     * @param uri the uri from which the terms will be downloaded
     * @param ignoreCache indicates that the cache should be ignored when determining whether or not to download
     * @return
     */
    public String pullTerms(String projectId, String languageId, String resourceId, Uri uri, boolean ignoreCache) {
        String path = termsPath(projectId, languageId, resourceId);

        String key = downloadAsset(uri, ignoreCache);
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
     * Returns the terms uri
     * @param projectId
     * @param languageId
     * @param resourceId
     * @return
     */
    public static Uri termsUri(String projectId, String languageId, String resourceId) {
        return Uri.parse("https://api.unfoldingword.org/ts/txt/" + API_VERSION + "/" + projectId + "/" + languageId + "/" + resourceId + "/terms.json");
    }

    /**
     * Returns the notes.
     * If downloaded the the existing notes will be replaced
     * @param projectId
     * @param languageId
     * @param resourceId
     * @param checkServer indicates an updated list of notes should be downloaded from the server
     * @param ignoreCache indicates that the cache should be ignored when determining whether or not to download
     * @return
     */
    public String pullNotes(String projectId, String languageId, String resourceId, boolean checkServer, boolean ignoreCache) {
        String path = notesPath(projectId, languageId, resourceId);

        if(checkServer) {
            String key = downloadAsset(notesUri(projectId, languageId, resourceId), ignoreCache);
            linkAsset(key, path);
        }
        return loadJSONAsset(path);
    }

    /**
     * Downloads and returns the notes.
     * Rather than using the standard download uri this method allows you to specify from which uri
     * to download the notes. This is especially helpful when cross referencing api's.
     * If downloaded the existing notes will be replaced
     * @param projectId
     * @param languageId
     * @param resourceId
     * @param uri the uri from which the notes will be downloaded
     * @param ignoreCache indicates that the cache should be ignored when determining whether or not to download
     * @return
     */
    public String pullNotes(String projectId, String languageId, String resourceId, Uri uri, boolean ignoreCache) {
        String path = notesPath(projectId, languageId, resourceId);

        String key = downloadAsset(uri, ignoreCache);
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
     * Returns the notes uri
     * @param projectId
     * @param languageId
     * @param resourceId
     * @return
     */
    public Uri notesUri(String projectId, String languageId, String resourceId) {
        return Uri.parse("https://api.unfoldingword.org/ts/txt/" + API_VERSION + "/" + projectId + "/" + languageId + "/" + resourceId + "/notes.json");
    }

    /**
     * Returns the source text
     * If downloaded this will replace the current source
     * @param projectId
     * @param languageId
     * @param resourceId
     * @param checkServer indicates an updated version of the source should be downloaded from the server
     * @param ignoreCache indicates that the cache should be ignored when determining whether or not to download
     * @return
     */
    public String pullSource(String projectId, String languageId, String resourceId, boolean checkServer, boolean ignoreCache) {
        String path = sourcePath(projectId, languageId, resourceId);

        if(checkServer) {
            String key = downloadAsset(sourceUri(projectId, languageId, resourceId), ignoreCache);
            linkAsset(key, path);
        }
        return loadJSONAsset(path);
    }

    /**
     * Downloads and returns the source.
     * Rather than using the standard download uri this method allows you to specify from which uri
     * to download the source. This is especially helpful when cross referencing api's.
     * If downloaded this will replace the current source
     * @param projectId
     * @param languageId
     * @param resourceId
     * @param uri the uri from which the source will be downloaded
     * @param ignoreCache indicates that the cache should be ignored when determining whether or not to download
     * @return
     */
    public String pullSource(String projectId, String languageId, String resourceId, Uri uri, boolean ignoreCache) {
        String path = sourcePath(projectId, languageId, resourceId);

        String key = downloadAsset(uri, ignoreCache);
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
     * Returns the source uri
     * @param projectId
     * @param languageId
     * @param resourceId
     * @return
     */
    public Uri sourceUri(String projectId, String languageId, String resourceId) {
        return Uri.parse("https://api.unfoldingword.org/ts/txt/" + API_VERSION + "/" + projectId + "/" + languageId + "/" + resourceId + "/source.json");
    }

    /**
     * Returns the file for the assets directory
     * @return
     */
    private static File assetsDir() {
        return new File(mContext.getFilesDir(), "assets");
    }

    /**
     * Returns the file for the temp assets directory
     * @return
     */
    private static File tempAssetsDir() {
        return new File(mContext.getCacheDir(), "assets");
    }

    /**
     * Validates the asset cache version
     */
    private void validateAssetCache() {
        // verify the cached assets match the expected server api level
        File cacheVersionFile = new File(assetsDir(), ".cache_api_version");
        if(cacheVersionFile.exists() && cacheVersionFile.isFile()) {
            try {
                // version is composed of API_VERSION.API_VERSION_INTERNAL e.g. "2.1"
                String[] version = FileUtils.readFileToString(cacheVersionFile).split("\\.");
                if(Integer.parseInt(version[0]) != API_VERSION || Integer.parseInt(version[1]) != API_VERSION_INTERNAL) {
                    cacheVersionFile.delete();
                }
            } catch (Exception e) {
                Logger.w(this.getClass().getName(), "failed to read the cached assets api version", e);
                cacheVersionFile.delete();
            }
        }
        if(!cacheVersionFile.exists() || !cacheVersionFile.isFile()) {
            Logger.i(this.getClass().getName(), "clearing the asset cache to support api version "+API_VERSION+"."+API_VERSION_INTERNAL);
            FileUtilities.deleteRecursive(assetsDir());
            FileUtilities.deleteRecursive(tempAssetsDir());
            // record cache version
            assetsDir().mkdirs();
            tempAssetsDir().mkdirs();
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
        validateAssetCache();

        File cachedAsset = new File(assetsDir(), path);

        // resolve links (if there are any)
        String key = resolveLink(cachedAsset);
        if(key != null) {
            cachedAsset = getAsset(key);
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
        String linkPath = FilenameUtils.removeExtension(path) + ".link";

        // resolve links in the packaged assets
        String key = resolvePackagedLink(linkPath);
        if(key != null) {
            // load the link
            try {
                InputStream is = mContext.getAssets().open("data/" + key);
                return FileUtilities.convertStreamToString(is);
            } catch (IOException e) {
                Logger.w(this.getClass().getName(), "The linked packaged asset data/"+key+" does not exist for "+path);
            }
        }

        // load the asset as usual
        try {
            InputStream is = mContext.getAssets().open(path);
            return FileUtilities.convertStreamToString(is);
        } catch (IOException e) {
            Logger.i(this.getClass().getName(), "The packaged asset does not exist "+path);
        }

        return null;
    }
}
