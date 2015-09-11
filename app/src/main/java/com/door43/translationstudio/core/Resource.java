package com.door43.translationstudio.core;

import org.json.JSONObject;

/**
 * Created by joel on 9/10/2015.
 */
public class Resource {
    private String mId;

    private Resource() {

    }

    public static Resource generate(JSONObject resourceJson) {
        return new Resource();
    }

    public String getId() {
        return mId;
    }
}
