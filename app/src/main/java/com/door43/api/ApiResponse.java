package com.door43.api;

import android.util.Log;

import com.google.gson.annotations.SerializedName;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by joel on 7/8/2014.
 */
public class ApiResponse {
    @SerializedName("v")
    protected String version;
    @SerializedName("t")
    protected double timeStamp;
    @SerializedName("error")
    protected Map<String, String> error = null;
    @SerializedName("ok")
    protected String ok = null;

    /**
     * Initializes a new api error response
     * @param errorType the name of the error class
     * @param errorMessage the error message
     */
    public ApiResponse(String errorType, String errorMessage) {
        this.error = new HashMap<String, String>();
        this.error.put("type", errorType);
        this.error.put("message", errorMessage);
    }

    public ApiResponse(String version, Double timeStamp, String ok) {
        this.version = version;
        this.timeStamp = timeStamp;
        this.ok = ok;
    }

    /**
     * Initializes a new api error response
     * @param errorType the name of the error class
     * @param errorMessage the error message
     */
    public static ApiResponse generateError(String errorType, String errorMessage) {
        ApiResponse response = new ApiResponse(errorType, errorMessage);
        return response;
    }

    /**
     * Check if the api responded with an ok or error
     * @return boolean true if the response status is "ok"
     */
    public boolean ok() {
        return this.error == null && this.ok != null;
    }

    /**
     * Retrieve a field from the ok response. This only works if the ok response is a json object (not an array).
     * @param field the name of the field to be retrieved
     * @return string
     */
    public String getField(String field) {
        try {
            JSONObject json = new JSONObject(this.ok);
            if(json.has(field)) {
                return json.get(field).toString();
            } else {
                return "";
            }
        } catch (JSONException e) {
            Log.w("api", e.getMessage());
            return "";
        }
    }

    /**
     * Parses the response into a json object and returns it if there are no errors
     * @return
     */
    public JSONObject getDataAsJsonObject() {
        try {
            return new JSONObject(this.ok);
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * Parses the response into a json array and returns it if there are no errors
     * @return
     */
    public JSONArray getDataASJsonArray() {
        try {
            return new JSONArray(this.ok);
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * Returns the raw json string from the ok response
     * @return string
     */
    public String getRawData() {
        return this.ok;
    }

    /**
     * Get the error message the api responded with
     * @return
     */
    public String errorMessage() {
        return this.error.get("message");
    }

    /**
     * Get the type of error the api responded with
     * @return
     */
    public String errorType() {
        return this.error.get("type");
    }
}
