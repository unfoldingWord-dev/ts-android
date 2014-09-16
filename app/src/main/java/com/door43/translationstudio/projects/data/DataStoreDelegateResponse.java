package com.door43.translationstudio.projects.data;

import com.door43.delegate.DelegateResponse;

/**
 * Created by joel on 9/3/2014.
 */
public class DataStoreDelegateResponse implements DelegateResponse {
    private MessageType mType;
    private String mJson;
    private String mProjectSlug;
    private int mLanguageIndex = -1;

    /**
     * Gets the project slug
     * @return
     */
    public String getProjectSlug() {
        return mProjectSlug;
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
     * @param projectSlug the project slug so listeners can identify what project the language is for
     * @param  languageIndex the language index so listeners can identify what language the source is for
     */
    public DataStoreDelegateResponse(MessageType type, String json, String projectSlug, int languageIndex) {
        mType = type;
        mJson = json;
        mProjectSlug = projectSlug;
        mLanguageIndex = languageIndex;
    }

    /**
     * Create a new delegate response
     * @param type
     * @param json
     * @param projectSlug the project slug so listeners can identify what the response is for.
     */
    public DataStoreDelegateResponse(MessageType type, String json, String projectSlug) {
        mType = type;
        mJson = json;
        mProjectSlug = projectSlug;
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
