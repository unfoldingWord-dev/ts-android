package com.door43.translationstudio.projects;

import com.door43.translationstudio.util.FileUtilities;

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
    private String mReference;
    private String mSelectedFrameId;
    private Project mProject;
    private Translation mTitleTranslation;
    private Translation mReferenceTranslation;

    /**
     * Create a new chapter
     * @param id the chapter id. This is effectively the chapter number.
     * @param title the human readable title of the chapter
     * @param reference a short description of the chapter
     */
    public Chapter(String id, String title, String reference) {
        mId = id;
        mTitle = title;
        mReference = reference;
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
     * Returns the chapter reference
     * @return
     */
    public String getReference() {
        return mReference;
    }

    /**
     * Stores the translated chapter title
     * @param translation
     */
    public void setTitleTranslation(String translation) {
        setTitleTranslation(translation, mProject.getSelectedTargetLanguage());
    }

    /**
     * Stores the translated title text.
     * Important! you should almost always use setTitleTranslation(translation) instead.
     * Only use this if you know what you are doing.
     * @param translation the text to store as a translation
     * @param targetLanguage the target language the translation was made in.
     */
    public void setTitleTranslation(String translation, Language targetLanguage) {
        if(mTitleTranslation != null && !mTitleTranslation.isLanguage(targetLanguage) && !mTitleTranslation.isSaved()) {
            // save pending changes first
            save();
        }
        mTitleTranslation = new Translation(targetLanguage, translation);
        mProject.setIsTranslating(true);
    }

    /**
     * Returns this chapter's title translation
     * @return
     */
    public Translation getTitleTranslation() {
        if(mTitleTranslation == null || !mTitleTranslation.isLanguage(mProject.getSelectedTargetLanguage())) {
            if(mTitleTranslation != null) {
                save();
            }
            // load translation from disk
            String path = Project.getRepositoryPath(mProject.getId(), mProject.getSelectedTargetLanguage().getId()) + getId() + "/" + TITLE_FILE;
            try {
                String text = FileUtilities.getStringFromFile(path);
                mTitleTranslation = new Translation(mProject.getSelectedTargetLanguage(), text);
                mTitleTranslation.isSaved(true);
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
        setReferenceTranslation(translation, mProject.getSelectedTargetLanguage());
    }

    /**
     * Stores the translated chapter reference
     * Important! you should almost always use setReferenceTranslation(translation) instead.
     * Only use this method if you know what you are doing.
     * @param translation the text to store as the translation
     * @param targetLanguage the language the translation was made in.
     */
    public void setReferenceTranslation(String translation, Language targetLanguage) {
        if(mReferenceTranslation != null && !mReferenceTranslation.isLanguage(targetLanguage) && !mReferenceTranslation.isSaved()) {
            // save pending changes first
            save();
        }
        mReferenceTranslation = new Translation(targetLanguage, translation);
        mProject.setIsTranslating(true);
    }

    /**
     * Returns this chapter's reference translation
     * @return
     */
    public Translation getReferenceTranslation() {
        if(mReferenceTranslation == null || !mReferenceTranslation.isLanguage(mProject.getSelectedTargetLanguage())) {
            if(mReferenceTranslation != null) {
                save();
            }
            // load translation from disk
            String path = Project.getRepositoryPath(mProject.getId(), mProject.getSelectedTargetLanguage().getId()) + getId() + "/" + REFERENCE_FILE;
            try {
                String text = FileUtilities.getStringFromFile(path);
                mReferenceTranslation = new Translation(mProject.getSelectedTargetLanguage(), text);
                mReferenceTranslation.isSaved(true);
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
        return mReference;
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
        // TODO: the universal image loader does not support multiple callbacks on a single image so the first frame doesn't load it's image on startup.
        // as a work around chapters load a copy of the first frame's image.
//        if(getFrame(0) != null) {
//            return getFrame(0).getImagePath();
//        } else {
//            return null;
//        }
        return "sourceTranslations/"+getProject().getId()+"/en/images/"+getProject().getId()+"-"+getId()+"-00.jpg";
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
     * Saves the reference and title to the disk
     */
    public void save() {
        if(mReferenceTranslation != null && mTitleTranslation != null) {
            String referencePath = Project.getRepositoryPath(mProject.getId(), mReferenceTranslation.getLanguage().getId()) + getId() + "/" + REFERENCE_FILE;
            String titlePath = Project.getRepositoryPath(mProject.getId(), mTitleTranslation.getLanguage().getId()) + getId() + "/" + TITLE_FILE;

            // save reference
            if(!mReferenceTranslation.isSaved()) {
                mReferenceTranslation.isSaved(true);
                File refFile = new File(referencePath);
                if(mReferenceTranslation.getText().isEmpty()) {
                    // delete empty file
                    refFile.delete();
                } else {
                    // write translation
                    if(!refFile.exists()) {
                        refFile.getParentFile().mkdirs();
                    }
                    try {
                        refFile.createNewFile();
                        PrintStream ps = new PrintStream(refFile);
                        ps.print(mReferenceTranslation.getText());
                        ps.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            // save title
            if(!mTitleTranslation.isSaved()) {
                mTitleTranslation.isSaved(true);
                File titleFile = new File(titlePath);
                if(mTitleTranslation.getText().isEmpty()) {
                    // delete empty file
                    titleFile.delete();
                } else {
                    // write translation
                    if(!titleFile.exists()) {
                        titleFile.getParentFile().mkdirs();
                    }
                    try {
                        titleFile.createNewFile();
                        PrintStream ps = new PrintStream(titleFile);
                        ps.print(mTitleTranslation.getText());
                        ps.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Checks if the chapter has started being translated
     * @return
     */
    public boolean translationInProgress() {
        return !getReferenceTranslation().getText().isEmpty() || !getTitleTranslation().getText().isEmpty();
    }
}
