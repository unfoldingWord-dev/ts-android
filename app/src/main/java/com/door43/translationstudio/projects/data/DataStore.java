package com.door43.translationstudio.projects.data;

import com.door43.delegate.DelegateSender;
import com.door43.translationstudio.MainApplication;

import java.io.IOException;
import java.io.InputStream;

/**
 * The data store handles all of the text and media resources within the app.
 * This class will look for data locally as well as on the server.
 */
public class DataStore extends DelegateSender {
    private static MainApplication mContext;
    private static String SOURCE_TRANSLATIONS_DIR = "sourceTranslations/";

    public DataStore(MainApplication context) {
        mContext = context;
    }

    /**
     * Returns a json array of valid source projects
     * @return
     */
    public void fetchProjectCatalog() {
        // TODO: check for updates on the server
        issueDelegateResponse(new DataStoreDelegateResponse(DataStoreDelegateResponse.MessageType.PROJECT, loadJSONAsset(SOURCE_TRANSLATIONS_DIR+"projects.json")));
    }

    /**
     * Returns a json array of source languages for a specific project
     * @param projectSlug the slug of the project for which languages will be returned
     * @return
     */
    public void fetchSourceLanguageCatalog(String projectSlug) {
        // TODO: check for updates on the server
        String path = SOURCE_TRANSLATIONS_DIR+projectSlug+"/languages.json";
        issueDelegateResponse(new DataStoreDelegateResponse(DataStoreDelegateResponse.MessageType.SOURCE_LANGUAGE, loadJSONAsset(path), projectSlug));
    }

    /**
     * Retusn a json array of target languages
     */
    public void fetchTargetLanguageCatalog() {
        // TODO: check for updates on the server
        String path = "target_languages.json";
        issueDelegateResponse(new DataStoreDelegateResponse(DataStoreDelegateResponse.MessageType.TARGET_LANGUAGE, loadJSONAsset(path)));
    }

    /**
     * Returns a json object of source text for a specific project and language
     * @param projectSlug the slug of the project for which the source text will be returned
     * @param languageCode the language code for which the source text will be returned
     * @return
     */
    public void fetchSourceText(String projectSlug, String languageCode) {
        // TODO: check for updates on the server
        String path = SOURCE_TRANSLATIONS_DIR+projectSlug+"/"+languageCode+"/source.json";
        issueDelegateResponse(new DataStoreDelegateResponse(DataStoreDelegateResponse.MessageType.SOURCE, loadJSONAsset(path), projectSlug));
    }

    /**
     * Load json from the local assets
     * @param path path to the json file within the assets directory
     * @return the string contents of the json file
     */
    private String loadJSONAsset(String path) {
        String json;
        try {
            InputStream is = mContext.getAssets().open(path);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }
}
