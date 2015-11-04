package com.door43.translationstudio.newui.publish;

import com.door43.translationstudio.core.SourceLanguage;
import com.door43.translationstudio.core.TargetLanguage;
import com.door43.translationstudio.core.TranslationFormat;

/**
 * A thin wrapper to represent a validation set on a translation
 */
public class ValidationItem {
    private final String mTitle;
    private final String mBody;
    private final boolean mRange;
    private final boolean mValid;
    private final boolean mIsFrame;
    private String mTargetTranslationId;
    private String mChapterId;
    private String mFrameId;
    private TargetLanguage bodyLanguage;
    private SourceLanguage titleLanguage;
    private TranslationFormat bodyFormat;

    private ValidationItem(String title, SourceLanguage titleLanguage, String body, boolean range, boolean valid, boolean isFrame) {
        mTitle = title;
        mBody = body;
        mRange = range;
        mValid = valid;
        mIsFrame = isFrame;
        this.titleLanguage = titleLanguage;
    }

    /**
     * Generates a new valid item
     * @param title
     * @param range
     * @return
     */
    public static ValidationItem generateValidFrame(String title, SourceLanguage titleLanguage, boolean range) {
        return new ValidationItem(title,titleLanguage,  "", range, true, true);
    }

    /**
     * Generates a new valid item
     * @param title
     * @param range
     * @return
     */
    public static ValidationItem generateValidGroup(String title, SourceLanguage titleLanguage, boolean range) {
        return new ValidationItem(title, titleLanguage, "", range, true, false);
    }

    /**
     * Generates a new invalid item
     * @param title
     * @param body
     * @param bodyLanguage
     * @param targetTranslationId
     * @param chapterId
     * @param frameId
     * @return
     */
    public static ValidationItem generateInvalidFrame(String title, SourceLanguage titleLanguage, String body, TargetLanguage bodyLanguage, TranslationFormat bodyFormat, String targetTranslationId, String chapterId, String frameId) {
        ValidationItem item = new ValidationItem(title, titleLanguage, body, false, false, true);
        item.mTargetTranslationId = targetTranslationId;
        item.mChapterId = chapterId;
        item.mFrameId = frameId;
        item.bodyFormat = bodyFormat;
        item.bodyLanguage = bodyLanguage;
        return item;
    }

    /**
     * Generates a new invalid item
     * For our purposes a group can be either a chapter or a project
     * @param title
     * @return
     */
    public static ValidationItem generateInvalidGroup(String title, SourceLanguage titleLanguage) {
        return new ValidationItem(title, titleLanguage, "", false, false, false);
    }

    /**
     * Returns the title of the validation item
     * @return
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Returns the body text of the validation item
     * this only applies to invalid items
     * @return
     */
    public String getBody() {
        return mBody;
    }

    /**
     * Checks if the validation item is over a range
     * @return
     */
    public boolean isRange() {
        return mRange;
    }

    /**
     * Checks if the validation item is valid
     * @return
     */
    public boolean isValid() {
        return mValid;
    }

    /**
     * Checks if the validation items represents a frame
     * @return
     */
    public boolean isFrame() {
        return mIsFrame;
    }

    /**
     * Returns the target translation id
     * @return
     */
    public String getTargetTranslationId() {
        return mTargetTranslationId;
    }

    /**
     * Returns the chapter id
     * @return
     */
    public String getChapterId() {
        return mChapterId;
    }

    /**
     * Returns the frame id
     * @return
     */
    public String getFrameId() {
        return mFrameId;
    }

    /**
     * Returns the translation format of the body
     * @return
     */
    public TargetLanguage getBodyLanguage() {
        return bodyLanguage;
    }

    public TranslationFormat getBodyFormat() {
        return bodyFormat;
    }

    public SourceLanguage getTitleLanguage() {
        return titleLanguage;
    }
}
