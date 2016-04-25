package com.door43.translationstudio.core;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a language to which a project is translated
 */
public class TargetLanguage implements Comparable {

    public final LanguageDirection direction;
    public final String region;
    public final String name;
    public final String code;

    /**
     * Returns the language code for the target language
     * @return
     */
    public String getId() {
        return code;
    }

    @Override
    public int compareTo(Object another) {
        String anotherCode = ((TargetLanguage)another).getId();
        return code.compareToIgnoreCase(anotherCode);
    }

    /**
     * Formats the object as json
     * @return
     * @throws JSONException
     */
    public JSONObject toJson() throws JSONException {
        // TODO: we should restructure this output to match what we see in the api. if we do we'll need to migrate all the old manifest files.
        JSONObject json = new JSONObject();
        json.put("direction", this.direction.toString());
        json.put("id", this.code);
        json.put("name", this.name);
        return json;
    }

    public TargetLanguage (String code, String name, String region, LanguageDirection direction) {
        this.code = code;
        this.name = name;
        this.region = region;
        this.direction = direction;
    }

    /**
     * Generates a new target language from json
     * @param json
     * @return
     */
    public static TargetLanguage generate(JSONObject json) throws JSONException {
        if(json == null) {
            return null;
        }
        return new TargetLanguage(
                json.getString("lc"),
                json.getString("ln"),
                json.getString("lr"),
                LanguageDirection.get(json.getString("ld"))
        );
    }

    public LanguageDirection getDirection() {
        return direction;
    }
}
