package com.door43.translationstudio.projects;

import android.util.Log;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.R;
import com.door43.translationstudio.util.FileUtilities;
import com.door43.translationstudio.util.MainContextLink;

import org.eclipse.jgit.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Frames encapsulates a specific piece of translated work
 */
public class Frame {
    private String mChapterFrameId;
    private String mText;
    private String mId;
    private String mChapterId;
    private String mImagePath;
    private Chapter mChapter;
    private Translation mTranslation;

    /**
     * Creates a new frame.
     * @param chapterFrameId This is a combination of the chapter id and frame id. e.g. 01-02 for chapter 1 frame 2.
     * @param text a short description of the frame
     */
    public Frame(String chapterFrameId, String image, String text) {
        // clean image
        String imageRaw = image.replaceAll("\\}\\}$", "").replaceAll("^\\{\\{:", "");
        String[] imagePieces = imageRaw.split("\\?");
        if(imagePieces.length == 2) {
            imagePieces = imagePieces[0].split(":");
            if(imagePieces.length == 3) {
                String lang = imagePieces[0];
                String project = imagePieces[1];
                String file = imagePieces[2];
                mImagePath = "sourceTranslations/"+project+"/"+lang+"/images/"+file;
            }
        }

        // parse id
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
     * Stores the translated frame text
     * @param translation
     */
    public void setTranslation(String translation) {
        if(mTranslation != null && mTranslation.getLanguage().getId() != mChapter.getProject().getSelectedLanguage().getId() && !mTranslation.isSaved()) {
            save();
        }
        mTranslation = new Translation(mChapter.getProject().getSelectedLanguage(), translation);
    }

    /**
     * Returns this frames translation
     * @return
     */
    public Translation getTranslation() {
        if(mTranslation == null || mTranslation.getLanguage().getId() != mChapter.getProject().getSelectedLanguage().getId()) {
            // init translation
            if(mTranslation == null) {
                mTranslation = new Translation(mChapter.getProject().getSelectedLanguage(), "");
            }
            // load translation from disk
            String path = mChapter.getProject().getRepositoryPath(mTranslation.getLanguage()) + getChapterId() + "/" + getId() + ".txt";
            try {
                String text = FileUtilities.getStringFromFile(path);
                mTranslation = new Translation(mChapter.getProject().getSelectedLanguage(), text);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return mTranslation;
    }

    /**
     * Specifies the chapter this frame belongs to.
     * This can only be set once.
     * @param chapter
     */
    public void setChapter(Chapter chapter) {
        if(mChapter == null) mChapter = chapter;
    }

    /**
     * Returns the chapter this frame belongs to
     * @return
     */
    public Chapter getChapter() {
        return mChapter;
    }

    public String getImagePath() {
        return mImagePath;
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

    /**
     * Saves the frane trabskatuib
     */
    public void save() {
        if(!getTranslation().isSaved()) {
            getTranslation().isSaved(true);
            String path = mChapter.getProject().getRepositoryPath(getTranslation().getLanguage()) + getChapterId() + "/" + getId() + ".txt";

            // write the file
            File file = new File(path);

            // create folder structure
            if(!file.exists()) {
                file.getParentFile().mkdir();
            }

            // write translation
            try {
                file.createNewFile();
                PrintStream ps = new PrintStream(file);
                ps.print(getTranslation().getText());
                ps.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
