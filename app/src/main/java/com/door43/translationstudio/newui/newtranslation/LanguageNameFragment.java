package com.door43.translationstudio.newui.newtranslation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;

import org.json.JSONObject;

/**
 * Created by blm on 2/23/16.
 * fragment used in RequestNewLanguage
 */
public class LanguageNameFragment extends RequestNewLanguageStepFragment {

    private int mCurrentTranslator = 0;
    private EditText mCalledText;
    private EditText mMeaningText;
    private EditText mAlternatesText;
    private CheckBox mOthersHaveNameCheck;
    private EditText mOthersCalledText;
    private EditText mOthersWhoText;
    private EditText mOthersMeaningText;
    private View mContributor;
    private TextView mContributorToggle;
    private JSONObject mAnswers;
    private View mRootView;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_new_language_name, container, false);

        Bundle args = getArguments();
        mAnswers = getAnswersFromArgs(args);

        mCalledText = (EditText) mRootView.findViewById(R.id.name_called_edittext);
        initEdit(mCalledText, mAnswers, RequestNewLanguage.TAG_NAME_CALLED);
        mMeaningText = (EditText) mRootView.findViewById(R.id.name_meaning_edittext);
        initEdit(mMeaningText, mAnswers, RequestNewLanguage.TAG_NAME_MEANING);
        mAlternatesText = (EditText) mRootView.findViewById(R.id.name_alternates_edittext);
        initEdit(mAlternatesText, mAnswers, RequestNewLanguage.TAG_NAME_ALTERNATES);

        mOthersHaveNameCheck = (CheckBox) mRootView.findViewById(R.id.name_others_checkBox);
        Boolean othersHaveNameChecked = initCheckbox(mOthersHaveNameCheck, mAnswers, RequestNewLanguage.TAG_NAME_OTHERS);
        mOthersHaveNameCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Boolean othersName = mOthersHaveNameCheck.isChecked();
                setOtherNameState(othersName);
            }
        });

        mOthersCalledText = (EditText) mRootView.findViewById(R.id.name_others_called_edittext);
        initEdit(mOthersCalledText, mAnswers, RequestNewLanguage.TAG_NAME_OTHERS_CALLED);
        mOthersWhoText = (EditText) mRootView.findViewById(R.id.name_others_who_edittext);
        initEdit(mOthersWhoText, mAnswers, RequestNewLanguage.TAG_NAME_OTHERS_WHO);
        mOthersMeaningText = (EditText) mRootView.findViewById(R.id.name_others_meaning_edittext);
        initEdit(mOthersMeaningText, mAnswers, RequestNewLanguage.TAG_NAME_OTHERS_MEANING);

        Button nextButton = (Button) mRootView.findViewById(R.id.next_button);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateAnswers();
            }
        });

        setOtherNameState(othersHaveNameChecked);
        return mRootView;
    }

    private void setOtherNameState(Boolean haveNameByOthers) {
        setVisible(R.id.name_others_called_card, haveNameByOthers);
        setVisible(R.id.name_others_who_card, haveNameByOthers);
        setVisible(R.id.name_others_meaning_card, haveNameByOthers);
    }

    private void setVisible(int resID, Boolean enable) {
        View view = (View) mRootView.findViewById(resID);
        if(null != view) {
            view.setVisibility(enable ? View.VISIBLE : View.GONE);
        }
    }

    private void validateAnswers() {

        String called = mCalledText.getText().toString();
        String meaning = mMeaningText.getText().toString();
        String alternates = mAlternatesText.getText().toString();
        Boolean othersName = mOthersHaveNameCheck.isChecked();
        String othersCalled = mOthersCalledText.getText().toString();
        String othersWho = mOthersWhoText.getText().toString();
        String othersMeaning = mOthersMeaningText.getText().toString();

        try {
            mAnswers.put(RequestNewLanguage.TAG_NAME_CALLED, called);
            mAnswers.put(RequestNewLanguage.TAG_NAME_MEANING, meaning);
            mAnswers.put(RequestNewLanguage.TAG_NAME_ALTERNATES, alternates);
            mAnswers.put(RequestNewLanguage.TAG_NAME_OTHERS, othersName.booleanValue());
            mAnswers.put(RequestNewLanguage.TAG_NAME_OTHERS_CALLED, othersCalled);
            mAnswers.put(RequestNewLanguage.TAG_NAME_OTHERS_WHO, othersWho);
            mAnswers.put(RequestNewLanguage.TAG_NAME_OTHERS_MEANING, othersMeaning);

        } catch (Exception e) {
            Logger.w(TAG, "could not save answers", e);
        }

        if(called.isEmpty()) {
            showAnswerRequiredBlocked(R.string.language_name_called);

        } else if( meaning.isEmpty() || alternates.isEmpty() ) {

            warnAnswersMissingBeforeContinue(R.string.answers_missing_continue, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    gotoNextStep();
                }
            });

        } else if (othersName && othersCalled.isEmpty()) {
            showAnswerRequiredBlocked(R.string.language_others_called);
        }
        else
        {
            gotoNextStep();
        }
    }

    private void gotoNextStep() {
        getListener().nextStep(mAnswers.toString());
    }
}

