package com.door43.translationstudio.core;

import android.content.Context;

import org.json.JSONObject;

/**
 * Created by blm on 3/9/16.
 */
public class NewLanguageQuestion {
    public static final String ID = "id";
    public static final String QUESTION = "question";
    public static final String HELP_TEXT = "helpText";
    public static final String INPUT = "input";
    public static final String REQUIRED = "required";
    public static final String QUERY = "query";
    public static final String TYPE = "type";
    public static final String CONDITIONAL_ID = "conditional_id";

    public long id;
    public String question;
    public String helpText;
    public String answer; // can be null
    public boolean required;
    public QuestionType type;
    public long conditionalID = -1;

    NewLanguageQuestion(long id, String question, String helpText, String answer,
                        QuestionType type, boolean required, long conditionalID) {
        this.id = id;
        this.question = question;
        this.helpText = helpText;
        this.answer = answer;
        this.required = required;
        this.type = type;
        this.conditionalID = conditionalID;
    }

    public static NewLanguageQuestion generateFromResources(Context context, long id, int questionResID, int hintResID,
                                                            QuestionType type, boolean required, long conditionalID) {
        String question = context.getResources().getString(questionResID);
        String hint = context.getResources().getString(hintResID);

        return new NewLanguageQuestion(id,question,hint,null,type,required, conditionalID);
    }

    public static NewLanguageQuestion generateFromResources(Context context, long id, int questionResID, int hintResID,
                                                            QuestionType type, boolean required) {

        return generateFromResources( context, id, questionResID, hintResID, type, required, -1);

    }

    public static NewLanguageQuestion generateFromJson(JSONObject json) {
        try {
            long id = json.getLong(ID);
            String question = json.getString(QUESTION);
            String answer = null;
            if(json.has(NewLanguageQuestion.INPUT)) {
                answer = json.getString(NewLanguageQuestion.INPUT);
            }
            String hint = json.getString(HELP_TEXT);
            boolean required = json.getBoolean(REQUIRED);
            String typeStr = json.getString(TYPE);
            QuestionType type = QuestionType.get(typeStr);
            long conditional = json.getLong(CONDITIONAL_ID);
            return new NewLanguageQuestion(id,question,hint,answer,type,required, conditional);
        } catch (Exception e) {

        }

        return null;
    }

    public JSONObject toJson() {
        JSONObject results = new JSONObject();
        try {
            results.put(ID, id);
            results.put(QUESTION, question);
            results.put(HELP_TEXT, helpText);
            if(null != answer) {
                results.put(INPUT, answer);
            }
            results.put(REQUIRED, required);
            results.put(TYPE, type.toString());
            results.put(CONDITIONAL_ID, conditionalID);
            return results;
        } catch (Exception e) {

        }
        return null;
    }

    public JSONObject getResults() {
        JSONObject results = new JSONObject();
        try {
            results.put(ID, id);
            results.put(QUESTION, question);
            results.put(HELP_TEXT, helpText);
            results.put(INPUT, (null != answer) ? answer : "(NULL)");
            results.put(REQUIRED, required);
            results.put(QUERY, "");
        } catch (Exception e) {

        }
        return results;
    }

    public enum QuestionType {
        EDIT_TEXT("edit_text"),
        CHECK_BOX("check_box");

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
