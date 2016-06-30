package com.door43.translationstudio.newui.newlanguage;

import android.app.Activity;
import android.content.Intent;
import android.support.test.espresso.NoActivityResumedException;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Questionnaire;

import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressBack;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class QuestionnaireActivityLargeUiTest extends NewLanguageActivityUiUtils {

    private int pageCount() {
        Questionnaire[] questionnaires = App.getLibrary().getQuestionnaires();
        return questionnaires[0].getNumPages();
    }

//    @Test
    public void fillToPage2AndPrevious() throws Exception {

        //given
        int pageNum = 0;
        boolean hideKeyboard = true;
        boolean requiredOnly = false;
        boolean valueForBooleans = false;
        boolean doNext = true;
        NewTempLanguageActivity currentActivity = launchNewLanguageActivity();
        verifyPageLayout(pageCount(), pageNum);
        fillPage(pageNum, doNext, requiredOnly, valueForBooleans, hideKeyboard);
        int pageNumExpected = 1;
        rotateScreen();
        verifyPageLayout(pageCount(), pageNumExpected);

        //when
        onView(withId(R.id.previous_button)).perform(click());
        rotateScreen();

        //then
        pageNumExpected = 0;
        verifyPageLayout(pageCount(), pageNumExpected);
        rotateScreen();
    }

//   @Test
    public void fillToPage2AndBack() throws Exception {
        //given
        int pageNum = 0;
        boolean hideKeyboard = true;
        boolean requiredOnly = false;
        boolean valueForBooleans = false;
        boolean doNext = true;
        NewTempLanguageActivity currentActivity = launchNewLanguageActivity();
        verifyPageLayout(pageCount(), pageNum);
        fillPage(pageNum, doNext, requiredOnly, valueForBooleans, hideKeyboard);
        int pageNumExpected = 1;
        verifyPageLayout(pageCount(), pageNumExpected);
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

    private NewTempLanguageActivity launchNewLanguageActivity() throws NoSuchFieldException {
        mActivityRule.launchActivity(new Intent());
        NewTempLanguageActivity currentActivity = mActivityRule.getActivity();
        setupForCaptureOfResultData();
        return currentActivity;
    }

//    @Test
    public void fillToPage2AndHome() throws Exception {

        //given
        int pageNum = 0;
        boolean hideKeyboard = true;
        boolean requiredOnly = false;
        boolean valueForBooleans = false;
        boolean doNext = true;
        NewTempLanguageActivity currentActivity = launchNewLanguageActivity();
        verifyPageLayout(pageCount(), pageNum);
        fillPage(pageNum, doNext, requiredOnly, valueForBooleans, hideKeyboard);
        int pageNumExpected = 1;
        verifyPageLayout(pageCount(), pageNumExpected);
        boolean appExit = false;

        //when
        try {
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


//    @Test
    public void fillAllPages() throws Exception {

        //given
        int pageNum = 0;
        NewTempLanguageActivity currentActivity = launchNewLanguageActivity();
        verifyPageLayout(pageCount(), pageNum);
        boolean expectedBoolean = true;
        boolean hideKeyboard = true;
        boolean requiredOnly = false;
        boolean doDone = true;

        //when
        fillAllPages(doDone, requiredOnly, expectedBoolean, hideKeyboard);

        //then
        int resultCode = getResultCode(currentActivity);
        clearResultData();
        currentActivity.finish();
        assertTrue("The result code is not ok. ", resultCode == Activity.RESULT_OK);
    }

    private void assertActivityReturnedCancelled(NewTempLanguageActivity currentActivity) throws NoSuchFieldException, IllegalAccessException {
        int resultCode = getResultCode(currentActivity);
        clearResultData();
        currentActivity.finish();
        assertTrue("The result code is cancel. ", resultCode == Activity.RESULT_CANCELED);
    }

}
