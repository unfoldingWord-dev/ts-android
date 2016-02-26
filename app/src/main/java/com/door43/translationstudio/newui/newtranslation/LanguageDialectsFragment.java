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
 * fragment used in RequestNewLanguageActivity
 */
public class LanguageDialectsFragment extends RequestNewLanguageStepFragment {

    private int mCurrentTranslator = 0;

    private EditText mWhereDifferentText;
    private EditText mWhereDifferentUnderstandText;
    private EditText mWhereDifferentComeText;
    private EditText mWhereDifferentNameText;
    private EditText mWhereDifferentGoneText;
    private String mWhereDifferent;
    private String mWhereDifferentUnderstand;
    private String mWhereDifferentGone;
    private String mWhereDifferentCome;
    private String mWhereDifferentName;

    private EditText mWherePureText;
    private EditText mWherePureComeText;
    private EditText mWherePureWhyText;
    private EditText mWherePureGoneText;
    private String mWherePure;
    private String mWherePureGone;
    private String mWherePureCome;
    private String mWherePureWhy;

    private View mContributor;
    private TextView mContributorToggle;
    private JSONObject mAnswers;
    private View mRootView;



    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_new_language_dialects, container, false);

        Bundle args = getArguments();
        mAnswers = getAnswersFromArgs(args);

        mWhereDifferentText = (EditText) mRootView.findViewById(R.id.where_different_edittext);
        initEdit(mWhereDifferentText, mAnswers, RequestNewLanguageActivity.TAG_WHERE_DIFFERENT);
        mWhereDifferentText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setDifferentState(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mWhereDifferentUnderstandText = (EditText) mRootView.findViewById(R.id.where_different_understand_edittext);
        initEdit(mWhereDifferentUnderstandText, mAnswers, RequestNewLanguageActivity.TAG_WHERE_DIFFERENT_UNDERSTAND);
        mWhereDifferentGoneText = (EditText) mRootView.findViewById(R.id.where_different_gone_edittext);
        initEdit(mWhereDifferentGoneText, mAnswers, RequestNewLanguageActivity.TAG_WHERE_DIFFERENT_GONE);
        mWhereDifferentComeText = (EditText) mRootView.findViewById(R.id.where_different_come_edittext);
        initEdit(mWhereDifferentComeText, mAnswers, RequestNewLanguageActivity.TAG_WHERE_DIFFERENT_COME);
        mWhereDifferentNameText = (EditText) mRootView.findViewById(R.id.where_different_name_edittext);
        initEdit(mWhereDifferentNameText, mAnswers, RequestNewLanguageActivity.TAG_WHERE_DIFFERENT_NAME);


        mWherePureText = (EditText) mRootView.findViewById(R.id.where_most_pure_edittext);
        initEdit(mWherePureText, mAnswers, RequestNewLanguageActivity.TAG_WHERE_PURE);
        mWherePureText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setPureState(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mWherePureWhyText = (EditText) mRootView.findViewById(R.id.where_most_pure_why_edittext);
        initEdit(mWherePureWhyText, mAnswers, RequestNewLanguageActivity.TAG_WHERE_PURE_WHY);
        mWherePureGoneText = (EditText) mRootView.findViewById(R.id.where_most_pure_gone_edittext);
        initEdit(mWherePureGoneText, mAnswers, RequestNewLanguageActivity.TAG_WHERE_PURE_GONE);
        mWherePureComeText = (EditText) mRootView.findViewById(R.id.where_most_pure_gone_edittext);
        initEdit(mWherePureComeText, mAnswers, RequestNewLanguageActivity.TAG_WHERE_PURE_COME);

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

        setDifferentState();
        setPureState();
        return mRootView;
    }

    private void setDifferentState() {
        Editable text = mWhereDifferentText.getText();
        setDifferentState(text);
    }

    private void setDifferentState(CharSequence text) {
        Boolean enabled = (null != text) && !text.toString().isEmpty();
        setVisible(R.id.where_different_understand_card, enabled);
        setVisible(R.id.where_different_gone_card, enabled);
        setVisible(R.id.where_different_come_card, enabled);
        setVisible(R.id.where_different_name_card, enabled);
    }

    private void setPureState() {
        Editable text = mWherePureText.getText();
        setPureState(text);
    }

    private void setPureState(CharSequence text) {
        Boolean enabled = (null != text) && !text.toString().isEmpty();
        setVisible(R.id.where_most_pure_why_card, enabled);
        setVisible(R.id.where_most_pure_gone_card, enabled);
        setVisible(R.id.where_most_pure_come_card, enabled);
    }

    private void setVisible(int resID, Boolean enable) {
        View view = (View) mRootView.findViewById(resID);
        if (null != view) {
            view.setVisibility(enable ? View.VISIBLE : View.GONE);
        }
    }

    private void validateAnswers() {

        saveAnswers();

        boolean differentAnswersMissing = !mWhereDifferent.isEmpty()
                && (mWhereDifferentGone.isEmpty() || mWhereDifferentCome.isEmpty()
                || mWhereDifferentUnderstand.isEmpty() || mWhereDifferentName.isEmpty());
        boolean pureAnswersMissing = !mWherePure.isEmpty()
                && (mWherePureGone.isEmpty() || mWherePureCome.isEmpty()
                || mWherePureWhy.isEmpty());

        if(mWhereDifferent.isEmpty()) {
            showAnswerRequiredBlocked(R.string.language_where_different);

        } else if(mWherePure.isEmpty()) {
            showAnswerRequiredBlocked(R.string.language_where_most_pure);

        } else if(differentAnswersMissing || pureAnswersMissing) {

                warnAnswersMissingBeforeContinue(R.string.answers_missing_continue, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        gotoNextStep();
                    }
                });

        } else
        {
            gotoNextStep();
        }
    }

    private void gotoNextStep() {
        getListener().nextStep(mAnswers.toString());
    }

    private void saveAnswers() {
        mWhereDifferent = mWhereDifferentText.getText().toString();
        mWhereDifferentUnderstand = mWhereDifferentUnderstandText.getText().toString();
        mWhereDifferentGone = mWhereDifferentGoneText.getText().toString();
        mWhereDifferentCome = mWhereDifferentComeText.getText().toString();
        mWhereDifferentName = mWhereDifferentNameText.getText().toString();

        mWherePure = mWherePureText.getText().toString();
        mWherePureGone = mWherePureGoneText.getText().toString();
        mWherePureCome = mWherePureComeText.getText().toString();
        mWherePureWhy = mWherePureWhyText.getText().toString();

        try {
            mAnswers.put(RequestNewLanguageActivity.TAG_WHERE_DIFFERENT, mWhereDifferent);
            mAnswers.put(RequestNewLanguageActivity.TAG_WHERE_DIFFERENT_UNDERSTAND, mWhereDifferentUnderstand);
            mAnswers.put(RequestNewLanguageActivity.TAG_WHERE_DIFFERENT_GONE, mWhereDifferentGone);
            mAnswers.put(RequestNewLanguageActivity.TAG_WHERE_DIFFERENT_COME, mWhereDifferentCome);
            mAnswers.put(RequestNewLanguageActivity.TAG_WHERE_DIFFERENT_NAME, mWhereDifferentName);

            mAnswers.put(RequestNewLanguageActivity.TAG_WHERE_PURE, mWherePure);
            mAnswers.put(RequestNewLanguageActivity.TAG_WHERE_PURE_GONE, mWherePureGone);
            mAnswers.put(RequestNewLanguageActivity.TAG_WHERE_PURE_COME, mWherePureCome);
            mAnswers.put(RequestNewLanguageActivity.TAG_WHERE_PURE_WHY, mWherePureWhy);

        } catch (Exception e) {
            Logger.w(TAG, "could not save answers", e);
        }
    }
}
