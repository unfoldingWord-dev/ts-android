package com.door43.translationstudio.newui.newlanguage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.NoActivityResumedException;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.KeyEvent;
import android.view.View;

import com.door43.translationstudio.R;
import com.door43.translationstudio.newui.newtranslation.NewTargetTranslationActivity;

import org.hamcrest.Matcher;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressBack;
import static android.support.test.espresso.action.ViewActions.pressKey;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class NewLanguageActivityLargeUiTest extends NewLanguageActivityUiUtils {

    @Test
    public void fillToPage2AndPrevious() throws Exception {

        //given
        String fileName = "new_language/fullQuestionaire.json";
        Intent intent = getIntentForTestFile(fileName);
        int pageCount = 11;
        int pageNum = 0;
        boolean hideKeyboard = true;
        boolean requiredOnly = false;
        boolean valueForBooleans = false;
        boolean doNext = true;
        NewLanguageActivity currentActivity = launchNewLanguageActivity(intent);
        verifyPageLayout(pageCount, pageNum);
        fillPage(pageNum, doNext, requiredOnly, valueForBooleans, hideKeyboard);
        int pageNumExpected = 1;
        rotateScreen();
        verifyPageLayout(pageCount, pageNumExpected);

        //when
        rotateScreen();
        onView(withId(R.id.previous_button)).perform(click());
        rotateScreen();
        //then
        pageNumExpected = 0;
        verifyPageLayout(pageCount, pageNumExpected);
        rotateScreen();
    }

   @Test
    public void fillToPage2AndBack() throws Exception {

        //given
        String fileName = "new_language/fullQuestionaire.json";
        Intent intent = getIntentForTestFile(fileName);
        int pageCount = 11;
        int pageNum = 0;
        boolean hideKeyboard = true;
        boolean requiredOnly = false;
        boolean valueForBooleans = false;
        boolean doNext = true;
        NewLanguageActivity currentActivity = launchNewLanguageActivity(intent);
        verifyPageLayout(pageCount, pageNum);
        fillPage(pageNum, doNext, requiredOnly, valueForBooleans, hideKeyboard);
        int pageNumExpected = 1;
        verifyPageLayout(pageCount, pageNumExpected);
        boolean appExit = false;

        //when
        try {
            onView(withId(R.id.toolbar)).perform(pressBack());
        } catch (NoActivityResumedException e) {
            appExit  = true;
        }

        //then
        if(appExit) {
            assertTrue("App exited by throwing exception", appExit);
        } else {
            assertActivityReturnedCancelled(currentActivity);
        }
    }

    private NewLanguageActivity launchNewLanguageActivity(Intent intent) throws NoSuchFieldException {
        mActivityRule.launchActivity(intent);
        NewLanguageActivity currentActivity = mActivityRule.getActivity();
        setupForCaptureOfResultData();
        return currentActivity;
    }

    @Test
    public void fillToPage2AndHome() throws Exception {

        //given
        String fileName = "new_language/fullQuestionaire.json";
        Intent intent = getIntentForTestFile(fileName);
        int pageCount = 11;
        int pageNum = 0;
        boolean hideKeyboard = true;
        boolean requiredOnly = false;
        boolean valueForBooleans = false;
        boolean doNext = true;
        NewLanguageActivity currentActivity = launchNewLanguageActivity(intent);
        verifyPageLayout(pageCount, pageNum);
        fillPage(pageNum, doNext, requiredOnly, valueForBooleans, hideKeyboard);
        int pageNumExpected = 1;
        verifyPageLayout(pageCount, pageNumExpected);
        boolean appExit = false;

        //when
        try {
//            onView(withId(R.id.toolbar)).perform(pressKey(KeyEvent.KEYCODE_HOME));
            onView(withContentDescription(R.string.abc_action_bar_up_description)).perform(click());
        } catch (NoActivityResumedException e) {
            appExit  = true;
        }

        //then
        if(appExit) {
            assertTrue("App exited by throwing exception", appExit);
        } else {
            assertActivityReturnedCancelled(currentActivity);
        }
    }


    @Test
    public void fillAllPages() throws Exception {

        //given
        String fileName = "new_language/partialQuestionaire.json";
        Intent intent = getIntentForTestFile(fileName);
        int pageCount = 4;
        int pageNum = 0;
        NewLanguageActivity currentActivity = launchNewLanguageActivity(intent);
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

    private void assertActivityReturnedCancelled(NewLanguageActivity currentActivity) throws NoSuchFieldException, IllegalAccessException {
        int resultCode = getResultCode(currentActivity);
        clearResultData();
        currentActivity.finish();
        assertTrue("The result code is cancel. ", resultCode == Activity.RESULT_CANCELED);
    }

}
