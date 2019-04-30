package com.door43.util;

import android.util.SparseArray;

import com.door43.translationstudio.ui.spannables.USFMVerseSpan;

import org.unfoldingword.door43client.models.ChunkMarker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides utilities for parsing usfm.
 */
public class Usfm {
    public static final Pattern VERSE_PATTERN = Pattern.compile(USFMVerseSpan.PATTERN);
    public static final Pattern CHAPTER_PATTERN = Pattern.compile("\\\\c\\s(\\d+(-\\d+)?)\\s");
    private Usfm() {
    }

    /**
     * Parses a usfm string
     * @param usfm
     * @return
     */
    public static Map<Integer, Map> parseBook(String usfm) {
        Map<Integer, Map> chapters = new HashMap<>();

        // clean line endings
        usfm = usfm.replaceAll("\r\n", "\n");

        Matcher matcher = CHAPTER_PATTERN.matcher(usfm);
        int chapterStrIndex = 0;
        int chapter = 0;
        while (matcher.find()) {
            if (chapter > 0) {
                CharSequence chapterUsfm = usfm.subSequence(chapterStrIndex, matcher.start());
                chapters.put(chapter, parseChapter(chapterUsfm.toString()));
            } else {
                CharSequence headingUsfm = usfm.subSequence(0, matcher.start());
                chapters.put(0, parseHeader(headingUsfm.toString()));
            }
            chapter = Integer.valueOf(matcher.group(1));
            chapterStrIndex = matcher.end();
        }
        // get the last chapter
        if(chapter > 0) {
            CharSequence chapterUsfm = usfm.subSequence(chapterStrIndex, usfm.length() - 1);
            chapters.put(chapter, parseChapter(chapterUsfm.toString()));
        }
        return chapters;
    }

    /**
     * Parses the book header
     * @param usfm
     * @return
     */
    private static Map<Integer, String> parseHeader(String usfm) {
        Map<Integer, String> headings = new HashMap<>();
        return headings;
    }

    /**
     * Parses the chapter into a sparse array of verses.
     * Chapter intros are stored in index 0.
     * @param usfm
     * @return
     */
    private static Map<Integer, String> parseChapter(String usfm) {
        Map<Integer, String> verses = new HashMap<>();
        Matcher matcher = VERSE_PATTERN.matcher(usfm);
        int verseStrIndex = 0;
        int verse = 0;
        while(matcher.find()) {
            if(verse > 0) {
                CharSequence verseUsfm = usfm.subSequence(verseStrIndex, matcher.start());
                verses.put(verse, verseUsfm.toString());
            } else {
                // save the chapter intro
                CharSequence introUsfm = usfm.subSequence(0, matcher.start());
                verses.put(0, introUsfm.toString());
            }

            verse = Integer.valueOf(matcher.group(1));
            verseStrIndex = matcher.end();
        }
        // get last verse range
        if(verse > 0) {
            CharSequence verseUsfm = usfm.subSequence(verseStrIndex, usfm.length() - 1);
            verses.put(verse, verseUsfm.toString());
        }

        return verses;
    }
}
