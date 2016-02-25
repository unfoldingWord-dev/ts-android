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
public class LanguageGatewayFragment extends RequestNewLanguageStepFragment {

    private int mCurrentTranslator = 0;

    private EditText mWhereSpokenBadlyText;
    private EditText mWhereSpokenBadlyWhyText;
    private EditText mWhereSpokenBadlyComeText;
    private EditText mWhereSpokenBadlyGoneText;
    private String mWhereSpokenBadly;
    private String mWhereSpokenBadlyWhy;
    private String mWhereSpokenBadlyGone;
    private String mWhereSpokenBadlyCome;


    private EditText mGatewayNameText;
    private EditText mGatewayUnderstandComeText;
    private EditText mGatewayUnderstandText;
    private EditText mGatewayUnderstandComeChildrenText;
    private String mGatewayName;
    private String mGatewayUnderstandComeChildren;
    private String mGatewayUnderstandCome;
    private String mGatewayUnderstand;

    private EditText mTravelText;
    private String mTravel;

    private EditText mTouristsText;
    private String mTourists;

    private View mContributor;
    private TextView mContributorToggle;
    private JSONObject mAnswers;
    private View mRootView;



    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_new_language_gateway, container, false);

        Bundle args = getArguments();
        mAnswers = getAnswersFromArgs(args);

        mWhereSpokenBadlyText = (EditText) mRootView.findViewById(R.id.where_spoken_badly_edittext);
        initEdit(mWhereSpokenBadlyText, mAnswers, RequestNewLanguage.TAG_WHERE_SPOKEN_BADLY);
        mWhereSpokenBadlyText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setBadlyState(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mWhereSpokenBadlyWhyText = (EditText) mRootView.findViewById(R.id.where_spoken_badly_why_edittext);
        initEdit(mWhereSpokenBadlyWhyText, mAnswers, RequestNewLanguage.TAG_WHERE_SPOKEN_BADLY_WHY);
        mWhereSpokenBadlyGoneText = (EditText) mRootView.findViewById(R.id.where_spoken_badly_gone_edittext);
        initEdit(mWhereSpokenBadlyGoneText, mAnswers, RequestNewLanguage.TAG_WHERE_SPOKEN_BADLY_GONE);
        mWhereSpokenBadlyComeText = (EditText) mRootView.findViewById(R.id.where_spoken_badly_come_edittext);
        initEdit(mWhereSpokenBadlyComeText, mAnswers, RequestNewLanguage.TAG_WHERE_SPOKEN_BADLY_COME);


        mGatewayNameText = (EditText) mRootView.findViewById(R.id.gateway_name_edittext);
        initEdit(mGatewayNameText, mAnswers, RequestNewLanguage.TAG_GATEWAY_NAME);
        mGatewayNameText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setGatewayState(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mGatewayUnderstandText = (EditText) mRootView.findViewById(R.id.gateway_understand_edittext);
        initEdit(mGatewayUnderstandText, mAnswers, RequestNewLanguage.TAG_GATEWAY_UNDERSTAND);
        mGatewayUnderstandComeText = (EditText) mRootView.findViewById(R.id.gateway_understand_come_edittext);
        initEdit(mGatewayUnderstandComeText, mAnswers, RequestNewLanguage.TAG_GATEWAY_UNDERSTAND_COME);
        mGatewayUnderstandComeChildrenText = (EditText) mRootView.findViewById(R.id.gateway_understand_children_come_edittext);
        initEdit(mGatewayUnderstandComeChildrenText, mAnswers, RequestNewLanguage.TAG_GATEWAY_UNDERSTAND_COME_CHILDREN);

        mTravelText = (EditText) mRootView.findViewById(R.id.where_travel_edittext);
        initEdit(mTravelText, mAnswers, RequestNewLanguage.TAG_WHERE_TRAVEL);
        mTouristsText = (EditText) mRootView.findViewById(R.id.tourists_edittext);
        initEdit(mTouristsText, mAnswers, RequestNewLanguage.TAG_TOURISTS_COME);

        Button doneButton = (Button) mRootView.findViewById(R.id.done_button);
        doneButton.setOnClickListener(new View.OnClickListener() {
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

        setBadlyState();
        setGatewayState();
        return mRootView;
    }

    private void setBadlyState() {
        Editable text = mWhereSpokenBadlyText.getText();
        setBadlyState(text);
    }

    private void setBadlyState(CharSequence text) {
        Boolean enabled = (null != text) && !text.toString().isEmpty();
        setVisible(R.id.where_spoken_badly_why_card, enabled);
        setVisible(R.id.where_spoken_badly_come_card, enabled);
        setVisible(R.id.where_spoken_badly_gone_card, enabled);
    }

    private void setGatewayState() {
        Editable text = mGatewayNameText.getText();
        setGatewayState(text);
    }

    private void setGatewayState(CharSequence text) {
        Boolean enabled = (null != text) && !text.toString().isEmpty();
        setVisible(R.id.gateway_understand_card, enabled);
        setVisible(R.id.gateway_understand_come_card, enabled);
        setVisible(R.id.gateway_understand_children_come_card, enabled);
    }

    private void setVisible(int resID, Boolean enable) {
        View view = (View) mRootView.findViewById(resID);
        if (null != view) {
            view.setVisibility(enable ? View.VISIBLE : View.GONE);
        }
    }

    private void validateAnswers() {

        saveAnswers();

        boolean spokenBadlyAnswersMissing = !mWhereSpokenBadly.isEmpty()
                && (mWhereSpokenBadlyGone.isEmpty() || mWhereSpokenBadlyCome.isEmpty()
                || mWhereSpokenBadlyWhy.isEmpty());
        boolean gatewayAnswersMissing = !mGatewayName.isEmpty()
                && (mGatewayUnderstandComeChildren.isEmpty() || mGatewayUnderstandCome.isEmpty()
                || mGatewayUnderstand.isEmpty());

        if(mWhereSpokenBadly.isEmpty()) {
            showAnswerRequiredBlocked(R.string.language_where_spoken_badly);

        } else if(mGatewayName.isEmpty()) {
            showAnswerRequiredBlocked(R.string.language_gateway_name);

        } else if(mTravel.isEmpty()) {
            showAnswerRequiredBlocked(R.string.language_where_travel);

        } else if(mTourists.isEmpty()) {
            showAnswerRequiredBlocked(R.string.language_tourists);

        } else if(spokenBadlyAnswersMissing || gatewayAnswersMissing) {

            warnAnswersMissingBeforeContinue(R.string.answers_missing_continue, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doFinished();
                }
            });

        } else
        {
            doFinished();
        }
    }

    private void doFinished() {
        getListener().finishLanguageRequest(mAnswers.toString());
    }

    private void saveAnswers() {
        mWhereSpokenBadly = mWhereSpokenBadlyText.getText().toString();
        mWhereSpokenBadlyWhy = mWhereSpokenBadlyWhyText.getText().toString();
        mWhereSpokenBadlyGone = mWhereSpokenBadlyGoneText.getText().toString();
        mWhereSpokenBadlyCome = mWhereSpokenBadlyComeText.getText().toString();

        mGatewayName = mGatewayNameText.getText().toString();
        mGatewayUnderstandComeChildren = mGatewayUnderstandComeChildrenText.getText().toString();
        mGatewayUnderstandCome = mGatewayUnderstandComeText.getText().toString();
        mGatewayUnderstand = mGatewayUnderstandText.getText().toString();

        mTravel = mTravelText.getText().toString();
        mTourists = mTouristsText.getText().toString();

        try {
            mAnswers.put(RequestNewLanguage.TAG_WHERE_SPOKEN_BADLY, mWhereSpokenBadly);
            mAnswers.put(RequestNewLanguage.TAG_WHERE_SPOKEN_BADLY_WHY, mWhereSpokenBadlyWhy);
            mAnswers.put(RequestNewLanguage.TAG_WHERE_SPOKEN_BADLY_GONE, mWhereSpokenBadlyGone);
            mAnswers.put(RequestNewLanguage.TAG_WHERE_SPOKEN_BADLY_COME, mWhereSpokenBadlyCome);

            mAnswers.put(RequestNewLanguage.TAG_GATEWAY_NAME, mGatewayName);
            mAnswers.put(RequestNewLanguage.TAG_GATEWAY_UNDERSTAND_COME_CHILDREN, mGatewayUnderstandComeChildren);
            mAnswers.put(RequestNewLanguage.TAG_GATEWAY_UNDERSTAND_COME, mGatewayUnderstandCome);
            mAnswers.put(RequestNewLanguage.TAG_GATEWAY_UNDERSTAND, mGatewayUnderstand);

            mAnswers.put(RequestNewLanguage.TAG_WHERE_TRAVEL, mTravel);
            mAnswers.put(RequestNewLanguage.TAG_TOURISTS_COME, mTourists);
        } catch (Exception e) {
            Logger.w(TAG, "could not save answers", e);
        }
    }
}
