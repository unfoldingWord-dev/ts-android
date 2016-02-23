package com.door43.translationstudio.newui.newtranslation;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.dialogs.CustomAlertDialog;

import org.json.JSONObject;

/**
 * Created by blm on 2/23/16.
 */
public class NameFragment  extends RequestNewLanguageStepFragment {

    private int mCurrentTranslator = 0;
    private EditText mCalledText;
    private EditText mMeaningText;
    private EditText mAlternatesText;
    private View mContributor;
    private TextView mContributorToggle;
    private JSONObject mAnswers;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_new_language_name, container, false);

        Bundle args = getArguments();
        mAnswers = getAnswersFromArgs(args);

        mCalledText = (EditText)rootView.findViewById(R.id.name_called_edittext);
        mMeaningText = (EditText)rootView.findViewById(R.id.name_meaning_edittext);
        mAlternatesText = (EditText)rootView.findViewById(R.id.name_alternates_edittext);

        Button nextButton = (Button)rootView.findViewById(R.id.next_button);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateAnswers();
            }
        });

        return rootView;
    }

    private void validateAnswers() {

        String called = mCalledText.getText().toString();
        String meaning = mMeaningText.getText().toString();
        String alternates = mAlternatesText.getText().toString();

        try {
            mAnswers.put(RequestNewLanguage.TAG_NAME_CALLED, called);
        } catch (Exception e) {
            Logger.w(TAG, "could not save answers", e);
        }

        if(called.isEmpty()) {
            Resources res = getActivity().getResources();
            String message = String.format(res.getString(R.string.answer_required_for),
                                             res.getString(R.string.language_name_called));
            CustomAlertDialog.Create(getActivity())
                    .setTitle(R.string.invalid_entry_title)
                    .setMessage(message)
                    .setPositiveButton(R.string.label_ok, null)
                    .show(getFragmentManager(), "MissingAnswer");

        } else if( meaning.isEmpty() || alternates.isEmpty() ) {

            CustomAlertDialog.Create(getActivity())
                    .setTitle(R.string.answers_missing_title)
                    .setMessage(R.string.answers_missing_continue)
                    .setPositiveButton(R.string.yes, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            getListener().nextStep(mAnswers.toString());
                        }
                    })
                    .setNegativeButton(R.string.no, null)
                    .show(getFragmentManager(),"MissingAnswers");

        }  else {
            getListener().nextStep(mAnswers.toString());
        }
    }
}

