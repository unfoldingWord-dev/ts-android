package com.door43.translationstudio.projects.data;

import com.door43.delegate.DelegateResponse;

/**
 * Created by joel on 9/3/2014.
 */
public class DataStoreDelegateResponse implements DelegateResponse {
    private MessageType mType;
    private String mJson;
    private String mProjectSlug;

    /**
     * Gets the project slug
     * @return
     */
    public String getProjectSlug() {
        return mProjectSlug;
    }

    public enum MessageType {
        PROJECT, SOURCE_LANGUAGE, TARGET_LANGUAGE, SOURCE, AUDIO, IMAGES, TERMS
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
