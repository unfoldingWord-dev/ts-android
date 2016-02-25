package com.door43.gogs;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A gogs user
 */
public class User {

    private String username = "";
    private String password = "";
    public String email = "";
    private String fullName = "";
    private String avatarUrl = "";
    private Token token = null;

    private User() {}

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Returns a user parsed from json
     * @param json
     * @return
     */
    public static User parse(JSONObject json) {
        if(json != null) {
            try {
                User user = new User();
                user.username = json.getString("username");
                user.fullName = json.getString("full_name");
                user.email = json.getString("email");
                user.avatarUrl = json.getString("avatar_url");
                return user;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public Token getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

}
