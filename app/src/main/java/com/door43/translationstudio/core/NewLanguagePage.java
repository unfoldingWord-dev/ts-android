package com.door43.translationstudio.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a page of questions in the new language questionnaire
 */
public class NewLanguagePage {
    private Map<Long, NewLanguageQuestion> questions = new HashMap<>();

    /**
     * Adds a question to this page
     * @param question
     */
    public void addQuestion(NewLanguageQuestion question) {
        this.questions.put(question.id, question);
    }

    /**
     * Checks if this page contains the question
     * @param id
     * @return
     */
    public boolean containsQuestion(long id) {
        return this.questions.containsKey(id);
    }

    /**
     * Returns the questions on this page
     * @return
     */
    public List<NewLanguageQuestion> getQuestions() {
        return new ArrayList<>(this.questions.values());
    }

    /**
     * Returns the number of questions on this page
     * @return
     */
    public int getNumQuestions() {
        return this.questions.size();
    }

    /**
     * Returns the question on this page by position
     * @param position
     * @return
     */
    public NewLanguageQuestion getQuestion(int position) {
        return getQuestions().get(position);
    }

    /**
     * Returns the question on this page by id
     * @param id
     * @return
     */
    public NewLanguageQuestion getQuestionById(long id) {
        return this.questions.get(id);
    }
}
