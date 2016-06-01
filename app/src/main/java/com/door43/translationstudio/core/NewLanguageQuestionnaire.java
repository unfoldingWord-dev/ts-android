package com.door43.translationstudio.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 5/31/16.
 */
public class NewLanguageQuestionnaire {
    public final long dbId;
    public final int door43Id;
    public final String languageSlug;
    public final String languageName;
    public final String languageDirection;
    private final static int QUESTIONS_PER_PAGE = 3;
    private List<NewLanguagePage> pages = new ArrayList<>();

    public NewLanguageQuestionnaire(long dbId, int door43Id, String languageSlug, String languageName, String languageDirection) {

        this.dbId = dbId;
        this.door43Id = door43Id;
        this.languageSlug = languageSlug;
        this.languageName = languageName;
        this.languageDirection = languageDirection;
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
            if(!currentPage.containsQuestion(q.reliantQuestionId) && currentPage.size() >= QUESTIONS_PER_PAGE) {
                // close page
                pages.add(currentPage);
                currentPage = new NewLanguagePage();
            } else if(questionsAdded.contains(q.reliantQuestionId)) {
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
                }
                continue;
            }

            // add question to page
            currentPage.addQuestion(q);
            questionsAdded.add(q.id);
        }
    }

    /**
     * Returns the number of pages in the questionnaire
     */
    public int getNumPages() {
        return 0;
    }
}
