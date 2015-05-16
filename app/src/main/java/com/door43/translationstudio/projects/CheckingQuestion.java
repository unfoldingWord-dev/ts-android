package com.door43.translationstudio.projects;

import com.door43.util.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 5/16/2015.
 */
public class CheckingQuestion {

    public final String question;
    public final String answer;
    public final List<String> references;
    public final String chapterId;

    protected CheckingQuestion(String chapterId, String question, String answer, List<String> references) {
        this.question = question;
        this.answer = answer;
        this.references = references;
        this.chapterId = chapterId;
    }

    /**
     * Generates a checking question from json
     * @param chapterId
     * @param json
     * @return
     */
    public static CheckingQuestion generate(String chapterId, JSONObject json) {
        try {
            List<String> references = new ArrayList<>();
            JSONArray jsonRefs = json.getJSONArray("ref");
            for(int i = 0; i < jsonRefs.length(); i ++) {
                references.add(jsonRefs.getString(i));
            }
            return new CheckingQuestion(chapterId, json.getString("q"), json.getString("a"), references);
        } catch (JSONException e) {
            Logger.e(CheckingQuestion.class.getName(), "failed to parse checking question", e);
            return null;
        }
    }
}
