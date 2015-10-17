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
    public final String imageUrl;
    private String mTitle;
    private int[] mVerses = null;

    public Frame(String frameId, String chapterId, String body, TranslationFormat format, String imageUrl) {
        mChapterId = chapterId;
        this.body = body;
        mFormat = format;
        mId = frameId;
        this.imageUrl = imageUrl;
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
     * Generates a dummy frame with just the chapter id
     * @param chapterId
     * @return
     */
    public static Frame generateDummy(String chapterId) {
        return new Frame(null, chapterId, "", TranslationFormat.DEFAULT, "");
    }

    /**
     * Generates a new frame from json
     *
     * Note: we receive the chapter id rather than parsing it from the frame id to keep things future proof
     *
     * @param chapterId
     * @param json
     * @return
     */
    public static Frame generate(String chapterId, JSONObject json) throws JSONException {
        if(json == null) {
            return null;
        }
        TranslationFormat format = TranslationFormat.DEFAULT;
        if(json.has("format")) {
            format = TranslationFormat.get(json.getString("format"));
        }
        String img = "";
        if(json.has("img")) {
            img = json.getString("img");
        }
        String[] complexId = json.getString("id").split("-");
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
                json.getString("text"),
                format,
                img);
    }

    /**
     * Returns the title of the frame
     * @return
     */
    public String getTitle() {
        if(mFormat == TranslationFormat.USX) {
            // get verse range
            int[] verses = getVerseRange();
            if(verses.length == 1) {
                mTitle = verses[0] + "";
            } else if(verses.length == 2) {
                mTitle = verses[0] + "-" + verses[1];
            } else {
                mTitle = Integer.parseInt(mId) + "";
            }
            return mTitle;
        } else {
            return Integer.parseInt(mId) + "";
        }
    }

    /**
     * Returns the formatted beginning verse in this frame.
     * @return
     */
    public String getStartVerse() {
        if(mFormat == TranslationFormat.USX) {
            // get verse range
            int[] verses = getVerseRange();
            if(verses.length > 0) {
                return verses[0] + "";
            }
        }
        return Integer.parseInt(mId) + "";
    }

    /**
     * Returns the formatted ending verse for this frame.
     * @return
     */
    public String getEndVerse() {
        if(mFormat == TranslationFormat.USX) {
            // get verse range
            int[] verses = getVerseRange();
            if(verses.length == 1) {
                return verses[0] + "";
            } else if(verses.length == 2) {
                return verses[1] + "";
            }
        }
        return Integer.parseInt(mId) + "";
    }

    /**
     * Returns the range of verses that the body spans
     * @return int[0] if no verses, int[1] if one verse, int[2] if a range of verses
     */
    public int[] getVerseRange() {
        if(mVerses == null) {
            mVerses = getVerseRange(body);
        }
        return mVerses;
    }

    /**
     * Returns the range of verses that a chunk of text spans
     *
     * @param text
     * @return int[0] if no verses, int[1] if one verse, int[2] if a range of verses
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

    /**
     * Returns the complex chapter-frame id
     * @return
     */
    public String getComplexId() {
        return mChapterId + "-" + mId;
    }
}
