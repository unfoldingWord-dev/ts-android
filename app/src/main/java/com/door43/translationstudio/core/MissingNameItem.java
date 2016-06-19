package com.door43.translationstudio.core;

import android.util.Log;

import org.unfoldingword.tools.logger.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by blm on 4/14/16.
 */
public class MissingNameItem {
    public static final String TAG = MissingNameItem.class.getSimpleName();
    public String description;
    public String invalidName;
    public String contents;

    public MissingNameItem(String description, String invalidName, String contents) {
        this.description = description;
        this.invalidName = invalidName;
        this.contents = contents;
    }

    public JSONObject toJson() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.putOpt("description", description);
            jsonObject.putOpt("invalidName", invalidName);
            jsonObject.putOpt("contents", contents);

            return jsonObject;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static MissingNameItem generate(JSONObject jsonObject) {
        try {
            String description = (String) getOpt(jsonObject,"description");
            String invalidName = (String) getOpt(jsonObject,"invalidName");
            String contents = (String) getOpt(jsonObject,"contents");
            return new MissingNameItem(description, invalidName, contents);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static JSONArray toJsonArray(List<MissingNameItem> array) {
        JSONArray jsonArray = new JSONArray();
        for (MissingNameItem item : array) {
            jsonArray.put(item.toJson());
        }
        return jsonArray;
    }

    static List<MissingNameItem> fromJsonArray(String jsonStr) {
        try {
            JSONArray jsonArray = new JSONArray(jsonStr);
            return fromJsonArray(jsonArray);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static List<MissingNameItem> fromJsonArray(JSONArray jsonArray) throws JSONException {
        List<MissingNameItem> array = new ArrayList<>();

        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                MissingNameItem item = generate(json);
                array.add(item);
            }
        } catch (Exception e) {
            Logger.e(TAG,"could not parse item",e);
        }
        return array;
    }

    static Object getOpt(JSONObject json, String key) {
        try {
            if(json.has(key)) {
                return json.get(key);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}


