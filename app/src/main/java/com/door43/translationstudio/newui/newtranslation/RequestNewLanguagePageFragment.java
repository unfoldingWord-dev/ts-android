package com.door43.translationstudio.newui.newtranslation;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.NewLanguageQuestion;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.newui.BaseFragment;

import org.json.JSONObject;

import java.util.List;

/**
 * Created by blm on 2/23/16.
 */
public class RequestNewLanguagePageFragment extends BaseFragment {
    public static final String ARG_SOURCE_TRANSLATION_ID = "arg_source_translation_id";
    public static final String ARG_NEW_LANG_FINISHED = "arg_publish_finished";
    public static final String ARG_FIRST_PAGE = "first_page";
    public static final String ARG_LAST_PAGE = "last_page";
    public static final String TAG = RequestNewLanguagePageFragment.class.getSimpleName();
    private OnEventListener mListener;
    private View mRootView;
    private List<NewLanguageQuestion> mQuestions;
    private RequestNewLanguagePageAdapter mAdapter;
    private boolean mFirstPage;
    private boolean mLastPage;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_new_language, container, false);

        Bundle args = getArguments();
        mQuestions = getAnswersFromArgs(args);
        mFirstPage = args.getBoolean(ARG_FIRST_PAGE);
        mLastPage = args.getBoolean(ARG_LAST_PAGE);

        final ListView layout = (ListView) mRootView.findViewById(R.id.controlsLayout);

        mAdapter = new RequestNewLanguagePageAdapter();
        layout.setAdapter(mAdapter);
        mAdapter.loadQuestions(mQuestions);

        Button nextButton = (Button) mRootView.findViewById(R.id.next_button);
        Button doneButton = (Button) mRootView.findViewById(R.id.done_button);
        if(!mLastPage) {
            nextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    validateAnswers();
                }
            });
            doneButton.setVisibility(View.GONE);
        } else {
            doneButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doFinished();
                }
            });
            nextButton.setVisibility(View.GONE);
        }

        Button previousButton = (Button) mRootView.findViewById(R.id.previous_button);
        if(!mFirstPage) {
            previousButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    saveAnswers();
                    getListener().previousStep(RequestNewLanguageActivity.getQuestions(mQuestions).toString());
                }
            });
        } else {
            previousButton.setVisibility(View.GONE);
        }

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

        boolean missingAnswers = false;
        boolean valid = true;
        NewLanguageQuestion incompleteQuestion = null;
        NewLanguageQuestion missingAnswerQuestion = null;

        for (int i =  mQuestions.size() - 1; i >= 0; i--) {
            NewLanguageQuestion question = mQuestions.get(i);
            boolean hasAnswer = hasAnswer(question);
            if(question.required) {
                if(!hasAnswer) {
                    valid = false;
                    incompleteQuestion = question;
                }
            }
            if(!hasAnswer) {
                missingAnswers = true;
                missingAnswerQuestion = question;
            }
        }

        if(!valid) {
            showAnswerRequiredBlocked(incompleteQuestion.question);
        } else
        if(missingAnswers) {

            warnAnswersMissingBeforeContinue(missingAnswerQuestion.question, new View.OnClickListener() {
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

    private boolean hasAnswer(NewLanguageQuestion question) {
        if(null == question.answer) {
            if(question.type == NewLanguageQuestion.QuestionType.CHECK_BOX) {
                question.answer = RequestNewLanguagePageAdapter.FALSE_STR; // checked always has a state, defaults to false
            } else {
                return false;
            }
        }
        if(question.answer.isEmpty()) {
            return false;
        }
        return true;
    }
    private void doFinished() {
        saveAnswers();
        String answers = RequestNewLanguageActivity.getQuestions(mQuestions).toString();
        getListener().finishLanguageRequest(answers);
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

    /**
     * Registeres the click listener
     * @param listener
     */
    public void setOnEventListener(OnEventListener listener) {
        mListener = listener;
    }


    protected List<NewLanguageQuestion> getAnswersFromArgs(Bundle args) {
        String questionsJson = args.getString(RequestNewLanguageActivity.EXTRA_NEW_LANGUAGE_QUESTIONS);
        List<NewLanguageQuestion> questions = RequestNewLanguageActivity.parseJsonStrIntoQuestions(questionsJson);
        return questions;
    }


    protected void showAnswerRequiredBlocked(String question) {
        Resources res = getActivity().getResources();
        String message = String.format(res.getString(R.string.answer_required_for), question);
        CustomAlertDialog.Create(getActivity())
                .setTitle(R.string.invalid_entry_title)
                .setMessage(message)
                .setPositiveButton(R.string.label_ok, null)
                .show(getFragmentManager(), "MissingAnswer");
    }

    protected void warnAnswerMissingBeforeContinue(String question, View.OnClickListener listener) {
        Resources res = getActivity().getResources();
        String message = String.format(res.getString(R.string.answer_missing_for),question);
        CustomAlertDialog.Create(getActivity())
                .setTitle(R.string.answers_missing_title)
                .setMessage(message)
                .setPositiveButton(R.string.yes, listener)
                .setNegativeButton(R.string.no, null)
                .show(getFragmentManager(),"MissingAnswers");
    }

    protected void warnAnswersMissingBeforeContinue(String question, View.OnClickListener listener) {
        CustomAlertDialog.Create(getActivity())
                .setTitle(R.string.answers_missing_title)
                .setMessage(R.string.answers_missing_continue)
                .setPositiveButton(R.string.yes, listener)
                .setNegativeButton(R.string.no, null)
                .show(getFragmentManager(),"MissingAnswers");
    }
}

