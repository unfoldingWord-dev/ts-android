package com.door43.translationstudio.core;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a language from which a project is translated
 */
public class SourceLanguage implements Comparable {
    public final String code;
    public final String name;
    public final LanguageDirection direction;
    public final int dateModified;
    public final String projectTitle;

    public SourceLanguage(String code, String name, int dateModified, LanguageDirection direction, String projectTitle) {
        this.code = code;
        this.name = name;
        this.dateModified = dateModified;
        this.direction = direction;
        this.projectTitle = projectTitle;
    }

    public String getId() {
        return code;
    }

    public LanguageDirection getDirection() {
        return direction;
    }

    /**
     * Generates a source language object from json
     * @param json
     * @return
     * @throws JSONException
     */
    public static SourceLanguage generate(JSONObject json) throws JSONException {
        if(json == null) {
            return null;
        }
        JSONObject projectJson = json.getJSONObject("project");
        return new SourceLanguage(
                json.getString("slug"),
                json.getString("name"),
                json.getInt("date_modified"),
                LanguageDirection.get(json.getString("direction")),
                projectJson.getString("name"));
    }

    @Override
    public int compareTo(Object another) {
        String anotherCode = ((SourceLanguage)another).getId();
        return code.compareToIgnoreCase(anotherCode);
    }
}
