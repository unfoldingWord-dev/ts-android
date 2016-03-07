package com.door43.translationstudio.core;

/**
 * Created by joel on 9/16/2015.
 */
public class ChapterTranslation {

    public final String reference;
    public final String title;
    private final String mId;
    private final boolean titleFinished;
    private final boolean referenceFinished;
    private TranslationFormat mTranslationFormat;

    public ChapterTranslation(String title, String reference, String chapterId, boolean titleFinished, boolean referenceFinished, TranslationFormat translationFormat) {
        this.title = title;
        this.reference = reference;
        mId = chapterId;
        this.titleFinished = titleFinished;
        this.referenceFinished = referenceFinished;
        mTranslationFormat = translationFormat;
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
        return this.referenceFinished;
    }

    /**
     * Checks if the chapter title is finished being translated
     * @return
     */
    public boolean isTitleFinished() {
        return this.titleFinished;
    }

    /**
     * sets the translation format for the chapter title and reference
     * @param translationFormat
     */
    public void setFormat(TranslationFormat translationFormat) {
        this.mTranslationFormat = translationFormat;
    }

    /**
     * Returns the translation format for the chapter title and reference
     * @return
     */
    public TranslationFormat getFormat() {
        return mTranslationFormat;
    }
}
