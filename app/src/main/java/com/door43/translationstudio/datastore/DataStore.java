package com.door43.translationstudio.datastore;

import com.door43.delegate.DelegateSender;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The data store handles all of the text and media resources within the app.
 * This class will look for data locally as well as on the server.
 */
public class DataStore extends DelegateSender {

    /**
     * Returns a json array of valid source projects
     * @return
     */
    public void fetchProjectCatalog() {
        // TODO: do some processing then notify listeners that we have the data
        issueDelegateResponse(new DataStoreDelegateResponse(DataStoreDelegateResponse.MessageType.PROJECT, ""));
    }

    /**
     * Returns a json array of source languages for a specific project
     * @param projectSlug the slug of the project for which languages will be returned
     * @return
     */
    public void fetchLanguageCatalog(String projectSlug) {
        issueDelegateResponse(new DataStoreDelegateResponse(DataStoreDelegateResponse.MessageType.LANGUAGE, ""));
    }

    /**
     * Returns a json object of source text for a specific project and language
     * @param projectSlug the slug of the project for which the source text will be returned
     * @param languageCode the language code for which the source text will be returned
     * @return
     */
    public void fetchSourceText(String projectSlug, String languageCode) {
        issueDelegateResponse(new DataStoreDelegateResponse(DataStoreDelegateResponse.MessageType.SOURCE, ""));
    }
}
