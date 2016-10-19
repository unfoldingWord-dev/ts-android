package com.door43.questionnaire;

import android.support.annotation.Nullable;

import com.door43.questionnaire.QuestionnairePage;

import org.unfoldingword.door43client.models.Question;
import org.unfoldingword.door43client.models.Questionnaire;

import java.util.ArrayList;
import java.util.List;

/**
 * paginates a questionnaire
 */

public class QuestionnairePager {

    public final Questionnaire questionnaire;
    private final static int QUESTIONS_PER_PAGE = 3;
    public List<QuestionnairePage> pages = new ArrayList<>();

    public QuestionnairePager(Questionnaire questionnaire) {
        this.questionnaire = questionnaire;
    }

    /**
     * Checks if a data field exists.
     *
     * @param key
     * @return
     */
    public boolean hasDataField(String key) {
        return questionnaire.dataFields.containsKey(key);
    }

    /**
     * Returns the tdID of the question link to this data field
     * @param key
     * @return
     */
    public long getDataField(String key) {
        return questionnaire.dataFields.get(key);
    }

    /**
     * Loads the questions in pages
     * @param questions these questions should already be sorted correctly
     */
    public void loadQuestions(List<Question> questions) {
        pages = new ArrayList<>();
        List<Long> questionsAdded = new ArrayList<>();
        QuestionnairePage currentPage = new QuestionnairePage();

        for(Question q:questions) {
            if(!currentPage.containsQuestion(q.dependsOn) && currentPage.getNumQuestions() >= QUESTIONS_PER_PAGE) {
                // close full page
                pages.add(currentPage);
                currentPage = new QuestionnairePage();
            } else if(!currentPage.containsQuestion(q.dependsOn) && questionsAdded.contains(q.dependsOn)) {
                // add out of order question to correct page
                boolean placedQuestion = false;
                for(QuestionnairePage processedPage:pages) {
                    if(processedPage.containsQuestion(q.dependsOn)) {
                        processedPage.addQuestion(q);
                        placedQuestion = true;
                        break;
                    }
                }
                if(placedQuestion) {
                    questionsAdded.add(q.tdId);
                    continue;
                }
            } else if(currentPage.containsQuestion(q.dependsOn)
                    && currentPage.getQuestionById(q.dependsOn).dependsOn < 0
                    && currentPage.indexOf(q.dependsOn) > 0) {
                // place non-dependent reliant question in it's own page
                Question reliantQuestion = currentPage.getQuestionById(q.dependsOn);
                currentPage.removeQuestion(q.dependsOn);

                // close page
                pages.add(currentPage);
                currentPage = new QuestionnairePage();

                // add questions to page
                currentPage.addQuestion(reliantQuestion);
                currentPage.addQuestion(q);
                questionsAdded.add(q.tdId);
                continue;
            }

            // add question to page
            currentPage.addQuestion(q);
            questionsAdded.add(q.tdId);
        }
        if(currentPage.getNumQuestions() > 0) {
            pages.add(currentPage);
        }
    }

    /**
     * Returns the number of pages
     * @return
     */
    public int size() {
        return pages.size();
    }

    /**
     * Returns a page by its position
     * @param position
     * @return
     */
    @Nullable
    public QuestionnairePage getPage(int position) {
        if(position >= 0 && position < size()) {
            return pages.get(position);
        }
        return null;
    }

    /**
     * Returns a question if found in this pager
     * @param tdId
     * @return
     */
    @Nullable
    public Question getQuestion(long tdId) {
        for(QuestionnairePage page:pages) {
            for(Question question:page.getQuestions()) {
                if(question.tdId == tdId) return question;
            }
        }
        return null;
    }
}
