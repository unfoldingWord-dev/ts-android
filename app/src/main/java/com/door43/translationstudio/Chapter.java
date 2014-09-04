package com.door43.translationstudio;

import java.util.ArrayList;

/**
 * Created by joel on 9/2/2014.
 */
public class Chapter {

    private ArrayList<Frame> mFrames = new ArrayList<Frame>();
    private int mId;
    private String mTitle;
    private String mDescription;
    private int mSelectedFrame;

    /**
     * Creates a new chapter.
     * @param id the chapter id. This is effectively the chapter number.
     * @param title the human readable title of the chapter
     * @param description a short description of the chapter
     */
    public Chapter(int id, String title, String description) {
        mId = id;
        mTitle = title;
        mDescription = description;
    }

    /**
     * Get the chapter id. This is effectively the chapter number.
     * @return
     */
    public int getId() {
        return mId;
    }

    /**
     * Get the chapter title
     * @return
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Get the chapter description
     * @return
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     * Sets the currently selected frame in the application
     * @param i
     * @return boolean return true of the index is valid
     */
    public boolean setSelectedFrame(int i) {
        if (mFrames.size() > i && i >= 0) {
            mSelectedFrame = i;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Gets a frame by index
     * @param i the index of the frame
     * @return
     */
    public Frame getFrame(int i) {
        if(mFrames.size() > i && i >= 0) {
            return mFrames.get(i);
        } else {
            // out of bounds
            return null;
        }
    }

    /**
     * Returns the number of frames in this chapter
     * @return
     */
    public int numFrames() {
        return mFrames.size();
    }

    /**
     * Returns the currently selected frame in the application
     * @return
     */
    public Frame getSelectedFrame() {
        return getFrame(mSelectedFrame);
    }
}
