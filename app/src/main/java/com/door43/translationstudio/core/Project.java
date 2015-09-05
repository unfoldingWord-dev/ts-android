package com.door43.translationstudio.core;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by joel on 8/29/2015.
 */
public class Project {
    public final int dateModified;
    public final String description;
    public final String name;
    private String mId;

    protected Project(String projectId, String name, String description, int dateModified) {
        mId = projectId;
        this.name = name;
        this.description = description;
        this.dateModified = dateModified;
    }

    /**
     * Returns the project id
     * @return
     */
    public String getId() {
        return mId;
    }

    /**
     * Generates a new project object from json
     * @param project
     * @return
     */
    public static Project generate(JSONObject project, JSONObject sourceLanguage) {
        try {
            String projectId = project.getString("slug");
            int dateModified = project.getInt("date_modified");
            JSONObject projectLanguageJson = sourceLanguage.getJSONObject("project");
            String name = projectLanguageJson.getString("name");
            String description = projectLanguageJson.getString("desc");

            return new Project(projectId, name, description, dateModified);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
