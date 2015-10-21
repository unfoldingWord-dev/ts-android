package com.door43.translationstudio.core;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a single source translation.
 */
public class SourceTranslation {
    public final String projectSlug;
    public final String sourceLanguageSlug;
    public final String resourceSlug;
    private final String mProjectTitle;
    private final String mResourceTitle;
    private final int mCheckingLevel;
    private final String mSourceLanguageTitle;
    private final int mDateModified;
    private final String mVersion;

    private SourceTranslation(String projectId, String sourceLanguageSlug, String resourceSlug, String projectTitle, String sourceLanguageTitle, String resourceTitle, int checkingLevel, int dateModified, String version) {
        this.projectSlug = projectId;
        this.sourceLanguageSlug = sourceLanguageSlug;
        this.resourceSlug = resourceSlug;
        mProjectTitle = projectTitle;
        mSourceLanguageTitle = sourceLanguageTitle;
        mResourceTitle = resourceTitle;
        mCheckingLevel = checkingLevel;
        mDateModified = dateModified;
        mVersion = version;
    }

    /**
     * Generates a new source translation from json
     *
     * @param projectId
     * @param sourceLanguageJson
     * @param resourceJson
     * @return
     * @throws JSONException
     */
    public static SourceTranslation generate(String projectId, JSONObject sourceLanguageJson, JSONObject resourceJson) throws JSONException {
        String sourceLanguageTitle = sourceLanguageJson.getString("name");
        String sourceLanguageId = sourceLanguageJson.getString("slug");
        JSONObject projectLanguageJson = sourceLanguageJson.getJSONObject("project");
        String projectTitle = projectLanguageJson.getString("name");
        String resourceTitle = resourceJson.getString("name");
        String resourceId = resourceJson.getString("slug");
        JSONObject statusJson = resourceJson.getJSONObject("status");
        int checkingLevel = statusJson.getInt("checking_level");
        int dateModified = sourceLanguageJson.getInt("date_modified");
        String version = statusJson.getString("version");
        return new SourceTranslation(projectId, sourceLanguageId, resourceId, projectTitle, sourceLanguageTitle, resourceTitle, checkingLevel, dateModified, version);
    }

    /**
     * Generates a new source translation from json
     *
     * @param projectId
     * @param sourceLanguageJson
     * @param resource
     * @return
     * @throws JSONException
     */
    public static SourceTranslation generate(String projectId, JSONObject sourceLanguageJson, Resource resource) throws JSONException {
        String sourceLanguageTitle = sourceLanguageJson.getString("name");
        String sourceLanguageId = sourceLanguageJson.getString("slug");
        JSONObject projectLanguageJson = sourceLanguageJson.getJSONObject("project");
        String projectTitle = projectLanguageJson.getString("name");
        String resourceTitle = resource.getTitle();
        String resourceId = resource.getId();
        int checkingLevel = resource.getCheckingLevel();
        int dateModified = sourceLanguageJson.getInt("date_modified");
        String version = resource.getVersion();
        return new SourceTranslation(projectId, sourceLanguageId, resourceId, projectTitle, sourceLanguageTitle, resourceTitle, checkingLevel, dateModified, version);
    }

    /**
     * Creates a new simple source translation
     *
     * This object will not contain any extra information about the translation other than the ids
     *
     * @param projectId
     * @param sourceLanguageSlug
     * @param resourceSlug
     */
    private SourceTranslation(String projectId, String sourceLanguageSlug, String resourceSlug) {
        this.projectSlug = projectId;
        this.sourceLanguageSlug = sourceLanguageSlug;
        this.resourceSlug = resourceSlug;
        mProjectTitle = "";
        mSourceLanguageTitle = "";
        mResourceTitle = "";
        mCheckingLevel = 0;
        mDateModified = 0;
        mVersion = "0.0.0";
    }

    /**
     * Generates a simple source translation object.
     *
     * This object is just a container for the ids
     *
     * @param projectId
     * @param sourceLanguageId
     * @param resourceId
     * @return
     */
    public static SourceTranslation simple(String projectId, String sourceLanguageId, String resourceId) {
        return new SourceTranslation(projectId, sourceLanguageId, resourceId);
    }

    /**
     * Returns the date on which the source translation was last modified
     * @return
     */
    public int getDateModified() {
        return mDateModified;
    }

    /**
     * Returns the checking level for this source translation
     * @return
     */
    public int getCheckingLevel() {
        return mCheckingLevel;
    }

    /**
     * Returns the source language id
     * @return
     */
    public String getId() {
        return projectSlug + "-" + sourceLanguageSlug + "-" + resourceSlug;
    }

    /**
     * Returns the title of the project
     * @return
     */
    public String getProjectTitle() {
        return mProjectTitle;
    }

    /**
     * Returns the title of the source language
     * @return
     */
    public String getSourceLanguageTitle() {
        return mSourceLanguageTitle;
    }

    /**
     * Returns the title of the resource
     * @return
     */
    public String getResourceTitle() {
        return mResourceTitle;
    }

    /**
     * Returns the project id
     * @param sourceTranslationId
     * @return
     */
    public static String getProjectIdFromId(String sourceTranslationId) {
        String[] complexId = sourceTranslationId.split("-", 3);
        if(complexId.length == 3) {
            return complexId[0];
        } else {
            throw new StringIndexOutOfBoundsException("malformed source translation id" + sourceTranslationId);
        }
    }

    /**
     * Returns the source language id
     * @param sourceTranslationId
     * @return
     */
    public static String getSourceLanguageIdFromId(String sourceTranslationId) {
        String[] complexId = sourceTranslationId.split("-");
        if(complexId.length >= 3) {
            // TRICKY: source language id's can have dashes in them.
            String sourceLanguageId = complexId[1];
            for(int i = 2; i < complexId.length - 1; i ++) {
                sourceLanguageId += "-" + complexId[i];
            }
            return sourceLanguageId;
        } else {
            throw new StringIndexOutOfBoundsException("malformed source translation id" + sourceTranslationId);
        }
    }

    /**
     * Returns the resource id
     * @param sourceTranslationId
     * @return
     */
    public static String getResourceIdFromId(String sourceTranslationId) {
        String[] complexId = sourceTranslationId.split("-");
        if(complexId.length >= 3) {
            return complexId[complexId.length - 1];
        } else {
            throw new StringIndexOutOfBoundsException("malformed source translation id" + sourceTranslationId);
        }
    }

    /**
     * Returns the version of the source translation
     * @return
     */
    public String getVersion() {
        return mVersion;
    }
}
