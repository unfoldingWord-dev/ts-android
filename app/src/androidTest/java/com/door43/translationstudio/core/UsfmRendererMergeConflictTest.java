package com.door43.translationstudio.core;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.door43.translationstudio.rendering.USFMRenderer;
import com.door43.util.FileUtilities;

import org.unfoldingword.tools.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by blm on 7/25/16.
 */
public class UsfmRendererMergeConflictTest extends InstrumentationTestCase {

    public static final String TAG = UsfmRendererMergeConflictTest.class.getSimpleName();
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
        String testId = "render/full_conflict";

        //when
        doRenderMergeConflicts(testId);

        //then
        verifyRenderText("test01ProcessFullConflict");
    }

    public void test02ProcessTwoConflict() throws Exception {
        //given
        String testId = "render/two_conflict";

        //when
        doRenderMergeConflicts(testId);

        //then
        verifyRenderText("test02ProcessTwoConflict");
    }

    public void test03ProcessPartialConflict() throws Exception {
        //given
        String testId = "render/partial_conflict";

        //when
        doRenderMergeConflicts(testId);

        //then
        verifyRenderText("test03ProcessPartialConflict");
    }

    public void test04ProcessNoConflict() throws Exception {
        //given
        String testTextFile = "render/partial_conflict_part1.data";
        String expectTextFile = "render/partial_conflict_part1.data";

        //when
        doRenderMergeConflicts(testTextFile, expectTextFile);

        //then
        verifyRenderText("test04ProcessNoConflict");
    }

    public void test05DetectTwoMergeConflict() throws Exception {
        //given
        String testFile = "render/two_conflict_raw.data";
        boolean expectedConflict = true;

        //when
        boolean conflicted = doDetectMergeConflict(testFile);

        //then
        assertEquals(expectedConflict, conflicted);
    }

    public void test06DetectFullMergeConflict() throws Exception {
        //given
        String testFile = "render/full_conflict_raw.data";
        boolean expectedConflict = true;

        //when
        boolean conflicted = doDetectMergeConflict(testFile);

        //then
        assertEquals(expectedConflict, conflicted);
    }

    public void test07DetectPartialMergeConflict() throws Exception {
        //given
        String testFile = "render/partial_conflict_raw.data";
        boolean expectedConflict = true;

        //when
        boolean conflicted = doDetectMergeConflict(testFile);

        //then
        assertEquals(expectedConflict, conflicted);
    }

    public void test07DetectNoMergeConflict() throws Exception {
        //given
        String testFile = "render/two_conflict_part1.data";
        boolean expectedConflict = false;

        //when
        boolean conflicted = doDetectMergeConflict(testFile);

        //then
        assertEquals(expectedConflict, conflicted);
    }

    private void verifyRenderText(String id) {
        verifyProcessedText(id + ": Head", mExpectedGroup1Text, mGroup1Text);
        verifyProcessedText(id + ": Tail", mExpectedGroup2Text, mGroup2Text);
    }

    private void doRenderMergeConflicts( String testId) throws IOException {
        mGroup1Text = doRenderMergeConflict(testId, USFMRenderer.MergeHeadPart);
        mExpectedGroup1Text = mExpectedText; // save expected text for this section
        mGroup2Text = doRenderMergeConflict(testId, USFMRenderer.MergeTailPart);
        mExpectedGroup2Text = mExpectedText; // save expected text for this section
    }

    private void doRenderMergeConflicts(String testTextFile, String expectTextFile ) throws IOException {
        mGroup1Text = doRenderMergeConflict(USFMRenderer.MergeHeadPart, testTextFile, expectTextFile );
        mExpectedGroup1Text = mExpectedText; // save expected text for this section
        mGroup2Text = doRenderMergeConflict(USFMRenderer.MergeTailPart, testTextFile, expectTextFile );
        mExpectedGroup2Text = mExpectedText; // save expected text for this section
    }

    private boolean doDetectMergeConflict(String testFile) throws IOException {
        InputStream testTextStream = mTestContext.getAssets().open(testFile);
        mTestText = FileUtilities.readStreamToString(testTextStream);
        assertNotNull(mTestText);
        assertFalse(mTestText.isEmpty());

        boolean conflicted = USFMRenderer.isMergeConflicted(mTestText);
        return conflicted;
    }

    private String doRenderMergeConflict(String testId, int sourceGroup) throws IOException {
        String testTextFile = testId+ "_raw.data";
        String expectTextFile = testId+ "_part" + sourceGroup + ".data";
        return doRenderMergeConflict(sourceGroup, testTextFile, expectTextFile);
    }

    private String doRenderMergeConflict(int sourceGroup, String testTextFile, String expectTextFile) throws IOException {
        InputStream testTextStream = mTestContext.getAssets().open(testTextFile);
        mTestText = FileUtilities.readStreamToString(testTextStream);
        assertNotNull(mTestText);
        assertFalse(mTestText.isEmpty());
        InputStream testExpectedStream = mTestContext.getAssets().open(expectTextFile);
        mExpectedText = FileUtilities.readStreamToString(testExpectedStream);
        assertNotNull(mExpectedText);
        assertFalse(mExpectedText.isEmpty());

        String out = (new USFMRenderer()).renderMergeConflict(mTestText, sourceGroup).toString();
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