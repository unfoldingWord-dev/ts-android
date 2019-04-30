package com.door43.util;

import com.door43.translationstudio.ui.spannables.USFMVerseSpan;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides utilities for parsing usfm.
 */
public class Usfm {
    private static final Pattern VERSE_PATTERN = Pattern.compile(USFMVerseSpan.PATTERN);
    private static final Pattern CHAPTER_PATTERN = Pattern.compile("\\\\c\\s(\\d+(-\\d+)?)\\s");
    private static final Pattern BOOK_TITLE_PATTERN = Pattern.compile("\\\\toc2\\s([^\\n]*)");
    private static final Pattern USFM_TAG_PATTERN = Pattern.compile("\\\\([\\w\\d]+)\\s([^\\n\\\\]*)");

    private Usfm() {
    }

    /**
     * Parses a usfm string
     *
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
        if (chapter > 0) {
            CharSequence chapterUsfm = usfm.subSequence(chapterStrIndex, usfm.length());
            chapters.put(chapter, parseChapter(chapterUsfm.toString()));
        }
        return chapters;
    }

    /**
     * Parses the book header
     *
     * @param usfm
     * @return
     */
    private static Map<String, String> parseHeader(String usfm) {
        Map<String, String> headings = new HashMap<>();
        Matcher matcher = BOOK_TITLE_PATTERN.matcher(usfm);
        if (matcher.find()) {
            String[] markers = new String[]{"c", "id", "ide", "h", "toc1", "toc2", "toc3", "mt", "p", "s5"};
            String intro = stripMarkup(usfm, markers);
            if (!intro.isEmpty()) {
                headings.put("intro", intro.trim());
            }
            headings.put("title", matcher.group(1).trim());
        }
        return headings;
    }

    /**
     * Parses the chapter into a map of verses.
     * Chapter intros are stored in index 0.
     *
     * @param usfm
     * @return
     */
    private static Map<Integer, String> parseChapter(String usfm) {
        Map<Integer, String> verses = new HashMap<>();
        usfm = stripMarkup(usfm, new String[]{"s5"});
        Matcher matcher = VERSE_PATTERN.matcher(usfm);
        int verseStrIndex = 0;
        int verse = 0;
        while (matcher.find()) {
            if (verse > 0) {
                CharSequence verseUsfm = usfm.subSequence(verseStrIndex, matcher.start());
                verses.put(verse, verseUsfm.toString().trim());
            } else {
                // save the chapter intro
                CharSequence introUsfm = usfm.subSequence(0, matcher.start());
                // TRICKY: don't include the intro if it's just some tags
                String cleanedIntro = stripMarkup(introUsfm.toString(), new String[]{"p"});
                if(!cleanedIntro.isEmpty()) {
                    verses.put(0, introUsfm.toString().trim());
                }
            }

            verse = Integer.valueOf(matcher.group(1));
            verseStrIndex = matcher.end();
        }
        // get last verse range
        if (verse > 0) {
            CharSequence verseUsfm = usfm.subSequence(verseStrIndex, usfm.length());
            verses.put(verse, verseUsfm.toString().trim());
        }

        return verses;
    }

    /**
     * Removes usfm markup tags from the text.
     *
     * @param usfm
     * @param tags an array of tags to be removed
     * @return
     */
    private static String stripMarkup(String usfm, String[] tags) {
        Matcher matcher = USFM_TAG_PATTERN.matcher(usfm);
        StringBuilder cleaned = new StringBuilder();
        int lastStrIndex = 0;
        while (matcher.find()) {
            CharSequence marker = matcher.group(1);

            for (String tag : tags) {
                if (marker.equals(tag)) {
                    CharSequence precedingText = usfm.subSequence(lastStrIndex, matcher.start());
                    cleaned.append(precedingText);
                    lastStrIndex = matcher.end();
                }
            }
        }
        if (lastStrIndex < usfm.length()) {
            cleaned.append(usfm.subSequence(lastStrIndex, usfm.length()));
        }
        return cleaned.toString().replaceAll("\n+", "\n").trim();
    }
}
