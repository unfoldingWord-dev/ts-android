package com.door43.translationstudio.newui.newtranslation;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.newui.BaseFragment;

import org.json.JSONObject;

/**
 * Created by blm on 2/23/16.
 */
public abstract class RequestNewLanguageStepFragment extends BaseFragment {
    public static final String ARG_SOURCE_TRANSLATION_ID = "arg_source_translation_id";
    public static final String ARG_NEW_LANG_FINISHED = "arg_publish_finished";
    public static final String TAG = RequestNewLanguageStepFragment.class.getSimpleName();
    private OnEventListener mListener;

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.mListener = (OnEventListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnEventListener");
        }
    }

    protected OnEventListener getListener() {
        return mListener;
    }

    public interface OnEventListener {
        void nextStep(String answersJson);

        void previousStep(String answersJson);

        void finishLanguageRequest(String answersJson);
    }

    protected JSONObject parseAnswers(String answersJson) {
        JSONObject answers;
        try {
            answers = new JSONObject(answersJson);
        } catch (Exception e) {
            Logger.w(TAG, "could not parse answers", e);
            answers = new JSONObject();
        }
        return answers;
    }

    protected void initEdit(EditText edit, JSONObject answers, String key) {
        if(null != edit) {
            try {
                String text = answers.getString(key);
                edit.setText(text);
            } catch (Exception e) {
                edit.setText("");
            }
        }
    }

    protected Boolean initCheckbox(CheckBox checkBox, JSONObject answers, String key) {
        Boolean checked = false;

        try {
            checked = answers.getBoolean(key);
        } catch (Exception e) {
            checked = false;
        }

        if(null != checkBox) {
            checkBox.setChecked(checked);
        }
        return checked;
    }

    protected JSONObject getAnswersFromArgs(Bundle args) {
        String answersJson = args.getString(RequestNewLanguageActivity.EXTRA_NEW_LANGUAGE_ANSWERS);
        JSONObject answers = parseAnswers(answersJson);
        return answers;
    }


    protected void showAnswerRequiredBlocked(int answerID) {
        Resources res = getActivity().getResources();
        String message = String.format(res.getString(R.string.answer_required_for),
                res.getString(answerID));
        CustomAlertDialog.Create(getActivity())
                .setTitle(R.string.invalid_entry_title)
                .setMessage(message)
                .setPositiveButton(R.string.label_ok, null)
                .show(getFragmentManager(), "MissingAnswer");
    }

    protected void warnAnswerMissingBeforeContinue(int answerID, View.OnClickListener listener) {
        Resources res = getActivity().getResources();
        String message = String.format(res.getString(R.string.answer_missing_for),
                res.getString(answerID));
        CustomAlertDialog.Create(getActivity())
                .setTitle(R.string.answers_missing_title)
                .setMessage(message)
                .setPositiveButton(R.string.yes, listener)
                .setNegativeButton(R.string.no, null)
                .show(getFragmentManager(),"MissingAnswers");
    }

    protected void warnAnswersMissingBeforeContinue(int answerID, View.OnClickListener listener) {
        CustomAlertDialog.Create(getActivity())
                .setTitle(R.string.answers_missing_title)
                .setMessage(R.string.answers_missing_continue)
                .setPositiveButton(R.string.yes, listener)
                .setNegativeButton(R.string.no, null)
                .show(getFragmentManager(),"MissingAnswers");
    }
}

