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
    private final String mVersion;

    private Resource(String title, String id, int checkingLevel, String version) {
        mTitle = title;
        mId = id;
        mCheckingLevel = checkingLevel;
        mVersion = version;
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
            statusJson.getInt("checking_level"),
            statusJson.getString("version")
        );
    }

    /**
     * Returns the id of the resource
     * @return
     */
    public String getId() {
        return mId;
    }

    /**
     * Returns the checking level of the resource
     * @return
     */
    public int getCheckingLevel() {
        return mCheckingLevel;
    }

    /**
     * Returns the title of the resource
     * @return
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Returns the version of the resource
     * @return
     */
    public String getVersion() {
        return mVersion;
    }
}
