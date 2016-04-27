package com.door43.translationstudio.newui.newlanguage;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.NewLanguagePackage;
import com.door43.translationstudio.core.NewLanguageQuestion;
import com.door43.translationstudio.newui.BaseActivity;
import com.door43.translationstudio.newui.newtranslation.NewTargetTranslationActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Activity for getting answers to new language questions
 */
public class NewLanguageActivity extends BaseActivity implements NewLanguagePageFragment.OnEventListener {

    public static final String TAG = NewLanguageActivity.class.getSimpleName();

    private static final String STATE_LANGUAGE_STEP = "state_language_step";
    private static final String STATE_NEW_LANGUAGE_FINISHED = "state_new_language_finished";
    private static final String STATE_NEW_LANGUAGE_QUESTIONS = "state_new_language_questions";

    public static final String EXTRA_CALLING_ACTIVITY = "extra_calling_activity";
    public static final String EXTRA_NEW_LANGUAGE_QUESTIONS = "extra_new_language_questions";
    public static final String STATE_NEW_LANGUAGE_QUESTION_ID = "state_new_language_question_id";

    private int mCurrentPage = 0;
    public static final int ACTIVITY_HOME = 1001;
    private boolean mLanguageFinished = false;
    private NewLanguagePageFragment mFragment;
    private List<List<NewLanguageQuestion>> mQuestionPages;
    private long mQuestionnaireID = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_new_language);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(savedInstanceState != null) {
            mCurrentPage = savedInstanceState.getInt(STATE_LANGUAGE_STEP, -1);
            mLanguageFinished = savedInstanceState.getBoolean(STATE_NEW_LANGUAGE_FINISHED, false);
            String questions = savedInstanceState.getString(STATE_NEW_LANGUAGE_QUESTIONS);
            mQuestionPages = parseJsonStrIntoPages(questions);
            mQuestionnaireID = savedInstanceState.getLong(STATE_NEW_LANGUAGE_QUESTION_ID, -1);
        } else {
            mQuestionPages = readQuestionnaire();
        }

        // inject fragments
        if(findViewById(R.id.fragment_container) != null) {
            if(savedInstanceState != null) {
                mFragment = (NewLanguagePageFragment)getFragmentManager().findFragmentById(R.id.fragment_container);
                if(mFragment != null)  {
                    mFragment.setOnEventListener(this); // restore listener
                }
            } else {
                doPage(mCurrentPage, null);
            }
        }
     }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_LANGUAGE_STEP, mCurrentPage);
        outState.putBoolean(STATE_NEW_LANGUAGE_FINISHED, mLanguageFinished);
        String questions = getQuestionPages().toString();
        outState.putString(STATE_NEW_LANGUAGE_QUESTIONS, questions);
        outState.putLong(STATE_NEW_LANGUAGE_QUESTION_ID,mQuestionnaireID);
        super.onSaveInstanceState(outState);
    }

    /**
     * create new questions page with these questions
     * @param page
     * @return
     */
    private List<NewLanguageQuestion> pushPage(List<NewLanguageQuestion> page) {
        mQuestionPages.add(page);
        List<NewLanguageQuestion> newPage = new ArrayList<>();
        return newPage;
    }

    /**
     * get questionnaire from API
     * @return
     */
    private List<List<NewLanguageQuestion>> readQuestionnaire() {
        HashMap<Long,Integer> idIndex = new HashMap<>();
        List<NewLanguageQuestion> questions = new ArrayList<>();
        List<NewLanguageQuestion> page = new ArrayList<>();
        mQuestionPages = new ArrayList<>();

        try {
            JSONObject questionnaire = (new NewLanguageAPI()).readQuestionnaire(this, "en"); // TODO: 4/25/16 get actual language

            JSONArray questionsJson = questionnaire.getJSONArray(NewLanguageAPI.QUESTIONAIRE_DATA_KEY);
            JSONObject questionaireMeta = questionnaire.getJSONObject(NewLanguageAPI.QUESTIONNAIRE_META_KEY);
            mQuestionnaireID = questionnaire.getLong(NewLanguageAPI.QUESTIONNAIRE_ID_KEY);
            JSONArray questionaireOrder = questionaireMeta.getJSONArray(NewLanguageAPI.QUESTIONNAIRE_ORDER_KEY);

            for(int i = 0; i < questionsJson.length(); i++) {

                // get question
                JSONObject questionJson = questionsJson.getJSONObject(i);
                NewLanguageQuestion questionObj = NewLanguageQuestion.parse(questionJson);
                questions.add(questionObj);
                idIndex.put(questionObj.id, i);
            }

            for(int i = 0; i < questionaireOrder.length(); i++) {
                JSONArray pageOrderJson = questionaireOrder.getJSONArray(i);
                for(int j = 0; j < pageOrderJson.length(); j++) {
                    Long id = pageOrderJson.getLong(j);
                    int index = idIndex.get(id);
                    page.add(questions.get(index));
                }
                page = pushPage(page);
            }

        } catch (Exception e) {
            Logger.e(TAG,"Error parsing questionnaire",e);
            return null;
        }

        return mQuestionPages;
   }

    /**
     * extract answers to questions from JSON
     * @param answersJson
     * @param page
     * @return
     */
    private boolean parseAnswers(String answersJson, int page) {
        try {
            List<NewLanguageQuestion> mPage = mQuestionPages.get(page);
            JSONArray answersPage = new JSONArray(answersJson);

            int length = answersPage.length();
            for(int i = 0; i < length; i++ ) {
                JSONObject question = (JSONObject) answersPage.get(i);
                NewLanguageQuestion originalQuestion = mPage.get(i);
                int questionID = question.getInt(NewLanguageQuestion.ID_KEY);
                if((null == question) || (originalQuestion.id != questionID)) {
                    return false; // response does not match original
                } else {
                    if(question.has(NewLanguageQuestion.ANSWER_KEY)) {
                        originalQuestion.answer = question.getString(NewLanguageQuestion.ANSWER_KEY);
                    } else {
                        originalQuestion.answer = null;
                    }
                }
            }

        } catch (Exception e) {
            Logger.w(TAG, "could not parse answers", e);
            return false;
        }
        return true;
    }

    /**
     * parse json string of questions into list of new language questions
     * @param questionsJson
     * @return
     */
    public static List<NewLanguageQuestion> parseJsonStrIntoQuestions(String questionsJson) {
        List<NewLanguageQuestion> page = null;
        try {
            JSONArray answers = new JSONArray(questionsJson);
            page = parseJsonIntoQuestions( answers);

        } catch (Exception e) {
            Logger.w(TAG, "could not parse questions", e);
            page = null;
        }

        return page;
    }

    /**
     * parse json questions into list of new language questions
     * @param answers
     * @return
     */
    public static List<NewLanguageQuestion> parseJsonIntoQuestions(JSONArray answers) {
        List<NewLanguageQuestion> page = new ArrayList<>();
        try {
            int length = answers.length();
            for(int i = 0; i < length; i++ ) {
                JSONObject questionStr = (JSONObject) answers.get(i);
                NewLanguageQuestion question = NewLanguageQuestion.parse(questionStr);
                if(null != question) {
                    page.add(question);
                }
            }

        } catch (Exception e) {
            Logger.w(TAG, "could not parse questions", e);
            page = null;
        }

        return page;
    }

    /**
     * parse json string of question pages into list of question pages
     * @param pagesJsonStr
     * @return
     */
    public static List<List<NewLanguageQuestion>> parseJsonStrIntoPages(String pagesJsonStr) {
        List<List<NewLanguageQuestion>> pages = new ArrayList<>();
        try {
            JSONArray pagesJson = new JSONArray(pagesJsonStr);

            int length = pagesJson.length();
            for(int i = 0; i < length; i++ ) {
                JSONArray pageStr = (JSONArray) pagesJson.get(i);
                List<NewLanguageQuestion> page = parseJsonIntoQuestions(pageStr);
                 if(null != page) {
                    pages.add(page);
                }
            }

        } catch (Exception e) {
            Logger.w(TAG, "could not parse questions", e);
            pages = null;
        }

        return pages;
    }

    /**
     * get questions for page in JSONArray format
     * @param page
     * @return
     */
    private JSONArray getQuestions(int page) {
        try {
            List<NewLanguageQuestion> mPage = mQuestionPages.get(page);
            return getQuestions(mPage);

        } catch (Exception e) {
            Logger.w(TAG, "could not generate questions", e);
        }

        return null;
    }

    /**
     * get questions from list in JSONArray format
     * @param questions
     * @return
     */
    public static JSONArray getQuestions(List<NewLanguageQuestion> questions) {
        JSONArray answers = new JSONArray();
        if(null != questions) {
            try {
                for (NewLanguageQuestion question : questions) {
                    if (null != question) {
                        JSONObject questionJson = question.toJson();
                        answers.put(questionJson);
                    }
                }

            } catch (Exception e) {
                Logger.w(TAG, "could not parse answers", e);
                answers = null;
            }
        }

        return answers;
    }

    /**
     * get all questions in JSONArray format
     * @return
     */
    private JSONArray getQuestionPages() {
        JSONArray pages = new JSONArray();
        try {
            for (int i = 0; i < mQuestionPages.size(); i++) {
                JSONArray page = getQuestions(i);
                pages.put(page);
            }

        } catch (Exception e) {
            Logger.w(TAG, "could not generate questions", e);
            pages = null;
        }

        return pages;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                goBack();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void goBack(){
        cleanup();
        Intent data = new Intent();
        setResult(RESULT_CANCELED, data);
        finish();
    }

    @Override
    public void onBackPressed() {
        goBack();
    }

    /**
     * moves to the next question page
     * @param answersJson - answers returned by current page
     */
    @Override
    public void nextPage(String answersJson) {
        doPage(mCurrentPage + 1, answersJson);
    }

    /**
     * moves to the previous question page
     * @param answersJson - answers returned by current page
     */
    @Override
    public void previousPage(String answersJson) {
        doPage(mCurrentPage - 1, answersJson);
    }

    @Override
    public void finishLanguageRequest(String answersJson) {

        try {
            cleanup();

            if(answersJson != null) {
                parseAnswers(answersJson, mCurrentPage); // save answers from current page
            }

            mLanguageFinished = true;

            List<NewLanguageQuestion> mergedQuestions = new ArrayList<>();
            for (int i = 0; i < mQuestionPages.size(); i++) {
                List<NewLanguageQuestion> questions = mQuestionPages.get(i);
                mergedQuestions.addAll(questions);
            }

            NewLanguagePackage newLang = NewLanguagePackage.newInstance(mQuestionnaireID, mergedQuestions);
            String newLanguageDataStr = newLang.toJson().toString(2);

            JSONObject api = NewLanguageAPI.getPostData(newLang); // TODO: 4/28/16 remove

            Intent data = new Intent();
            data.putExtra(NewTargetTranslationActivity.EXTRA_NEW_LANGUAGE_DATA, newLanguageDataStr);
            setResult(RESULT_OK, data);
            finish();

        } catch(Exception e) {
            Logger.e(TAG,"Error getting question data",e);
        }
    }

    public void cleanup() {
        if(mFragment != null) {
            mFragment.cleanup();
        }
    }

    /**
     * Moves to a specific question page
     * @param page
     */
    private void doPage(int page, String answersJson) {

        if(answersJson != null) {
            parseAnswers(answersJson, mCurrentPage); // save answers from current page
        }

        if(page > mQuestionPages.size()) {
            mCurrentPage = mQuestionPages.size() - 1;
        } else if(page < 0) {
            mCurrentPage = 0;
        } else{
            mCurrentPage = page;
        }

        mFragment = new NewLanguagePageFragment();

        Bundle args = getIntent().getExtras();
        args.putBoolean(NewLanguagePageFragment.ARG_NEW_LANG_FINISHED, mLanguageFinished);
        args.putBoolean(NewLanguagePageFragment.ARG_FIRST_PAGE, mCurrentPage == 0);
        args.putBoolean(NewLanguagePageFragment.ARG_LAST_PAGE, mCurrentPage == (mQuestionPages.size() - 1));
        args.putString(NewLanguageActivity.EXTRA_NEW_LANGUAGE_QUESTIONS, getQuestions(mCurrentPage).toString());
        mFragment.setArguments(args);
        mFragment.setOnEventListener(this);
        getFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();
    }
}

