package com.door43.translationstudio.core;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by joel on 8/29/2015.
 */
@Deprecated
public class Project extends org.unfoldingword.resourcecontainer.Project {
    public final int dateModified;
    public final String sourceLanguageId;
    public final String sourceLanguageCatalog;
    public final int sourceLanguageCatalogLocalDateModified;
    public final int sourceLanguageCatalogServerDateModified;
    public final String chunkMarkerCatalog;
    public final int chunkMarkerCatalogLocalDateModified;
    public final int chunkMarkerCatalogServerDateModified;

    /**
     * @param projectId The id of the project
     * @param sourceLanguageId The id of the source language used for the name and description
     * @param name the name of the project
     * @param description the description of the project
     * @param dateModified the date the project was last modified
     * @param sourceLanguageCatalog
     * @param sourceLanguageCatalogLocalDateModified
     * @param sourceLanguageCatalogServerDateModified
     */
    public Project(String projectId, String sourceLanguageId, String name, String description, int dateModified, int sort,
                   String sourceLanguageCatalog, int sourceLanguageCatalogLocalDateModified, int sourceLanguageCatalogServerDateModified,
                   String chunkMarkerCatalog, int chunkMarkerCatalogLocalDateModified, int chunkMarkerCatalogServerDateModified) {
        super(projectId, name, sort);
        this.description = description;

        this.dateModified = dateModified;
        this.sourceLanguageId = sourceLanguageId;
        this.sourceLanguageCatalog = sourceLanguageCatalog;
        this.sourceLanguageCatalogLocalDateModified = sourceLanguageCatalogLocalDateModified;
        this.sourceLanguageCatalogServerDateModified = sourceLanguageCatalogServerDateModified;
        this.chunkMarkerCatalog = chunkMarkerCatalog;
        this.chunkMarkerCatalogLocalDateModified = chunkMarkerCatalogLocalDateModified;
        this.chunkMarkerCatalogServerDateModified = chunkMarkerCatalogServerDateModified;
    }

    /**
     * Returns the project id
     * @return
     */
    public String getId() {
        return slug;
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
            int sourceLanguageServerModified = Util.getDateFromUrl(sourceLanguageCatalog);
            int sort = json.getInt("sort");
            // TODO: 4/7/16 eventually we'll grab the chunks info
            return new Project(projectId, sourceLanguageId, name, description, dateModified, sort, sourceLanguageCatalog, 0, sourceLanguageServerModified, "", 0, 0);
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
            int sourceLanguageServerModified = Util.getDateFromUrl(sourceLanguageCatalog);
            // TODO: 4/7/16 eventually we'll grab the chunks info
            return new Project(projectId, null, null, null, dateModified, sort, sourceLanguageCatalog, 0, sourceLanguageServerModified, "", 0, 0);
        }
        return null;
    }
}
