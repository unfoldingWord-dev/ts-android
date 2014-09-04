package com.door43.translationstudio.datastore;

import com.door43.delegate.DelegateResponse;

import org.json.JSONArray;

/**
 * Created by joel on 9/3/2014.
 */
public class DataStoreDelegateResponse implements DelegateResponse {
    private MessageType mType;
    private String mJson;

    public enum MessageType {
        PROJECT, LANGUAGE, SOURCE, AUDIO, IMAGES
    }

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
