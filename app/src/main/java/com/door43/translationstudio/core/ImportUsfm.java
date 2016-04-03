package com.door43.translationstudio.core;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.spannables.USFMVerseSpan;
import com.door43.util.Zip;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by blm on 4/2/16.
 */
public class ImportUsfm {
    public static final String TAG = ImportUsfm.class.getSimpleName();
    public static final String BOOK_NAME_MARKER = "\\\\toc1\\s([^\\n]*)";
    public static final String BOOK_SHORT_NAME_MARKER = "\\\\toc3\\s([^\\n]*)";
    public static final String SECTION_MARKER = "\\\\s5([^\\n]*)";
    public static final String CHAPTER_NUMBER_MARKER = "\\\\c\\s(\\d+(-\\d+)?)\\s";

    private File mTempDir;
    private File mTempDest;
    private File mTempSrce;
    private String mChapter;
    private List<File> mSourceFiles;
    private HashMap<String,JSONObject> mChunks;
    private List<String> mErrors;

    private String mBookName;
    private String mBookShortName;
    private JSONObject mChunk;


    public ImportUsfm() {
        createTempFolders();
        mSourceFiles = new ArrayList<>();
        mErrors = new ArrayList<>();
        mChunks = new HashMap<>();
    }

     public boolean importZipStream(InputStream usfmStream) {

        boolean success = true;
        mTempDir = null;
        try {
            Zip.unzipFromStream(usfmStream, mTempSrce);
            File[] usfmFiles = mTempSrce.listFiles();

            for (File usfmFile : usfmFiles) {
                addFilesInFolder(usfmFile);
            }
            Logger.i(TAG, "found files: " + mSourceFiles.size());

        } catch (Exception e) {
            Logger.e(TAG, "error reading stream ", e);
        }

        return success;
    }

    public boolean importFile(File usfm) {

        boolean success = true;
        mTempDir = null;

        createTempFolders();
        addFilesInFolder(usfm);
        Logger.i(TAG, "found files: " + mSourceFiles.size());
        return success;
    }

    public boolean addChunk(String book, JSONArray chunk) {
        try {
            JSONObject processedChunk = new JSONObject();
            int length = chunk.length();
            for(int i = 0; i < length; i++) {
                JSONObject item = (JSONObject) chunk.get(i);
                String chapter = item.getString("chp");
                String firstverse = item.getString("firstvs");

                JSONArray verses = null;
                if(processedChunk.has(chapter)) {
                    verses = processedChunk.getJSONArray(chapter);
                } else {
                    verses = new JSONArray();
                    processedChunk.put(chapter,verses);
                }
                verses.put(firstverse);
            }

            mChunks.put(book.toLowerCase(), processedChunk);
        } catch (Exception e) {
            Logger.e(TAG, "error parsing chunk " + book, e);
            return false;
        }
        return true;
    }

    public boolean addChunk(String book, String chunkStr) {
        try {
            JSONArray chunkJson = new JSONArray(chunkStr);
            return addChunk(book, chunkJson);
        } catch (Exception e) {
            Logger.e(TAG, "error parsing chunk " + book, e);
        }
        return false;
    }

    public String[] getErrors() {
        if( mErrors != null) {
            return mErrors.toArray(new String[mErrors.size()]);
        }
        return new String[0];
    }

    private void addError(String error) {
        mErrors.add("Error: " + error);
    }

    private void addWarning(String error) {
        mErrors.add("Error: " + error);
    }

    public boolean processBook(String book) {
        mBookName = extractString(book,BOOK_NAME_MARKER);
        mBookShortName = extractString(book,BOOK_SHORT_NAME_MARKER).toLowerCase();

        if(isEmpty(mBookShortName)) {
            addError("Missing book short name");
            return false;
        }

        if(isEmpty(mBookName)) {
            addWarning("Missing book name, using short name");
            mBookName = mBookShortName;
        }

        boolean hasSections = isPresent(book, SECTION_MARKER);

        if(!isPresent(book, USFMVerseSpan.PATTERN)) { // check for verses
            if(!hasSections) {
                addError("No verses found");
                return false;
            }

            addWarning("Using sections");
            extractChaptersFromText(book);
            return true;
        }

        if(!mChunks.containsKey(mBookShortName)) {
            addError("No Chunk found for " + mBookShortName);
            return false;
        }
        mChunk = mChunks.get(mBookShortName);

        extractChaptersFromBook(book);

        // TODO: 4/3/16 build tstudio package
        cleanup();
        return true;
    }

    public boolean extractChaptersFromBook(CharSequence text) {
        Pattern ChapterPattern = Pattern.compile(CHAPTER_NUMBER_MARKER);
        Matcher matcher = ChapterPattern.matcher(text);
        int lastIndex = 0;
        CharSequence section;
        mChapter = null;
        boolean success = true;
        while(matcher.find()) {
            section = text.subSequence(lastIndex, matcher.start()); // get section before this chapter marker
            success = success && breakUpChapter(section);
            mChapter = matcher.group(1); // chapter number for next section
            lastIndex = matcher.start();
        }
        section = text.subSequence(lastIndex, text.length()); // get last section
        success = success && breakUpChapter(section);
        return success;
    }

    private boolean breakUpChapter(CharSequence text) {
        boolean success = true;
        if(!isEmpty(mChapter)) {
            try {
                String chapter = mChapter;
                if (!mChunk.has(chapter)) {
                    chapter = "0" + chapter;
                    if (!mChunk.has(chapter)) {
                        chapter = "0" + chapter;
                    }
                }

                if (!mChunk.has(chapter)) {
                    addError("COuld not find chapter: " + mChapter);
                    return false;
                }

                JSONArray versebreaks = mChunk.getJSONArray(chapter);
                String lastFirst = null;
                for (int i = 0; i < versebreaks.length(); i++) {
                    String first = versebreaks.getString(i);
                    success = success && extractVerses(chapter, text, lastFirst, first);
                    lastFirst = first;
                }
                success = success && extractVerses(chapter, text, lastFirst, "999999");

            } catch (Exception e) {
                Logger.e(TAG, "error parsing chapter " + mChapter, e);
                return false;
            }
        }
        return success;
    }

    private boolean extractVerses(String chapter, CharSequence text, String start, String end) {
        boolean success = true;
        if(null != start) {
            int startVerse = Integer.valueOf(start);
            int endVerse = Integer.valueOf(end);
            success = extractVerseRange(chapter, text, startVerse, endVerse, start);
        }
        return success;
    }

    private boolean extractVerseRange(String chapter, CharSequence text, int start, int end, String firstVerse) {
        boolean success = true;
        if(!isEmpty(chapter)) {
            Pattern versePattern = Pattern.compile(USFMVerseSpan.PATTERN);
            Matcher matcher = versePattern.matcher(text);
            int lastIndex = 0;
            String section = "";
            int currentVerse = 0;
            boolean done = false;
            while (matcher.find()) {
                if(currentVerse >= end) {
                    done = true;
                    break;
                }

                if(currentVerse >= start) {
                    section = section + text.subSequence(lastIndex, matcher.start()); // get section before this chunk marker
                }

                String verse = matcher.group(1);
                currentVerse = Integer.valueOf(verse);
                lastIndex = matcher.start();
            }

            if(!done) {
                section = section + text.subSequence(lastIndex, matcher.start()); // get section before this chunk marker
            }

            if(!section.isEmpty()) {
                success = success && saveChunk(chapter, firstVerse, section);
            }
        }
        return success;
    }

    private boolean saveChunk(String chapter, String firstVerse, CharSequence chunk) {
        File chapterFolder = new File(mTempDest, chapter);
        try {
            FileUtils.forceMkdir(chapterFolder);
            File output = new File(chapterFolder, firstVerse + ".txt");
            FileUtils.write(output,chunk);
            return true;
        } catch (Exception e) {
            Logger.e(TAG, "error parsing chapter " + mChapter, e);
            addError("Error writing " + chapter + "/" + firstVerse);
            return false;
        }
    }

    private boolean isEmpty(CharSequence text) {
        if(null == text) {
            return true;
        }
        return text.length() == 0;
    }

    private boolean extractChaptersFromText(CharSequence text) {
        Pattern ChapterPattern = Pattern.compile(CHAPTER_NUMBER_MARKER);
        Matcher matcher = ChapterPattern.matcher(text);
        int lastIndex = 0;
        CharSequence section;
        mChapter = null;
        while(matcher.find()) {
            section = text.subSequence(lastIndex, matcher.start()); // get section before this chapter marker
            extractChunksFromChapter(section);
            mChapter = matcher.group(1); // chapter number for next section
            lastIndex = matcher.end();
        }
        section = text.subSequence(lastIndex, text.length()); // get last section
        extractChunksFromChapter(section);
        return true;
    }

    private void extractChunksFromChapter(CharSequence text) {
        if(!isEmpty(mChapter)) {
            Pattern ChunkPattern = Pattern.compile(SECTION_MARKER);
            Matcher matcher = ChunkPattern.matcher(text);
            int lastIndex = 0;
            CharSequence section;
            while (matcher.find()) {
                section = text.subSequence(lastIndex, matcher.start()); // get section before this chunk marker
                processSection(section);
                lastIndex = matcher.end();
            }
            section = text.subSequence(lastIndex, text.length()); // get last section
            processSection(section);
        }
    }

    private boolean processSection(CharSequence section) {
        if(!isEmpty(section)) {
            String firstVerse = extractString(section, USFMVerseSpan.PATTERN);
            if (null == firstVerse) {
                addError("Missing verse");
                return false;
            }

            saveChunk(mChapter, firstVerse, section);
        }
        return true;
    }

    private String extractString(CharSequence section, String regexPattern) {
        if(section.length() > 0) {
            // find instance
            Pattern findPattern = Pattern.compile(regexPattern);
            Matcher matcher = findPattern.matcher(section);
            String foundItem = null;
            if(matcher.find()) {
                foundItem = matcher.group(1);
            }
            return foundItem.trim();
        }

        return null;
    }

    private boolean isPresent(CharSequence section, String regexPattern) {
        if(section.length() > 0) {
            // find instance
            Pattern versePattern = Pattern.compile(regexPattern);
            Matcher matcher = versePattern.matcher(section);
            if(matcher.find()) {
                return true;
            }
        }

        return false;
    }

    private void createTempFolders() {
        mTempDir = new File(AppContext.context().getCacheDir(), System.currentTimeMillis() + "");
        mTempDir.mkdirs();
        mTempSrce = new File(mTempDir,"source");
        mTempSrce.mkdirs();
        mTempDest = new File(mTempDir,"new");
        mTempDest.mkdirs();
    }

    private void cleanup() {
        FileUtils.deleteQuietly(mTempDir);
    }

    private boolean addFilesInFolder(File usfmFile) {
        Logger.i(TAG, "processing folder: " + usfmFile.toString());

        if (usfmFile.isDirectory()) {
            File[] usfmSubFiles = usfmFile.listFiles();
            for (File usfmSuile : usfmSubFiles) {
                addFilesInFolder(usfmSuile);
            }
            Logger.i(TAG, "found files: " + usfmSubFiles.toString());
        } else {
            addFile(usfmFile);
        }
        return true;
    }

    private boolean addFile(File usfmFile) {
        Logger.i(TAG, "processing file: " + usfmFile.toString());
        mSourceFiles.add(usfmFile);
        return true;
    }
}