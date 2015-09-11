package com.door43.translationstudio.core;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by joel on 9/10/2015.
 */
public class Resource {
    private final int mCheckingLevel;
    private final String mTitle;
    private final String mId;

    private Resource(String title, String id, int checkingLevel) {
        mTitle = title;
        mId = id;
        mCheckingLevel = checkingLevel;
    }

    /**
     * Generates a new resource from json
     * @param json
     * @return
     * @throws JSONException
     */
    public static Resource generate(JSONObject json) throws JSONException {
        if(json == null) {
            return null;
        }
        JSONObject statusJson = json.getJSONObject("status");
        return new Resource(
            json.getString("name"),
            json.getString("slug"),
            statusJson.getInt("checking_level")
        );
    }

    public String getId() {
        return mId;
    }

    public int getCheckingLevel() {
        return mCheckingLevel;
    }

    public String getTitle() {
        return mTitle;
    }
}
