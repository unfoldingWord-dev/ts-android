package com.door43.translationstudio.core;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by joel on 8/26/2015.
 */
public class Frame {

    public final String body;
    private final String mId;

    protected Frame(String body, String frameId) {
        this.body = body;
        mId = frameId;
    }

    /**
     * Returns the frame id
     * @return
     */
    public String getId() {
        return mId;
    }

    /**
     * Generates a new frame from json
     * @param frame
     * @return
     */
    public static Frame generate(JSONObject frame) {
        try {
            return new Frame(frame.getString("text"), frame.getString("id"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
