package com.door43.translationstudio.core;

import org.json.JSONException;
import org.json.JSONObject;
import org.unfoldingword.gogsclient.Token;
import org.unfoldingword.gogsclient.User;

/**
 * Represents a single user profile
 */
public class Profile {
    private static final long serialVersionUID = 0L;
    private String fullName;
    public User gogsUser;
    private int termsOfUseLastAccepted = 0;

    /**
     * Creates a new profile
     * @param fullName
     */
    public Profile(String fullName) {
        this.fullName = fullName;
    }

    /**
     * Returns the profile represented as a json object
     * @return
     */
    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("serial_version_uid", serialVersionUID);
        if(gogsUser != null) {
            json.put("gogs_user", gogsUser.toJSON());
            json.put("gogs_token", gogsUser.token.toJSON());
        } else {
            json.put("full_name", fullName);
        }
        json.put("terms_last_accepted", termsOfUseLastAccepted);
        return json;
    }

    /**
     * Loads the user profile from json
     * @param json
     * @return
     * @throws Exception
     */
    public static Profile fromJSON(JSONObject json) throws Exception {
        Profile profile = null;
        if(json != null) {
            long versionUID = json.getLong("serial_version_uid");
            if(versionUID != serialVersionUID) {
                throw new Exception("Unsupported profile version " + versionUID + ". Expected " + serialVersionUID);
            }
            String fullName = null;
            if(json.has("full_name")) {
                fullName = json.getString("full_name");
            }
            User gogsUser = null;
            if(json.has("gogs_user")) {
                gogsUser = User.fromJSON(json.getJSONObject("gogs_user"));
            }
            Token gogsToken = null;
            if(json.has("gogs_token")) {
                gogsToken = Token.fromJSON(json.getJSONObject("gogs_token"));
            }
            int termsLastAccepted = 0;
            if(json.has("terms_last_accepted")) {
                termsLastAccepted = json.getInt("terms_last_accepted");
            }

            if(gogsUser != null) {
                fullName = gogsUser.fullName;
                gogsUser.token = gogsToken;
            }
            profile = new Profile(fullName);
            profile.gogsUser = gogsUser;
            profile.setTermsOfUseLastAccepted(termsLastAccepted);
        }
        return profile;
    }

    /**
     * Returns the name of the translator.
     * The name from their gogs account will be used if it exists
     * @return
     */
    public String getFullName() {
        if(this.gogsUser != null) {
            return this.gogsUser.fullName;
        } else {
            return this.fullName;
        }
    }

    /**
     * Returns a native speaker version of this profile.
     * This is used when recording translators who contribute to a translation
     * @return
     */
    public NativeSpeaker getNativeSpeaker() {
        return new NativeSpeaker(getFullName());
    }

    public int getTermsOfUseLastAccepted() {
        return termsOfUseLastAccepted;
    }

    public void setTermsOfUseLastAccepted(int termsOfUseLastAccepted) {
        this.termsOfUseLastAccepted = termsOfUseLastAccepted;
    }
}
