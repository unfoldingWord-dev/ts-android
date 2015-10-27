package com.door43.translationstudio.core;

/**
 * Represents a translation of a frame
 */
public class FrameTranslation {
    public final String body;
    private final String mChapterId;
    private final boolean mFinished;
    private TranslationFormat mFormat;
    private int[] mVerses;
    private String mTitle;
    private String mId;

    public FrameTranslation(String frameId, String chapterId, String body, TranslationFormat format, boolean finished) {
        mChapterId = chapterId;
        this.body = body;
        mFormat = format;
        mId = frameId;
        mFinished = finished;
    }

    /**
     * Returns the title of the frame translation
     * @return
     */
    public String getTitle() {
        if(mFormat == TranslationFormat.USX) {
            // get verse range
            mVerses = Frame.getVerseRange(body);
            if(mVerses.length == 1) {
                mTitle = mVerses[0] + "";
            } else if(mVerses.length == 2) {
                mTitle = mVerses[0] + "-" + mVerses[1];
            } else {
                mTitle = Integer.parseInt(mId) + "";
            }
            return mTitle;
        } else {
            return Integer.parseInt(mId) + "";
        }
    }

    /**
     * Returns the frame id
     * @return
     */
    public String getId() {
        return mId;
    }

    /**
     * Returns the complex chapter-frame id
     * @return
     */
    public String getComplexId() {
        return mChapterId + "-" + mId;
    }

    /**
     * Returns the id of the chapter to which this frame belongs
     * @return
     */
    public String getChapterId() {
        return mChapterId;
    }

    /**
     * Returns the format of the text
     * @return
     */
    public TranslationFormat getFormat() {
        return mFormat;
    }

    /**
     * Checks if the translation is finished
     * @return
     */
    public boolean isFinished() {
        return mFinished;
    }

}
