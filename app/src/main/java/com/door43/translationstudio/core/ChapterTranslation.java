package com.door43.translationstudio.core;

/**
 * Created by joel on 9/16/2015.
 */
public class ChapterTranslation {

    public final String reference;
    public final String title;
    private final String mId;

    public ChapterTranslation(String title, String reference, String chapterId) {
        this.title = title;
        this.reference = reference;
        mId = chapterId;
    }

    /**
     * Returns the chapter id
     * @return
     */
    public String getId() {
        return mId;
    }

    /**
     * Checks if the chapter reference is finished being translated
     * @return
     */
    public boolean isReferenceFinished() {
        // TODO: 9/30/2015 finish
        return false;
    }

    /**
     * Checks if the chapter reference is finished being translated
     * @return
     */
    public boolean isTitleFinished() {
        // TODO: 9/30/2015 finish
        return false;
    }

    /**
     * Returns the translation format for the chapter title and reference
     * @return
     */
    public TranslationFormat getFormat() {
        return TranslationFormat.DEFAULT;
    }
}
