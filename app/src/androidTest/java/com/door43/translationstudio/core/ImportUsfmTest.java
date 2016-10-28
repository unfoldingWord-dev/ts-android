package com.door43.translationstudio.core;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.door43.translationstudio.App;
import com.door43.translationstudio.ui.spannables.USFMVerseSpan;
import com.door43.util.FileUtilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.models.ChunkMarker;
import org.unfoldingword.tools.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unfoldingword.door43client.models.TargetLanguage;


/**
 * Created by blm on 4/19/16.
 */
public class ImportUsfmTest extends InstrumentationTestCase {

    private JSONArray mExpectedBooks;
    private TargetLanguage mTargetLanguage;
    private ImportUsfm mUsfm;
    private Context mTestContext;
    private Context mAppContext;
    private Door43Client mLibrary;
    private HashMap<String, List<String>> mChunks;
    private String[] mChapters;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mExpectedBooks = new JSONArray();
        mLibrary = App.getLibrary();
        Logger.flush();
        mTargetLanguage = mLibrary.index().getTargetLanguage("es");
        mTestContext = getInstrumentation().getContext();
        mAppContext = App.context();
        if(App.getProfile() == null) { // make sure this is initialized
            App.setProfile(new Profile("testing"));
        }
    }

    @Override
    public void tearDown() throws Exception {
        if(mUsfm != null) {
            mUsfm.cleanup();
        }
    }

    public void test01ValidImportMark() throws Exception {
        //given
        String source = "mrk.usfm";
        addExpectedBook(source, "mrk", true, false);
        boolean expectSucccess = true;
        boolean expectNoEmptyChunks = true;
        boolean exactVerseCount = true;
        mUsfm = new ImportUsfm(mAppContext, mTargetLanguage);

        //when
        boolean success = mUsfm.readResourceFile(mTestContext, "usfm/" + source);

        //then
        verifyResults( success, expectSucccess, mExpectedBooks, expectNoEmptyChunks, exactVerseCount);
    }

    public void test02ImportMarkMissingName() throws Exception {
        //given
        String source = "mrk_no_id.usfm";
        addExpectedBook(source, "", false, true);
        boolean expectNoEmptyChunks = true;
        boolean expectSucccess = true;
        boolean exactVerseCount = true;
        mUsfm = new ImportUsfm(mAppContext, mTargetLanguage);

        //when
        boolean success = mUsfm.readResourceFile(mTestContext, "usfm/" + source);

        //then
        verifyResults( success, expectSucccess, mExpectedBooks, expectNoEmptyChunks, exactVerseCount);
    }

    public void test03ImportMarkMissingNameForce() throws Exception {
        //given
        String source = "mrk_no_id.usfm";
        String useName = "Mrk";
        addExpectedBook(source, useName, true, false);
        boolean expectSucccess = true;
        boolean expectNoEmptyChunks = true;
        boolean exactVerseCount = true;
        mUsfm = new ImportUsfm(mAppContext, mTargetLanguage);
        InputStream usfmStream = mTestContext.getAssets().open("usfm/" + source);
        String text = FileUtilities.readStreamToString(usfmStream);

        //when
        boolean success = mUsfm.processText(text, source, false, useName);

        //then
        verifyResults( success, expectSucccess, mExpectedBooks, expectNoEmptyChunks, exactVerseCount);
    }

    public void test04ValidImportPsalms() throws Exception {
        //given
        String source = "19-PSA.usfm"; // psalms has a verse range
        addExpectedBook(source, "psa", true, false);
        boolean expectNoEmptyChunks = true;
        boolean expectSucccess = true;
        boolean exactVerseCount = false;
        mUsfm = new ImportUsfm(mAppContext, mTargetLanguage);

        //when
        boolean success = mUsfm.readResourceFile(mTestContext, "usfm/" + source);

        //then
        verifyResults( success, expectSucccess, mExpectedBooks, expectNoEmptyChunks, exactVerseCount);
    }

    public void test05ImportMarkNoChapters() throws Exception {
        //given
        String source = "mrk_no_chapter.usfm";
        addExpectedBook(source, "mrk", false, false);
        boolean expectNoEmptyChunks = true;
        boolean expectSucccess = false;
        boolean exactVerseCount = true;
        mUsfm = new ImportUsfm(mAppContext, mTargetLanguage);

        //when
        boolean success = mUsfm.readResourceFile(mTestContext, "usfm/" + source);

        //then
        verifyResults( success, expectSucccess, mExpectedBooks, expectNoEmptyChunks, exactVerseCount);
    }

    public void test06ImportMarkMissingChapters() throws Exception {
        //given
        String source = "mrk_one_chapter.usfm";
        addExpectedBook(source, "mrk", false, false);
        boolean expectSucccess = true;
        boolean expectNoEmptyChunks = false;
        boolean exactVerseCount = true;
        mUsfm = new ImportUsfm(mAppContext, mTargetLanguage);

        //when
        boolean success = mUsfm.readResourceFile(mTestContext, "usfm/" + source);

        //then
        verifyResults( success, expectSucccess, mExpectedBooks, expectNoEmptyChunks, exactVerseCount);
    }

    public void test07ImportMarkNoVerses() throws Exception {
        //given
        String source = "mrk_one_chapter.usfm";
        addExpectedBook(source, "mrk", false, false);
        boolean expectSucccess = true;
        boolean expectNoEmptyChunks = false;
        boolean exactVerseCount = true;
        mUsfm = new ImportUsfm(mAppContext, mTargetLanguage);

        //when
        boolean success = mUsfm.readResourceFile(mTestContext, "usfm/" + source);

        //then
        verifyResults( success, expectSucccess, mExpectedBooks, expectNoEmptyChunks, exactVerseCount);
    }

    public void test08ImportMarkMissingVerse() throws Exception {
        //given
        String source = "mrk_missing_verse.usfm";
        addExpectedBook(source, "mrk", false, false);
        boolean expectSucccess = true;
        boolean expectNoEmptyChunks = true;
        boolean exactVerseCount = false;
        mUsfm = new ImportUsfm(mAppContext, mTargetLanguage);

        //when
        boolean success = mUsfm.readResourceFile(mTestContext, "usfm/" + source);

        //then
        verifyResults( success, expectSucccess, mExpectedBooks, expectNoEmptyChunks, exactVerseCount);
    }

    public void test09ImportMarkEmptyChapter() throws Exception {
        //given
        String source = "mrk_empty_chapter.usfm";
        addExpectedBook(source, "mrk", false, false);
        boolean expectSucccess = true;
        boolean expectNoEmptyChunks = false;
        boolean exactVerseCount = true;
        mUsfm = new ImportUsfm(mAppContext, mTargetLanguage);

        //when
        boolean success = mUsfm.readResourceFile(mTestContext, "usfm/" + source);

        //then
        verifyResults( success, expectSucccess, mExpectedBooks, expectNoEmptyChunks, exactVerseCount);
    }


    public void test10ImportJudeNoVerses() throws Exception {
        //given
        String source = "jude.no_verses.usfm";
        addExpectedBook(source, "jud", false, false);
        boolean expectSucccess = false;
        boolean expectNoEmptyChunks = true;
        boolean exactVerseCount = true;
        mUsfm = new ImportUsfm(mAppContext, mTargetLanguage);

        //when
        boolean success = mUsfm.readResourceFile(mTestContext, "usfm/" + source);

        //then
        verifyResults( success, expectSucccess, mExpectedBooks, expectNoEmptyChunks, exactVerseCount);
    }

    public void test11ImportJudeNoChapter() throws Exception {
        //given
        String source = "jude.no_chapter_or_verses.usfm";
        addExpectedBook(source, "jud", false, false);
        boolean expectSucccess = false;
        boolean expectNoEmptyChunks = true;
        boolean exactVerseCount = true;
        mUsfm = new ImportUsfm(mAppContext, mTargetLanguage);

        //when
        boolean success = mUsfm.readResourceFile(mTestContext, "usfm/" + source);

        //then
        verifyResults( success, expectSucccess, mExpectedBooks, expectNoEmptyChunks, exactVerseCount);
    }


    public void test12ImportPhpNoChapter1() throws Exception {
        //given
        String source = "php_usfm_NoC1.usfm";
        addExpectedBook(source, "php", false, false);
        boolean expectSucccess = true;
        boolean expectNoEmptyChunks = false;
        boolean exactVerseCount = true;
        mUsfm = new ImportUsfm(mAppContext, mTargetLanguage);

        //when
        boolean success = mUsfm.readResourceFile(mTestContext, "usfm/" + source);

        //then
        verifyResults( success, expectSucccess, mExpectedBooks, expectNoEmptyChunks, exactVerseCount);
    }

    public void test13ImportPhpNoChapter2() throws Exception {
        //given
        String source = "php_usfm_NoC2.usfm";
        addExpectedBook(source, "php", false, false);
        boolean expectSucccess = true;
        boolean expectNoEmptyChunks = false;
        boolean exactVerseCount = true;
        mUsfm = new ImportUsfm(mAppContext, mTargetLanguage);

        //when
        boolean success = mUsfm.readResourceFile(mTestContext, "usfm/" + source);

        //then
        verifyResults( success, expectSucccess, mExpectedBooks, expectNoEmptyChunks, exactVerseCount);
    }

    public void test14ImportPhpChapter3OutOfOrder() throws Exception {
        //given
        String source = "php_usfm_C3_out_of_order.usfm";
        addExpectedBook(source, "php", false, false);
        boolean expectSucccess = false;
        boolean expectNoEmptyChunks = true;
        boolean exactVerseCount = true;
        mUsfm = new ImportUsfm(mAppContext, mTargetLanguage);

        //when
        boolean success = mUsfm.readResourceFile(mTestContext, "usfm/" + source);

        //then
        verifyResults( success, expectSucccess, mExpectedBooks, expectNoEmptyChunks, exactVerseCount);
    }

    public void test15ImportPhpMissingLastChapter() throws Exception {
        //given
        String source = "php_usfm_missing_last_chapter.usfm";
        addExpectedBook(source, "php", false, false);
        boolean expectSucccess = true;
        boolean expectNoEmptyChunks = false;
        boolean exactVerseCount = true;
        mUsfm = new ImportUsfm(mAppContext, mTargetLanguage);

        //when
        boolean success = mUsfm.readResourceFile(mTestContext, "usfm/" + source);

        //then
        verifyResults( success, expectSucccess, mExpectedBooks, expectNoEmptyChunks, exactVerseCount);
    }

    public void test16ImportPhpNoChapter1Marker() throws Exception {
        //given
        String source = "php_usfm_NoC1_marker.usfm";
        addExpectedBook(source, "php", false, false);
        boolean expectSucccess = true;
        boolean expectNoEmptyChunks = true;
        boolean exactVerseCount = true;
        mUsfm = new ImportUsfm(mAppContext, mTargetLanguage);

        //when
        boolean success = mUsfm.readResourceFile(mTestContext, "usfm/" + source);

        //then
        verifyResults( success, expectSucccess, mExpectedBooks, expectNoEmptyChunks, exactVerseCount);
    }

    public void test17ImportPhpNoChapter2Marker() throws Exception {
        //given
        String source = "php_usfm_NoC2_marker.usfm";
        addExpectedBook(source, "php", false, false);
        boolean expectSucccess = true;
        boolean expectNoEmptyChunks = false;
        boolean exactVerseCount = true;
        mUsfm = new ImportUsfm(mAppContext, mTargetLanguage);

        //when
        boolean success = mUsfm.readResourceFile(mTestContext, "usfm/" + source);

        //then
        verifyResults( success, expectSucccess, mExpectedBooks, expectNoEmptyChunks, exactVerseCount);
    }

    public void test18ImportPhpMissingLastChapterMarker() throws Exception {
        //given
        String source = "php_usfm_missing_last_chapter_marker.usfm";
        addExpectedBook(source, "php", true, false);
        boolean expectSucccess = true;
        boolean expectNoEmptyChunks = false;
        boolean exactVerseCount = true;
        mUsfm = new ImportUsfm(mAppContext, mTargetLanguage);

        //when
        boolean success = mUsfm.readResourceFile(mTestContext, "usfm/" + source);

        //then
        verifyResults( success, expectSucccess, mExpectedBooks, expectNoEmptyChunks, exactVerseCount);
    }

    public void test19ImportJudeOutOfOrderVerses() throws Exception {
        //given
        String source = "jude.out_order_verses.usfm";
        addExpectedBook(source, "jud", false, false);
        boolean expectSucccess = true;
        boolean expectNoEmptyChunks = true;
        boolean exactVerseCount = false;
        mUsfm = new ImportUsfm(mAppContext, mTargetLanguage);

        //when
        boolean success = mUsfm.readResourceFile(mTestContext, "usfm/" + source);

        //then
        verifyResults( success, expectSucccess, mExpectedBooks, expectNoEmptyChunks, exactVerseCount);
    }



    public void addExpectedBook(String filename, String book, boolean success, boolean missingName) throws JSONException {
        JSONObject expectedBook = new JSONObject();
        expectedBook.put("filename", filename);
        expectedBook.put("book", book);
        expectedBook.put("success", success);
        expectedBook.put("missingName", missingName);
        mExpectedBooks.put(expectedBook);
    }

    public String getFileName(JSONObject object) throws JSONException {
        return object.getString("filename");
    }

    public String getBook(JSONObject object) throws JSONException {
        return object.getString("book");
    }

    public boolean getMissingName(JSONObject object) throws JSONException {
        return object.getBoolean("missingName");
    }

    public boolean getSuccess(JSONObject object) throws JSONException {
        return object.getBoolean("success");
    }

    public void verifyResults(boolean success, boolean expected, JSONArray expectedBooks, boolean noEmptyChunks, boolean exactVerseCount) throws JSONException {
        String results = mUsfm.getResultsString();
        assertTrue("results text should not be empty", !results.isEmpty());
        assertEquals("results", expected, success);
        assertEquals("results", expected, mUsfm.isProcessSuccess());
        String[] resultLines = results.split("\n");

        int missingNamesCount = 0;

        for(int i = 0; i < expectedBooks.length(); i++) {
            JSONObject object = expectedBooks.getJSONObject(i);
            String fileName = getFileName(object);
            String book = getBook(object);
            boolean expectedsuccess = getSuccess(object);
            boolean missingName = getMissingName(object);
            verifyBookResults(resultLines, fileName, book, expectedsuccess, noEmptyChunks, success, exactVerseCount);
            if(missingName) {
                findMissingName( fileName);
                missingNamesCount++;
            }
        }
        MissingNameItem[] missingNameItems = mUsfm.getBooksMissingNames();
        assertEquals("Missing name count should equal", missingNamesCount, missingNameItems.length);
    }

    public void findMissingName(String filename) {
        MissingNameItem[] missingNameItems = mUsfm.getBooksMissingNames();
        boolean found = false;
        for (MissingNameItem missingNameItem : missingNameItems) {
            if(missingNameItem.description.indexOf(filename) >= 0) {
                found = true;
                break;
            }
        }
        assertTrue(filename + " should be missing name ", found);
    }

    /**
     * parse chunk markers (contains verses and chapters) into map of verses indexed by chapter
     *
     * @param chunks
     * @return
     */
    public boolean parseChunks(List<ChunkMarker> chunks) {
        mChunks = new HashMap<>(); // clear old map
        try {
            for (ChunkMarker chunkMarker : chunks) {

                String chapter = chunkMarker.chapter;
                String firstVerse = chunkMarker.verse;

                List<String> verses = null;
                if (mChunks.containsKey(chapter)) {
                    verses = mChunks.get(chapter);
                } else {
                    verses = new ArrayList<>();
                    mChunks.put(chapter, verses);
                }

                verses.add(firstVerse);
            }

            //extract chapters
            List<String> foundChapters = new ArrayList<>();
            for (String chapter : mChunks.keySet()) {
                foundChapters.add(chapter);
            }
            Collections.sort(foundChapters);
            mChapters = foundChapters.toArray(new String[foundChapters.size()]);;

        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void verifyBookResults(String[] results, String filename, String book, boolean noErrorsExpected, boolean noEmptyChunks, boolean success, boolean exactVerseCount) {
        String bookLine = filename;
        if(!book.isEmpty()) {
            bookLine = book.toLowerCase() + " = " + filename;
        }
        String foundBookMarker = "Found book: ";
        String expectLine = foundBookMarker + bookLine;
        boolean bookFound = false;
        for(int i = 0; i < results.length; i++) {
            String line = results[i];

            if(line.indexOf(expectLine) >= 0) {
                boolean noErrorsFound = false;

                for(int j = i + 1; j < results.length; j++) {
                    String resultsLine = results[j];

                    int pos = resultsLine.indexOf(foundBookMarker); // if starting next book, then done
                    if(pos >= 0) {
                        break;
                    }

                    pos = resultsLine.indexOf("No errors Found");
                    if(pos >= 0) {
                        noErrorsFound = true;
                        break;
                    }
                }
                assertEquals(bookLine + " found, no errors expected " + noErrorsExpected, noErrorsExpected, noErrorsFound);
                bookFound = true;
                break;
            }
        }
        assertTrue(bookLine + " not found", bookFound);

        String chunk = "";

        // verify chapters and verses
        if(success  && !book.isEmpty()) {
            File[] projects = mUsfm.getImportProjects();
            if(success) {
                assertTrue("Import Projects count should be greater than zero, but is " + projects.length, projects.length > 0);
            }

             for (File project : projects) {

                List<ChunkMarker> chunks = App.getLibrary().index().getChunkMarkers(book, "en-US");
                parseChunks(chunks);

                for (String chapter : mChapters) {
                    // verify chapter
                    File chapterPath = new File(project, getRightChapterLength(chapter));
                    boolean exists = chapterPath.exists();
                    if(!exists) {
                        assertTrue("Chapter missing " + chapterPath.toString(), exists);
                    }

                    // verify chunks
                    List<String> chapterFrameSlugs = mChunks.get(chapter);
                    for (int i = 0; i < chapterFrameSlugs.size(); i++) {
                        String chapterFrameSlug = chapterFrameSlugs.get(i);
                        int expectCount = -1;
                        if(i + 1 < chapterFrameSlugs.size()) {
                            String nextSlug = chapterFrameSlugs.get(i+1);
                            int nextStart = Integer.valueOf(nextSlug);
                            if(nextStart > 0) {
                                expectCount = nextStart - Integer.valueOf(chapterFrameSlug);
                            }
                        }

                        File chunkPath = new File(chapterPath, chapterFrameSlug + ".txt");
                        assertTrue("Chunk missing " + chunkPath.toString(), chunkPath.exists());
                        try {
                            chunk = FileUtilities.readFileToString(chunkPath);
                            int count = getVerseCount(chunk);
                            if(noEmptyChunks) {
                                boolean emptyChunk = chunk.isEmpty();
                                assertTrue("Chunk is empty " + chunkPath.toString(),!emptyChunk);
                                assertTrue("VerseCount should not be zero: " + count + " in chunk " + chunkPath.toString(), count > 0);
                                if((expectCount >= 0) && exactVerseCount) {
                                    assertEquals("Verse Count" + " in chunk " + chunkPath.toString(), expectCount, count);
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            fail("Could not read chunk " + chunkPath.toString());
                        }
                    }
                }
            }
        }
    }

    /**
     * right size the chapter.  App expects chapter numbers under 100 to be only two digits.
     * @param chapterN
     * @return
     */
    private String getRightChapterLength(String chapterN) {
        Integer chapterNInt = strToInt(chapterN, -1);
        if((chapterNInt >= 0) && (chapterNInt < 100)) {
            chapterN = chapterN.substring(chapterN.length()-2);
        }
        return chapterN;
    }

    /**
     * do string to integer with default value on conversion error
     * @param value
     * @param defaultValue
     * @return
     */
    public static int strToInt(String value, int defaultValue) {
        try {
            int retValue = Integer.parseInt(value);
            return retValue;
        } catch (Exception e) {
            Log.d(ImportUsfmTest.class.getSimpleName(), "Cannot convert to int: " + value);
        }
        return defaultValue;
    }

    private static final Pattern PATTERN_USFM_VERSE_SPAN = Pattern.compile(USFMVerseSpan.PATTERN);

    /**
     * get verse count
     */
    private int getVerseCount(String text) {
        int foundVerseCount = 0;
        Pattern pattern = PATTERN_USFM_VERSE_SPAN;
        Matcher matcher = pattern.matcher(text);
        int currentVerse = 0;
        int endVerseRange = 0;

        while (matcher.find()) {

            String verse = matcher.group(1);
            int[] verseRange = getVerseRange(verse);
            if (null == verseRange) {
                break;
            }
            currentVerse = verseRange[0];
            endVerseRange = verseRange[1];

            if (endVerseRange > 0) {
                foundVerseCount += (endVerseRange - currentVerse + 1);
            } else {
                foundVerseCount++;
            }
        }
        return foundVerseCount;
    }

    /**
     * parse verse number to get range
     * @param verse
     * @return
     */
    private int[] getVerseRange(String verse) {
        int[] verseRange;
        int currentVerse;
        int endVerseRange;
        try {
            int currentVers = Integer.valueOf(verse);
            verseRange = new int[] {currentVers, 0};
        } catch (NumberFormatException e) { // might be a range in format 12-13
            String[] range = verse.split("-");
            if (range.length < 2) {
                verseRange = null;
            } else {
                currentVerse = Integer.valueOf(range[0]);
                endVerseRange = Integer.valueOf(range[1]);
                verseRange = new int[]{currentVerse, endVerseRange};
            }
        }
        return verseRange;
    }


}