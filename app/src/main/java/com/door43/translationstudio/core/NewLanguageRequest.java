package com.door43.translationstudio.core;

import android.content.Context;
import android.provider.Settings;
import android.support.annotation.Nullable;

import com.door43.tools.reporting.Logger;
import com.door43.util.Security;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Created by joel on 6/1/16.
 */
public class NewLanguageRequest {
    private static final String LANGUAGE_PREFIX = "qaa-x-";

    private Map<Long, String> answers = new TreeMap<>();
    public final Map<String, Long> dataFields;
    public final String requestUUID;
    public final String tempLanguageCode;
    public final long questionnaireId;
    public final String app;
    public final String requester;
    private long submittedAt = 0;

    /**
     * Instanciates a new questionnaire response
     * @param requestUUID an id that identifies this response
     * @param tempLanguageCode the temporary language code that will be assigned
     * @param questionnaireId the translationDatabase id of the questionnaire
     * @param app the name of the app generating this response
     * @param requester the name of the translator requesting the custom language code
     */
    private NewLanguageRequest(String requestUUID, String tempLanguageCode, long questionnaireId, String app, String requester, Map<String, Long> dataFields) {
        this.requestUUID = requestUUID;
        this.tempLanguageCode = tempLanguageCode;
        this.questionnaireId = questionnaireId;
        this.app = app;
        this.requester = requester;
        this.dataFields = dataFields;
    }

    /**
     * Creates a new questionnaire response
     * @return
     */
    public static NewLanguageRequest newInstance(Context context, Questionnaire questionnaire, String app, String requester) {
        // generate language code
        String udid = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        long time = System.currentTimeMillis();
        String uniqueString = udid + time;
        String hash = Security.sha1(uniqueString);
        String languageCode  = LANGUAGE_PREFIX + hash.substring(0, 6);

        return new NewLanguageRequest(UUID.randomUUID().toString(), languageCode, questionnaire.door43Id, app, requester, questionnaire.dataFields);
    }

    /**
     * Adds or updates an answer
     * @param questionTdId
     * @param answer
     */
    @Nullable
    public void setAnswer(long questionTdId, String answer) {
        this.answers.put(questionTdId, answer);
    }

    /**
     * Represents the questionnaire response as json
     * @return
     */
    public String toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("request_id", this.requestUUID);
            json.put("temp_code", this.tempLanguageCode);
            json.put("questionnaire_id", this.questionnaireId);
            json.put("app", this.app);
            json.put("requester", this.requester);
            json.put("submitted_at", this.submittedAt);

            JSONObject dataFieldsJson = new JSONObject();
            for(String field:this.dataFields.keySet()) {
                dataFieldsJson.put(field, this.dataFields.get(field));
            }
            json.put("data_fields", dataFieldsJson);

            JSONArray answersJson = new JSONArray();
            for(Long key:this.answers.keySet()) {
                JSONObject answer = new JSONObject();
                answer.put("question_id", key);
                answer.put("text", this.answers.get(key));
                answersJson.put(answer);
            }
            json.put("answers", answersJson);
            return json.toString();
        } catch (JSONException e) {
            Logger.w(this.getClass().getName(), "Failed to create json object", e);
        }
        return null;
    }

    /**
     * Creates a questionnaire response from json
     *
     * @param jsonString
     * @return
     */
    @Nullable
    public static NewLanguageRequest generate(String jsonString) {
        if (jsonString != null) {
            try {
                JSONObject json = new JSONObject(jsonString);
                String requestUUID = json.getString("request_id");
                String tempCode = json.getString("temp_code");
                long questionnaireId = json.getLong("questionnaire_id");
                String app = json.getString("app");
                String requester = json.getString("requester");
                long submittedAt = 0;
                if(json.has("submitted_at")) {
                    submittedAt = json.getLong("submitted_at");
                }

                Map<String, Long> dataFields = new HashMap<>();
                if(json.has("data_fields")) {
                    JSONObject dataFieldsJson = json.getJSONObject("data_fields");
                    Iterator<String> fieldItr = dataFieldsJson.keys();
                    while (fieldItr.hasNext()) {
                        String field = fieldItr.next();
                        dataFields.put(field, dataFieldsJson.getLong(field));
                    }
                }
                NewLanguageRequest request = new NewLanguageRequest(requestUUID, tempCode, questionnaireId, app, requester, dataFields);
                request.setSubmittedAt(submittedAt);

                JSONArray answers = json.getJSONArray("answers");
                for (int i = 0; i < answers.length(); i++) {
                    JSONObject answer = answers.getJSONObject(i);
                    request.setAnswer(answer.getLong("question_id"), answer.getString("text"));
                }
                return request;
            } catch (JSONException e) {
                Logger.w(NewLanguageRequest.class.getName(), "Failed to parse questionnaire response json: " + jsonString, e);
            }
        }
        return null;
    }

    /**
     * returns the answer by the question id
     * @param questionTdId
     * @return
     */
    @Nullable
    public String getAnswer(long questionTdId) {
        return answers.get(questionTdId);
    }

    /**
     * Returns the answers in this request
     * @return
     */
    public Map<Long, String> getAnswers() {
        return answers;
    }

    /**
     * Returns the time when this request was submitted
     * @return
     */
    public long getSubmittedAt() {
        return submittedAt;
    }

    /**
     * Sets the time when this request was submitted
     * @param submittedAt
     */
    public void setSubmittedAt(long submittedAt) {
        this.submittedAt = submittedAt;
    }

    /**
     * Returns the temporary target language.
     * Properties of the language are derrived from the questionnaire answers
     * @return
     */
    public TargetLanguage getTempTargetLanguage() {
        String name = this.tempLanguageCode;
        String region = "unknown";
        LanguageDirection direction = LanguageDirection.LeftToRight;

        if(this.dataFields.containsKey("ln")) {
            name = this.getAnswer(this.dataFields.get("ln"));
        }
        if(this.dataFields.containsKey("cc")) {
            region = this.getAnswer(this.dataFields.get("cc"));
        }
        if(this.dataFields.containsKey("ld")) {
            Boolean isLeftToRight = Boolean.parseBoolean(this.getAnswer(this.dataFields.get("ld")));
            direction = isLeftToRight ? LanguageDirection.LeftToRight : LanguageDirection.RightToLeft;
        }

        return new TargetLanguage(this.tempLanguageCode, name, region, direction);
    }
}
