package com.door43.translationstudio.newui.newlanguage;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
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
import com.door43.translationstudio.core.NewLanguageQuestion;
import com.door43.translationstudio.core.NewLanguageQuestionnaire;
import com.door43.translationstudio.core.NewLanguageQuestionnaireResponse;
import com.door43.translationstudio.newui.BaseActivity;
import com.door43.widget.ViewUtil;

import java.util.ArrayList;
import java.util.List;


/**
 * Activity for getting answers to new language questions
 */
public class NewLanguageActivity extends BaseActivity implements NewLanguagePageAdapter.OnEventListener {

    public static final String TAG = NewLanguageActivity.class.getSimpleName();
    public static final String EXTRA_QUESTIONNAIRE_RESPONSE = "questionnaire_response";
    public static final String EXTRA_MESSAGE = "message";

    private static final String STATE_PAGE = "current_page";
    private static final String STATE_FINISHED = "finished";

    private int mCurrentPage = 0;
    private boolean mQuestionnaireFinished = false;
    private NewLanguageQuestionnaire mQuestionnaire;
    private NewLanguageQuestionnaireResponse mResponse = null;
    private RecyclerView mRecyclerView;
    private NewLanguagePageAdapter mAdapter;
    private CardView mPreviousButton;
    private CardView mNextButton;
    private CardView mDoneButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_new_language);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mQuestionnaire = AppContext.getLibrary().getNewLanguageQuestionnaire();
        if(mQuestionnaire == null) {
            Logger.e(this.getClass().getName(), "Cannot begin the new language questionnaire because no questionnaire was found.");
            Intent data = new Intent();
            data.putExtra(EXTRA_MESSAGE, getResources().getString(R.string.missing_questionnaire));
            setResult(RESULT_FIRST_USER, data);
            finish();
            return;
        }

        setTitle(mQuestionnaire.languageName);

        if(savedInstanceState != null) {
            mCurrentPage = savedInstanceState.getInt(STATE_PAGE, 0);
            mQuestionnaireFinished = savedInstanceState.getBoolean(STATE_FINISHED, false);
            mResponse = NewLanguageQuestionnaireResponse.generate(savedInstanceState.getString(EXTRA_QUESTIONNAIRE_RESPONSE));
            if(mResponse == null) {
                Snackbar snack = Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.error), Snackbar.LENGTH_LONG);
                ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                snack.show();

                // restart
                mResponse = NewLanguageQuestionnaireResponse.newInstance(this, mQuestionnaire.door43Id, "android", AppContext.getProfile().getFullName());
                mCurrentPage = -1;
                return;
            }
        } else {
            mResponse = NewLanguageQuestionnaireResponse.newInstance(this, mQuestionnaire.door43Id, "android", AppContext.getProfile().getFullName());
        }

        mRecyclerView = (RecyclerView)findViewById(R.id.recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mAdapter = new NewLanguagePageAdapter(this);
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
                goToPage(mCurrentPage + 1);
            }
        });
        mDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent data = new Intent();
                data.putExtra(EXTRA_QUESTIONNAIRE_RESPONSE, mResponse.toJson());
                setResult(RESULT_OK, data);
                finish();
            }
        });

        goToPage(mCurrentPage);
     }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_PAGE, mCurrentPage);
        outState.putBoolean(STATE_FINISHED, mQuestionnaireFinished);
        outState.putSerializable(EXTRA_QUESTIONNAIRE_RESPONSE, mResponse.toJson());
        super.onSaveInstanceState(outState);
    }


    public static List<NewLanguageQuestion> mergePagesOfNewLanguageQuestions(List<List<NewLanguageQuestion>> questionPages) {
        List<NewLanguageQuestion> mergedQuestions = new ArrayList<>();
        for (int i = 0; i < questionPages.size(); i++) {
            List<NewLanguageQuestion> questions = questionPages.get(i);
            mergedQuestions.addAll(questions);
        }
        return mergedQuestions;
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

        String titleFormat = getResources().getString(R.string.new_language_questionnaire_title);
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
    public String onGetAnswer(NewLanguageQuestion question) {
        return mResponse.getAnswer(question.id);
    }

    @Override
    public void onAnswerChanged(NewLanguageQuestion question, String answer) {
        mResponse.setAnswer(question.id, answer);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }
}

