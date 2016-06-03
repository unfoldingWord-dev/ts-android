package com.door43.translationstudio.core;

import com.door43.tools.reporting.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * New Language Questionnaires are used to generate a new target language code.
 * The questionnaire must be completed before a custom language code will be assigned.
 * Later the results of the questionnaire will be submitted for processing and, if approved,
 * assignment to a real language code.
 */
public class NewLanguageQuestionnaire {
    public long dbId;
    public final long door43Id;
    public final String languageSlug;
    public final String languageName;
    public final LanguageDirection languageDirection;
    private final static int QUESTIONS_PER_PAGE = 3;
    private List<NewLanguagePage> pages = new ArrayList<>();

    public NewLanguageQuestionnaire(long door43Id, String languageSlug, String languageName, LanguageDirection direction) {
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
     * Loads questions into the questionnaire
     * @param questions the questions should already be sorted
     */
    public void loadQuestions(NewLanguageQuestion[] questions) {
        pages = new ArrayList<>();
        List<Long> questionsAdded = new ArrayList<>();
        NewLanguagePage currentPage = new NewLanguagePage();

        for(NewLanguageQuestion q:questions) {
            if(!currentPage.containsQuestion(q.reliantQuestionId) && currentPage.getNumQuestions() >= QUESTIONS_PER_PAGE) {
                // close full page
                pages.add(currentPage);
                currentPage = new NewLanguagePage();
            } else if(!currentPage.containsQuestion(q.reliantQuestionId) && questionsAdded.contains(q.reliantQuestionId)) {
                // add out of order question to correct page
                boolean placedQuestion = false;
                for(NewLanguagePage processedPage:pages) {
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
                NewLanguageQuestion reliantQuestion = currentPage.getQuestionById(q.reliantQuestionId);
                currentPage.removeQuestion(q.reliantQuestionId);

                // close page
                pages.add(currentPage);
                currentPage = new NewLanguagePage();

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
    public static NewLanguageQuestionnaire generate(String jsonString) {
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
                return new NewLanguageQuestionnaire(questionnaireId, languageSlug, languageName, direction);
            } catch (JSONException e) {
                Logger.w(NewLanguageQuestionnaire.class.getName(), "Failed to parse new language questionnaire: " + jsonString, e);
            }
        }
        return null;
    }

    /**
     * Returns a page by its position
     * @param position
     * @return
     */
    public NewLanguagePage getPage(int position) {
        return pages.get(position);
    }
}
