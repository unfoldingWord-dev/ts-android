package com.door43.translationstudio.core;

import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import org.unfoldingword.tools.logger.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Questionnaires contain a series of questions grouped by page that must be answered by the user
 */
public class Questionnaire {
    public long dbId;
    public final long door43Id;
    public final String languageSlug;
    public final String languageName;
    public final LanguageDirection languageDirection;
    private final static int QUESTIONS_PER_PAGE = 3;
    private List<QuestionnairePage> pages = new ArrayList<>();
    public Map<String, Long> dataFields = new HashMap<>();

    public Questionnaire(long door43Id, String languageSlug, String languageName, LanguageDirection direction) {
        this.door43Id = door43Id;
        this.languageSlug = languageSlug;
        this.languageName = languageName;
        this.languageDirection = direction;
    }

    /**
     * Sets the id used in the local database
     * @param dbId
     */
    public void setDBId(long dbId) {
        this.dbId = dbId;
    }

    /**
     * Loads the data fields into the questionnaire
     * Data fields indicate which questions contain specific data.
     * @param fields
     */
    public void loadDataFields(Map<String, Long> fields) {
        this.dataFields = fields;
    }

    /**
     * Loads questions into the questionnaire
     * @param questions the questions should already be sorted
     */
    public void loadQuestions(QuestionnaireQuestion[] questions) {
        pages = new ArrayList<>();
        List<Long> questionsAdded = new ArrayList<>();
        QuestionnairePage currentPage = new QuestionnairePage();

        for(QuestionnaireQuestion q:questions) {
            if(!currentPage.containsQuestion(q.reliantQuestionId) && currentPage.getNumQuestions() >= QUESTIONS_PER_PAGE) {
                // close full page
                pages.add(currentPage);
                currentPage = new QuestionnairePage();
            } else if(!currentPage.containsQuestion(q.reliantQuestionId) && questionsAdded.contains(q.reliantQuestionId)) {
                // add out of order question to correct page
                boolean placedQuestion = false;
                for(QuestionnairePage processedPage:pages) {
                    if(processedPage.containsQuestion(q.reliantQuestionId)) {
                        processedPage.addQuestion(q);
                        placedQuestion = true;
                        break;
                    }
                }
                if(placedQuestion) {
                    questionsAdded.add(q.id);
                    continue;
                }
            } else if(currentPage.containsQuestion(q.reliantQuestionId)
                    && currentPage.getQuestionById(q.reliantQuestionId).reliantQuestionId < 0
                    && currentPage.indexOf(q.reliantQuestionId) > 0) {
                // place non-dependent reliant question in it's own page
                QuestionnaireQuestion reliantQuestion = currentPage.getQuestionById(q.reliantQuestionId);
                currentPage.removeQuestion(q.reliantQuestionId);

                // close page
                pages.add(currentPage);
                currentPage = new QuestionnairePage();

                // add questions to page
                currentPage.addQuestion(reliantQuestion);
                currentPage.addQuestion(q);
                questionsAdded.add(q.id);
                continue;
            }

            // add question to page
            currentPage.addQuestion(q);
            questionsAdded.add(q.id);
        }
        if(currentPage.getNumQuestions() > 0) {
            pages.add(currentPage);
        }
    }

    /**
     * Returns the number of pages in the questionnaire
     */
    public int getNumPages() {
        return pages.size();
    }

    /**
     * Generates a new questionnaire from json
     * @param jsonString
     * @return
     */
    @Nullable
    public static Questionnaire generate(String jsonString) {
        if(jsonString != null) {
            try {
                JSONObject json = new JSONObject(jsonString);
                String languageName = json.getString("name");
                LanguageDirection direction = LanguageDirection.get(json.getString("dir"));
                if(direction == null) {
                    direction = LanguageDirection.LeftToRight;
                }
                String languageSlug = json.getString("slug");
                long questionnaireId = json.getLong("questionnaire_id");
                return new Questionnaire(questionnaireId, languageSlug, languageName, direction);
            } catch (JSONException e) {
                Logger.w(Questionnaire.class.getName(), "Failed to parse new language questionnaire: " + jsonString, e);
            }
        }
        return null;
    }

    /**
     * Returns a page by its position
     * @param position
     * @return
     */
    @Nullable
    public QuestionnairePage getPage(int position) {
        if(position >= 0 && position < getNumPages()) {
            return pages.get(position);
        }
        return null;
    }
}
