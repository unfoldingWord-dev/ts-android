package com.door43.translationstudio.core;

import android.support.v7.internal.widget.DialogTitle;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by joel on 9/5/2015.
 */
public class Chapter {
    private final String mId;
    public final String reference;
    public final String title;

    /**
     *
     * @param title
     * @param reference
     * @param chapterId
     */
    protected Chapter(String title, String reference, String chapterId) {
        this.title = title;
        this.reference = reference;
        mId = chapterId;
    }

    /**
     * Returns the chapter id
     * @return
     */
    public String getId() {
        return mId;
    }

    /**
     * Generates an instance of a chapter from json
     * @param chapter
     * @return
     */
    public static Chapter generate(JSONObject chapter) {
        try {
            return new Chapter(chapter.getString("title"), chapter.getString("ref"), chapter.getString("number"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
