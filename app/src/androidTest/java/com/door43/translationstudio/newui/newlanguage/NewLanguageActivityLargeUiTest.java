package com.door43.translationstudio.newui.newlanguage;

import android.app.Activity;
import android.content.Intent;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.door43.translationstudio.newui.newtranslation.NewTargetTranslationActivity;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressBack;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class NewLanguageActivityLargeUiTest extends NewLanguageActivityUiUtils {

    @Test
    public void backOutOfApp() throws Exception {

        //given
        String fileName = "new_language/partialQuestionaire.json";
        Intent intent = getIntentForTestFile(fileName);
        int pageCount = 4;
        int pageNum = 0;
        mActivityRule.launchActivity(intent);
        NewLanguageActivity currentActivity = mActivityRule.getActivity();
        setupForCaptureOfResultData();
        verifyPageLayout(pageCount, pageNum);

        //when
        pressBack();

        //then
        int resultCode = getResultCode(currentActivity);
        String key = NewTargetTranslationActivity.EXTRA_NEW_LANGUAGE_DATA;
        Intent data = getResultData(currentActivity);
        clearResultData();
        currentActivity.finish();
        assertTrue("The result code is cancel. ", resultCode == Activity.RESULT_CANCELED);
        assertNull("The new language data is empty. ", data);
    }

    @Test
    public void fillAllPages() throws Exception {

        //given
        String fileName = "new_language/partialQuestionaire.json";
        Intent intent = getIntentForTestFile(fileName);
        int pageCount = 4;
        int pageNum = 0;
        mActivityRule.launchActivity(intent);
        NewLanguageActivity currentActivity = mActivityRule.getActivity();
        setupForCaptureOfResultData();
        verifyPageLayout(pageCount, pageNum);
        boolean expectedBoolean = true;
        boolean hideKeyboard = true;
        boolean requiredOnly = false;
        boolean doDone = true;

        //when
        fillAllPages(doDone, requiredOnly, expectedBoolean, hideKeyboard);

        //then
        int resultCode = getResultCode(currentActivity);
        String key = NewTargetTranslationActivity.EXTRA_NEW_LANGUAGE_DATA;
        JSONObject jsonData = getJsonData(currentActivity, key);
        clearResultData();
        currentActivity.finish();
        assertTrue("The result code is not ok. ", resultCode == Activity.RESULT_OK);
        JSONArray answers = jsonData.getJSONArray("answers");
        assertTrue("The result data is empty. ", answers != null);
        verifyAnswers(answers, expectedBoolean);
    }
}
