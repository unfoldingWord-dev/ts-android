package com.door43.translationstudio.projects;

import android.util.Log;

/**
 * Frames encapsulates a specific piece of translated work
 */
public class Frame {
    private String mChapterFrameId;
    private String mText;
    private String mId;
    private String mChapterId;

    /**
     * Creates a new frame.
     * @param chapterFrameId This is a combination of the chapter id and frame id. e.g. 01-02 for chapter 1 frame 2.
     * @param text a short description of the frame
     */
    public Frame(String chapterFrameId, String text) {
        String[] pieces = chapterFrameId.split("-");
        if(pieces.length == 2) {
            mChapterId = pieces[0];
            mId = pieces[1];
        } else {
            Log.w("Frame", "The frame has an invalid id");
        }
        mChapterFrameId = chapterFrameId;
        mText = text;
    }

    /**
     * Returns the frame text
     * @return
     */
    public String getText() {
        return mText;
    }

    /**
     * Returns the chapter-frame id
     * @return
     */
    public String getChapterFrameId() {
        return mChapterFrameId;
    }

    /**
     * Returns the frame id
     * @return
     */
    public String getId() {
        return mId;
    }

    /**
     * Returns the chapter id
     * @return
     */
    public String getChapterId() {
        return mChapterId;
    }
}
