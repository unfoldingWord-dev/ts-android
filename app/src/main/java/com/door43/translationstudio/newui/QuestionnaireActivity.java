package com.door43.translationstudio.newui;

import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.QuestionnairePage;
import com.door43.translationstudio.core.QuestionnaireQuestion;
import com.door43.translationstudio.core.Questionnaire;
import com.door43.translationstudio.dialogs.CustomAlertDialog;


/**
 * Activity for presenting a questionnaire to the user
 */
public abstract class QuestionnaireActivity extends BaseActivity implements QuestionnaireAdapter.OnEventListener {
    public static final String EXTRA_MESSAGE = "message";

    private static final String STATE_PAGE = "current_page";
    private static final String STATE_FINISHED = "finished";

    private int mCurrentPage = 0;
    private boolean mQuestionnaireFinished = false;
    private Questionnaire mQuestionnaire;
    private RecyclerView mRecyclerView;
    private QuestionnaireAdapter mAdapter;
    private CardView mPreviousButton;
    private CardView mNextButton;
    private CardView mDoneButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questionnaire);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mQuestionnaire = getQuestionnaire();
        if(mQuestionnaire == null) {
            Logger.e(this.getClass().getName(), "Cannot begin the questionnaire because no questionnaire was found.");
            return;
        }

        if(savedInstanceState != null) {
            mCurrentPage = savedInstanceState.getInt(STATE_PAGE, 0);
            mQuestionnaireFinished = savedInstanceState.getBoolean(STATE_FINISHED, false);
        }

        mRecyclerView = (RecyclerView)findViewById(R.id.recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mAdapter = new QuestionnaireAdapter(this);
        mAdapter.setOnEventListener(this);
        mRecyclerView.setAdapter(mAdapter);

        mPreviousButton = (CardView)findViewById(R.id.previous_button);
        mNextButton = (CardView)findViewById(R.id.next_button);
        mDoneButton = (CardView)findViewById(R.id.done_button);

        mPreviousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToPage(mCurrentPage - 1);
            }
        });
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // validate page completion
                QuestionnairePage page = mQuestionnaire.getPage(mCurrentPage);
                if(page != null) {
                    for (QuestionnaireQuestion q :page.getQuestions()) {
                        String answer = onGetAnswer(q);
                        if(q.required && (answer == null || answer.isEmpty())) {
                            CustomAlertDialog.Builder(QuestionnaireActivity.this)
                                    .setTitle(R.string.missing_question_answer)
                                    .setMessage(q.question)
                                    .setPositiveButton(R.string.dismiss, null)
                                    .show("missing-required-answer");
                            return;
                        }
                    }
                }
                if(onLeavePage(page)) {
                    nextPage();
                }
            }
        });
        mDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFinished();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        goToPage(mCurrentPage);
    }

    /**
     * Move to the next page
     */
    protected void nextPage() {
        goToPage(mCurrentPage + 1);
    }

    /**
     * Returns the questionnaire to be used
     * @return
     */
    protected abstract Questionnaire getQuestionnaire();

    /**
     * Called when the user navigates to the next page of questions
     */
    protected abstract boolean onLeavePage(QuestionnairePage page);

    /**
     * Called when the questionnaire has been completed
     */
    protected abstract void onFinished();

    /**
     * Moves the questionnaire back to the beginning
     */
    protected void restartQuestionnaire() {
        mCurrentPage = -1;
        goToPage(mCurrentPage);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_PAGE, mCurrentPage);
        outState.putBoolean(STATE_FINISHED, mQuestionnaireFinished);
        super.onSaveInstanceState(outState);
    }

    /**
     * Moves to a specific question page
     * @param page
     */
    private void goToPage(int page) {
        mRecyclerView.scrollToPosition(0);

        boolean animateRight = page > mCurrentPage;
        if(page >= mQuestionnaire.getNumPages()) {
            mCurrentPage = mQuestionnaire.getNumPages() - 1;
        } else if(page < 0) {
            mCurrentPage = 0;
        } else {
            mCurrentPage = page;
        }

        String titleFormat = getResources().getString(R.string.questionnaire_title);
        String title = String.format(titleFormat, mCurrentPage + 1, mQuestionnaire.getNumPages());
        setTitle(title);
        mAdapter.setPage(mQuestionnaire.getPage(mCurrentPage), animateRight);

        // update controls
        mPreviousButton.setVisibility(View.GONE);
        mDoneButton.setVisibility(View.GONE);
        mNextButton.setVisibility(View.GONE);
        if(mCurrentPage == 0 && mQuestionnaire.getNumPages() > 1) {
            mNextButton.setVisibility(View.VISIBLE);
        } else if(mCurrentPage == mQuestionnaire.getNumPages() - 1) {
            mPreviousButton.setVisibility(View.VISIBLE);
            mDoneButton.setVisibility(View.VISIBLE);
        } else {
            mNextButton.setVisibility(View.VISIBLE);
            mPreviousButton.setVisibility(View.VISIBLE);
        }

        AppContext.closeKeyboard(this);
    }

    @Override
    public void onDestroy() {
        if(mAdapter != null) {
            mAdapter.setOnEventListener(null);
        }
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            confirmExit();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        confirmExit();
    }

    private void confirmExit() {
        // display confirmation before closing the app
        CustomAlertDialog.Builder(this)
                .setTitle(R.string.confirm)
                .setMessage(R.string.confirm_leave_questionnaire)
                .setPositiveButton(R.string.yes, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show("confirm-exit");
    }
}

