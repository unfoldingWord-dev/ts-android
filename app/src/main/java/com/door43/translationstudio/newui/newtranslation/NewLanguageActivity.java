package com.door43.translationstudio.newui.newtranslation;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.NewLanguageQuestion;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.newui.BaseActivity;
import com.door43.translationstudio.newui.translate.TargetTranslationActivity;
import com.door43.translationstudio.AppContext;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by blm on 2/23/16.
 */
public class NewLanguageActivity extends BaseActivity implements NewLanguagePageFragment.OnEventListener {

    public static final String TAG = NewLanguageActivity.class.getSimpleName();

    private static final String STATE_LANGUAGE_STEP = "state_language_step";
    private static final String STATE_NEW_LANGUAGE_FINISHED = "state_new_language_finished";
    private static final String STATE_NEW_LANGUAGE_ANSWERS = "state_new_language_answers";

    public static final String EXTRA_CALLING_ACTIVITY = "extra_calling_activity";
    public static final String EXTRA_NEW_LANGUAGE_QUESTIONS = "extra_new_language_questions";

    public static final String NEW_LANGUAGE_REQUEST_ID = "request_id";
    public static final String NEW_LANGUAGE_TEMP_CODE = "temp_code";
    public static final String NEW_LANGUAGE_QUESTIONAIRE_ID = "questionaire_id";
    public static final String NEW_LANGUAGE_ANSWERS = "answers";
    public static final String NEW_LANGUAGE_ANSWER = "answer";
    public static final String NEW_LANGUAGE_QUESTION_ID = "question_id";

    private Translator mTranslator;
    private TargetTranslation mTargetTranslation;
    private int mCurrentPage = 0;
    public static final int ACTIVITY_HOME = 1001;
    public static final int ACTIVITY_TRANSLATION = 1002;
    private boolean mLanguageFinished = false;
    private int mCallingActivity;
    private NewLanguagePageFragment mFragment;
    private List<List<NewLanguageQuestion>> mQuestionPages;
    private int mQuestionaireID = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_new_language);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mTranslator = AppContext.getTranslator();

        // validate parameters
        Bundle args = getIntent().getExtras();

        // identify calling activity
        mCallingActivity = args.getInt(EXTRA_CALLING_ACTIVITY, 0);
        if(mCallingActivity == 0) {
            throw new InvalidParameterException("you must specify the calling activity");
        }

        if(savedInstanceState != null) {
            mCurrentPage = savedInstanceState.getInt(STATE_LANGUAGE_STEP, 0);
            mLanguageFinished = savedInstanceState.getBoolean(STATE_NEW_LANGUAGE_FINISHED, false);
            String answers = savedInstanceState.getString(STATE_NEW_LANGUAGE_ANSWERS);
            mQuestionPages = parseJsonStrIntoPages(answers);
        } else {
            mQuestionPages = new ArrayList<>();
            createQuestions();
        }

        // inject fragments
        if(findViewById(R.id.fragment_container) != null) {
            if(savedInstanceState != null) {
                mFragment = (NewLanguagePageFragment)getFragmentManager().findFragmentById(R.id.fragment_container);
            } else {
                doPage(mCurrentPage, null);
            }
        }
     }

    private List<NewLanguageQuestion> pushPage(List<NewLanguageQuestion> page) {
        mQuestionPages.add(page);
        List<NewLanguageQuestion> newPage = new ArrayList<>();
        return newPage;
    }

    private void createQuestions() {
        final boolean required = true;
        final boolean not_required = false;
        List<NewLanguageQuestion> page = new ArrayList<>();

        page.add(NewLanguageQuestion.generateFromResources(getApplication(), 100, R.string.language_name_called, R.string.enter_answer, NewLanguageQuestion.QuestionType.EDIT_TEXT, required ));
        page.add(NewLanguageQuestion.generateFromResources(getApplication(), 101, R.string.language_name_meaning, R.string.enter_answer, NewLanguageQuestion.QuestionType.EDIT_TEXT, not_required, 100 ));
        page.add(NewLanguageQuestion.generateFromResources(getApplication(), 102, R.string.language_name_alternates, R.string.enter_answer, NewLanguageQuestion.QuestionType.EDIT_TEXT, not_required, 100 ));
        page = pushPage(page);

        page.add(NewLanguageQuestion.generateFromResources(getApplication(), 200, R.string.language_others_name, R.string.enter_answer, NewLanguageQuestion.QuestionType.CHECK_BOX, required ));
        page.add(NewLanguageQuestion.generateFromResources(getApplication(), 201, R.string.language_others_called, R.string.enter_answer, NewLanguageQuestion.QuestionType.EDIT_TEXT, not_required, 200 ));
        page.add(NewLanguageQuestion.generateFromResources(getApplication(), 202, R.string.language_others_who, R.string.enter_answer, NewLanguageQuestion.QuestionType.EDIT_TEXT, not_required, 200 ));
        page.add(NewLanguageQuestion.generateFromResources(getApplication(), 203, R.string.language_others_meaning, R.string.enter_answer, NewLanguageQuestion.QuestionType.EDIT_TEXT, not_required, 200 ));
        page = pushPage(page);

        page.add(NewLanguageQuestion.generateFromResources(getApplication(), 300, R.string.language_where_else_spoken, R.string.enter_answer, NewLanguageQuestion.QuestionType.EDIT_TEXT, required ));
        page.add(NewLanguageQuestion.generateFromResources(getApplication(), 400, R.string.language_where_slightly_different, R.string.enter_answer, NewLanguageQuestion.QuestionType.EDIT_TEXT, not_required ));
        page.add(NewLanguageQuestion.generateFromResources(getApplication(), 401, R.string.language_where_slightly_different_gone, R.string.enter_answer, NewLanguageQuestion.QuestionType.EDIT_TEXT, not_required, 400 ));
        page.add(NewLanguageQuestion.generateFromResources(getApplication(), 402, R.string.language_where_slightly_different_come, R.string.enter_answer, NewLanguageQuestion.QuestionType.EDIT_TEXT, not_required, 400 ));
        page.add(NewLanguageQuestion.generateFromResources(getApplication(), 403, R.string.language_where_slightly_different_name, R.string.enter_answer, NewLanguageQuestion.QuestionType.EDIT_TEXT, not_required, 400 ));
        page = pushPage(page);

        page.add(NewLanguageQuestion.generateFromResources(getApplication(), 500, R.string.language_where_different, R.string.enter_answer, NewLanguageQuestion.QuestionType.EDIT_TEXT, required ));
        page.add(NewLanguageQuestion.generateFromResources(getApplication(), 501, R.string.language_where_different_understand, R.string.enter_answer, NewLanguageQuestion.QuestionType.EDIT_TEXT, not_required, 500 ));
        page.add(NewLanguageQuestion.generateFromResources(getApplication(), 502, R.string.language_where_different_gone, R.string.enter_answer, NewLanguageQuestion.QuestionType.EDIT_TEXT, not_required, 500 ));
        page.add(NewLanguageQuestion.generateFromResources(getApplication(), 503, R.string.language_where_different_come, R.string.enter_answer, NewLanguageQuestion.QuestionType.EDIT_TEXT, not_required, 500 ));
        page.add(NewLanguageQuestion.generateFromResources(getApplication(), 504, R.string.language_where_different_name, R.string.enter_answer, NewLanguageQuestion.QuestionType.EDIT_TEXT, not_required, 500 ));
        page = pushPage(page);

        page.add(NewLanguageQuestion.generateFromResources(getApplication(), 600, R.string.language_where_most_pure, R.string.enter_answer, NewLanguageQuestion.QuestionType.EDIT_TEXT, not_required ));
        page.add(NewLanguageQuestion.generateFromResources(getApplication(), 601, R.string.language_where_most_pure_why, R.string.enter_answer, NewLanguageQuestion.QuestionType.EDIT_TEXT, not_required, 600 ));
        page.add(NewLanguageQuestion.generateFromResources(getApplication(), 602, R.string.language_where_most_pure_gone, R.string.enter_answer, NewLanguageQuestion.QuestionType.EDIT_TEXT, not_required, 600 ));
        page.add(NewLanguageQuestion.generateFromResources(getApplication(), 603, R.string.language_where_most_pure_come, R.string.enter_answer, NewLanguageQuestion.QuestionType.EDIT_TEXT, not_required, 600 ));
        page = pushPage(page);

        page.add(NewLanguageQuestion.generateFromResources(getApplication(), 700, R.string.language_where_spoken_badly, R.string.enter_answer, NewLanguageQuestion.QuestionType.EDIT_TEXT, not_required ));
        page.add(NewLanguageQuestion.generateFromResources(getApplication(), 701, R.string.language_where_spoken_badly_why, R.string.enter_answer, NewLanguageQuestion.QuestionType.EDIT_TEXT, not_required, 700 ));
        page.add(NewLanguageQuestion.generateFromResources(getApplication(), 702, R.string.language_where_spoken_badly_gone, R.string.enter_answer, NewLanguageQuestion.QuestionType.EDIT_TEXT, not_required, 700 ));
        page.add(NewLanguageQuestion.generateFromResources(getApplication(), 703, R.string.language_where_spoken_badly_come, R.string.enter_answer, NewLanguageQuestion.QuestionType.EDIT_TEXT, not_required, 700 ));
        page = pushPage(page);

        page.add(NewLanguageQuestion.generateFromResources(getApplication(), 800, R.string.language_gateway_name, R.string.enter_answer, NewLanguageQuestion.QuestionType.EDIT_TEXT, not_required ));
        page.add(NewLanguageQuestion.generateFromResources(getApplication(), 801, R.string.language_gateway_understand, R.string.enter_answer, NewLanguageQuestion.QuestionType.EDIT_TEXT, not_required, 800 ));
        page.add(NewLanguageQuestion.generateFromResources(getApplication(), 802, R.string.language_gateway_understand_come, R.string.enter_answer, NewLanguageQuestion.QuestionType.EDIT_TEXT, not_required, 800 ));
        page.add(NewLanguageQuestion.generateFromResources(getApplication(), 803, R.string.language_gateway_understand_children_come, R.string.enter_answer, NewLanguageQuestion.QuestionType.EDIT_TEXT, not_required, 800 ));
        page = pushPage(page);

        page.add(NewLanguageQuestion.generateFromResources(getApplication(), 900, R.string.language_where_travel, R.string.enter_answer, NewLanguageQuestion.QuestionType.EDIT_TEXT, required ));
        page.add(NewLanguageQuestion.generateFromResources(getApplication(), 1000, R.string.language_tourists, R.string.enter_answer, NewLanguageQuestion.QuestionType.EDIT_TEXT, required ));
        pushPage(page);
    }

    private boolean parseAnswers(String answersJson, int page) {
        try {
            List<NewLanguageQuestion> mPage = mQuestionPages.get(page);
            JSONArray answersPage = new JSONArray(answersJson);

            int length = answersPage.length();
            for(int i = 0; i < length; i++ ) {
                JSONObject question = (JSONObject) answersPage.get(i);
                NewLanguageQuestion originalQuestion = mPage.get(i);
                int questionID = question.getInt(NewLanguageQuestion.ID);
                if((null == question) || (originalQuestion.id != questionID)) {
                    return false; // response does not match original
                } else {
                    if(question.has(NewLanguageQuestion.INPUT)) {
                        originalQuestion.answer = question.getString(NewLanguageQuestion.INPUT);
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

    public static List<NewLanguageQuestion> parseJsonStrIntoQuestions(String answersJson) {
        List<NewLanguageQuestion> page = null;
        try {
            JSONArray answers = new JSONArray(answersJson);
            page = parseJsonIntoQuestions( answers);

        } catch (Exception e) {
            Logger.w(TAG, "could not parse questions", e);
            page = null;
        }

        return page;
    }

    public static List<NewLanguageQuestion> parseJsonIntoQuestions(JSONArray answers) {
        List<NewLanguageQuestion> page = new ArrayList<>();
        try {
            int length = answers.length();
            for(int i = 0; i < length; i++ ) {
                JSONObject questionStr = (JSONObject) answers.get(i);
                NewLanguageQuestion question = NewLanguageQuestion.generateFromJson(questionStr);
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

    private JSONArray getQuestions(int page) {
        JSONArray answers = new JSONArray();
        try {
            List<NewLanguageQuestion> mPage = mQuestionPages.get(page);

            return getQuestions(mPage);

        } catch (Exception e) {
            Logger.w(TAG, "could not generate questions", e);
            answers = null;
        }

        return answers;
    }

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
    public void onBackPressed() {
        // TRICKY: the translation activity is finished after opening the publish activity
        // because we may have to go back and forth and don't want to fill up the stack
        if(mCallingActivity == ACTIVITY_TRANSLATION) {
            Intent intent = new Intent(this, TargetTranslationActivity.class);
            Bundle args = new Bundle();
            args.putString(AppContext.EXTRA_TARGET_TRANSLATION_ID, mTargetTranslation.getId());
            intent.putExtras(args);
            startActivity(intent);
        }
        finish();
    }


    @Override
    public void nextStep(String answersJson) {
        doPage(mCurrentPage + 1, answersJson);
    }

    @Override
    public void previousStep(String answersJson) {
        doPage(mCurrentPage - 1, answersJson);
    }

    @Override
    public void finishLanguageRequest(String answersJson) {

        try {
            if(answersJson != null) {
                parseAnswers(answersJson, mCurrentPage); // save answers from current page
            }
            mLanguageFinished = true;

            JSONArray questionsJson = new JSONArray();

            for (int i = 0; i < mQuestionPages.size(); i++) {
                List<NewLanguageQuestion> questions = mQuestionPages.get(i);
                for (NewLanguageQuestion question : questions) {
                    JSONObject answer = new JSONObject();
                    answer.put(NEW_LANGUAGE_QUESTION_ID,question.id);
                    answer.put(NEW_LANGUAGE_ANSWER,question.answer);
                    questionsJson.put(answer);
                }
            }

            JSONObject newLanguageData = new JSONObject();
            newLanguageData.put(NEW_LANGUAGE_REQUEST_ID, UUID.randomUUID().toString());
            newLanguageData.put(NEW_LANGUAGE_TEMP_CODE, getNewLanguageCode());
            newLanguageData.put(NEW_LANGUAGE_QUESTIONAIRE_ID, mQuestionaireID);
            newLanguageData.put(NEW_LANGUAGE_ANSWERS, questionsJson);

            String newLanguageDataStr = newLanguageData.toString(2);

            Intent startNewLanguage = new Intent(this, NewTargetTranslationActivity.class);
            startNewLanguage.putExtra(NewTargetTranslationActivity.EXTRA_NEW_LANGUAGE_DATA, newLanguageDataStr);
            startActivity(startNewLanguage);
            finish();

        } catch(Exception e) {
            Logger.e(TAG,"Error getting question data",e);
        }
    }

    private String getNewLanguageCode() {
        String languageCode;
        languageCode= "qaa-x-";

        String androidId = AppContext.udid();

        long ms = (new Date()).getTime();
        String uniqueString = androidId + ms;
        String sha1Value = getSha1Hex(uniqueString);
        languageCode += sha1Value.substring(0, 6);
        return languageCode.toLowerCase();
    }

    public static String getSha1Hex(String clearString)
    {
        try
        {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            messageDigest.update(clearString.getBytes("UTF-8"));
            byte[] bytes = messageDigest.digest();
            StringBuilder buffer = new StringBuilder();
            for (byte b : bytes)
            {
                buffer.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }
            return buffer.toString();
        }
        catch (Exception ignored)
        {
            ignored.printStackTrace();
            return null;
        }
    }

    /**
     * Moves to the a stage in the publish process
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

    private class ViewHolder {
        private final LinearLayout mButtonLayout;
        private final ImageView mActiveView;
        private final ImageView mDoneView;
        private final TextView mStepView;
        private final TextView mTitleView;
        private final ImageView mCircleView;
        private boolean mVisited = false;
        private boolean mDone;

        public ViewHolder(LinearLayout buttonLayout, ImageView activeView, ImageView doneView, TextView stepView, TextView titleView, ImageView circleView) {
            mButtonLayout = buttonLayout;
            mActiveView = activeView;
            mDoneView = doneView;
            mStepView = stepView;
            mTitleView = titleView;
            mCircleView = circleView;
        }

        public void onSaveInstanceState(Bundle out) {
            out.putInt(STATE_LANGUAGE_STEP, mCurrentPage);
            out.putBoolean(STATE_NEW_LANGUAGE_FINISHED, mLanguageFinished);
            out.putString(STATE_NEW_LANGUAGE_ANSWERS, getQuestionPages().toString());
        }
    }
}

