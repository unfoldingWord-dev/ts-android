package com.door43.translationstudio.core;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a language to which a project is translated
 */
@Deprecated
public class TargetLanguage extends org.unfoldingword.door43client.models.TargetLanguage {

    public final LanguageDirection direction;

    /**
     * Returns the language code for the target language
     * @return
     */
    public String getId() {
        return slug;
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
        json.put("region", this.region);
        json.put("id", this.slug);
        json.put("name", this.name);
        return json;
    }

    public TargetLanguage (String slug, String name, String region, LanguageDirection direction) {
        super(slug, name, "", direction.toString(), region, false);
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

    /**
     * Formats the object as json
     * @return
     * @throws JSONException
     */
    public JSONObject toApiFormatJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("ld", this.direction.toString());
            json.put("lc", this.slug);
            json.put("ln", this.name);
            json.put("lr", this.region);
            return json;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public LanguageDirection getDirection() {
        return direction;
    }
}
