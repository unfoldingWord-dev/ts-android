package com.door43.translationstudio.core;

import android.test.InstrumentationTestCase;

import com.door43.translationstudio.AppContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by blm on 4/19/16.
 */
public class ImportUsfmTest extends InstrumentationTestCase {

    private JSONArray mExpectedBooks;
    private TargetLanguage mTargetLanguage;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mExpectedBooks = new JSONArray();
        Library library = AppContext.getLibrary();
        mTargetLanguage = library.getTargetLanguage("es");
    }

    @Override
    public void tearDown() throws Exception {

    }

    public void test01ValidImportMark() throws Exception {
        //given
        String source = "mrk.usfm.txt";
        addExpectedBook(source, "mrk", true);
        boolean expectSucccess = true;
        ImportUsfm usfm = new ImportUsfm(AppContext.context(), mTargetLanguage);

        //when
        boolean success = usfm.readResourceFile(getInstrumentation().getContext(), "usfm/" + source);

        //then
        verifyResults( success, expectSucccess, usfm, mExpectedBooks);
    }

    public void addExpectedBook(String filename, String book, boolean success) throws JSONException {
        JSONObject expectedBook = new JSONObject();
        expectedBook.put("filename", filename);
        expectedBook.put("book", book);
        expectedBook.put("success", success);
        mExpectedBooks.put(expectedBook);
    }

    public String getFileName(JSONObject object) throws JSONException {
        return object.getString("filename");
    }

    public String getBook(JSONObject object) throws JSONException {
        return object.getString("book");
    }

    public boolean getSuccess(JSONObject object) throws JSONException {
        return object.getBoolean("success");
    }

    public void verifyResults(boolean success, boolean expected, ImportUsfm usfm, JSONArray expectedBooks) throws JSONException {
        assertEquals(success, expected);
        assertEquals(usfm.isProcessSuccess(), expected);
        String results = usfm.getResultsString();
        assertTrue(!results.isEmpty());
        String[] resultLines = results.split("\n");

        boolean overallSuccess = true;

        for(int i = 0; i < expectedBooks.length(); i++) {
            JSONObject object = expectedBooks.getJSONObject(i);
            String fileName = getFileName(object);
            String book = getBook(object);
            boolean expectedsuccess = getSuccess(object);
            verifyBookResults(resultLines, fileName, book, expectedsuccess);
        }
    }

    public void verifyBookResults(String[] results, String filename, String book, boolean success) {
        String bookLine = book + " = " + filename;
        String foundBookMarker = "Found book: ";
        String expectLine = foundBookMarker + bookLine;
        for(int i = 0; i < results.length; i++) {
            String line = results[i];

            if(line.indexOf(expectLine) >= 0) {

                boolean noErrors = false;

                for(int j = i + 1; j < results.length; j++) {
                    String resultsLine = results[j];
                    int pos = resultsLine.indexOf("No errors Found");
                    if(pos >= 0) {
                        noErrors = true;
                        break;
                    }

                    pos = resultsLine.indexOf(foundBookMarker); // if starting next book, then done
                    if(pos >= 0) {
                        break;
                    }
                }
                assertEquals(bookLine + " found expected " + success, noErrors, success);
                return;
            }
        }
        fail(bookLine + " not found");
    }
}