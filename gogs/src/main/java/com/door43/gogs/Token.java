package com.door43.gogs;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents an application authentication token
 */
public class Token {
    private String name = "";
    private String sha1 = "";

    private Token() {}

    public Token(String name) {
        this.name = name;
    }

    /**
     * Returns a token parsed from json
     * @param json
     * @return
     */
    public static Token parse(JSONObject json) {
        if(json != null) {
            Token token = new Token();
            try {
                token.name = json.getString("name");
                token.sha1 = json.getString("sha1");
                return token;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return this.sha1;
    }

    public String getName() {
        return name;
    }
}
