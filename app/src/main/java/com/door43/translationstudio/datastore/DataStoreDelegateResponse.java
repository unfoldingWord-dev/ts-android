package com.door43.translationstudio.datastore;

import android.os.Message;

import com.door43.delegate.DelegateResponse;

import org.json.JSONArray;

/**
 * Created by joel on 9/3/2014.
 */
public class DataStoreDelegateResponse implements DelegateResponse {
    private MessageType mType;
    private String mJson;
    private int mProjectIndex = -1;
    private int mLanguageIndex = -1;

    /**
     * Gets the project index
     * @return
     */
    public int getProjectIndex() {
        return mProjectIndex;
    }

    /**
     * Gets the language index
     * @return
     */
    public int getLanguageIndex() {
        return mLanguageIndex;
    }

    public enum MessageType {
        PROJECT, LANGUAGE, SOURCE, AUDIO, IMAGES
    }

    /**
     * Create a new delegate response
     * @param type
     * @param json
     * @param projectIndex the project index so listeners can identify what project the language is for
     * @param  languageIndex the language index so listeners can identify what language the source is for
     */
    public DataStoreDelegateResponse(MessageType type, String json, int projectIndex, int languageIndex) {
        mType = type;
        mJson = json;
        mProjectIndex = projectIndex;
        mLanguageIndex = languageIndex;
    }

    /**
     * Create a new delegate response
     * @param type
     * @param json
     * @param projectIndex the project index so listeners can identify what the response is for.
     */
    public DataStoreDelegateResponse(MessageType type, String json, int projectIndex) {
        mType = type;
        mJson = json;
        mProjectIndex = projectIndex;
    }

    /**
     * Create a new delegate response
     * @param type
     * @param json
     */
    public DataStoreDelegateResponse(MessageType type, String json) {
        mType = type;
        mJson = json;
    }

    /**
     * Returns the message type
     * @return
     */
    public MessageType getType() {
        return mType;
    }

    /**
     * Returns the json payload
     * @return
     */
    public String getJSON() {
        return mJson;
    }
}
