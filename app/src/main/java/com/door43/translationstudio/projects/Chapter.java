package com.door43.translationstudio.projects;

import android.util.Log;

import com.door43.translationstudio.R;
import com.door43.translationstudio.util.FileUtilities;
import com.door43.translationstudio.util.MainContextLink;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chapters encapsulate a specific set of translation Frames regardless of language. Chapters mostly act to organize the translation effort into sections for better navigation
 */
public class Chapter {
    private static final String REFERENCE_FILE = "reference.txt";
    private static final String TITLE_FILE = "title.txt";
    // so we can look up by index
    private List<Frame> mFrames = new ArrayList<Frame>();
    // so we can look up by id
    private Map<String, Frame> mFrameMap = new HashMap<String, Frame>();

    private String mId;
    private String mTitle;
    private String mDescription;
    private String mSelectedFrameId;
    private Project mProject;
    private Translation mTitleTranslation;
    private Translation mReferenceTranslation;

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
     * Specifies the project this chapter belongs to.
     * This can only be set once.
     * @param project
     */
    public void setProject(Project project){
        if(mProject == null) mProject = project;
    }

    /**
     * Returns the project this chapter belongs to
     * @return
     */
    public Project getProject() {
        return mProject;
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
     * Stores the translated chapter title
     * @param translation
     */
    public void setTitleTranslation(String translation) {
        if(mTitleTranslation != null && mTitleTranslation.getLanguage().getId() != mProject.getSelectedLanguage().getId() && !mTitleTranslation.isSaved()) {
            // save pending changes first
            save();
        }
        mTitleTranslation = new Translation(mProject.getSelectedLanguage(), translation);
    }

    /**
     * Returns this chapter's title translation
     * @return
     */
    public Translation getTitleTranslation() {
        if(mTitleTranslation == null || mTitleTranslation.getLanguage().getId() != mProject.getSelectedLanguage().getId()) {
            // init translation
            if(mTitleTranslation == null) {
                mTitleTranslation = new Translation(mProject.getSelectedLanguage(), "");
            }
            // load translation from disk
            String path = mProject.getRepositoryPath(mTitleTranslation.getLanguage()) + getId() + "/" + TITLE_FILE;
            try {
                String text = FileUtilities.getStringFromFile(path);
                mTitleTranslation = new Translation(mProject.getSelectedLanguage(), text);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return mTitleTranslation;
    }

    /**
     * Stores the translated chapter reference
     * @param translation
     */
    public void setReferenceTranslation(String translation) {
        if(mReferenceTranslation != null && mReferenceTranslation.getLanguage().getId() != mProject.getSelectedLanguage().getId() && !mReferenceTranslation.isSaved()) {
            // save pending changes first
            save();
        }
        mReferenceTranslation = new Translation(mProject.getSelectedLanguage(), translation);
    }

    /**
     * Returns this chapter's reference translation
     * @return
     */
    public Translation getReferenceTranslation() {
        if(mReferenceTranslation == null || mReferenceTranslation.getLanguage().getId() != mProject.getSelectedLanguage().getId()) {
            // init translation
            if(mReferenceTranslation == null) {
                mReferenceTranslation = new Translation(mProject.getSelectedLanguage(), "");
            }
            // load translation from disk
            String path = mProject.getRepositoryPath(mReferenceTranslation.getLanguage()) + getId() + "/" + REFERENCE_FILE;
            try {
                String text = FileUtilities.getStringFromFile(path);
                mReferenceTranslation = new Translation(mProject.getSelectedLanguage(), text);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return mReferenceTranslation;
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
        Frame selectedFrame = getFrame(mSelectedFrameId);
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
            f.setChapter(this);
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

    /**
     * Saves the reference and title
     */
    public void save() {
        String referencePath = mProject.getRepositoryPath(getReferenceTranslation().getLanguage()) + mId + "/" + REFERENCE_FILE;
        String titlePath = mProject.getRepositoryPath(getTitleTranslation().getLanguage()) + mId + "/" + TITLE_FILE;

        // save reference
        if(!getReferenceTranslation().isSaved()) {
            getReferenceTranslation().isSaved(true);
            File refFile = new File(referencePath);
            if(!refFile.exists()) {
                refFile.getParentFile().mkdirs();
            }
            try {
                refFile.createNewFile();
                PrintStream ps = new PrintStream(refFile);
                ps.print(getReferenceTranslation().getText());
                ps.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // save title
        if(!getTitleTranslation().isSaved()) {
            getTitleTranslation().isSaved(true);
            File titleFile = new File(titlePath);
            if(!titleFile.exists()) {
                titleFile.getParentFile().mkdirs();
            }
            try {
                titleFile.createNewFile();
                PrintStream ps = new PrintStream(titleFile);
                ps.print(getTitleTranslation().getText());
                ps.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
