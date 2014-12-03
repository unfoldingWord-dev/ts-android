package com.door43.translationstudio.projects.data;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.util.FileUtilities;
import com.door43.translationstudio.util.MainContext;
import com.door43.translationstudio.util.ServerUtilities;

import org.apache.http.util.ByteArrayBuffer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * The data store handles all of the text and media resources within the app.
 * This class will look for data locally as well as on the server.
 */
public class DataStore {
    private static MainApplication mContext;
    private static String SOURCE_TRANSLATIONS_DIR = "sourceTranslations/";

    public DataStore(MainApplication context) {
        mContext = context;
    }

    /**
     * Returns a json array of valid source projects
     * @return
     */
    public String fetchProjectCatalog(boolean checkServer) {
        if(checkServer) {
            // TODO: check for updates on the server
            // https://api.unfoldingword.org/ts/txt/1/ts-catalog.json
            // TODO: store catalog
            return "";
        } else {
            // TODO: check for stored catalogs
            return loadJSONAsset(SOURCE_TRANSLATIONS_DIR + "projects.json");
        }
    }

    /**
     * Returns a json array of source languages for a specific project
     * This should not be ran on the main thread.
     * @param projectSlug the slug of the project for which languages will be returned
     * @return
     */
    public String fetchSourceLanguageCatalog(String projectSlug, boolean checkServer) {
        String path = SOURCE_TRANSLATIONS_DIR + projectSlug + "/languages.json";

        if(checkServer) {
            // https://api.unfoldingword.org/[project id]/txt/1/[project id]-catalog.json
            URL url;
            try {
                url = new URL("https://api.unfoldingword.org/"+projectSlug+"/txt/1/"+projectSlug+"-catalog.json");
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            }
            File file = new File(mContext.getCacheDir(), "assets/" + path);
            ServerUtilities.downloadFile(url, file);
        }
        return loadJSONAsset(path);
    }
    /**
     * Retusn a json array of target languages
     */
    public String fetchTargetLanguageCatalog() {
        // TODO: check for updates on the server
        // https://api.unfoldingword.org/td/txt/1/langnames.json
        String path = "target_languages.json";
        return loadJSONAsset(path);
    }

    /**
     * Returns a json array of key terms
     * Terms are case sensitive
     */
    public String fetchTermsText(String projectSlug, String languageCode, boolean checkServer) {
        String path = SOURCE_TRANSLATIONS_DIR+projectSlug+"/"+languageCode+"/terms.json";
        if(checkServer) {
            // https://api.unfoldingword.org/[project id]/txt/1/[langcode]/kt-[langcode].json
            URL url;
            try {
                url = new URL("https://api.unfoldingword.org/"+projectSlug+"/txt/1/"+languageCode+"/kt-"+languageCode+".json");
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            }
            File file = new File(mContext.getCacheDir(), "assets/" + path);
            ServerUtilities.downloadFile(url, file);
        }
        return loadJSONAsset(path);
    }

    public String fetchTranslationNotes(String projectSlug, String languageCode, boolean checkServer) {
        String path = SOURCE_TRANSLATIONS_DIR+projectSlug+"/"+languageCode+"/notes.json";
        if(checkServer) {
            // https://api.unfoldingword.org/obs/txt/1/en/tN-en.json
            URL url;
            try {
                url = new URL("https://api.unfoldingword.org/"+projectSlug+"/txt/1/"+languageCode+"/tN-"+languageCode+".json");
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            }
            File file = new File(mContext.getCacheDir(), "assets/" + path);
            ServerUtilities.downloadFile(url, file);
        }
        return loadJSONAsset(path);
    }

    /**
     * Returns a json object of source text for a specific project and language
     * @param projectSlug the slug of the project for which the source text will be returned
     * @param languageCode the language code for which the source text will be returned
     * @return
     */
    public String fetchSourceText(String projectSlug, String languageCode, boolean checkServer) {
        String path = SOURCE_TRANSLATIONS_DIR + projectSlug + "/" + languageCode + "/source.json";
        if(checkServer) {
            // api.unfoldingword.org/[project id]/txt/1/[langcode]/[project id]-[langcode].json
            URL url;
            try {
                url = new URL("https://api.unfoldingword.org/"+projectSlug+"/txt/1/"+languageCode+"/"+projectSlug+"-"+languageCode+".json");
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            }
            File file = new File(mContext.getCacheDir(), "assets/" + path);
            ServerUtilities.downloadFile(url, file);
        }
        return loadJSONAsset(path);
    }

    /**
     * Load json from the local assets
     * @param path path to the json file within the assets directory
     * @return the string contents of the json file
     */
    private String loadJSONAsset(String path) {
        File cacheAsset = new File(mContext.getCacheDir(), "assets/" + path);
        if(cacheAsset.exists()) {
            // attempt to load from the cached assets first
            try {
                return FileUtilities.getStringFromFile(cacheAsset);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            // load from the packaged assets as a backup
            try {
                InputStream is = mContext.getAssets().open(path);
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                return new String(buffer, "UTF-8");
            } catch (IOException ex) {
                ex.printStackTrace();
                return null;
            }
        }
    }
}
