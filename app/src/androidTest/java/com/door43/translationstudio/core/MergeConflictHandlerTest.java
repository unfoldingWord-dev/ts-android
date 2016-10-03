package com.door43.translationstudio.core;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.door43.translationstudio.rendering.MergeConflictHandler;
import com.door43.util.FileUtilities;

import org.unfoldingword.tools.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by blm on 7/25/16.
 */
public class MergeConflictHandlerTest extends InstrumentationTestCase {

    public static final String TAG = MergeConflictHandlerTest.class.getSimpleName();
    File mTempFolder;
    private Context mTestContext;
    private String mTestText;
    private String mExpectedText;
    private String mExpectedHeadText;
    private String mExpectedTailText;
    private String mHeadText;
    private String mTailText;
    private boolean mExpectFullBlockMergeConflict;
    private boolean mExpectHeadNested;
    private boolean mExpectTailNested;
    private boolean mFoundFullBlockMergeConflict;
    private boolean mFoundHeadNested;
    private boolean mFoundTailNested;
    private boolean mFoundNested;


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
        String testId = "merge/full_conflict";
        mExpectFullBlockMergeConflict = true;
        mExpectHeadNested = false;
        mExpectTailNested = false;

        //when
        doRenderMergeConflicts(testId);

        //then
        verifyRenderText("test01ProcessFullConflict");
    }

    public void test02ProcessTwoConflict() throws Exception {
        //given
        String testId = "merge/two_conflict";
        mExpectFullBlockMergeConflict = false;
        mExpectHeadNested = false;
        mExpectTailNested = false;

        //when
        doRenderMergeConflicts(testId);

        //then
        verifyRenderText("test02ProcessTwoConflict");
    }

    public void test03ProcessPartialConflict() throws Exception {
        //given
        String testId = "merge/partial_conflict";
        mExpectFullBlockMergeConflict = false;
        mExpectHeadNested = false;
        mExpectTailNested = false;

        //when
        doRenderMergeConflicts(testId);

        //then
        verifyRenderText("test03ProcessPartialConflict");
    }

    public void test04ProcessNestedHeadConflict() throws Exception {
        //given
        String testId = "merge/head_nested_conflict";
        mExpectFullBlockMergeConflict = true;
        mExpectHeadNested = true;
        mExpectTailNested = false;

        //when
        doRenderMergeConflicts(testId);

        //then
        verifyRenderText("test04ProcessNestedHeadConflict");
    }

    public void test05ProcessNestedTailConflict() throws Exception {
        //given
        String testId = "merge/tail_nested_conflict";
        mExpectFullBlockMergeConflict = true;
        mExpectHeadNested = false;
        mExpectTailNested = true;

        //when
        doRenderMergeConflicts(testId);

        //then
        verifyRenderText("test05ProcessNestedTailConflict");
    }

    public void test06ProcessNotFullNestedConflict() throws Exception {
        //given
        String testId = "merge/not_full_double_nested_conflict";
        mExpectFullBlockMergeConflict = false;
        mExpectHeadNested = true;
        mExpectTailNested = true;

        //when
        doRenderMergeConflicts(testId);

        //then
        verifyRenderText("test06ProcessNotFullNestedConflict");
    }

    public void test07ProcessNotFullEndNestedConflict() throws Exception {
        //given
        String testId = "merge/not_full_double_nested_conflict_end";
        mExpectFullBlockMergeConflict = false;
        mExpectHeadNested = true;
        mExpectTailNested = true;

        //when
        doRenderMergeConflicts(testId);

        //then
        verifyRenderText("test07ProcessNotFullEndNestedConflict");
    }

    public void test08ProcessNoConflict() throws Exception {
        //given
        String testTextFile = "merge/partial_conflict_part1.data";
        String expectTextFile = "merge/partial_conflict_part1.data";
        mExpectFullBlockMergeConflict = false;
        mExpectHeadNested = false;
        mExpectTailNested = false;

        //when
        doRenderMergeConflicts(testTextFile, expectTextFile);

        //then
        verifyRenderText("test07ProcessNoConflict");
    }

    public void test09DetectTwoMergeConflict() throws Exception {
        //given
        String testFile = "merge/two_conflict_raw.data";
        boolean expectedConflict = true;

        //when
        boolean conflicted = doDetectMergeConflict(testFile);

        //then
        assertEquals(expectedConflict, conflicted);
    }

    public void test10DetectFullMergeConflict() throws Exception {
        //given
        String testFile = "merge/full_conflict_raw.data";
        boolean expectedConflict = true;

        //when
        boolean conflicted = doDetectMergeConflict(testFile);

        //then
        assertEquals(expectedConflict, conflicted);
    }

    public void test11DetectPartialMergeConflict() throws Exception {
        //given
        String testFile = "merge/partial_conflict_raw.data";
        boolean expectedConflict = true;

        //when
        boolean conflicted = doDetectMergeConflict(testFile);

        //then
        assertEquals(expectedConflict, conflicted);
    }

    public void test12DetectNoMergeConflict() throws Exception {
        //given
        String testFile = "merge/two_conflict_part1.data";
        boolean expectedConflict = false;

        //when
        boolean conflicted = doDetectMergeConflict(testFile);

        //then
        assertEquals(expectedConflict, conflicted);
    }

    private void verifyRenderText(String id) {
        verifyProcessedText(id + ": Head", mExpectedHeadText, mHeadText);
        verifyProcessedText(id + ": Tail", mExpectedTailText, mTailText);

        assertEquals(id + ": FullBlockMergeConflict", mExpectFullBlockMergeConflict, mFoundFullBlockMergeConflict);
        assertEquals(id + ": HeadNested", mExpectHeadNested, mFoundHeadNested);
        assertEquals(id + ": TailNested", mExpectTailNested, mFoundTailNested);
    }

    private void doRenderMergeConflicts( String testId) throws IOException {
        mHeadText = doRenderMergeConflict(testId, MergeConflictHandler.MERGE_HEAD_PART);
        mExpectedHeadText = mExpectedText; // save expected text for this section
        boolean foundFullBlockMergeConflict = mFoundFullBlockMergeConflict;
        mFoundHeadNested = mFoundNested;

        mTailText = doRenderMergeConflict(testId, MergeConflictHandler.MERGE_TAIL_PART);
        mExpectedTailText = mExpectedText; // save expected text for this section
        mFoundTailNested = mFoundNested;

        assertEquals(testId + ": mFoundFullBlockMergeConflict should get same results from both passes", foundFullBlockMergeConflict, mFoundFullBlockMergeConflict);
    }

    private void doRenderMergeConflicts(String testTextFile, String expectTextFile ) throws IOException {
        mHeadText = doRenderMergeConflict(MergeConflictHandler.MERGE_HEAD_PART, testTextFile, expectTextFile );
        mExpectedHeadText = mExpectedText; // save expected text for this section
        boolean foundFullBlockMergeConflict = mFoundFullBlockMergeConflict;
        mFoundHeadNested = mFoundNested;

        mTailText = doRenderMergeConflict(MergeConflictHandler.MERGE_TAIL_PART, testTextFile, expectTextFile );
        mExpectedTailText = mExpectedText; // save expected text for this section
        mFoundTailNested = mFoundNested;

        assertEquals(testTextFile + ": mFoundFullBlockMergeConflict should get same results from both passes", foundFullBlockMergeConflict,mFoundFullBlockMergeConflict);
    }

    private boolean doDetectMergeConflict(String testFile) throws IOException {
        InputStream testTextStream = mTestContext.getAssets().open(testFile);
        mTestText = FileUtilities.readStreamToString(testTextStream);
        assertNotNull(mTestText);
        assertFalse(mTestText.isEmpty());

        boolean conflicted = MergeConflictHandler.isMergeConflicted(mTestText);
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

        MergeConflictHandler renderer = new MergeConflictHandler();
        renderer.renderMergeConflict(mTestText, sourceGroup);
        String out = renderer.getConflictPart(sourceGroup).toString();

        mFoundFullBlockMergeConflict = renderer.isFullBlockMergeConflict();
        mFoundNested = renderer.isNested(sourceGroup);

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
            Log.e(TAG, "error in: " + id);
        }
        assertEquals(id, out, expectedText);
    }
}