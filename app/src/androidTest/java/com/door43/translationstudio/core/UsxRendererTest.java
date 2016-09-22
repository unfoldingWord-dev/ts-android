package com.door43.translationstudio.core;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.door43.translationstudio.rendering.USXRenderer;
import com.door43.util.FileUtilities;

import org.unfoldingword.tools.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by blm on 7/25/16.
 */
public class UsxRendererTest extends InstrumentationTestCase {

    public static final String TAG = UsxRendererTest.class.getSimpleName();
    File mTempFolder;
    private Context mTestContext;
    private String mTestText;
    private String mExpectedText;
    private String mExpectedGroup1Text;
    private String mExpectedGroup2Text;
    private String mGroup1Text;
    private String mGroup2Text;


    @Override
    public void setUp() throws Exception {
        super.setUp();
        Logger.flush();
        mTestContext = getInstrumentation().getContext();
    }

    @Override
    public void tearDown() throws Exception {
    }

    public void test01ProcessFullConflict() throws Exception {
        //given
        String search = null;
        String testId = "render/full_conflict";

        //when
        doRenderMergeConflicts(search, testId);

        //then
        verifyRenderText("test01ProcessFullConflict");
    }

    public void test02ProcessTwoConflict() throws Exception {
        //given
        String search = null;
        String testId = "render/two_conflict";

        //when
        doRenderMergeConflicts(search, testId);

        //then
        verifyRenderText("test02ProcessTwoConflict");
    }

    private void verifyRenderText(String id) {
        verifyProcessedText(id + ": Head", mExpectedGroup1Text, mGroup1Text);
        verifyProcessedText(id + ": Tail", mExpectedGroup2Text, mGroup2Text);
    }

    private void doRenderMergeConflicts(String search, String testId) throws IOException {
        mGroup1Text = doRenderMergeConflict(search, testId, USXRenderer.MergeFirstPart);
        mExpectedGroup1Text = mExpectedText; // save expected text for this section
        mGroup2Text = doRenderMergeConflict(search, testId, USXRenderer.MergeSecondPart);
        mExpectedGroup2Text = mExpectedText; // save expected text for this section
    }

    private String doRenderMergeConflict(String search, String testId, int sourceGroup) throws IOException {
        String testTextFile = testId+ "_raw.data";
        String expectTextFile = testId+ "_part" + sourceGroup + ".data";
        InputStream testTextStream = mTestContext.getAssets().open(testTextFile);
        mTestText = FileUtilities.readStreamToString(testTextStream);
        assertNotNull(mTestText);
        assertFalse(mTestText.isEmpty());
        InputStream testExpectedStream = mTestContext.getAssets().open(expectTextFile);
        mExpectedText = FileUtilities.readStreamToString(testExpectedStream);
        assertNotNull(mExpectedText);
        assertFalse(mExpectedText.isEmpty());

        String out = (new USXRenderer()).renderMergeConflict(mTestText, sourceGroup).toString();
        return out;
    }

    private void verifyProcessedText(String id, String expectedText, String out) {
        assertNotNull(id, out);
        assertFalse(id, out.isEmpty());
        if(!out.equals(expectedText)) {
            Log.e(TAG, "error in: " + id);
            if(out.length() != expectedText.length()) {
                Log.e(TAG, "expected length " + expectedText.length() + " but got length " + out.length());
            }

            for( int ptr = 0; ; ptr++) {
                if(ptr >= out.length()) {
                    Log.e(TAG, "expected extra text at position " + ptr + ": '" + expectedText.substring(ptr) + "'");
                    if (ptr < expectedText.length()) {
                        Log.e(TAG, "character: '" + expectedText.charAt(ptr) + "', " + Character.codePointAt(expectedText, ptr) );
                    }
                    break;
                }
                if(ptr >= expectedText.length()) {
                    Log.e(TAG, "not expected extra text at position " + ptr + ": '" + out.substring(ptr) + "'");
                    Log.e(TAG, "character: '" + out.charAt(ptr) + "', " + Character.codePointAt(out, ptr));
                    break;
                }

                char cOut = out.charAt(ptr);
                char cExpect = expectedText.charAt(ptr);
                if(cOut != cExpect) {
                    Log.e(TAG, "expected different at position " + ptr );
                    Log.e(TAG, "expected: '" + expectedText.substring(ptr) + "'");
                    Log.e(TAG, "but got: '" + out.substring(ptr) + "'");
                    Log.e(TAG, "expected character: '" + expectedText.charAt(ptr) + "', " + Character.codePointAt(expectedText, ptr) );
                    Log.e(TAG, "but got character: '" + out.charAt(ptr) + "', " + Character.codePointAt(out, ptr));
                    break;
                }
            }
        }
        assertEquals(id, out, expectedText);
    }
}