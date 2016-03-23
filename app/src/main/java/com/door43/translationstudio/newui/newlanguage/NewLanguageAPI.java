package com.door43.translationstudio.newui.newlanguage;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.core.NewLanguageQuestion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by blm on 3/21/16.
 */
public class NewLanguageAPI {
    public static final String INPUT_TYPE_STRING = "str";
    public static final String INPUT_TYPE_BOOLEAN = "boolean";

    public static final String QUESTIONNAIRE_ID_KEY = "id";
    public static final String QUESTIONNAIRE_META_KEY = "meta";
    public static final String QUESTIONAIRE_DATA_KEY = "data";
    public static final String QUESTIONNAIRE_ORDER_KEY = "order";
    public static final String TAG = NewLanguageAPI.class.getSimpleName();
    private JSONArray mPageData;
    private JSONArray mPageMeta;

    private JSONArray mData;
    private JSONArray mMeta;
    public NewLanguageAPI() {
        
    }
    
    public JSONObject readQuestionnaire() {
        return createTestQuestionaire(); // TODO: 3/21/16 change to read from API
    }

    private JSONObject createTestQuestionaire() {
        JSONObject questionnaire = new JSONObject();
        mData = new JSONArray();
        mMeta = new JSONArray();
        
        try {
            final boolean required = true;
            final boolean not_required = false;
            
            mPageData = new JSONArray();
            mPageMeta = new JSONArray();

            addQuestion(1, "1. What do you call your language?", "(Enter Answer Here)", INPUT_TYPE_STRING, required);
            addQuestion(2, "1.1  Does that have a special meaning?", "(Enter Answer Here)", INPUT_TYPE_STRING, not_required);
            addQuestion(3, "1.2  Do you have other names for your language?", "(Enter Answer Here)", INPUT_TYPE_STRING, not_required);
            pushPage();

            addQuestion(4, "2. Other people have a different name for your language?", "(Enter Answer Here)", INPUT_TYPE_BOOLEAN, required);
            addQuestion(5, "2.1. What do they call it?", "(Enter Answer Here)", INPUT_TYPE_STRING, not_required);
            addQuestion(6, "2.2. Who calls it that?", "(Enter Answer Here)", INPUT_TYPE_STRING, not_required);
            addQuestion(7, "2.3. Does that have a special meaning?  What does it mean?", "(Enter Answer Here)", INPUT_TYPE_STRING, not_required);
            pushPage();

            addQuestion(8, "3. Where else do people speak just the same way as you do?", "(Enter Answer Here)", INPUT_TYPE_STRING, required);
            addQuestion(9, "4. Where do people speak just a little bit differently?", "(Enter Answer Here)", INPUT_TYPE_STRING, not_required);
            addQuestion(10, "4.1. Have you yourself gone to these places?", "(Enter Answer Here)", INPUT_TYPE_STRING, not_required);
            addQuestion(11, "4.2. Do people from there also come here?", "(Enter Answer Here)", INPUT_TYPE_STRING, not_required);
            addQuestion(12, "4.3. Do you have a name for this other dialect in your own language?", "(Enter Answer Here)", INPUT_TYPE_STRING, not_required);
            pushPage();

            addQuestion(13, "5. Where do people speak very differently?", "(Enter Answer Here)", INPUT_TYPE_STRING, required);
            addQuestion(14, "5.1. How much of their speech do you understand? (few words, main ideas, most, all)", "(Enter Answer Here)", INPUT_TYPE_STRING, not_required);
            addQuestion(15, "5.2. Have you yourself gone to these places?", "(Enter Answer Here)", INPUT_TYPE_STRING, not_required);
            addQuestion(16, "5.3. Do people from there come here?", "(Enter Answer Here)", INPUT_TYPE_STRING, not_required);
            addQuestion(17, "5.4. Do you have a name for this other dialect in your own language?", "(Enter Answer Here)", INPUT_TYPE_STRING, not_required);
            pushPage();

            addQuestion(18, "6. Where do people speak your own language the most purely?", "(Enter Answer Here)", INPUT_TYPE_STRING, not_required);
            addQuestion(19, "6.1. Why do you say that is the most pure?", "(Enter Answer Here)", INPUT_TYPE_STRING, not_required);
            addQuestion(20, "6.2. Have you yourself gone to these places?", "(Enter Answer Here)", INPUT_TYPE_STRING, not_required);
            addQuestion(21, "6.3. Do people from there come here?", "(Enter Answer Here)", INPUT_TYPE_STRING, not_required);
            pushPage();

            addQuestion(22, "7. Where is your language spoken badly?", "(Enter Answer Here)", INPUT_TYPE_STRING, not_required);
            addQuestion(23, "7.1. Why do you say that it is spoken badly?", "(Enter Answer Here)", INPUT_TYPE_STRING, not_required);
            addQuestion(24, "7.2. Have you yourself gone to these places?", "(Enter Answer Here)", INPUT_TYPE_STRING, not_required);
            addQuestion(25, "7.3. Have people from there come here?", "(Enter Answer Here)", INPUT_TYPE_STRING, not_required);
            pushPage();

            addQuestion(26, "8. What do you call the language people speak in [large place, 1-2  day’s march distance]?", "(Enter Answer Here)", INPUT_TYPE_STRING, not_required);
            addQuestion(27, "8.1. Do people understand you when you speak your language there?", "(Enter Answer Here)", INPUT_TYPE_STRING, not_required);
            addQuestion(28, "8.2. If a man from there come here, can everybody understand his speech?", "(Enter Answer Here)", INPUT_TYPE_STRING, not_required);
            addQuestion(29, "8.3. Even the women and children?", "(Enter Answer Here)", INPUT_TYPE_STRING, not_required);
            pushPage();

            addQuestion(30, "9. What other places do you travel to?", "(Enter Answer Here)", INPUT_TYPE_STRING, required);
            addQuestion(31, "10. Do many people travel outside from here?", "(Enter Answer Here)", INPUT_TYPE_STRING, required);
            pushPage();

            questionnaire.put(QUESTIONNAIRE_ID_KEY, 1010);
            JSONObject meta = new JSONObject();
            meta.put(QUESTIONNAIRE_ORDER_KEY, mMeta);
            questionnaire.put(QUESTIONNAIRE_META_KEY, meta);
            questionnaire.put(QUESTIONAIRE_DATA_KEY, mData);
//            String formatted = questionnaire.toString(2);
//            Logger.i(TAG,"created: " + formatted);
            return questionnaire;

        } catch (Exception e) {
            Logger.e(TAG,"Error creating questionnaire",e);
        }

        return null;
    }

    private void addQuestion(long id, String question, String helpText, String inputType, boolean required) throws JSONException {
        addQuestion( id, question, helpText, inputType, required, Long.toString(id));
    }
    
    private void addQuestion(long id, String question, String helpText, String inputType, boolean required, String query) throws JSONException {
        JSONObject questionData = new JSONObject();
        questionData.put(NewLanguageQuestion.ID_KEY,id);
        questionData.put(NewLanguageQuestion.QUESTION_KEY,question);
        questionData.put(NewLanguageQuestion.HELP_TEXT_KEY,helpText);
        questionData.put(NewLanguageQuestion.INPUT_TYPE_KEY,inputType);
        questionData.put(NewLanguageQuestion.REQUIRED_KEY,required);
        questionData.put(NewLanguageQuestion.QUERY_KEY,query);
        mPageData.put(questionData);
        mPageMeta.put(id);
    }
    
    private void pushPage( ) throws JSONException {
        for(int i = 0; i <mPageData.length(); i++) {
            mData.put(mPageData.getJSONObject(i));
        }
        mMeta.put(mPageMeta);

        mPageData = new JSONArray();
        mPageMeta = new JSONArray();
    }
}
