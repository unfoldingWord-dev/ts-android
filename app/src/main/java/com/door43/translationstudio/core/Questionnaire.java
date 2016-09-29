package com.door43.translationstudio.core;

import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import org.unfoldingword.tools.logger.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Questionnaires contain a series of questions grouped by page that must be answered by the user
 */
@Deprecated
public class Questionnaire {
    public final long door43Id;
    public final String languageSlug;
    public final String languageName;
    public final LanguageDirection languageDirection;

    public Questionnaire(long door43Id, String languageSlug, String languageName, LanguageDirection direction) {
        this.door43Id = door43Id;
        this.languageSlug = languageSlug;
        this.languageName = languageName;
        this.languageDirection = direction;
    }

    /**
     * Generates a new questionnaire from json
     * @param jsonString
     * @return
     */
    @Nullable
    public static Questionnaire generate(String jsonString) {
        if(jsonString != null) {
            try {
                JSONObject json = new JSONObject(jsonString);
                String languageName = json.getString("name");
                LanguageDirection direction = LanguageDirection.get(json.getString("dir"));
                if(direction == null) {
                    direction = LanguageDirection.LeftToRight;
                }
                String languageSlug = json.getString("slug");
                long questionnaireId = json.getLong("questionnaire_id");
//                return new Questionnaire(questionnaireId, languageSlug, languageName, direction);
            } catch (JSONException e) {
//                Logger.w(Questionnaire.class.getName(), "Failed to parse new language questionnaire: " + jsonString, e);
            }
        }
        return null;
    }


}
