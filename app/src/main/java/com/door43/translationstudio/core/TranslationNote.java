package com.door43.translationstudio.core;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a translation note
 */
public class TranslationNote {
    private final String mChapterId;
    private final String mFrameId;
    private final String mId;
    private final String mRef;
    private final String mText;

    private TranslationNote(String chapterId, String frameId, String noteId, String ref, String text) {
        mChapterId = chapterId;
        mFrameId = frameId;
        mId = noteId;
        mRef = ref;
        mText = text;
    }

    /**
     * Generates a new note from json
     * @param chapterId
     * @param frameId
     * @param json
     * @return
     */
    public static TranslationNote generate(String chapterId, String frameId, JSONObject json) {
        try {
            return new TranslationNote(
                    chapterId,
                    frameId,
                    json.getString("id"),
                    json.getString("ref"),
                    json.getString("text"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns the id of this note
     * @return
     */
    public String getId() {
        return mId;
    }

    /**
     * Returns the id of the frame in which this note exists
     * @return
     */
    public String getFrameId() {
        return mFrameId;
    }

    /**
     * Returns the id of the chapter in which this note exists
     * @return
     */
    public String getChapterId() {
        return mChapterId;
    }

    /**
     * Returns the title of the translation note
     * @return
     */
    public String getTitle() {
        return mRef;
    }

    /**
     * Returns the body of the translation note
     * @return
     */
    public String getBody() {
        return mText;
    }
}
