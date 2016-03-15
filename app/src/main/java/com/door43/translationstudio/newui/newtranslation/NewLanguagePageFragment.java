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

import java.util.HashMap;
import java.util.List;

/**
 * Created by blm on 2/23/16.
 */
public class NewLanguagePageFragment extends BaseFragment {
    public static final String ARG_SOURCE_TRANSLATION_ID = "arg_source_translation_id";
    public static final String ARG_NEW_LANG_FINISHED = "arg_publish_finished";
    public static final String ARG_FIRST_PAGE = "first_page";
    public static final String ARG_LAST_PAGE = "last_page";
    public static final String TAG = NewLanguagePageFragment.class.getSimpleName();
    private OnEventListener mListener;
    private View mRootView;
    private List<NewLanguageQuestion> mQuestions;
    private HashMap<Integer,Integer> mQuestionIndex;
    private NewLanguagePageAdapter mAdapter;
    private boolean mFirstPage;
    private boolean mLastPage;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_new_language, container, false);

        Bundle args = getArguments();
        mQuestions = getAnswersFromArgs(args);
        mFirstPage = args.getBoolean(ARG_FIRST_PAGE);
        mLastPage = args.getBoolean(ARG_LAST_PAGE);

        final ListView layout = (ListView) mRootView.findViewById(R.id.controlsLayout);

        mQuestionIndex = NewLanguagePageFragment.generateIdMap(mQuestions);

        mAdapter = new NewLanguagePageAdapter();
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
                    validateAnswers();
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
                    getListener().previousStep(NewLanguageActivity.getQuestions(mQuestions).toString());
                }
            });
        } else {
            previousButton.setVisibility(View.GONE);
        }

        return mRootView;
    }

    static public HashMap<Integer,Integer> generateIdMap(List<NewLanguageQuestion> questions) {
        HashMap<Integer,Integer> questionIndex = new HashMap<Integer,Integer>();
        for (int i = 0; i < questions.size(); i++) {
            NewLanguageQuestion question = questions.get(i);
            questionIndex.put(question.id, i);
        }
        return questionIndex;
    }

    static public NewLanguageQuestion getQuestionPositionByID(List<NewLanguageQuestion> questions,
                                                              HashMap<Integer,Integer> questionIndex,
                                                              int id) {
        if(id < 0) {
            return null;
        }
        try {
            if(!questionIndex.containsKey(id)) {
                return null;
            }
            Integer pos = questionIndex.get(id);
            NewLanguageQuestion question = questions.get(pos);
            return question;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean shouldHaveAnswer( NewLanguageQuestion question) {
        if (question.conditionalID >= 0) {
            NewLanguageQuestion conditionalQuestion = getQuestionPositionByID(mQuestions,mQuestionIndex,question.conditionalID);
            if(conditionalQuestion != null) {
                if (conditionalQuestion.type == NewLanguageQuestion.QuestionType.CHECK_BOX) {
                    return NewLanguagePageAdapter.isCheckBoxAnswerTrue(conditionalQuestion);
                } else {
                    return conditionalQuestion.answer != null; // should have answer if question it depends on has answer
                }
            }
        }

        return true;
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

            if(!hasAnswer && shouldHaveAnswer(question)) {
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
                doNext();
                }
            });

        } else
        {
            doNext();
        }
    }

    private boolean hasAnswer(NewLanguageQuestion question) {
        if(null == question.answer) {
            if(question.type == NewLanguageQuestion.QuestionType.CHECK_BOX) {
                question.answer = NewLanguagePageAdapter.FALSE_STR; // checked always has a state, defaults to false
            } else {
                return false;
            }
        }
        if(question.answer.isEmpty()) {
            return false;
        }
        return true;
    }
    private void doNext() {
        saveAnswers();
        String answers = NewLanguageActivity.getQuestions(mQuestions).toString();
        if (mLastPage) {
            getListener().finishLanguageRequest(answers);
        } else {
            getListener().nextStep(answers);
        }
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
        String questionsJson = args.getString(NewLanguageActivity.EXTRA_NEW_LANGUAGE_QUESTIONS);
        List<NewLanguageQuestion> questions = NewLanguageActivity.parseJsonStrIntoQuestions(questionsJson);
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

