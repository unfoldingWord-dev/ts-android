package com.door43.translationstudio.core;

import android.support.annotation.Nullable;

import com.door43.tools.reporting.Logger;

import org.json.JSONObject;

/**
 * Represents a single question in a new language questionnaire
 */
public class NewLanguageQuestion {
    public long id;
    public String question;
    public String helpText;
    public boolean required;
    public InputType type;
    public long reliantQuestionId = -1;
    public final int sort;

    /**
     * Instanciates a new question
     * @param id the translationDatabase id of the question
     * @param question the question text
     * @param helpText the help text
     * @param type the input type of the question
     * @param required if the question is required
     * @param reliantQuestionId the question that must be answered before this question
     * @param sort the order of display of this question
     */
    public NewLanguageQuestion(long id, String question, String helpText,
                               InputType type, boolean required,
                               long reliantQuestionId, int sort) {
        this.id = id;
        this.question = question;
        this.helpText = helpText;
        this.required = required;
        this.type = type;
        this.reliantQuestionId = reliantQuestionId;
        this.sort = sort;
    }

    /**
     * creates a Nquestion object from JSON
     * @param json
     * @return
     */
    public static NewLanguageQuestion generate(JSONObject json) {
        if(json != null) {
            try {
                long id = json.getLong("id");
                String text = json.getString("text");
                String help = json.getString("help");
                boolean required = json.getBoolean("required");
                String inputType = json.getString("input_type");
                int sort = json.getInt("sort");

                InputType type = InputType.get(inputType);
                if (null == type) {
                    type = InputType.String;
                }
                long reliantQuestionId = json.optLong("depends_on", -1);
                return new NewLanguageQuestion(id, text, help, type, required, reliantQuestionId, sort);
            } catch (Exception e) {
                Logger.w(NewLanguageQuestion.class.getSimpleName(), "Error parsing new language question: " + json.toString(), e);
            }
        }
        return null;
    }

    /**
     * Returns a json representation of the question
     * @return
     */
    @Nullable
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("text", question);
            json.put("help", helpText);
            json.put("required", required);
            json.put("input_type", type.getLabel());
            json.put("sort", sort);
            json.put("depends_on", reliantQuestionId);
            return json;
        } catch (Exception e) {
            Logger.w(NewLanguageQuestion.class.getSimpleName(), "Failed to build question json", e);
        }
        return null;
    }

    /**
     * set answer string for boolean state
     * @param anwer
     * @return
     */
    public void setAnswer(boolean anwer) {
//        this.answer = anwer ? "true" : "false";
    }

    /**
     * test to see if boolean is true, null answer is treated as false
     * @return
     */
    public boolean isBooleanAnswerTrue() {
//        return "true".equals(answer);
        return false;
    }

    /**
     * test to see if boolean is false, null answer is treated as false
     * @return
     */
    public boolean isBooleanAnswerFalse() {
//        return "false".equals(answer);
        return true;
    }

    /**
     * get answer string and replace null with ""
     * @return
     */
    public String getAnswerNotNull() {
//        if(null == answer) {
            return "";
//        }
//        return answer;
    }

    /**
     * test if answer to question is empty (either null or empty string)
     * @return
     */
    public boolean isAnswerEmpty() {
//        return (null == answer ) || answer.isEmpty();
        return true;
    }

     /**
     * Specifies the input type of a question
     */
    public enum InputType {
         String("string"),
         Boolean("boolean"),
         Date("date");

         InputType(String label) {
             this.label = label;
         }

         private final String label;

         public String getLabel() {
             return label;
         }

        @Override
        public String toString() {
            return getLabel();
        }

        /**
         * Returns an input type by it's label
         * @param label
         * @return
         */
        public static InputType get(String label) {
            if(label != null) {
                for (InputType t : InputType.values()) {
                    if (t.getLabel().equals(label.toLowerCase())) {
                        return t;
                    }
                }
            }
            return null;
        }
    }
}
