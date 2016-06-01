package com.door43.translationstudio.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a page of questions in the new language questionnaire
 */
public class NewLanguagePage {
    private List<NewLanguageQuestion> questions = new ArrayList<>();
    private List<Long> ids = new ArrayList<>();

    /**
     * Adds a question to this page
     * @param question
     */
    public void addQuestion(NewLanguageQuestion question) {
        this.questions.add(question);
        this.ids.add(question.id);
    }

    /**
     * Checks if this page contains the question
     * @param id
     * @return
     */
    public boolean containsQuestion(long id) {
        return this.ids.contains(id);
    }

    /**
     * Returns the questions on this page
     * @return
     */
    public List<NewLanguageQuestion> getQuestions() {
        return this.questions;
    }

    /**
     * Returns the number of questions on this page
     * @return
     */
    public int size() {
        return this.questions.size();
    }
}
