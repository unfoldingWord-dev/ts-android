package com.door43.translationstudio.newui.newtranslation;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;

import org.json.JSONObject;

/**
 * Created by blm on 2/23/16.
 * fragment used in RequestNewLanguage
 */
public class LanguageUnderstandingFragment  extends RequestNewLanguageStepFragment {

    private int mCurrentTranslator = 0;
    private EditText mWhereElseSpokenText;
    private EditText mWhereSlightlyDifferentText;
    private EditText mWhereSLightlyDifferentGoneCheck;
    private EditText mWhereSLightlyDifferentComeCheck;
    private EditText mWhereSlightlyDifferentNameText;
    private View mContributor;
    private TextView mContributorToggle;
    private JSONObject mAnswers;
    private View mRootView;
    private String mWhereElseSpoken = mWhereElseSpokenText.getText().toString();
    private String mWhereSlightlyDifferent = mWhereSlightlyDifferentText.getText().toString();
    private String mWhereSlightlyDifferentName = mWhereSlightlyDifferentNameText.getText().toString();
    private String mWhereSlightlyDifferentGone = mWhereSLightlyDifferentGoneCheck.getText().toString();
    private String mWhereSlightlyDifferentCome = mWhereSLightlyDifferentComeCheck.getText().toString();


    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_new_language_understanding, container, false);

        Bundle args = getArguments();
        mAnswers = getAnswersFromArgs(args);

        mWhereElseSpokenText = (EditText) mRootView.findViewById(R.id.where_else_spoken_edittext);
        initEdit(mWhereElseSpokenText, mAnswers, RequestNewLanguage.TAG_WHERE_ELSE_SPOKEN);
        mWhereSlightlyDifferentText = (EditText) mRootView.findViewById(R.id.where_slightly_different_edittext);
        initEdit(mWhereSlightlyDifferentText, mAnswers, RequestNewLanguage.TAG_WHERE_SLIGHTLY_DIFFERENT);
        mWhereSlightlyDifferentText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setSlightlyDifferentState(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mWhereSlightlyDifferentNameText = (EditText) mRootView.findViewById(R.id.where_slightly_different_name_edittext);
        initEdit(mWhereSlightlyDifferentNameText, mAnswers, RequestNewLanguage.TAG_WHERE_SLIGHTLY_DIFFERENT_NAME);

        mWhereSLightlyDifferentGoneCheck = (EditText) mRootView.findViewById(R.id.where_slightly_different_gone_edittext);
        initEdit(mWhereSLightlyDifferentGoneCheck, mAnswers, RequestNewLanguage.TAG_WHERE_SLIGHTLY_DIFFERENT_GONE);

        mWhereSLightlyDifferentComeCheck = (EditText) mRootView.findViewById(R.id.where_slightly_different_come_edittext);
        initEdit(mWhereSLightlyDifferentComeCheck, mAnswers, RequestNewLanguage.TAG_WHERE_SLIGHTLY_DIFFERENT_COME);

        Button nextButton = (Button) mRootView.findViewById(R.id.next_button);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateAnswers();
            }
        });

        Button previousButton = (Button) mRootView.findViewById(R.id.previous_button);
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAnswers();
                getListener().previousStep(mAnswers.toString());
            }
        });

        setSlightlyDifferentState();
        return mRootView;
    }

    private void setSlightlyDifferentState() {
        Editable text = mWhereSlightlyDifferentText.getText();
        setSlightlyDifferentState(text);
    }

    private void setSlightlyDifferentState(CharSequence text) {
        Boolean enabled = (null != text) && !text.toString().isEmpty();
        setVisible(R.id.where_slightly_different_gone_card, enabled);
        setVisible(R.id.where_slightly_different_come_card, enabled);
        setVisible(R.id.where_slightly_different_name_card, enabled);
    }

    private void setVisible(int resID, Boolean enable) {
        View view = (View) mRootView.findViewById(resID);
        if (null != view) {
            view.setVisibility(enable ? View.VISIBLE : View.GONE);
        }
    }

    private void validateAnswers() {

        saveAnswers();

        if(mWhereElseSpoken.isEmpty()) {
            showAnswerRequiredBlocked(R.string.language_where_else_spoken);

        } else if( !mWhereSlightlyDifferent.isEmpty()
                && (mWhereSlightlyDifferentName.isEmpty() || mWhereSlightlyDifferentGone.isEmpty() || mWhereSlightlyDifferentGone.isEmpty()  )) {

            warnBeforeCOntinue(R.string.answers_missing_continue, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getListener().nextStep(mAnswers.toString());
                }
            });

        } else
        {
            getListener().nextStep(mAnswers.toString());
        }
    }

    private void saveAnswers() {
        mWhereElseSpoken = mWhereElseSpokenText.getText().toString();
        mWhereSlightlyDifferent = mWhereSlightlyDifferentText.getText().toString();
        mWhereSlightlyDifferentName = mWhereSlightlyDifferentNameText.getText().toString();
        mWhereSlightlyDifferentGone = mWhereSLightlyDifferentGoneCheck.getText().toString();
        mWhereSlightlyDifferentCome = mWhereSLightlyDifferentComeCheck.getText().toString();

        try {
            mAnswers.put(RequestNewLanguage.TAG_WHERE_ELSE_SPOKEN, mWhereElseSpoken);
            mAnswers.put(RequestNewLanguage.TAG_WHERE_SLIGHTLY_DIFFERENT, mWhereSlightlyDifferent);
            mAnswers.put(RequestNewLanguage.TAG_WHERE_SLIGHTLY_DIFFERENT_NAME, mWhereSlightlyDifferentName);
            mAnswers.put(RequestNewLanguage.TAG_WHERE_SLIGHTLY_DIFFERENT_GONE, mWhereSlightlyDifferentGone);
            mAnswers.put(RequestNewLanguage.TAG_WHERE_SLIGHTLY_DIFFERENT_COME, mWhereSlightlyDifferentCome);

        } catch (Exception e) {
            Logger.w(TAG, "could not save answers", e);
        }
    }
}
