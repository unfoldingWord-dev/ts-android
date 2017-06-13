package com.door43.translationstudio.ui.spannables;

/**
 * Created by joel on 2/24/17.
 */
public class TranslationWordLinkSpan extends Span {
    private String title;

    public TranslationWordLinkSpan(String title, String id) {
        super(title, id);
        this.title = title;
    }

    public void setTitle(String title) {
        this.title = title;
        setHumanReadable(title);
    }
}
