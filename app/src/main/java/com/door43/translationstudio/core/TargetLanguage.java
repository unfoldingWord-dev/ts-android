package com.door43.translationstudio.core;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by joel on 8/29/2015.
 */
public class TargetLanguage {

    public final LanguageDirection direction;
    public final Boolean isGatewayLanguage;
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

    public enum LanguageDirection {
        LeftToRight("ltr"),
        RightToLeft("rtl");

        LanguageDirection(String label) {
            this.label = label;
        }

        private String label;

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return getLabel();
        }

        /**
         * Returns a direction by it's label
         * @param label
         * @return
         */
        public static LanguageDirection get(String label) {
            for (LanguageDirection l : LanguageDirection.values()) {
                if (l.getLabel().equals(label.toLowerCase())) {
                    return l;
                }
            }
            return null;
        }
    }

    private TargetLanguage (String code, String name, String region, Boolean isGatewayLanguage, LanguageDirection direction) {
        this.code = code;
        this.name = name;
        this.region = region;
        this.isGatewayLanguage = isGatewayLanguage;
        this.direction = direction;
    }

    /**
     * Generates a new target language from json
     * @param json
     * @return
     */
    public static TargetLanguage Generate(JSONObject json) {
        if(json == null) {
            return null;
        }
        try {
            return new TargetLanguage(
                    json.getString("lc"),
                    json.getString("ln"),
                    json.getString("lr"),
                    json.getBoolean("gw"),
                    LanguageDirection.get(json.getString("ld"))
            );
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
