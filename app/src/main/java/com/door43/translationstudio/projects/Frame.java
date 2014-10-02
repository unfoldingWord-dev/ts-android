package com.door43.translationstudio.projects;

import android.util.Log;

import org.eclipse.jgit.util.StringUtils;

/**
 * Frames encapsulates a specific piece of translated work
 */
public class Frame {
    private String mChapterFrameId;
    private String mText;
    private String mId;
    private String mChapterId;
    private String mImagePath;

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
}
