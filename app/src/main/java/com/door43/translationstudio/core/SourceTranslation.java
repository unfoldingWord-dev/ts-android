package com.door43.translationstudio.core;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a single source translation.
 */
public class SourceTranslation {
    public final String projectId;
    public final String sourceLanguageId;
    public final String resourceId;
    private final String mProjectTitle;
    private final String mResourceTitle;
    private final int mCheckingLevel;
    private final String mSourceLanguageTitle;

    private SourceTranslation(String projectId, String sourceLanguageId, String resourceId, String projectTitle, String sourceLanguageTitle, String resourceTitle, int checkingLevel) {
        this.projectId = projectId;
        this.sourceLanguageId = sourceLanguageId;
        this.resourceId = resourceId;
        mProjectTitle = projectTitle;
        mSourceLanguageTitle = sourceLanguageTitle;
        mResourceTitle = resourceTitle;
        mCheckingLevel = checkingLevel;
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
        return new SourceTranslation(projectId, sourceLanguageId, resourceId, projectTitle, sourceLanguageTitle, resourceTitle, checkingLevel);
    }

    /**
     * Creates a new simple source translation
     *
     * This object will not contain any extra information about the translation other than the ids
     *
     * @param projectId
     * @param sourceLanguageId
     * @param resourceId
     */
    private SourceTranslation(String projectId, String sourceLanguageId, String resourceId) {
        this.projectId = projectId;
        this.sourceLanguageId = sourceLanguageId;
        this.resourceId = resourceId;
        mProjectTitle = "";
        mSourceLanguageTitle = "";
        mResourceTitle = "";
        mCheckingLevel = 0;
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
        return projectId + "-" + sourceLanguageId + "-" + resourceId;
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
        String[] complexId = sourceTranslationId.split("-", 3);
        if(complexId.length == 3) {
            return complexId[1];
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
        String[] complexId = sourceTranslationId.split("-", 3);
        if(complexId.length == 3) {
            return complexId[2];
        } else {
            throw new StringIndexOutOfBoundsException("malformed source translation id" + sourceTranslationId);
        }
    }
}
