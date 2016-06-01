package com.door43.translationstudio.newui.newlanguage;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.NewLanguageQuestion;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.newui.BaseFragment;

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
    public static final String EXTRA_NEW_LANGUAGE_FOCUS_POSITION = "extra_new_language_focus_position";
    public static final String EXTRA_NEW_LANGUAGE_SELECTION = "extra_new_language_selection";
    private OnEventListener mListener;
    private View mRootView;
    private List<NewLanguageQuestion> mQuestions;
    private HashMap<Long,Integer> mQuestionIndex;
    private NewLanguagePageAdapter mAdapter;
    private boolean mFirstPage;
    private boolean mLastPage;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_new_language, container, false);

        Bundle args = getArguments();
        mFirstPage = args.getBoolean(ARG_FIRST_PAGE);
        mLastPage = args.getBoolean(ARG_LAST_PAGE);

        if(savedInstanceState != null) {
            mQuestions = getAnswersFromArgs(savedInstanceState);
        } else {
            mQuestions = getAnswersFromArgs(args);
        }

        mQuestionIndex = NewLanguagePageFragment.generateIdMap(mQuestions);

        LinearLayout layout = (LinearLayout) mRootView.findViewById(R.id.content_layout);
        mAdapter = new NewLanguagePageAdapter();
        mAdapter.setContentsView(layout);
        mAdapter.loadQuestions(mQuestions);

        if(savedInstanceState != null) { // need to restore focus
            int focusedPosition = savedInstanceState.getInt(EXTRA_NEW_LANGUAGE_FOCUS_POSITION);
            int selection = savedInstanceState.getInt(EXTRA_NEW_LANGUAGE_SELECTION);
            mAdapter.restoreFocus(focusedPosition, selection);
        }

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
                    getListener().previousPage(NewLanguageActivity.getQuestionsAsJson(mQuestions).toString());
                }
            });
        } else {
            previousButton.setVisibility(View.GONE);
        }

        return mRootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        saveAnswers();
        cleanup();
        String questionsJson = NewLanguageActivity.getQuestionsAsJson(mQuestions).toString();
        outState.putString(NewLanguageActivity.EXTRA_NEW_LANGUAGE_QUESTIONS, questionsJson);
        outState.putInt(EXTRA_NEW_LANGUAGE_FOCUS_POSITION, mAdapter.getFocusedPosition());
        outState.putInt(EXTRA_NEW_LANGUAGE_SELECTION, mAdapter.getSelection());
        super.onSaveInstanceState(outState);
    }

    public void cleanup() {
        if(mAdapter != null) {
            mAdapter.cleanup();
        }
    }

    public void saveAnswers() {
        if(mAdapter != null) {
            mAdapter.updateAnswers();
        }
    }

    /**
     * create a map indexed by question IDs to position in array so we don't have to do iterative searches on keys
     * @param questions
     * @return
     */
    static public HashMap<Long,Integer> generateIdMap(List<NewLanguageQuestion> questions) {
        HashMap<Long,Integer> questionIndex = new HashMap<Long,Integer>();
        for (int i = 0; i < questions.size(); i++) {
            NewLanguageQuestion question = questions.get(i);
            questionIndex.put(question.id, i);
        }
        return questionIndex;
    }

    /**
     * find question by looking up position in HashMap by id
     * @param questions
     * @param questionIndex
     * @param id
     * @return
     */
    static public NewLanguageQuestion getQuestionPositionByID(List<NewLanguageQuestion> questions,
                                                              HashMap<Long,Integer> questionIndex,
                                                              long id) {
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

    /**
     * returns true if question should have an answer.  This doesn't mean it is required, but just that
     * question is enabled and has no answer.  This is used to warn user that they may have missed a
     * question.
     * @param question
     * @return
     */
    private boolean shouldHaveAnswer( NewLanguageQuestion question) {
        if (question.reliantQuestionId >= 0) {
            NewLanguageQuestion conditionalQuestion = getQuestionPositionByID(mQuestions,mQuestionIndex,question.reliantQuestionId);
            if(conditionalQuestion != null) {
                if (conditionalQuestion.type == NewLanguageQuestion.QuestionType.INPUT_TYPE_BOOLEAN) {
                    return conditionalQuestion.isBooleanAnswerTrue();
                } else {
                    return conditionalQuestion.answer != null; // should have answer if question it depends on has answer
                }
            }
        }
        return true;
    }

    /***
     * go through all the questions and validate the answers
     */
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

    /***
     * returns true if question has been answered (not empty)
     * @param question
     * @return
     */
    private boolean hasAnswer(NewLanguageQuestion question) {
        if(null == question.answer) {
            return false;
        }
        if(question.answer.isEmpty()) {
            return false;
        }
        return true;
    }

    /***
     * move to next page, or if last page then finished
     */
    private void doNext() {
        String answers = NewLanguageActivity.getQuestionsAsJson(mQuestions).toString();
        if (mLastPage) {
            getListener().finishLanguageRequest(answers);
        } else {
            getListener().nextPage(answers);
        }
    }

    protected OnEventListener getListener() {
        return mListener;
    }

    public interface OnEventListener {
        void nextPage(String answersJson);

        void previousPage(String answersJson);

        void finishLanguageRequest(String answersJson);
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

    /**
     * Show dialog to let user know that a required question has not been answered.  The user cannot
     * advance to next page.
     * @param question
     */
    protected void showAnswerRequiredBlocked(String question) {
        Resources res = getActivity().getResources();
        String message = String.format(res.getString(R.string.answer_required_for), question);
        CustomAlertDialog.Create(getActivity())
                .setTitle(R.string.invalid_entry_title)
                .setMessage(message)
                .setPositiveButton(R.string.label_ok, null)
                .show(getFragmentManager(), "MissingAnswer");
    }

    /**
     * Show dialog to warn that a question has not been answered.  User still has option of continuing.
     * @param question
     * @param listener
     */
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

    /**
     * Show dialog to warn that multiple question have not been answered
     * @param question
     * @param listener
     */
    protected void warnAnswersMissingBeforeContinue(String question, View.OnClickListener listener) {
        CustomAlertDialog.Create(getActivity())
                .setTitle(R.string.answers_missing_title)
                .setMessage(R.string.answers_missing_continue)
                .setPositiveButton(R.string.yes, listener)
                .setNegativeButton(R.string.no, null)
                .show(getFragmentManager(),"MissingAnswers");
    }
}

