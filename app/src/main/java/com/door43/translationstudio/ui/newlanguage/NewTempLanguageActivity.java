package com.door43.translationstudio.ui.newlanguage;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.NewLanguageRequest;
import com.door43.questionnaire.QuestionnairePage;
import com.door43.questionnaire.QuestionnairePager;
import com.door43.questionnaire.QuestionnaireActivity;
import com.door43.widget.ViewUtil;

import org.unfoldingword.door43client.models.Question;
import org.unfoldingword.door43client.models.Questionnaire;
import org.unfoldingword.door43client.models.TargetLanguage;

import java.util.List;

/**
 * Created by joel on 6/8/16.
 */
public class NewTempLanguageActivity extends QuestionnaireActivity implements LanguageSuggestionsDialog.OnClickListener {
    public static final String EXTRA_LANGUAGE_REQUEST = "new_language_request";
    public static final String EXTRA_RESULT_CODE = "result_code";
    public static final String EXTRA_LANGUAGE_ID = "language_id";
    public static final int RESULT_MISSING_QUESTIONNAIRE = 0;
    public static final int RESULT_USE_EXISTING_LANGUAGE = 1;
    private NewLanguageRequest request = null;
    private QuestionnairePager questionnairePager;
    private LanguageSuggestionsDialog languageSuggestionsDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.questionnairePager = getQuestionnaire();

        if(questionnairePager != null) {
            // TRICKY: the profile can be null when running unit tests
            String translatorName = App.getProfile() != null ? App.getProfile().getFullName() : "unknown";
            if (savedInstanceState != null) {
                request = NewLanguageRequest.generate(savedInstanceState.getString(EXTRA_LANGUAGE_REQUEST));
                if (request == null) {
                    Snackbar snack = Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.error), Snackbar.LENGTH_LONG);
                    ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                    snack.show();

                    // restart
                    request = NewLanguageRequest.newInstance(this, questionnairePager, "android", translatorName);
                    restartQuestionnaire();
                    return;
                }
            } else {
                // begin
                request = NewLanguageRequest.newInstance(this, questionnairePager, "android", translatorName);
            }
        } else {
            // missing questionnaire
            Intent data = new Intent();
            data.putExtra(EXTRA_RESULT_CODE, RESULT_MISSING_QUESTIONNAIRE);
            setResult(RESULT_FIRST_USER, data);
            finish();
            return;
        }

        LanguageSuggestionsDialog prev = (LanguageSuggestionsDialog) getFragmentManager().findFragmentByTag(LanguageSuggestionsDialog.TAG);
        if(prev != null) {
            prev.setOnClickListener(this);
        }
    }

    @Override
    protected QuestionnairePager getQuestionnaire() {
        // TRICKY: for now we only have one questionnaire
        List<Questionnaire> questionnaires = App.getLibrary().index().getQuestionnaires();
        if(questionnaires.size() > 0) {
            return new QuestionnairePager(questionnaires.get(0));
        }
        return null;
    }

    @Override
    protected boolean onLeavePage(QuestionnairePage page) {
        // check for a matching language that already exists
        if(questionnairePager.hasDataField("ln")) {
            Question q = page.getQuestionById(questionnairePager.getDataField("ln"));
            if(q != null) {
                String answer = request.getAnswer(q.tdId);
                if (answer != null) {
                    List<TargetLanguage> languages = App.getLibrary().index().findTargetLanguage(answer.trim());
                    if (languages.size() > 0) {
                        App.closeKeyboard(this);
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        Fragment prev = getFragmentManager().findFragmentByTag(LanguageSuggestionsDialog.TAG);
                        if(prev != null) {
                            ft.remove(prev);
                        }
                        ft.addToBackStack(null);

                        languageSuggestionsDialog = new LanguageSuggestionsDialog();
                        Bundle args  = new Bundle();
                        args.putString(LanguageSuggestionsDialog.ARG_LANGUAGE_QUERY, answer.trim());
                        languageSuggestionsDialog.setArguments(args);
                        languageSuggestionsDialog.setOnClickListener(this);
                        languageSuggestionsDialog.show(ft, LanguageSuggestionsDialog.TAG);
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public String onGetAnswer(Question question) {
        if(request != null) {
            return request.getAnswer(question.tdId);
        }
        return null;
    }

    @Override
    public void onAnswerChanged(Question question, String answer) {
        if(request != null) {
            request.setAnswer(question.tdId, answer);
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

    @Override
    public void onDestroy() {
        if(languageSuggestionsDialog != null) {
            languageSuggestionsDialog.setOnClickListener(null);
        }
        super.onDestroy();
    }

    @Override
    public void onAcceptLanguageSuggestion(TargetLanguage language) {
        Intent data = new Intent();
        data.putExtra(EXTRA_RESULT_CODE, RESULT_USE_EXISTING_LANGUAGE);
        data.putExtra(EXTRA_LANGUAGE_ID, language.slug);
        setResult(RESULT_FIRST_USER, data);
        finish();
    }

    @Override
    public void onDismissLanguageSuggestion() {
        nextPage();
    }
}
