package com.door43.translationstudio.projects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by joel on 9/2/2014.
 */
public class Chapter {

    private Map<String, Frame> mFrames = new HashMap<String, Frame>();
    private ArrayList<String> mFrameIndex = new ArrayList<String>();
    private Integer mId;
    private String mTitle;
    private String mDescription;
    private String mSelectedFrame;

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
    public Integer getId() {
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
     * @param id
     * @return boolean return true of the index is valid
     */
    public boolean setSelectedFrame(String id) {
        if (mFrames.containsKey(id)) {
            mSelectedFrame = id;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Gets a frame by index
     * @param id the id of the frame
     * @return
     */
    public Frame getFrame(String id) {
        if(mFrames.containsKey(id)) {
            return mFrames.get(id);
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
        Frame selectedFrame = getFrame(mSelectedFrame);;
        if(selectedFrame == null) {
            // atuo select the first project if no other project has been selected yet.
            String key = (String) getFramesKeySet().get(0);
            setSelectedFrame(key);
            return getFrame(key);
        } else {
            return selectedFrame;
        }
    }

    /**
     * Add a new frame to the chapter
     * @param f the frame to add
     */
    public Frame addFrame(Frame f) {
        if(!this.mFrames.containsKey(f.getFrameId())) {
            mFrameIndex.add(f.getFrameId());
            this.mFrames.put(f.getFrameId(), f);
            return f;
        } else {
            // TODO: is this nessesary? need to double check that the object signatures are different. If they are the same we should just always return the input frame.
            return getFrame(f.getFrameId());
        }
    }

    /**
     * Returns a keyset of frame keys so list adapters can use indexes to identify frames.
     * @return
     */
    public List getFramesKeySet() {
        return mFrameIndex;
    }
}
