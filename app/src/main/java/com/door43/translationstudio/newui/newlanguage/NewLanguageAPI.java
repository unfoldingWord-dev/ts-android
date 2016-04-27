package com.door43.translationstudio.newui.newlanguage;

import android.content.Context;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.core.NewLanguagePackage;
import com.door43.translationstudio.core.NewLanguageQuestion;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
    public static final int QUESTIONS_PER_PAGE = 3;

    public static final String API_READ_LANGUAGES = "languages";
    public static final String API_READ_QUESTIONS_LANGUAGE_SLUG = "slug";
    public static final String API_READ_QUESTION_ID = "id";
    public static final String API_READ_QUESTION_TEXT = "text";
    public static final String API_READ_HELP = "help";
    public static final String API_READ_INPUT_TYPE = "input_type";
    public static final String API_READ_SORT = "sort";
    public static final String API_READ_DEPENDS_ON = "depends_on";
    public static final String API_READ_QUESTIONS = "questions";
    public static final String API_READ_REQUIRED = "required";

    private JSONArray mPageData;
    private JSONArray mPageMeta;

    private JSONArray mData;
    private JSONArray mMeta;

    public NewLanguageAPI() {
        
    }
    
    public JSONObject readQuestionnaire(Context context, String sourceLangID) {
        try {
            InputStream is = context.getResources().getAssets().open("newLanguageQuestionaire.json"); // // TODO: 4/28/16 replace with library call
            if(is != null) {
                String data = IOUtils.toString(is);
                return parseQuestionsFromApi(data, sourceLangID);
            }
        } catch (Exception e) {
            Logger.e(TAG,"Error getting questionnaire",e);
        }

        return null;
    }

    /**
     * generate the data to be posted to new language API
     * @return
     */
    public static JSONObject getPostData(NewLanguagePackage pkg)  {
        try {
            JSONObject apis = new JSONObject();
            apis.put(NewLanguagePackage.API_REQUEST_ID, AppContext.udid());
            apis.put(NewLanguagePackage.API_TEMP_CODE, pkg.tempLanguageCode);
            apis.put(NewLanguagePackage.API_QUESTIONNAIRE_ID, pkg.questionaireID+"");
            apis.put(NewLanguagePackage.API_APP, pkg.app);
            apis.put(NewLanguagePackage.API_REQUESTER, pkg.requester);
            JSONArray answers = new JSONArray();
            for (int i = 0; i < pkg.answersJson.length(); i++) {
                JSONObject answer = pkg.answersJson.getJSONObject(i);
                JSONObject obj = new JSONObject();
                obj.put(NewLanguagePackage.API_QUESTION_ID, answer.getInt(NewLanguagePackage.API_QUESTION_ID));
                obj.put(NewLanguagePackage.API_ANSWER, answer.optString(NewLanguagePackage.QUESTION_ANSWER,""));
                answers.put(obj);
            }
            apis.put(NewLanguagePackage.API_ANSWERS, answers.toString());
            return apis;

        } catch (Exception e) {
            Logger.e(TAG, "Could not create API json", e);
        }

        return null;
    }

    /**
     * parse API new language data into new object.  Returns null if error.
     *
     * @param jsonStr
     * @return
     */
    private JSONObject parseQuestionsFromApi(String jsonStr, String sourceLangID) {
        List<NewLanguageQuestion> questions = new ArrayList<>();
        try {
            JSONObject apiData = new JSONObject(jsonStr);
            JSONArray languages = apiData.getJSONArray(API_READ_LANGUAGES);

            if(languages != null) {
                for (int i = 0; i < languages.length(); i++) {
                    JSONObject language = languages.getJSONObject(i);
                    String langSlug = language.getString(API_READ_QUESTIONS_LANGUAGE_SLUG);
                    String questionaireID = language.getString(NewLanguagePackage.API_QUESTIONNAIRE_ID);
                    if(sourceLangID.equals(langSlug)) {
                        JSONArray questionsStr = language.getJSONArray(API_READ_QUESTIONS);
                        return packageQuestions(Integer.valueOf(questionaireID), questionsStr);
                    }
                }
            }
        } catch (Exception e) {
            Logger.e(TAG, "Failed to parse data", e);
        }
        return null;
    }

    private JSONObject packageQuestions(int questionaireID, JSONArray questions) {
        JSONObject questionnaire = new JSONObject();
        mData = new JSONArray();
        mMeta = new JSONArray();
        
        try {
            final boolean required = true;
            final boolean not_required = false;
            
            mPageData = new JSONArray();
            mPageMeta = new JSONArray();

            // group dependencies
            List<JSONObject> questionList = new ArrayList<>();
            for (int j = 0; j < questions.length(); j++) {
                JSONObject question = questions.getJSONObject(j);
                int sort = Integer.valueOf(question.getString(API_READ_SORT));

                while(questionList.size() <= sort) {  // make sure item will fit
                    questionList.add(null);
                }

                questionList.set(sort, question);
            }

            // remove missing items
            int j = 0;
            while ( j < questionList.size() ) {
                JSONObject question = questionList.get(j);
                if( null == question) {
                    questionList.remove(j);
                } else {
                    j++;
                }
            }

            for ( j = 0; j < questionList.size(); j++) {
                JSONObject question = questionList.get(j);

                int id = Integer.valueOf(question.getString(API_READ_QUESTION_ID));

                int k = j + 1;
                JSONObject nextQuestion = getNextDependentQuestion( questionList, id, k);
                if(nextQuestion != null) {

                    if(mPageData.length() > 1) {
                        pushPage();
                    }

                    addQuestion(question);
                    addQuestion(nextQuestion);
                    int lastPos = k;
                    lastPos = getDependentQuestions(questionList, id, k+1, lastPos);
                    j = lastPos; //skip over the dependent questions
                    continue;
                }

                addQuestion(question);

                if(mPageData.length() >= QUESTIONS_PER_PAGE) {
                    pushPage();
                }
            }

            if(mPageData.length() > 0) {
                pushPage();
            }

            questionnaire.put(QUESTIONNAIRE_ID_KEY, questionaireID);
            JSONObject meta = new JSONObject();
            meta.put(QUESTIONNAIRE_ORDER_KEY, mMeta);
            questionnaire.put(QUESTIONNAIRE_META_KEY, meta);
            questionnaire.put(QUESTIONAIRE_DATA_KEY, mData);
            return questionnaire;

        } catch (Exception e) {
            Logger.e(TAG,"Error creating questionnaire",e);
        }

        return null;
    }

    private int getDependentQuestions(List<JSONObject> questionList, int id, int k, int lastPos) throws Exception {
        for (; k < questionList.size(); k++) {
            JSONObject nextQuestion = getNextDependentQuestion( questionList, id, k);
            if(nextQuestion != null) {
                lastPos = k;
                addQuestion(nextQuestion);
            } else {
                break;
            }
        }

        pushPage();
        return lastPos;
    }

    private JSONObject getNextDependentQuestion(List<JSONObject> questionList, int id, int pos) throws Exception {
        if(pos >= questionList.size()) {
            return null;
        }

        JSONObject questionNext = questionList.get(pos);

        // find adjacent dependencies
        long dependsOn = questionNext.optLong(API_READ_DEPENDS_ON, -1);
        if (id != dependsOn) {
            return null;
        }

        return questionNext;
    }

    private void addQuestion(JSONObject question) throws JSONException {
        String id = question.getString(API_READ_QUESTION_ID);
        String questionString = question.getString(API_READ_QUESTION_TEXT);
        String helpText = question.getString(API_READ_HELP);
        boolean required = question.getBoolean(API_READ_REQUIRED);
        String inputType = question.getString(API_READ_INPUT_TYPE);
        long dependsOn = question.optLong(API_READ_DEPENDS_ON, -1);
        addQuestion(Long.valueOf(id), questionString, helpText, inputType, required, dependsOn);
    }

    private void addQuestion(long id, String question, String helpText, String inputType, boolean required, long dependsOn) throws JSONException {
        addQuestion( id, question, helpText, inputType, required, dependsOn, Long.toString(id));
    }
    
    private void addQuestion(long id, String question, String helpText, String inputType, boolean required, long dependsOn, String query) throws JSONException {
        JSONObject questionData = new JSONObject();
        questionData.put(NewLanguageQuestion.ID_KEY,id);
        questionData.put(NewLanguageQuestion.QUESTION_KEY,question);
        questionData.put(NewLanguageQuestion.HELP_TEXT_KEY,helpText);
        questionData.put(NewLanguageQuestion.INPUT_TYPE_KEY,inputType);
        questionData.put(NewLanguageQuestion.REQUIRED_KEY,required);
        questionData.put(NewLanguageQuestion.QUERY_KEY,query);
        questionData.put(NewLanguageQuestion.CONDITIONAL_ID_KEY,dependsOn);
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
