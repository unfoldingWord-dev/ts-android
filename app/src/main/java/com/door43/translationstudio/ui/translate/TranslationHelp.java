package com.door43.translationstudio.ui.translate;

/**
 * Represents a single translation question
 */
public class TranslationHelp {

    public final String title;
    public final String body;

    /**
     *
     * @param title the question title
     * @param body the question body
     */
    public TranslationHelp(String title, String body) {

        this.title = title;
        this.body = body;
    }
}
