package com.door43.translationstudio.core;

import com.door43.translationstudio.spannables.VerseSpan;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by joel on 8/26/2015.
 */
public class Frame {

    public final String body;
    private final String mId;
    private final TranslationFormat mFormat;
    private final String mChapterId;
    private String mTitle;
    private int[] mVerses;

    private Frame(String frameId, String chapterId, String body, TranslationFormat format) {
        mChapterId = chapterId;
        this.body = body;
        mFormat = format;
        mId = frameId;
    }

    /**
     * Returns the frame id
     * @return
     */
    public String getId() {
        return mId;
    }

    /**
     * Returns the id of the chapter to which this frame belongs
     * @return
     */
    public String getChapterId() {
        return mChapterId;
    }

    /**
     * Returns the format of the text
     * @return
     */
    public TranslationFormat getFormat() {
        return mFormat;
    }

    /**
     * Generates a new frame from json
     *
     * Note: we receive the chapter id rather than parsing it from the frame id to keep things future proof
     *
     * @param chapterId
     * @param frame
     * @return
     */
    public static Frame generate(String chapterId, JSONObject frame) {
        try {
            TranslationFormat format = TranslationFormat.DEFAULT;
            if(frame.has("format")) {
                format = TranslationFormat.get(frame.getString("format"));
            }
            String[] complexId = frame.getString("id").split("-");
            String frameId;
            if(complexId.length > 1) {
                frameId = complexId[1];
            } else {
                // future proof
                frameId = complexId[0];
            }
            return new Frame(
                    frameId,
                    chapterId,
                    frame.getString("text"),
                    format);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns the title of the chapter
     * @return
     */
    public String getTitle() {
        if(mFormat == TranslationFormat.USX) {
            // get verse range
            mVerses = getVerseRange(body);
            if(mVerses.length == 1) {
                mTitle = mVerses[0] + "";
            } else if(mVerses.length == 2) {
                mTitle = mVerses[0] + "-" + mVerses[1];
            } else {
                mTitle = Integer.parseInt(mId) + "";
            }
            return mTitle;
        } else {
            return Integer.parseInt(mId) + "";
        }
    }

    /**
     * Returns the range of verses that the body spans
     * @return
     */
    public int[] getVerseRange() {
        return getVerseRange(body);
    }

    /**
     * Returns the range of verses that a chunk of text spans
     *
     * @param text
     * @return new int[0] if no verses, new int[1] if one verse, new int[2] if a range of verses
     */
    public static int[] getVerseRange(CharSequence text) {
        // locate verse range
        Pattern pattern = Pattern.compile(VerseSpan.PATTERN);
        Matcher matcher = pattern.matcher(text);
        int numVerses = 0;
        int startVerse = 0;
        int endVerse = 0;
        VerseSpan verse = null;
        while(matcher.find()) {
            verse = new VerseSpan(matcher.group(1));

            if(numVerses == 0) {
                // first verse
                startVerse = verse.getStartVerseNumber();
                endVerse = verse.getEndVerseNumber();
            }
            numVerses ++;
        }
        if(verse != null) {
            if(verse.getEndVerseNumber() > 0) {
                endVerse = verse.getEndVerseNumber();
            } else {
                endVerse = verse.getStartVerseNumber();
            }
        }
        if(startVerse <= 0 || endVerse <= 0) {
            // no verse range
            return new int[0];
        } else if(startVerse == endVerse) {
            // single verse
            return new int[]{startVerse};
        } else {
            // verse range
            return new int[]{startVerse, endVerse};
        }
    }
}
