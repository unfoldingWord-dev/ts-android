package com.door43.translationstudio.core;

import android.content.Context;
import android.provider.Settings;
import android.support.annotation.Nullable;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.util.Security;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by joel on 6/1/16.
 */
public class NewLanguageQuestionnaireResponse {
    private static final String LANGUAGE_PREFIX = "qaa-x-";

    private Map<Long, String> answers = new HashMap<>();

    public final String requestUUID;
    public final String tempLanguageCode;
    public final long questionnaireId;
    public final String app;
    public final String requester;

    /**
     * Instanciates a new questionnaire response
     * @param requestUUID
     * @param tempLanguageCode
     * @param questionnaireId
     * @param app
     * @param requester
     */
    private NewLanguageQuestionnaireResponse(String requestUUID, String tempLanguageCode, long questionnaireId, String app, String requester) {
        this.requestUUID = requestUUID;
        this.tempLanguageCode = tempLanguageCode;
        this.questionnaireId = questionnaireId;
        this.app = app;
        this.requester = requester;
    }

    /**
     * Creates a new questionnaire response
     * @return
     */
    public static NewLanguageQuestionnaireResponse newInstance(Context context, long questionnaireId, String app, String requester) {
        // generate language code
        String udid = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        long time = System.currentTimeMillis();
        String uniqueString = udid + time;
        String hash = Security.sha1(uniqueString);
        String languageCode  = LANGUAGE_PREFIX + hash.substring(0, 6);

        return new NewLanguageQuestionnaireResponse(UUID.randomUUID().toString(), languageCode, questionnaireId, app, requester);
    }

    /**
     * Adds or updates an answer
     * @param questionId
     * @param answer
     */
    @Nullable
    public void addAnswer(long questionId, String answer) {
        this.answers.put(questionId, answer);
    }

    /**
     * Represents the questionnaire response as json
     * @return
     */
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("request_id", this.requestUUID);
            json.put("temp_code", this.tempLanguageCode);
            json.put("questionnaire_id", this.questionnaireId);
            json.put("app", this.app);
            json.put("requester", this.requester);

            JSONArray answersJson = new JSONArray();
            for(Long key:this.answers.keySet()) {
                JSONObject answer = new JSONObject();
                answer.put("question_id", key);
                answer.put("text", this.answers.get(key));
                answersJson.put(answer);
            }

            json.put("answers", answersJson);
            return json;
        } catch (JSONException e) {
            Logger.w(this.getClass().getName(), "Failed to create json object", e);
        }
        return null;
    }

    /**
     * Creates a questionnaire response from json
     * @param json
     * @return
     */
    public static NewLanguageQuestionnaireResponse generate(JSONObject json) {
        if(json != null) {
            try {
                String requestUUID = json.getString("request_id");
                String tempCode = json.getString("temp_code");
                long questionnaireId = json.getLong("questionnaire_id");
                String app = json.getString("app");
                String requester = json.getString("requester");
                NewLanguageQuestionnaireResponse response = new NewLanguageQuestionnaireResponse(requestUUID, tempCode, questionnaireId, app, requester);

                JSONArray answers = json.getJSONArray("answers");
                for(int i = 0; i < answers.length(); i ++) {
                    JSONObject answer = answers.getJSONObject(i);
                    response.addAnswer(answer.getLong("question_id"), answer.getString("text"));
                }
                return response;
            } catch (JSONException e) {
                Logger.w(NewLanguageQuestionnaireResponse.class.getName(), "Failed to parse questionnaire response json: " + json.toString(), e);
            }
        }
        return null;
    }
}
