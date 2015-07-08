package com.door43.translationstudio.projects;

import com.door43.util.reporting.Logger;
import com.door43.util.Security;

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
    private boolean mViewed;
    private String mId = null;

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

    /**
     * Sets whether or not the question has been viewed
     * @param viewed
     */
    public void setViewed(boolean viewed) {
        mViewed = viewed;
        // TODO: we may want to get a timestamp and keep track of when questions where viewed so we don't have to re-view questions for frames that have not changed.
    }

    /**
     * Checks if the question has been viewed
     * @return
     */
    public boolean isViewed() {
        return mViewed;
    }

    /**
     * Returns the id of the checking question
     * @return
     */
    public String getId() {
        if(mId == null) {
            mId = Security.md5(this.chapterId + this.question);
        }
        return mId;
    }
}
