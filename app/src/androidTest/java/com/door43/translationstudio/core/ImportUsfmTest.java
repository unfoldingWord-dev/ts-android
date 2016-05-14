package com.door43.translationstudio.core;

import android.content.Context;
import android.test.InstrumentationTestCase;

import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.tasks.UploadCrashReportTask;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;


/**
 * Created by blm on 4/19/16.
 */
public class ImportUsfmTest extends InstrumentationTestCase {

    private JSONArray mExpectedBooks;
    private TargetLanguage mTargetLanguage;
    private ImportUsfm mUsfm;
    private Context mTestContext;
    private Context mAppContext;
    private Library mLibrary;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mExpectedBooks = new JSONArray();
        mLibrary = AppContext.getLibrary();
        UploadCrashReportTask.archiveErrorLogs();
        mTargetLanguage = mLibrary.getTargetLanguage("es");
        mTestContext = getInstrumentation().getContext();
        mAppContext = AppContext.context();
        if(AppContext.getProfile() == null) { // make sure this is initialized
            AppContext.setProfile(new Profile("testing"));
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
        String source = "mrk.usfm.txt";
        addExpectedBook(source, "mrk", true, false);
        boolean expectSucccess = true;
        mUsfm = new ImportUsfm(mAppContext, mTargetLanguage);

        //when
        boolean success = mUsfm.readResourceFile(mTestContext, "usfm/" + source);

        //then
        verifyResults( success, expectSucccess, mExpectedBooks);
    }

    public void test02ImportMarkMissingName() throws Exception {
        //given
        String source = "mrk.usfm_no_id.txt";
        addExpectedBook(source, "", false, true);
        boolean expectSucccess = true;
        mUsfm = new ImportUsfm(mAppContext, mTargetLanguage);

        //when
        boolean success = mUsfm.readResourceFile(mTestContext, "usfm/" + source);

        //then
        verifyResults( success, expectSucccess, mExpectedBooks);
    }

    public void test03ImportMarkMissingNameForce() throws Exception {
        //given
        String source = "mrk.usfm_no_id.txt";
        String useName = "Mrk";
        addExpectedBook(source, useName, true, false);
        boolean expectSucccess = true;
        mUsfm = new ImportUsfm(mAppContext, mTargetLanguage);
        InputStream usfmStream = mTestContext.getAssets().open("usfm/" + source);
        String text = IOUtils.toString(usfmStream, "UTF-8");

        //when
        boolean success = mUsfm.processText(text, source, false, useName);

        //then
        verifyResults( success, expectSucccess, mExpectedBooks);
    }

    public void test04ValidImportPsalms() throws Exception {
        //given
        String source = "19-PSA.usfm"; // psalms has a verse range
        addExpectedBook(source, "psa", true, false);
        boolean expectSucccess = true;
        mUsfm = new ImportUsfm(mAppContext, mTargetLanguage);

        //when
        boolean success = mUsfm.readResourceFile(mTestContext, "usfm/" + source);

        //then
        verifyResults( success, expectSucccess, mExpectedBooks);
    }

    public void test05ImportMarkNoChapters() throws Exception {
        //given
        String source = "mrk.usfm_no_chapter.txt";
        addExpectedBook(source, "mrk", false, false);
        boolean expectSucccess = false;
        mUsfm = new ImportUsfm(mAppContext, mTargetLanguage);

        //when
        boolean success = mUsfm.readResourceFile(mTestContext, "usfm/" + source);

        //then
        verifyResults( success, expectSucccess, mExpectedBooks);
    }

    public void test06ImportMarkMissingChapters() throws Exception {
        //given
        String source = "mrk.usfm_one_chapter.txt";
        addExpectedBook(source, "mrk", false, false);
        boolean expectSucccess = false;
        mUsfm = new ImportUsfm(mAppContext, mTargetLanguage);

        //when
        boolean success = mUsfm.readResourceFile(mTestContext, "usfm/" + source);

        //then
        verifyResults( success, expectSucccess, mExpectedBooks);
    }

    public void test07ImportMarkNoVerses() throws Exception {
        //given
        String source = "mrk.usfm_one_chapter.txt";
        addExpectedBook(source, "mrk", false, false);
        boolean expectSucccess = false;
        mUsfm = new ImportUsfm(mAppContext, mTargetLanguage);

        //when
        boolean success = mUsfm.readResourceFile(mTestContext, "usfm/" + source);

        //then
        verifyResults( success, expectSucccess, mExpectedBooks);
    }

    public void test08ImportMarkMissingVerse() throws Exception {
        //given
        String source = "mrk.usfm_missing_verse.txt";
        addExpectedBook(source, "mrk", false, false);
        boolean expectSucccess = true;
        mUsfm = new ImportUsfm(mAppContext, mTargetLanguage);

        //when
        boolean success = mUsfm.readResourceFile(mTestContext, "usfm/" + source);

        //then
        verifyResults( success, expectSucccess, mExpectedBooks);
    }

    public void test09ImportMarkEmptyChapter() throws Exception {
        //given
        String source = "mrk.usfm_empty_chapter.txt";
        addExpectedBook(source, "mrk", false, false);
        boolean expectSucccess = true;
        mUsfm = new ImportUsfm(mAppContext, mTargetLanguage);

        //when
        boolean success = mUsfm.readResourceFile(mTestContext, "usfm/" + source);

        //then
        verifyResults( success, expectSucccess, mExpectedBooks);
    }


    public void test10ImportJudeNoVerses() throws Exception {
        //given
        String source = "jude.no_verses.usfm";
        addExpectedBook(source, "jud", false, false);
        boolean expectSucccess = false;
        mUsfm = new ImportUsfm(mAppContext, mTargetLanguage);

        //when
        boolean success = mUsfm.readResourceFile(mTestContext, "usfm/" + source);

        //then
        verifyResults( success, expectSucccess, mExpectedBooks);
    }

    public void test11ImportJudeNoChapter() throws Exception {
        //given
        String source = "jude.no_chapter_or_verses.usfm";
        addExpectedBook(source, "jud", false, false);
        boolean expectSucccess = false;
        mUsfm = new ImportUsfm(mAppContext, mTargetLanguage);

        //when
        boolean success = mUsfm.readResourceFile(mTestContext, "usfm/" + source);

        //then
        verifyResults( success, expectSucccess, mExpectedBooks);
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

    public void verifyResults(boolean success, boolean expected, JSONArray expectedBooks) throws JSONException {
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
            verifyBookResults(resultLines, fileName, book, expectedsuccess);
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

    public void verifyBookResults(String[] results, String filename, String book, boolean noErrorsExpected) {
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
                assertEquals(bookLine + " found, errors expected " + noErrorsExpected, noErrorsExpected, noErrorsFound);
                bookFound = true;
                break;
            }
        }
        assertTrue(bookLine + " not found", bookFound);

        // verify chapters and verses
        if(noErrorsExpected && !book.isEmpty()) {
            SourceTranslation sourceTranslation = mLibrary.getSourceTranslation(book.toLowerCase(), "en", "ulb");
            File[] projects = mUsfm.getImportProjects();
            for (File project : projects) {
                Chapter[] chapters = mLibrary.getChapters(sourceTranslation);
                for (Chapter chapter : chapters) {
                    // verify chapter
                    File chapterPath = new File(project, chapter.getId());
                    assertTrue("Chapter missing " + chapterPath.toString(), chapterPath.exists());

                    // verify chunks
                }
            }
        }
    }
}