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
    public final String sourceLanguageId;
    public final int sort;
    public final String sourceLanguageCatalog;
    private String mId;

    /**
     *
     * @param projectId The id of the project
     * @param sourceLanguageId The id of the source language used for the name and description
     * @param name the name of the project
     * @param description the description of the project
     * @param dateModified the date the project was last modified
     */
    private Project(String projectId, String sourceLanguageId, String name, String description, int dateModified, int sort, String sourceLanguageCatalog) {
        mId = projectId;
        this.name = name;
        this.description = description;
        this.dateModified = dateModified;
        this.sourceLanguageId = sourceLanguageId;
        this.sourceLanguageCatalog = sourceLanguageCatalog;
        this.sort = sort;
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
     * @param json
     * @return
     */
    public static Project generate(JSONObject json, JSONObject sourceLanguage) throws JSONException {
        if(json != null) {
            String projectId = json.getString("slug");
            int dateModified = json.getInt("date_modified");
            String sourceLanguageId = sourceLanguage.getString("slug");
            JSONObject projectLanguageJson = sourceLanguage.getJSONObject("project");
            String name = projectLanguageJson.getString("name");
            String description = projectLanguageJson.getString("desc");
            String sourceLanguageCatalog = json.getString("lang_catalog");
            int sort = json.getInt("sort");

            return new Project(projectId, sourceLanguageId, name, description, dateModified, sort, sourceLanguageCatalog);
        }
        return null;
    }

    /**
     * Generates a simple form of the project, that is, with out any source translation
     * @param json
     * @return
     */
    public static Project generateSimple(JSONObject json) throws JSONException {
        if(json != null) {
            String projectId = json.getString("slug");
            int dateModified = json.getInt("date_modified");
            int sort = json.getInt("sort");
            String sourceLanguageCatalog = json.getString("lang_catalog");

            return new Project(projectId, null, null, null, dateModified, sort, sourceLanguageCatalog);
        }
        return null;
    }
}
