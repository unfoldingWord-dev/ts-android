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
    private File mTempOutput;
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

    /**
     * add chunkJson (contains verses for each section) to map
     * @param book
     * @param chunk
     * @return
     */
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

    /**
     * add chunkJson (contains verses for each section) to map
     * @param book
     * @param chunkStr
     * @return
     */
    public boolean addChunk(String book, String chunkStr) {
        try {
            JSONArray chunkJson = new JSONArray(chunkStr);
            return addChunk(book, chunkJson);
        } catch (Exception e) {
            Logger.e(TAG, "error parsing chunk " + book, e);
        }
        return false;
    }

    /**
     * get error list
     * @return
     */
    public String[] getErrors() {
        if( mErrors != null) {
            return mErrors.toArray(new String[mErrors.size()]);
        }
        return new String[0];
    }

    /**
     * add error to error list
     * @param error
     */
    private void addError(String error) {
        mErrors.add("Error: " + error);
    }

    /**
     * add warning to error list
     * @param error
     */
    private void addWarning(String error) {
        mErrors.add("Warning: " + error);
    }

    /**
     * process book text
     * @param book
     * @return
     */
    public boolean processBook(String book) {
        boolean success = true;
        try {
            mBookName = extractString(book,BOOK_NAME_MARKER);
            mBookShortName = extractString(book,BOOK_SHORT_NAME_MARKER).toLowerCase();

            if(isEmpty(mBookShortName)) {
                addError("Missing book short name");
                return false;
            }

            mTempDest = new File(mTempOutput, mBookShortName);

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
                extractChaptersFromDocument(book);
                return true;
            }

            if(!mChunks.containsKey(mBookShortName)) {
                addError("No Chunk found for " + mBookShortName);
                return false;
            }
            mChunk = mChunks.get(mBookShortName);

            success = extractChaptersFromBook(book);

            // TODO: 4/3/16 build tstudio package
            copyProjectToDownloads();

        } catch (Exception e) {
            Logger.e(TAG, "error parsing book", e);
            return false;
        } finally {
            try {
            cleanup();
            } catch (Exception e) {
                Logger.e(TAG, "error cleaning up", e);
                success = false;
            }
        }
        return success;
    }

    /**
     * copy output folder into downloads for testing
     * @return
     */
    public boolean copyProjectToDownloads() {
        File dest = null;
        try {
            File target = AppContext.getPublicDownloadsDirectory();
            dest = new File(target,"test");
            FileUtils.forceDelete(dest);
            FileUtils.copyDirectory(mTempOutput, dest);
        } catch (Exception e) {
            Logger.e(TAG, "error moving files to " + dest.toString(), e);
            return false;
        }
        return true;
    }

    /**
     * extract chapters in book
     * @param text
     * @return
     */
    public boolean extractChaptersFromBook(CharSequence text) {
        Pattern pattern = Pattern.compile(CHAPTER_NUMBER_MARKER);
        Matcher matcher = pattern.matcher(text);
        int lastIndex = 0;
        CharSequence section;
        mChapter = null;
        boolean successOverall = true;
        boolean success;
        while(matcher.find()) {
            section = text.subSequence(lastIndex, matcher.start()); // get section before this chapter marker
            success = breakUpChapter(section);
            successOverall = successOverall && success;
            mChapter = matcher.group(1); // chapter number for next section
            lastIndex = matcher.start();
        }
        section = text.subSequence(lastIndex, text.length()); // get last section
        success = breakUpChapter(section);
        successOverall = successOverall && success;
        return successOverall;
    }

    /**
     * break up chapter into sections
     * @param text
     * @return
     */
    private boolean breakUpChapter(CharSequence text) {
        boolean successOverall = true;
        boolean success;
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
                    success = extractVerses(chapter, text, lastFirst, first);
                    successOverall = successOverall && success;
                    lastFirst = first;
                }
                success = extractVerses(chapter, text, lastFirst, "999999");
                successOverall = successOverall && success;

            } catch (Exception e) {
                Logger.e(TAG, "error parsing chapter " + mChapter, e);
                return false;
            }
        }
        return successOverall;
    }

    /**
     * extract verses in range start to end into section
     * @param chapter
     * @param text
     * @param start
     * @param end
     * @return
     */
    private boolean extractVerses(String chapter, CharSequence text, String start, String end) {
        boolean success = true;
        if(null != start) {
            int startVerse = Integer.valueOf(start);
            int endVerse = Integer.valueOf(end);
            success = extractVerseRange(chapter, text, startVerse, endVerse, start);
        }
        return success;
    }

    /**
     * extract verses in range start to end into section
     * @param chapter
     * @param text
     * @param start
     * @param end
     * @param firstVerse
     * @return
     */
    private boolean extractVerseRange(String chapter, CharSequence text, int start, int end, String firstVerse) {
        boolean successOverall = true;
        boolean success;
        if(!isEmpty(chapter)) {
            Pattern pattern = Pattern.compile(USFMVerseSpan.PATTERN);
            Matcher matcher = pattern.matcher(text);
            int lastIndex = -1;
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

            if(!done && (lastIndex >= 0) && (currentVerse < end)) {
                section = section + text.subSequence(lastIndex, text.length()); // get section before this chunk marker
            }

            if(!section.isEmpty()) {
                success = saveSection(chapter, firstVerse, section);
                successOverall = successOverall && success;
            }
        }
        return successOverall;
    }

    /**
     * save section in file in chapter folder and book folder
     * @param chapter
     * @param firstVerse
     * @param section
     * @return
     */
    private boolean saveSection(String chapter, String firstVerse, CharSequence section) {
        File chapterFolder = new File(mTempDest, chapter);
        try {
            String cleanChunk = removePattern(section, SECTION_MARKER);
            FileUtils.forceMkdir(chapterFolder);
            File output = new File(chapterFolder, firstVerse + ".txt");
            FileUtils.write(output,cleanChunk);
            return true;
        } catch (Exception e) {
            Logger.e(TAG, "error parsing chapter " + mChapter, e);
            addError("Error writing " + chapter + "/" + firstVerse);
            return false;
        }
    }

    /**
     * test if CharSequence is null or empty
     * @param text
     * @return
     */
    private boolean isEmpty(CharSequence text) {
        if(null == text) {
            return true;
        }
        return text.length() == 0;
    }

    /**
     * extract chapters from document text
     * @param text
     * @return
     */
    private boolean extractChaptersFromDocument(CharSequence text) {
        Pattern pattern = Pattern.compile(CHAPTER_NUMBER_MARKER);
        Matcher matcher = pattern.matcher(text);
        int lastIndex = 0;
        CharSequence section;
        mChapter = null;
        while(matcher.find()) {
            section = text.subSequence(lastIndex, matcher.start()); // get section before this chapter marker
            extractSectionsFromChapter(section);
            mChapter = matcher.group(1); // chapter number for next section
            lastIndex = matcher.end();
        }
        section = text.subSequence(lastIndex, text.length()); // get last section
        extractSectionsFromChapter(section);
        return true;
    }

    /**
     * extract sections from chapter
     * @param chapter
     */
    private void extractSectionsFromChapter(CharSequence chapter) {
        if(!isEmpty(mChapter)) {
            Pattern pattern = Pattern.compile(SECTION_MARKER);
            Matcher matcher = pattern.matcher(chapter);
            int lastIndex = 0;
            CharSequence section;
            while (matcher.find()) {
                section = chapter.subSequence(lastIndex, matcher.start()); // get section before this chunk marker
                processSection(section);
                lastIndex = matcher.end();
            }
            section = chapter.subSequence(lastIndex, chapter.length()); // get last section
            processSection(section);
        }
    }

    /**
     * extract verses from section
     * @param section
     * @return
     */
    private boolean processSection(CharSequence section) {
        if(!isEmpty(section)) {
            String firstVerse = extractString(section, USFMVerseSpan.PATTERN);
            if (null == firstVerse) {
                addError("Missing verse");
                return false;
            }

            saveSection(mChapter, firstVerse, section);
        }
        return true;
    }

    /**
     * extract string in group 1 of regex if present
     * @param text
     * @param regexPattern
     * @return
     */
    private String extractString(CharSequence text, String regexPattern) {
        if(text.length() > 0) {
            // find instance
            Pattern findPattern = Pattern.compile(regexPattern);
            Matcher matcher = findPattern.matcher(text);
            String foundItem = null;
            if(matcher.find()) {
                foundItem = matcher.group(1);
            }
            return foundItem.trim();
        }

        return null;
    }

    /**
     * remove pattern if present in text
     * @param text
     * @param removePattern
     * @return
     */
    private String removePattern(CharSequence text, String removePattern) {
        String out = "";
        Pattern pattern = Pattern.compile(removePattern);
        Matcher matcher = pattern.matcher(text);
        int lastIndex = 0;
        while (matcher.find()) {
            out = out + text.subSequence(lastIndex, matcher.start()); // get section before this chunk marker
            lastIndex = matcher.end();
        }
        out = out + text.subSequence(lastIndex, text.length()); // get last section
        return out;
    }

    /**
     * test to see if regex pattern is present in text
     * @param text
     * @param regexPattern
     * @return
     */
    private boolean isPresent(CharSequence text, String regexPattern) {
        if(text.length() > 0) {
            // find instance
            Pattern versePattern = Pattern.compile(regexPattern);
            Matcher matcher = versePattern.matcher(text);
            if(matcher.find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * create the necessary temp folders for unzipped source and output
     */
    private void createTempFolders() {
        mTempDir = new File(AppContext.context().getCacheDir(), System.currentTimeMillis() + "");
        mTempDir.mkdirs();
        mTempSrce = new File(mTempDir,"source");
        mTempSrce.mkdirs();
        mTempOutput = new File(mTempDir,"output");
        mTempOutput.mkdirs();
    }

    /**
     * cleanup
     */
    private void cleanup() {
        FileUtils.deleteQuietly(mTempDir);
    }

    /**
     * add file and files in sub-folders to list of files to process
     * @param usfmFile
     * @return
     */
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

    /**
     * add file to list of files to process
     * @param usfmFile
     * @return
     */
    private boolean addFile(File usfmFile) {
        Logger.i(TAG, "processing file: " + usfmFile.toString());
        mSourceFiles.add(usfmFile);
        return true;
    }
}