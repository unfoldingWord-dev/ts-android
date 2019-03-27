package com.door43.translationstudio.core;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.door43.translationstudio.ui.translate.ReviewModeAdapter;
import com.door43.translationstudio.rendering.Clickables;
import com.door43.translationstudio.rendering.RenderingGroup;
import com.door43.util.FileUtilities;

import org.unfoldingword.tools.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by blm on 7/25/16.
 */
public class UsxBrokenRenderTest extends InstrumentationTestCase {

    public static final String TAG = UsxBrokenRenderTest.class.getSimpleName();
    File mTempFolder;
    private Context mTestContext;
    private String mTestText;
    private String mExpectedText;


    @Override
    public void setUp() throws Exception {
        super.setUp();
        Logger.flush();
        mTestContext = getInstrumentation().getContext();
    }

    @Override
    public void tearDown() throws Exception {
    }

    public void test01ProcessMk_1_1() throws Exception {
        //given
        String search = null;
        String testId = "usx/mk_1_1";

        //when
        String out = doRender(search, testId);

        //then
        verifyProcessedText(mExpectedText, out);
    }

    public void test02ProcessMk_7_6() throws Exception {
        //given
        String search = null;
        String testId = "usx/mk_7_6";

        //when
        String out = doRender(search, testId);

        //then
        verifyProcessedText(mExpectedText, out);
    }

    public void test03ProcessMk_7_14() throws Exception {
        //given
        String search = null;
        String testId = "usx/mk_7_14";

        //when
        String out = doRender(search, testId);

        //then
        verifyProcessedText(mExpectedText, out);
    }

    public void test04ProcessMk_11_24() throws Exception {
        //given
        String search = null;
        String testId = "usx/mk_11_24";

        //when
        String out = doRender(search, testId);

        //then
        verifyProcessedText(mExpectedText, out);
    }

    public void test05ProcessMk_16_19() throws Exception {
        //given
        String search = null;
        String testId = "usx/mk_16_19";

        //when
        String out = doRender(search, testId);

        //then
        verifyProcessedText(mExpectedText.trim(), out.trim());
    }

    public void test06ProcessMk_1_1Search() throws Exception {
        //given
        String search = "</"; // make sure matching part of token does not break rendering
        String testId = "usx/mk_1_1";

        //when
        String out = doRender(search, testId);

        //then
        verifyProcessedText(mExpectedText, out);
    }

    public void test07ProcessMk_7_6Search() throws Exception {
        //given
        String search = "</"; // make sure matching part of token does not break rendering
        String testId = "usx/mk_7_6";

        //when
        String out = doRender(search, testId);

        //then
        verifyProcessedText(mExpectedText, out);
    }

    public void test08ProcessMk_7_14Search() throws Exception {
        //given
        String search = "</"; // make sure matching part of token does not break rendering
        String testId = "usx/mk_7_14";

        //when
        String out = doRender(search, testId);

        //then
        verifyProcessedText(mExpectedText, out);
    }

    public void test09ProcessMk_11_24Search() throws Exception {
        //given
        String search = "</"; // make sure matching part of token does not break rendering
        String testId = "usx/mk_11_24";

        //when
        String out = doRender(search, testId);

        //then
        verifyProcessedText(mExpectedText, out);
    }

    public void test10ProcessMk_16_19Search() throws Exception {
        //given
        String search = "</"; // make sure matching part of token does not break rendering
        String testId = "usx/mk_16_19";

        //when
        String out = doRender(search, testId);

        //then
        verifyProcessedText(mExpectedText.trim(), out.trim());
    }

    private String doRender(String search, String testId) throws IOException {
        String testTextFile = testId+ "_raw.data";
        String expectTextFile = testId+ "_processed.data";
        InputStream testTextStream = mTestContext.getAssets().open(testTextFile);
        mTestText = FileUtilities.readStreamToString(testTextStream);
        assertNotNull(mTestText);
        assertFalse(mTestText.isEmpty());
        InputStream testExpectedStream = mTestContext.getAssets().open(expectTextFile);
        mExpectedText = FileUtilities.readStreamToString(testExpectedStream);
        assertNotNull(mExpectedText);
        assertFalse(mExpectedText.isEmpty());
        mExpectedText = mExpectedText.substring(0,mExpectedText.length() - 1); // rendering does trim of whitespace
        RenderingGroup renderingGroup = new RenderingGroup();
        TranslationFormat format = TranslationFormat.USX;
        Clickables.setupRenderingGroup(format, renderingGroup, null, null, false);

        if( search != null ) {
            renderingGroup.setSearchString(search, ReviewModeAdapter.HIGHLIGHT_COLOR);
        }
        renderingGroup.init(mTestText);
        return renderingGroup.start().toString();
    }

    private void verifyProcessedText(String expectedText, String out) {
        assertNotNull(out);
        assertFalse(out.isEmpty());
        if(!out.equals(expectedText)) {
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
        assertEquals(out, expectedText);
    }


}