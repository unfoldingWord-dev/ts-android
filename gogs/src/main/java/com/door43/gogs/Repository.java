package com.door43.gogs;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a gogs repository
 */
public class Repository {

    private String name = "";
    private String description = "";
    private boolean isPrivate;
    private int id = 0;
    private String fullName = "";
    private boolean fork;
    private String htmlUrl = "";
    private String cloneUrl = "";
    private String sshUrl = "";
    private User owner;

    private Repository() {}

    public Repository(String name, String description, boolean isPrivate) {
        this.name = name;
        this.description = description;
        this.isPrivate = isPrivate;
    }

    /**
     * Returns a repository parsed from json
     * @param json
     * @return
     */
    public static Repository parse(JSONObject json) {
        if(json != null) {
            try {
                Repository repo = new Repository();
                repo.id = json.getInt("id");
                repo.fullName = json.getString("full_name");
                repo.isPrivate = json.getBoolean("private");
                repo.fork = json.getBoolean("fork");
                repo.htmlUrl = json.getString("html_url");
                repo.cloneUrl = json.getString("clone_url");
                repo.sshUrl = json.getString("ssh_url");
                repo.owner = User.parse(json.getJSONObject("owner"));
                // TODO: 2/24/2016 get permissions
                return repo;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean getIsPrivate() {
        return isPrivate;
    }

    public String getFullName() {
        return fullName;
    }
}
