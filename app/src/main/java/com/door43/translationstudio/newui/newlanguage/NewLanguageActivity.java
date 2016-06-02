package com.door43.translationstudio.newui.newlanguage;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.NewLanguageQuestion;
import com.door43.translationstudio.core.NewLanguageQuestionnaire;
import com.door43.translationstudio.core.NewLanguageQuestionnaireResponse;
import com.door43.translationstudio.newui.BaseActivity;

import java.util.ArrayList;
import java.util.List;


/**
 * Activity for getting answers to new language questions
 */
public class NewLanguageActivity extends BaseActivity implements NewLanguagePageFragment.OnEventListener {

    public static final String TAG = NewLanguageActivity.class.getSimpleName();
    public static final String EXTRA_QUESTIONNAIRE_RESPONSE = "questionnaire_response";

    private static final String STATE_PAGE = "current_page";
    private static final String STATE_FINISHED = "finished";

    private int mCurrentPage = 0;
    private boolean mQuestionnaireFinished = false;
    private NewLanguagePageFragment mFragment;
    private NewLanguageQuestionnaire mQuestionnaire;
    private NewLanguageQuestionnaireResponse mResponse = null;
    private RecyclerView mRecyclerView;
    private NewLanguagePageAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_new_language);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mQuestionnaire = AppContext.getLibrary().getNewLanguageQuestionnaire();
        if(mQuestionnaire == null) {
            Logger.e(this.getClass().getName(), "Cannot begin the new language questionnaire because no questionnaire was found.");
            // TODO: 6/1/16 display notice to user
            finish();
            return;
        }

        setTitle(mQuestionnaire.languageName);

        if(savedInstanceState != null) {
            mCurrentPage = savedInstanceState.getInt(STATE_PAGE, 0);
            mQuestionnaireFinished = savedInstanceState.getBoolean(STATE_FINISHED, false);
            mResponse = NewLanguageQuestionnaireResponse.generate(savedInstanceState.getString(EXTRA_QUESTIONNAIRE_RESPONSE));
            if(mResponse == null) {
                // TODO: 6/1/16 display an error to the user and restart
                finish();
                return;
            }
        } else {
            mResponse = NewLanguageQuestionnaireResponse.newInstance(this, mQuestionnaire.door43Id, "android", AppContext.getProfile().getFullName());
        }

        mRecyclerView = (RecyclerView)findViewById(R.id.recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mAdapter = new NewLanguagePageAdapter();
        mRecyclerView.setAdapter(mAdapter);

        // inject fragments
        if(findViewById(R.id.fragment_container) != null) {
            if(savedInstanceState != null) {
                // attach to existing fragment
                mFragment = (NewLanguagePageFragment)getFragmentManager().findFragmentById(R.id.fragment_container);
                if(mFragment != null)  {
                    mFragment.setOnEventListener(this);
                }
            } else {
                // inject new fragment
                goToPage(mCurrentPage, null);
            }
        }
     }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_PAGE, mCurrentPage);
        outState.putBoolean(STATE_FINISHED, mQuestionnaireFinished);
        outState.putSerializable(EXTRA_QUESTIONNAIRE_RESPONSE, mResponse.toJson());
//        String questions = getQuestionPagesAsJson().toString();
//        outState.putString(STATE_QUESTIONS, questions);
//        outState.putLong(STATE_QUESTIONNAIRE_ID,mQuestionnaireID);
//        outState.putString(STATE_LANGUAGE_TITLE, getTitle().toString());
        super.onSaveInstanceState(outState);
    }

//    /**
//     * create new questions page with these questions
//     * @param page
//     * @return
//     */
//    private static List<NewLanguageQuestion> pushPage(List<List<NewLanguageQuestion>> questionPages, List<NewLanguageQuestion> page) {
//        questionPages.add(page);
//        List<NewLanguageQuestion> newPage = new ArrayList<>();
//        return newPage;
//    }

//    /**
//     * get questionnaire from API
//     * @return
//     */
//    private List<List<NewLanguageQuestion>> getQuestionPages(String questionsJsonStr) {
//        List<List<NewLanguageQuestion>> questionPages = new ArrayList<>();
//
//        try {
//            JSONObject questionnaire = (new NewLanguageAPI()).readQuestionnaireIntoPages(this, questionsJsonStr, "en"); // TODO: 4/25/16 get actual language
//            mQuestionnaireID = getQuestionnaireID(questionnaire);
//            getQuestionPages(questionPages, questionnaire);
//
//        } catch (Exception e) {
//            Logger.e(TAG,"Error parsing questionnaire",e);
//            return null;
//        }
//
//        return questionPages;
//   }
//
//    /**
//     * get questionaire ID number from json
//     * @param questionnaire
//     * @return
//     * @throws JSONException
//     */
//    public static long getQuestionnaireID(JSONObject questionnaire) throws JSONException {
//        return questionnaire.getLong(NewLanguageAPI.QUESTIONNAIRE_ID_KEY);
//    }

//    /**
//     * get questions from json and divide into pages
//     * @param questionPages
//     * @param questionnaire
//     * @throws JSONException
//     */
//    public static void getQuestionPages(List<List<NewLanguageQuestion>> questionPages, JSONObject questionnaire) throws JSONException {
//        HashMap<Long,Integer> idIndex = new HashMap<>();
//        List<NewLanguageQuestion> questions = new ArrayList<>();
//        List<NewLanguageQuestion> page = new ArrayList<>();
//
//        JSONArray questionsJson = questionnaire.getJSONArray(NewLanguageAPI.QUESTIONAIRE_DATA_KEY);
//        JSONObject questionaireMeta = questionnaire.getJSONObject(NewLanguageAPI.QUESTIONNAIRE_META_KEY);
//        JSONArray questionaireOrder = questionaireMeta.getJSONArray(NewLanguageAPI.QUESTIONNAIRE_ORDER_KEY);
//
//        for(int i = 0; i < questionsJson.length(); i++) {
//
//            // get question
//            JSONObject questionJson = questionsJson.getJSONObject(i);
//            NewLanguageQuestion questionObj = NewLanguageQuestion.generate(questionJson);
//            questions.add(questionObj);
//            idIndex.put(questionObj.id, i);
//        }
//
//        for(int i = 0; i < questionaireOrder.length(); i++) {
//            JSONArray pageOrderJson = questionaireOrder.getJSONArray(i);
//            for(int j = 0; j < pageOrderJson.length(); j++) {
//                Long id = pageOrderJson.getLong(j);
//                int index = idIndex.get(id);
//                page.add(questions.get(index));
//            }
//            page = pushPage(questionPages, page);
//        }
//    }


//    private boolean parseAnswers(String answersJson, int page) {
//        try {
//            List<NewLanguageQuestion> mPage = mQuestionPages.get(page);
//            JSONArray answersPage = new JSONArray(answersJson);
//
//            int length = answersPage.length();
//            for(int i = 0; i < length; i++ ) {
//                JSONObject question = (JSONObject) answersPage.get(i);
//                NewLanguageQuestion originalQuestion = mPage.get(i);
//                int questionID = question.getInt("id");
//                if((null == question) || (originalQuestion.id != questionID)) {
//                    return false; // response does not match original
//                } else {
//                    if(question.has("answer")) {
//                        originalQuestion.answer = question.getString("answer");
//                    } else {
//                        originalQuestion.answer = null;
//                    }
//                }
//            }
//
//        } catch (Exception e) {
//            Logger.w(TAG, "could not parse answers", e);
//            return false;
////        }
//        return true;
//    }

//    /**
//     * parse json string of questions into list of new language questions
//     * @param questionsJson
//     * @return
//     */
//    public static List<NewLanguageQuestion> parseJsonStrIntoQuestions(String questionsJson) {
//        List<NewLanguageQuestion> page = null;
//        try {
//            JSONArray answers = new JSONArray(questionsJson);
//            page = parseJsonIntoQuestions( answers);
//
//        } catch (Exception e) {
//            Logger.w(TAG, "could not parse questions", e);
//            page = null;
//        }
//
//        return page;
//    }

//    /**
//     * parse json questions into list of new language questions
//     * @param answers
//     * @return
//     */
//    public static List<NewLanguageQuestion> parseJsonIntoQuestions(JSONArray answers) {
//        List<NewLanguageQuestion> page = new ArrayList<>();
//        try {
//            int length = answers.length();
//            for(int i = 0; i < length; i++ ) {
//                JSONObject questionStr = (JSONObject) answers.get(i);
//                NewLanguageQuestion question = NewLanguageQuestion.generate(questionStr);
//                if(null != question) {
//                    page.add(question);
//                }
//            }
//
//        } catch (Exception e) {
//            Logger.w(TAG, "could not parse questions", e);
//            page = null;
//        }
//
//        return page;
//    }

//    /**
//     * parse json string of question pages into list of question pages
//     * @param pagesJsonStr
//     * @return
//     */
//    public static List<List<NewLanguageQuestion>> parseJsonStrIntoPages(String pagesJsonStr) {
//        List<List<NewLanguageQuestion>> pages = new ArrayList<>();
//        try {
//            JSONArray pagesJson = new JSONArray(pagesJsonStr);
//
//            int length = pagesJson.length();
//            for(int i = 0; i < length; i++ ) {
//                JSONArray pageStr = (JSONArray) pagesJson.get(i);
//                List<NewLanguageQuestion> page = parseJsonIntoQuestions(pageStr);
//                 if(null != page) {
//                    pages.add(page);
//                }
//            }
//
//        } catch (Exception e) {
//            Logger.w(TAG, "could not parse questions", e);
//            pages = null;
//        }
//
//        return pages;
//    }

//    /**
//     * get questions for page in JSONArray format
//     * @param page
//     * @return
//     */
//    private JSONArray getQuestionsForPage(int page) {
//        try {
//            List<NewLanguageQuestion> mPage = mQuestionPages.get(page);
//            return getQuestionsAsJson(mPage);
//
//        } catch (Exception e) {
//            Logger.w(TAG, "could not generate questions", e);
//        }
//
//        return null;
//    }

//    /**
//     * get questions from list in JSONArray format
//     * @param questions
//     * @return
//     */
//    public static JSONArray getQuestionsAsJson(List<NewLanguageQuestion> questions) {
//        JSONArray answers = new JSONArray();
//        if(null != questions) {
//            try {
//                for (NewLanguageQuestion question : questions) {
//                    if (null != question) {
//                        JSONObject questionJson = question.toJson();
//                        answers.put(questionJson);
//                    }
//                }
//
//            } catch (Exception e) {
//                Logger.w(TAG, "could not parse answers", e);
//                answers = null;
//            }
//        }
//
//        return answers;
//    }

//    /**
//     * get all questions in JSONArray format
//     * @return
//     */
//    private JSONArray getQuestionPagesAsJson() {
//        JSONArray pages = new JSONArray();
//        try {
//            for (int i = 0; i < mQuestionPages.size(); i++) {
//                JSONArray page = getQuestionsForPage(i);
//                pages.put(page);
//            }
//
//        } catch (Exception e) {
//            Logger.w(TAG, "could not generate questions", e);
//            pages = null;
//        }
//
//        return pages;
//    }

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
        goToPage(mCurrentPage + 1, answersJson);
    }

    /**
     * moves to the previous question page
     * @param answersJson - answers returned by current page
     */
    @Override
    public void previousPage(String answersJson) {
        goToPage(mCurrentPage - 1, answersJson);
    }

    @Override
    public void onQuestionnaireFinished(String answersJson) {

        try {
            cleanup();

//            if(answersJson != null) {
//                parseAnswers(answersJson, mCurrentPage); // save answers from current page
//            }

            mQuestionnaireFinished = true;

//            List<NewLanguageQuestion> mergedQuestions = mergePagesOfNewLanguageQuestions(mQuestionPages);
//            NewLanguagePackage newLang = NewLanguagePackage.newInstance(mQuestionnaireID, mergedQuestions);
//            String newLanguageDataStr = newLang.toJson().toString(2);

//            HashMap<String, String> api = NewLanguageAPI.getPostData(newLang); // TODO: 4/28/16 remove

            Intent data = new Intent();
            data.putExtra(EXTRA_QUESTIONNAIRE_RESPONSE, ""); // TODO: add data
            setResult(RESULT_OK, data);
            finish();

        } catch(Exception e) {
            Logger.e(TAG,"Error getting question data",e);
        }
    }

    public static List<NewLanguageQuestion> mergePagesOfNewLanguageQuestions(List<List<NewLanguageQuestion>> questionPages) {
        List<NewLanguageQuestion> mergedQuestions = new ArrayList<>();
        for (int i = 0; i < questionPages.size(); i++) {
            List<NewLanguageQuestion> questions = questionPages.get(i);
            mergedQuestions.addAll(questions);
        }
        return mergedQuestions;
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
    private void goToPage(int page, String answersJson) {
        if(page > mQuestionnaire.getNumPages()) {
            mCurrentPage = mQuestionnaire.getNumPages() - 1;
        } else if(page < 0) {
            mCurrentPage = 0;
        } else{
            mCurrentPage = page;
        }

        String titleFormat = getResources().getString(R.string.new_language_questionnaire_title);
        String title = String.format(titleFormat, mCurrentPage + 1, mQuestionnaire.getNumPages());
        setTitle(title);

        // TODO: 6/1/16 update adapter

//        mFragment = new NewLanguagePageFragment();

//        Bundle args = getIntent().getExtras();
//        args.putBoolean(NewLanguagePageFragment.ARG_NEW_LANG_FINISHED, mQuestionnaireFinished);
//        args.putBoolean(NewLanguagePageFragment.ARG_FIRST_PAGE, mCurrentPage == 0);
//        args.putBoolean(NewLanguagePageFragment.ARG_LAST_PAGE, mCurrentPage == (mQuestionnaire.getNumPages() - 1));
//        args.putString(EXTRA_NEW_LANGUAGE_QUESTIONS, getQuestionsForPage(mCurrentPage).toString());
//        mFragment.setArguments(args);
//        mFragment.setOnEventListener(this);
//        getFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();
    }
}

