package com.door43.translationstudio.projects;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by joel on 9/3/2014.
 */
public class Frame {
    private String mId; // this is a combination of the chapter id and frame id.
    private String mText;
    private String mFrameId;
    private String mChapterId;

    /**
     * Creates a new frame.
     * @param id the frame id. This is a combination of the chapter id and frame id. e.g. 01-02 for chapter 1 frame 2.
     * @param text a short description of the frame
     */
    public Frame(String id, String text) {
        String[] pieces = id.split("-");
        if(pieces.length == 2) {
            mChapterId = pieces[0];
            mFrameId = pieces[1];
        } else {
            Log.w("Frame", "The frame has an invalid id");
        }
        mId = id;
        mText = text;
    }

    /**
     * Get the frame description
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
        return mId;
    }

    /**
     * Returns the frame id
     * @return
     */
    public String getFrameId() {
        return mFrameId;
    }

    /**
     * Returns the chapter id
     * @return
     */
    public String getChapterId() {
        return mChapterId;
    }
}
