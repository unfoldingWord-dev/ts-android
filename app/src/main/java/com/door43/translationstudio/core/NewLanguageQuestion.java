package com.door43.translationstudio.core;

import android.content.Context;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.newui.newtranslation.NewLanguageAPI;

import org.json.JSONObject;

/**
 * Created by blm on 3/9/16.
 */
public class NewLanguageQuestion {
    public static final String ID = "id";
    public static final String QUESTION = "question";
    public static final String HELP_TEXT = "helpText";
    public static final String ANSWER = "answer";
    public static final String REQUIRED = "required";
    public static final String QUERY = "query";
    public static final String INPUT_TYPE = "input";
    public static final String CONDITIONAL_ID = "conditional_id";

    public long id;
    public String question;
    public String helpText;
    public String answer; // can be null
    public boolean required;
    public QuestionType type;
    public String query;
    public long conditionalID = -1;

    public NewLanguageQuestion(long id, String question, String helpText, String answer,
                               QuestionType type, boolean required, String query,
                               long conditionalID) {
        this.id = id;
        this.question = question;
        this.helpText = helpText;
        this.answer = answer;
        this.required = required;
        this.type = type;
        this.conditionalID = conditionalID;
        this.query = query;
    }

    /**
     * create a NewLanguageQuestion object from resources
     * @param context
     * @param id
     * @param questionResID
     * @param hintResID
     * @param type
     * @param required
     * @param conditionalID
     * @return
     */
    public static NewLanguageQuestion generateFromResources(Context context, long id, int questionResID, int hintResID,
                                                            QuestionType type, boolean required, String query, long conditionalID) {
        String question = context.getResources().getString(questionResID);
        String hint = context.getResources().getString(hintResID);

        return new NewLanguageQuestion(id,question,hint,null,type,required, query, conditionalID);
    }

    /**
     * create a NewLanguageQuestion object from resources without conditional
     * @param context
     * @param id
     * @param questionResID
     * @param hintResID
     * @param type
     * @param required
     * @return
     */
    public static NewLanguageQuestion generateFromResources(Context context, long id, int questionResID, int hintResID,
                                                            QuestionType type, boolean required, String query) {

        return generateFromResources( context, id, questionResID, hintResID, type, required, query, -1);

    }

    /**
     * create a NewLanguageQuestion object from JSON data
     * @param json
     * @return
     */
    public static NewLanguageQuestion parse(JSONObject json) {
        try {
            long id = json.getLong(ID);
            String question = json.getString(QUESTION);
            String answer = null;
            if(json.has(NewLanguageQuestion.ANSWER)) {
                answer = json.getString(NewLanguageQuestion.ANSWER);
            }
            String hint = json.getString(HELP_TEXT);
            boolean required = json.getBoolean(REQUIRED);
            String typeStr = json.getString(INPUT_TYPE);
            QuestionType type = QuestionType.get(typeStr);
            long conditional = -1;
            if(json.has(NewLanguageQuestion.CONDITIONAL_ID)) {
                conditional = json.getLong(CONDITIONAL_ID);
            }
            String query = json.getString(QUERY);
            return new NewLanguageQuestion(id,question,hint,answer,type,required, query, conditional);
        } catch (Exception e) {
            Logger.e(NewLanguageQuestion.class.getSimpleName(),"Error parsing json: " + json.toString(),e);
        }

        return null;
    }

    /**
     * return the JSON data representation for object
     * @return
     */
    public JSONObject toJson() {
        JSONObject results = new JSONObject();
        try {
            results.put(ID, id);
            results.put(QUESTION, question);
            results.put(HELP_TEXT, helpText);
            if(null != answer) {
                results.put(ANSWER, answer);
            }
            results.put(REQUIRED, required);
            results.put(INPUT_TYPE, type.toString());
            results.put(CONDITIONAL_ID, conditionalID);
            results.put(QUERY, query);
            return results;
        } catch (Exception e) {

        }
        return null;
    }

    /**
     * returns JSON data in API format
     * @return
     */
    public JSONObject getResults() {
        JSONObject results = new JSONObject();
        try {
            results.put(ID, id);
            results.put(QUESTION, question);
            results.put(HELP_TEXT, helpText);
            results.put(ANSWER, (null != answer) ? answer : "(NULL)");
            results.put(REQUIRED, required);
            results.put(QUERY, query);
        } catch (Exception e) {

        }
        return results;
    }

    /**
     * enum to identify question types and do string conversions
     */
    public enum QuestionType {
        EDIT_TEXT(NewLanguageAPI.INPUT_TYPE_STR),
        CHECK_BOX(NewLanguageAPI.INPUT_TYPE_BOOLEAN);

        QuestionType(String s) {
            mName = s;
        }

        private final String mName;

        public String getName() {
            return mName;
        }

        @Override
        public String toString() {
            return mName;
        }

        /**
         * Returns a format by it's name
         * @param name
         * @return
         */
        public static QuestionType get(String name) {
            if(name != null) {
                for (QuestionType f : QuestionType.values()) {
                    if (f.getName().equals(name.toLowerCase())) {
                        return f;
                    }
                }
            }
            return null;
        }
    }
}
