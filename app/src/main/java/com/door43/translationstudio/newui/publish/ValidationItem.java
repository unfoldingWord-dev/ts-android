package com.door43.translationstudio.newui.publish;

/**
 * A thin wrapper to represent a validation set on a translation
 */
public class ValidationItem {
    private final String mTitle;
    private final String mBody;
    private final boolean mRange;
    private final boolean mValid;
    private final boolean mIsFrame;

    private ValidationItem(String title, String body, boolean range, boolean valid, boolean isFrame) {
        mTitle = title;
        mBody = body;
        mRange = range;
        mValid = valid;
        mIsFrame = isFrame;
    }

    /**
     * Generates a new valid item
     * @param title
     * @param range
     * @return
     */
    public static ValidationItem generateValidFrame(String title, boolean range) {
        return new ValidationItem(title, "", range, true, true);
    }

    /**
     * Generates a new valid item
     * @param title
     * @param range
     * @return
     */
    public static ValidationItem generateValidGroup(String title, boolean range) {
        return new ValidationItem(title, "", range, true, false);
    }

    /**
     * Generates a new invalid item
     * @param title
     * @param body
     * @return
     */
    public static ValidationItem generateInvalidFrame(String title, String body) {
        return new ValidationItem(title, body, false, false, true);

    }
    /**
     * Generates a new invalid item
     * For our purposes a group can be either a chapter or a project
     * @param title
     * @return
     */
    public static ValidationItem generateInvalidGroup(String title) {
        return new ValidationItem(title, "", false, false, false);
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
}
