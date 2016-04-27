package com.door43.translationstudio.core;

import android.content.Context;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.newui.newlanguage.NewLanguageAPI;

import org.json.JSONObject;

/**
 * Wrapper for new language questions
 */
public class NewLanguageQuestion {
    public static final String ID_KEY = "id";
    public static final String QUESTION_KEY = "question";
    public static final String HELP_TEXT_KEY = "helpText";
    public static final String ANSWER_KEY = "answer";
    public static final String REQUIRED_KEY = "required";
    public static final String QUERY_KEY = "query";
    public static final String INPUT_TYPE_KEY = "input";
    public static final String CONDITIONAL_ID_KEY = "conditional_id";

    public static final String TRUE_STR = "YES";
    public static final String FALSE_STR = "NO";
    public static final String TAG = NewLanguageQuestion.class.getSimpleName();

    public long id;
    public String question;
    public String helpText;
    public String answer; // can be null
    public boolean required;
    public QuestionType type;
    public String query;
    public long conditionalID = -1;

    /**
     * constructor
     * @param id
     * @param question
     * @param helpText
     * @param answer
     * @param type
     * @param required
     * @param query
     * @param conditionalID
     */
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
    public static NewLanguageQuestion newInstance(Context context, long id, int questionResID, int hintResID,
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
    public static NewLanguageQuestion newInstance(Context context, long id, int questionResID, int hintResID,
                                                  QuestionType type, boolean required, String query) {

        return newInstance(context, id, questionResID, hintResID, type, required, query, -1);

    }

    /**
     * create a NewLanguageQuestion object from JSON data
     * @param json
     * @return
     */
    public static NewLanguageQuestion parse(JSONObject json) {
        try {
            long id = json.getLong(ID_KEY);
            String question = json.getString(QUESTION_KEY);
            String answer = null;
            if(json.has(NewLanguageQuestion.ANSWER_KEY)) {
                answer = json.getString(NewLanguageQuestion.ANSWER_KEY);
            }
            String hint = json.getString(HELP_TEXT_KEY);
            boolean required = json.getBoolean(REQUIRED_KEY);
            String typeStr = json.getString(INPUT_TYPE_KEY);
            QuestionType type = QuestionType.get(typeStr);
            if(null == type) { // default to string input
                type = QuestionType.INPUT_TYPE_STRING;
            }
            long conditional = -1;
            if(json.has(NewLanguageQuestion.CONDITIONAL_ID_KEY)) {
                conditional = json.getLong(CONDITIONAL_ID_KEY);
            }
            String query = json.getString(QUERY_KEY);
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
            results.put(ID_KEY, id);
            results.put(QUESTION_KEY, question);
            results.put(HELP_TEXT_KEY, helpText);
            if(null != answer) {
                results.put(ANSWER_KEY, answer);
            }
            results.put(REQUIRED_KEY, required);
            results.put(INPUT_TYPE_KEY, type.toString());
            results.put(CONDITIONAL_ID_KEY, conditionalID);
            results.put(QUERY_KEY, query);
            return results;
        } catch (Exception e) {
            Logger.e(TAG,"json creation error", e);
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
            results.put(ID_KEY, id);
            results.put(QUESTION_KEY, question);
            results.put(HELP_TEXT_KEY, helpText);
            results.put(ANSWER_KEY, (null != answer) ? answer : "(NULL)");
            results.put(REQUIRED_KEY, required);
            results.put(QUERY_KEY, query);
        } catch (Exception e) {
            Logger.e(TAG,"json parse error", e);
        }
        return results;
    }

    /**
     * set answer string for boolean state
     * @param truth
     * @return
     */
    public void setAnswer(boolean truth) {
        this.answer = truth ? TRUE_STR : FALSE_STR;
    }

    /**
     * test to see if boolean is true, null answer is treated as false
     * @return
     */
    public boolean isBooleanAnswerTrue() {
        return TRUE_STR.equals(answer);
    }

    /**
     * test to see if boolean is false, null answer is treated as false
     * @return
     */
    public boolean isBooleanAnswerFalse() {
        return FALSE_STR.equals(answer);
    }

    /**
     * get answer string and replace null with ""
     * @return
     */
    public String getAnswerNotNull() {
        if(null == answer) {
            return "";
        }
        return answer;
    }

    /**
     * test if answer to question is empty (either null or empty string)
     * @return
     */
    public boolean isAnswerEmpty() {
        return (null == answer ) || answer.isEmpty();
    }

     /**
     * enum to identify question types and do string conversions
     */
    public enum QuestionType {
        INPUT_TYPE_STRING(NewLanguageAPI.INPUT_TYPE_STRING),
        INPUT_TYPE_BOOLEAN(NewLanguageAPI.INPUT_TYPE_BOOLEAN);

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

                if("string".equalsIgnoreCase(name)) { // alternate for string type
                    return QuestionType.INPUT_TYPE_STRING;
                }
            }
            return null;
        }
    }
}
