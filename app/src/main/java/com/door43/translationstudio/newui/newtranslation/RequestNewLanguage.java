package com.door43.translationstudio.newui.newtranslation;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.newui.BaseActivity;
import com.door43.translationstudio.newui.BaseFragment;
import com.door43.translationstudio.newui.translate.TargetTranslationActivity;
import com.door43.translationstudio.AppContext;

import org.json.JSONObject;

import java.security.InvalidParameterException;

/**
 * Created by blm on 2/23/16.
 */
public class RequestNewLanguage extends BaseActivity implements RequestNewLanguageStepFragment.OnEventListener {

    public static final String TAG = RequestNewLanguage.class.getSimpleName();
    public static final int STEP_NAME = 0;
    public static final int STEP_UNDERSTANDING = 1;
    public static final int STEP_DIALECTS = 2;
    public static final int STEP_GATEWAY = 3;
    public static final int STEP_LAST = STEP_GATEWAY;
    public static final int STEP_COUNT = STEP_LAST+1;
    private static final String STATE_LANGUAGE_STEP = "state_language_step";
    private static final String STATE_NEW_LANGUAGE_FINISHED = "state_new_language_finished";
    private static final String STATE_NEW_LANGUAGE_ANSWERS = "state_new_language_answers";
    public static final String EXTRA_CALLING_ACTIVITY = "extra_calling_activity";
    public static final String EXTRA_NEW_LANGUAGE_ANSWERS = "extra_new_language_answers";
    public static final String TAG_NAME_CALLED = "name_called";
    private Translator mTranslator;
    private TargetTranslation mTargetTranslation;
    private int mCurrentStep = 0;
    public static final int ACTIVITY_HOME = 1001;
    public static final int ACTIVITY_TRANSLATION = 1002;
    private boolean mLanguageFinished = false;
    private int mCallingActivity;
    private BaseFragment mFragment;
    private JSONObject mAnswers;


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
            mCurrentStep = savedInstanceState.getInt(STATE_LANGUAGE_STEP, 0);
            mLanguageFinished = savedInstanceState.getBoolean(STATE_NEW_LANGUAGE_FINISHED, false);
            String answers = savedInstanceState.getString(STATE_NEW_LANGUAGE_ANSWERS);
            parseAnswers(answers);
        } else {
            mAnswers = new JSONObject();
        }

        // inject fragments
        if(findViewById(R.id.fragment_container) != null) {
            if(savedInstanceState != null) {
                mFragment = (BaseFragment)getFragmentManager().findFragmentById(R.id.fragment_container);
            } else {
                mFragment = new BaseFragment();
//                    mFragment.setArguments(args);
                getFragmentManager().beginTransaction().add(R.id.fragment_container, mFragment).commit();
            }
        }
     }

    private void parseAnswers(String answers) {
        try {
            mAnswers = new JSONObject(answers);
        } catch (Exception e) {
            Logger.w(TAG, "could not parse answers", e);
            mAnswers = new JSONObject();
        }
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
        doStep(mCurrentStep + 1, answersJson);
    }

    @Override
    public void previousStep(String answersJson) {
        doStep(mCurrentStep - 1, answersJson);
    }

    @Override
    public void finishLanguageRequest() {
        mLanguageFinished = true;
    }

    /**
     * Moves to the a stage in the publish process
     * @param step
     */
    private void doStep(int step, String answersJson) {
        if(step > STEP_LAST) {
            mCurrentStep = STEP_LAST;
        } else if(step < 0) {
            mCurrentStep = 0;
        } else{
            mCurrentStep = step;
        }

        parseAnswers(answersJson);

        switch(mCurrentStep) {
            case STEP_UNDERSTANDING:
//                    mFragment = new ProfileFragment();
                break;
            case STEP_DIALECTS:
//                    mFragment = new ReviewFragment();
                break;
            case STEP_GATEWAY:
//                    mFragment = new PublishFragment();
                break;
            case STEP_NAME:
            default:
                mFragment = new NameFragment();
                break;
        }

        Bundle args = getIntent().getExtras();
        args.putBoolean(RequestNewLanguageStepFragment.ARG_NEW_LANG_FINISHED, mLanguageFinished);
        mFragment.setArguments(args);
        getFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();
        // TODO: animate
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
            out.putInt(STATE_LANGUAGE_STEP, mCurrentStep);
            out.putBoolean(STATE_NEW_LANGUAGE_FINISHED, mLanguageFinished);
            out.putString(STATE_NEW_LANGUAGE_ANSWERS, mAnswers.toString());
        }
    }
}

