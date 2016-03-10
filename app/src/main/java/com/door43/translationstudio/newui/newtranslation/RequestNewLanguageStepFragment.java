package com.door43.translationstudio.newui.newtranslation;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.NewLanguageQuestion;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.filebrowser.DocumentFileBrowserAdapter;
import com.door43.translationstudio.newui.BaseFragment;

import org.json.JSONObject;

import java.util.List;

/**
 * Created by blm on 2/23/16.
 */
public class RequestNewLanguageStepFragment extends BaseFragment {
    public static final String ARG_SOURCE_TRANSLATION_ID = "arg_source_translation_id";
    public static final String ARG_NEW_LANG_FINISHED = "arg_publish_finished";
    public static final String TAG = RequestNewLanguageStepFragment.class.getSimpleName();
    private OnEventListener mListener;
    private View mRootView;
    private List<NewLanguageQuestion> mQuestions;
    private RequestNewLanguageStepAdapter mAdapter;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_new_language, container, false);

        Bundle args = getArguments();
        mQuestions = getAnswersFromArgs(args);

        final ListView layout = (ListView) mRootView.findViewById(R.id.controlsLayout);

        mAdapter = new RequestNewLanguageStepAdapter();
        layout.setAdapter(mAdapter);
        mAdapter.loadQuestions(mQuestions);

        return mRootView;
    }

     private void setVisible(int resID, Boolean enable) {
        View view = (View) mRootView.findViewById(resID);
        if (null != view) {
            view.setVisibility(enable ? View.VISIBLE : View.GONE);
        }
    }

    private void validateAnswers() {

        saveAnswers();

        if(false) {

        } else
        {
            doFinished();
        }
    }

    private void doFinished() {
        saveAnswers();
        getListener().finishLanguageRequest(RequestNewLanguageActivity.getQuestions(mQuestions).toString());
    }

    private void saveAnswers() {
        // nothing to do?
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

    protected List<NewLanguageQuestion> getAnswersFromArgs(Bundle args) {
        String questionsJson = args.getString(RequestNewLanguageActivity.EXTRA_NEW_LANGUAGE_QUESTIONS);
        List<NewLanguageQuestion> questions = RequestNewLanguageActivity.parseJsonStrIntoQuestions(questionsJson);
        return questions;
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

