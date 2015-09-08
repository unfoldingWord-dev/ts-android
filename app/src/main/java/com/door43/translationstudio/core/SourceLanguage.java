package com.door43.translationstudio.core;

import com.door43.translationstudio.targettranslations.LanguageDirection;

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

    public SourceLanguage(String code, String name, int dateModified, LanguageDirection direction) {
        this.code = code;
        this.name = name;
        this.dateModified = dateModified;
        this.direction = direction;
    }

    public String getId() {
        return code;
    }

    /**
     * Generates a source language object from json
     * @param json
     * @return
     * @throws JSONException
     */
    public static SourceLanguage Generate(JSONObject json) throws JSONException {
        if(json == null) {
            return null;
        }
        return new SourceLanguage(
                json.getString("slug"),
                json.getString("name"),
                json.getInt("date_modified"),
                LanguageDirection.get(json.getString("direction")));
    }

    @Override
    public int compareTo(Object another) {
        String anotherCode = ((SourceLanguage)another).getId();
        return code.compareToIgnoreCase(anotherCode);
    }
}
