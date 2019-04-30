package com.door43.util.usfm;

import com.door43.translationstudio.ui.spannables.USFMVerseSpan;

import org.json.JSONException;
import org.unfoldingword.door43client.models.ChunkMarker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
     * Splits a usfm into chunks based on the chunk markers.
     *
     * @param usfm
     * @param markers
     */
    public static List<Chunk> chunkBook(String usfm, List<ChunkMarker> markers) throws JSONException {
        Map<Integer, Map> parsedUsfm = parseBook(usfm);
        List<Chunk> chunks = new ArrayList<>();

        // add front matter
        if(parsedUsfm.containsKey(0)) {
            if(parsedUsfm.get(0).containsKey("title")) {
                String content = parsedUsfm.get(0).get("title").toString().trim();
                chunks.add(new Chunk("front", "title", content));
            }
            if(parsedUsfm.get(0).containsKey("intro")) {
                String content = parsedUsfm.get(0).get("intro").toString().trim();
                chunks.add(new Chunk("front", "intro", content));
            }
        }

        // add chunks
        int index = 0;
        for (ChunkMarker marker : markers) {
            Boolean lastChunkOfChapter = index + 1 >= markers.size() || !markers.get(index + 1).chapter.equals(marker.chapter);
            int chapter = Integer.valueOf(marker.chapter);
            int firstVerse = Integer.valueOf(marker.verse);
            int lastVerse = lastChunkOfChapter ? Integer.MAX_VALUE : Integer.parseInt(markers.get(index + 1).verse) - 1;

            if(parsedUsfm.containsKey(chapter)) {
                StringBuilder chunkContent = new StringBuilder();
                for(Integer i = firstVerse; i <= lastVerse; i ++) {
                    String verseText = (String)parsedUsfm.get(chapter).get(i);
                    if(verseText == null) break;
                    chunkContent.append("\\v ").append(i).append(" ");
                    chunkContent.append(verseText);
                    chunkContent.append("\n");
                }
                chunks.add(new Chunk(marker.chapter, marker.verse, chunkContent.toString().trim()));
            }

            index++;
        }

        return chunks;
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
                if (!cleanedIntro.isEmpty()) {
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

    /**
     * Converts usfm 3 to usfm 2.
     * @param usfm
     * @return
     */
    public static String convertUsfm3ToUsfm2(String usfm) {
        String usfm2 = usfm;
        // milestones
        usfm2 = usfm2.replaceAll("\\n?\\\\zaln-s.*\\n?", "");
        usfm2 = usfm2.replaceAll("\\n?\\\\zaln-e\\\\\\*\\n?", "");

        usfm2 = usfm2.replaceAll("\\n?\\\\ks-s.*\\n?", "");
        usfm2 = usfm2.replaceAll("\\n?\\\\ks-e\\\\\\*\\n?", "");

        // word data
        // remove empty word markers
        usfm2 = usfm2.replaceAll("\\\\w\\s*(\\|[^\\\\]*)?\\\\w\\*", "");
        // place words on their own lines so regex doesn't break
        usfm2 = usfm2.replaceAll("(\\\\w\\s+)", "\n$1");
        // remove words
        usfm2 = usfm2.replaceAll("\\\\w\\s+([^|\\\\]*).*\\\\w\\*", "$1");
        // group words onto single line
        usfm2 = usfm2.replaceAll("(\\n+)([^\\\\\\n +])", " $2");
        // stick text without markup on previous line
        usfm2 = usfm2.replaceAll("\\n^(?![\\\\])(.*)", " $1");

        // whitespace
        usfm2 = usfm2.replaceAll("^[ \\t]*", "");
        usfm2 = usfm2.replaceAll("[ \\t]*$", "");
        usfm2 = usfm2.replaceAll("^\\n{2,}", "\n\n");
        usfm2 = usfm2.replaceAll(" {2,}", " ");
        usfm2 = usfm2.replaceAll("\\n*(\\\\s5)\\s*", "\n\n$1\n");

        return usfm2;
    }
}

