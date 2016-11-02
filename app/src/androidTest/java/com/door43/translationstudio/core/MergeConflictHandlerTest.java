package com.door43.translationstudio.core;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.door43.translationstudio.rendering.MergeConflictHandler;
import com.door43.translationstudio.tasks.ParseMergeConflictsTask;
import com.door43.translationstudio.ui.translate.ReviewModeAdapter;
import com.door43.util.FileUtilities;

import org.unfoldingword.tools.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by blm on 7/25/16.
 */
public class MergeConflictHandlerTest extends InstrumentationTestCase {

    public static final String TAG = MergeConflictHandlerTest.class.getSimpleName();
    File mTempFolder;
    private Context mTestContext;
    private String mTestText;
    private String mExpectedText;
    private List<String> mExpectedTexts = new ArrayList<>();
    private String mExpectedHeadText;
    private String mExpectedTailText;
    private List<String>  mParsedText = new ArrayList<>();
    private boolean mExpectFullBlockMergeConflict;
    private boolean mExpectNested;
    private boolean mFoundFullBlockMergeConflict;
    private boolean mFoundNested;
    private int mExpectedConflictCount;
    private int mFoundConflictCount;
    private List<ReviewModeAdapter.MergeConflictCard> mLastMergeConflictCards;

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
        mExpectFullBlockMergeConflict = false;
        mExpectNested = false;
        mExpectedConflictCount = 2;

        //when
        doRenderMergeConflicts(testId);

        //then
        verifyRenderText("test01ProcessFullConflict");
    }

    public void test02ProcessTwoConflict() throws Exception {
        //given
        String testId = "merge/two_conflict";
        mExpectFullBlockMergeConflict = false;
        mExpectNested = false;
        mExpectedConflictCount = 2;

        //when
        doRenderMergeConflicts(testId);

        //then
        verifyRenderText("test02ProcessTwoConflict");
    }

    public void test03ProcessPartialConflict() throws Exception {
        //given
        String testId = "merge/partial_conflict";
        mExpectFullBlockMergeConflict = false;
        mExpectNested = false;
        mExpectedConflictCount = 2;

        //when
        doRenderMergeConflicts(testId);

        //then
        verifyRenderText("test03ProcessPartialConflict");
    }

    public void test04ProcessNestedHeadConflict() throws Exception {
        //given
        String testId = "merge/head_nested_conflict";
        mExpectFullBlockMergeConflict = false;
        mExpectNested = true;
        mExpectedConflictCount = 3;

        //when
        doRenderMergeConflicts(testId);

        //then
        verifyRenderText("test04ProcessNestedHeadConflict");
    }

    public void test05ProcessNestedTailConflict() throws Exception {
        //given
        String testId = "merge/tail_nested_conflict";
        mExpectFullBlockMergeConflict = false;
        mExpectNested = true;
        mExpectedConflictCount = 3;

        //when
        doRenderMergeConflicts(testId);

        //then
        verifyRenderText("test05ProcessNestedTailConflict");
    }

    public void test06ProcessNotFullNestedConflict() throws Exception {
        //given
        String testId = "merge/not_full_double_nested_conflict";
        mExpectFullBlockMergeConflict = false;
        mExpectNested = true;
        mExpectedConflictCount = 4;

        //when
        doRenderMergeConflicts(testId);

        //then
        verifyRenderText("test06ProcessNotFullNestedConflict");
    }

    public void test07ProcessNotFullEndNestedConflict() throws Exception {
        //given
        String testId = "merge/not_full_double_nested_conflict_end";
        mExpectFullBlockMergeConflict = false;
        mExpectNested = true;
        mExpectedConflictCount = 4;

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
        mExpectNested = false;
        mExpectedConflictCount = 1;

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

    public void test13ProcessFullTripleNestedConflict() throws Exception {
        //given
        String testId = "merge/full_triple_nested";
        mExpectFullBlockMergeConflict = false;
        mExpectNested = false;
        mExpectedConflictCount = 5;

        //when
        doRenderMergeConflicts(testId);

        //then
        verifyRenderText("test02ProcessTwoConflict");
    }



    private void verifyRenderText(String id) {

        assertEquals("merge counts should be the same", mExpectedTexts.size(), mParsedText.size());

        //sort to make sure in same order
        Collections.sort(mParsedText);
        Collections.sort(mExpectedTexts);

        for (int i = 0; i < mParsedText.size(); i++) {
            String got = mParsedText.get(i);
            String expected = mExpectedTexts.get(i);
            verifyProcessedText(id + ": Conflict text " + i, expected, got);
        }

        assertEquals(id + ": FullBlockMergeConflict", mExpectFullBlockMergeConflict, mFoundFullBlockMergeConflict);
    }

    private void doRenderMergeConflicts( String testId) throws IOException {
        for(int i = 0; i < mExpectedConflictCount; i++) {
            String text = doRenderMergeConflict(testId, i + 1);
            mParsedText.add(text);
            mExpectedTexts.add(mExpectedText);
        }

        if(mExpectedConflictCount != mFoundConflictCount) {
            for (int i = 0; i < mLastMergeConflictCards.size(); i++) {
                ReviewModeAdapter.MergeConflictCard conflict = mLastMergeConflictCards.get(i);
                Log.d(TAG, "conflict card " + i + ":\n" + conflict.text );
            }
            assertEquals("conflict count", mExpectedConflictCount, mFoundConflictCount);
        }
    }

    private void doRenderMergeConflicts(String testTextFile, String expectTextFile ) throws IOException {
        String text = doRenderMergeConflict(MergeConflictHandler.MERGE_HEAD_PART, testTextFile, expectTextFile );
        mParsedText.add(text);
        mExpectedTexts.add(mExpectedText);

        if(mLastMergeConflictCards.size() > 1) {
            text = doRenderMergeConflict(MergeConflictHandler.MERGE_TAIL_PART, testTextFile, expectTextFile);
            mParsedText.add(text);
            mExpectedTexts.add(mExpectedText);
        }
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

        ParseMergeConflictsTask parseTask = new ParseMergeConflictsTask(1, mTestText);
        parseTask.start();
        mLastMergeConflictCards = parseTask.getMergeConflictItems();
        mFoundNested = false;
        mFoundFullBlockMergeConflict = parseTask.isFullMergeConflict();
        mFoundConflictCount = mLastMergeConflictCards.size();
        if(mFoundConflictCount >= sourceGroup) {
            return mLastMergeConflictCards.get(sourceGroup - 1).text.toString();
        }
        return null;

//        MergeConflictHandler renderer = new MergeConflictHandler();
//        renderer.renderMergeConflict(mTestText, sourceGroup);
//        String out = renderer.getConflictPart(sourceGroup).toString();

//        mFoundFullBlockMergeConflict = renderer.isFullBlockMergeConflict();
//        mFoundNested = renderer.isNested(sourceGroup);

//        return out;
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
                    Log.e(TAG, "expected sub match: '" + expectedText.substring(ptr) + "'");
                    Log.e(TAG, "but got sub match: '" + out.substring(ptr) + "'");
                    Log.e(TAG, "expected character: '" + expectedText.charAt(ptr) + "', " + Character.codePointAt(expectedText, ptr) );
                    Log.e(TAG, "but got character: '" + out.charAt(ptr) + "', " + Character.codePointAt(out, ptr));
                    Log.e(TAG, "expected: '" + expectedText + "'");
                    Log.e(TAG, "but got: '" + out + "'");
                    break;
                }
            }
            Log.e(TAG, "error in: " + id);
        }
        if(!out.equals(expectedText)) {
            assertEquals(id, out, expectedText);
        }
    }
}