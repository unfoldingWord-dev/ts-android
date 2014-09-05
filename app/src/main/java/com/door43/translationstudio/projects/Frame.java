package com.door43.translationstudio.projects;

import java.util.ArrayList;

/**
 * Created by joel on 9/3/2014.
 */
public class Frame {
    private String mId;
    private String mText;

    /**
     * Creates a new frame.
     * @param id the frame id. This is a combination of the chapter id and frame id. e.g. 01-02 for chapter 1 frame 2.
     * @param text a short description of the frame
     */
    public Frame(String id, String text) {
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
     * Returns the frame id
     * @return
     */
    public String getID() {
        return mId;
    }
}
