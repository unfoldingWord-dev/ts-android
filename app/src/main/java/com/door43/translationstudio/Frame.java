package com.door43.translationstudio;

import java.util.ArrayList;

/**
 * Created by joel on 9/3/2014.
 */
public class Frame {
    private String mId;
    private String mDescription;

    /**
     * Creates a new frame.
     * @param id the frame id. This is a combination of the chapter id and frame id. e.g. 01-02 for chapter 1 frame 2.
     * @param description a short description of the frame
     */
    public Frame(String id, String description) {
        mId = id;
        mDescription = description;
    }

    /**
     * Get the frame description
     * @return
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     * Returns the frame id
     * @return
     */
    public String getID() {
        return mId;
    }
}
