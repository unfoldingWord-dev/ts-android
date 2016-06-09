package com.door43.translationstudio.newui.newlanguage;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;

import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.NewLanguageRequest;
import com.door43.translationstudio.core.Questionnaire;
import com.door43.translationstudio.core.QuestionnaireQuestion;
import com.door43.translationstudio.newui.QuestionnaireActivity;
import com.door43.widget.ViewUtil;

/**
 * Created by joel on 6/8/16.
 */
public class NewTempLanguageActivity extends QuestionnaireActivity {
    public static final String EXTRA_LANGUAGE_REQUEST = "new_language_request";
    private NewLanguageRequest request = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Questionnaire questionnaire = getQuestionnaire();

        if(questionnaire != null) {
            if (savedInstanceState != null) {
                request = NewLanguageRequest.generate(savedInstanceState.getString(EXTRA_LANGUAGE_REQUEST));
                if (request == null) {
                    Snackbar snack = Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.error), Snackbar.LENGTH_LONG);
                    ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                    snack.show();

                    // restart
                    request = NewLanguageRequest.newInstance(this, questionnaire.door43Id, "android", AppContext.getProfile().getFullName());
                    restartQuestionnaire();
                    return;
                }
            } else {
                // begin
                request = NewLanguageRequest.newInstance(this, questionnaire.door43Id, "android", AppContext.getProfile().getFullName());
            }
        } else {
            Intent data = new Intent();
            data.putExtra(EXTRA_MESSAGE, getResources().getString(R.string.missing_questionnaire));
            setResult(RESULT_FIRST_USER, data);
            finish();
        }
    }

    @Override
    protected Questionnaire getQuestionnaire() {
        // TRICKY: for now we only have one questionnaire
        Questionnaire[] questionnaires = AppContext.getLibrary().getQuestionnaires();
        if(questionnaires.length > 0) {
            return questionnaires[0];
        }
        return null;
    }

    @Override
    public String onGetAnswer(QuestionnaireQuestion question) {
        if(request != null) {
            return request.getAnswer(question.id);
        }
        return null;
    }

    @Override
    public void onAnswerChanged(QuestionnaireQuestion question, String answer) {
        if(request != null) {
            request.setAnswer(question.id, answer);
        }
    }

    @Override
    public void onFinished() {
        Intent data = new Intent();
        data.putExtra(EXTRA_LANGUAGE_REQUEST, request.toJson());
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(EXTRA_LANGUAGE_REQUEST, request.toJson());
        super.onSaveInstanceState(outState);
    }
}
