package com.door43.translationstudio.projects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chapters encapsulate a specific set of translation Frames regardless of language. Chapters mostly act to organize the translation effort into sections for better navigation
 */
public class Chapter {
    // so we can look up by index
    private List<Frame> mFrames = new ArrayList<Frame>();
    // so we can look up by id
    private Map<String, Frame> mFrameMap = new HashMap<String, Frame>();

    private String mId;
    private String mTitle;
    private String mDescription;
    private String mSelectedFrameId;

    /**
     * Create a new chapter
     * @param id the chapter id. This is effectively the chapter number.
     * @param title the human readable title of the chapter
     * @param description a short description of the chapter
     */
    public Chapter(String id, String title, String description) {
        mId = id;
        mTitle = title;
        mDescription = description;
    }

    /**
     * Returns the chapter id
     * @return
     */
    public String getId() {
        return mId;
    }

    /**
     * Returns the chapter title
     * @return
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Returns a description of the chapter
     * @return
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     * Returns the number of frames in this chapter
     * @return
     */
    public int numFrames() {
        return mFrameMap.size();
    }

    /**
     * Returns a frame by id
     * @param id the frame id
     * @return null if the frame does not exist
     */
    public Frame getFrame(String id) {
        if(mFrameMap.containsKey(id)) {
            return mFrameMap.get(id);
        } else {
            return null;
        }
    }

    /**
     * Returns a frame by index
     * @param index the frame index
     * @return null if the frame does not exist
     */
    public Frame getFrame(int index) {
        if(index < mFrames.size() && index >= 0) {
            return mFrames.get(index);
        } else {
            return null;
        }
    }

    /**
     * Returns the index of the given frame
     * @param f
     * @return
     */
    public int getFrameIndex(Frame f) {
        return mFrames.indexOf(f);
    }

    /**
     * Sets the currently selected frame in the chapter by id
     * @param id the frame id
     * @return true if the frame exists
     */
    public boolean setSelectedFrame(String id) {
        Frame f = getFrame(id);
        if(f != null) {
            mSelectedFrameId = f.getId();
        }
        return f != null;
    }

    /**
     * Sets the currently selected frame in the chapter by index
     * @param index the frame index
     * @return true if the frame exists
     */
    public boolean setSelectedFrame(int index) {
        Frame f = getFrame(index);
        if(f != null) {
            mSelectedFrameId = f.getId();
        }
        return f != null;
    }

    /**
     * Returns the currently selected frame in the chapter
     * @return
     */
    public Frame getSelectedFrame() {
        Frame selectedFrame = getFrame(mSelectedFrameId);;
        if(selectedFrame == null) {
            // auto select the first frame if no other frame has been selected
            int defaultFrameIndex = 0;
            setSelectedFrame(defaultFrameIndex);
            return getFrame(defaultFrameIndex);
        } else {
            return selectedFrame;
        }
    }

    /**
     * Add a frame to the chapter
     * @param f the frame to add
     */
    public void addFrame(Frame f) {
        if(!mFrameMap.containsKey(f.getId())) {
            mFrameMap.put(f.getId(), f);
            mFrames.add(f);
        }
    }

    /**
     * Returns the path to the image for the chapter
     * Uses the image from the first frame if available
     * @return
     */
    public String getImagePath() {
        if(getFrame(0) != null) {
            return getFrame(0).getImagePath();
        } else {
            return null;
        }
    }

    /**
     * Returns the next ordered frame
     * @return
     */
    public Frame getNextFrame() {
        int index = mFrames.indexOf(getSelectedFrame()) + 1;
        return getFrame(index);
    }

    public Frame getPreviousFrame() {
        int index = mFrames.indexOf(getSelectedFrame()) - 1;
        return getFrame(index);
    }
}
