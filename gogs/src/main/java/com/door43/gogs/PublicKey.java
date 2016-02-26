package com.door43.gogs;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by joel on 2/25/2016.
 */
public class PublicKey {

    private final String title;
    private final String key;
    private int id;
    private String url;
    private String createdAt;

    /**
     * Create a key that holds just the id. This allows us to look up the full key description from the api
     * @param id
     */
    public PublicKey(int id) {
        this.id = id;
        this.title = "";
        this.key = "";
    }

    public PublicKey(String title, String key) {
        this.title = title;
        this.key = key;
    }

    /**
     * Returns a public key parsed from json
     * @param json
     * @return
     */
    public static PublicKey parse(JSONObject json) {
        if(json != null) {
            try {
                PublicKey key = new PublicKey(json.getString("title"), json.getString("key"));
                key.id = json.getInt("id");
                key.url = json.getString("url");
                key.createdAt = json.getString("created_at");
                return key;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUrl() {
        return url;
    }

    public int getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return this.key;
    }
}
