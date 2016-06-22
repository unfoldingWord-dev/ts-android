package com.door43.translationstudio.newui.newlanguage;

import android.content.Intent;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Questionnaire;

import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class QuestionnaireActivityUiTest extends NewLanguageActivityUiUtils {

    private int pageCount() {
        Questionnaire[] questionnaires = App.getLibrary().getQuestionnaires();
        return questionnaires[0].getNumPages();
    }
    @Test
    public void fillPageBoolean() throws Exception {

        //given
        int pageNum = 0;
        boolean hideKeyboard = false;
        boolean requiredOnly = false;
        boolean valueForBooleans = false;
        boolean doNext = true;
        mActivityRule.launchActivity(new Intent());
        verifyPageLayout(pageCount(), pageNum);
        fillPage(pageNum, true, false, false, hideKeyboard);
        verifyPageLayout(pageCount(), pageNum + 1);

        //when
        fillPage(pageNum + 1, doNext, requiredOnly, valueForBooleans, hideKeyboard);

        //then
        verifyPageLayout(pageCount(), pageNum + 2);
    }


    @Test
    public void fillPageRequired() throws Exception {

        //given
        int pageNum = 0;
        boolean hideKeyboard = false;
        boolean requiredOnly = true;
        boolean valueForBooleans = true;
        boolean doNext = true;
        mActivityRule.launchActivity(new Intent());
        verifyPageLayout(pageCount(), pageNum);

        //when
        fillPage(pageNum, doNext, requiredOnly, valueForBooleans, hideKeyboard);

        //then
        thenShouldHaveMissingAnswerDialog();
    }

    @Test
    public void fillPageNotRequired() throws Exception {

        //given
        int pageNum = 0;
        boolean hideKeyboard = false;
        boolean requiredOnly = false;
        boolean valueForBooleans = false;
        boolean doNext = true;
        mActivityRule.launchActivity(new Intent());
        verifyPageLayout(pageCount(), pageNum);

        //when
        fillPage(pageNum, doNext, requiredOnly, valueForBooleans, hideKeyboard);

        //then
        int pageNumExpected = 1;
        verifyPageLayout(pageCount(), pageNumExpected);
    }


    @Test
    public void requiredAnswerContinue() throws Exception {

        //given
        int pageNum = 0;
        mActivityRule.launchActivity(new Intent());
        verifyPageLayout(pageCount(), pageNum);

        //when
//        currentActivity.recreate();
        onView(withId(R.id.next_button)).perform(click());
        thenShouldHaveRequiredAnswerDialog();
        onView(withText(R.string.dismiss)).perform(click());

        //then
        verifyPageLayout(pageCount(), pageNum);
    }

    @Test
    public void missingAnswerContinue() throws Exception {

        //given
        int pageNum = 0;
        boolean hideKeyboard = true;
        mActivityRule.launchActivity(new Intent());
        verifyPageLayout(pageCount(), pageNum);
        addEditText(0, 0, "language_name", hideKeyboard);

        //when
//        currentActivity.recreate();
        onView(withId(R.id.next_button)).perform(click());
        thenShouldHaveMissingAnswerDialog();
        onView(withId(R.id.positiveButton)).perform(click());

        //then
        int pageNumExpected = 1;
        verifyPageLayout(pageCount(), pageNumExpected);
    }

    @Test
    public void missingAnswerCancel() throws Exception {

        //given
        int pageNum = 0;
        boolean hideKeyboard = true;
        mActivityRule.launchActivity(new Intent());
        verifyPageLayout(pageCount(), pageNum);
        addEditText(0, 0, "language_name", hideKeyboard);

        //when
//        currentActivity.recreate();
        onView(withId(R.id.next_button)).perform(click());
        thenShouldHaveMissingAnswerDialog();
        onView(withId(R.id.negativeButton)).perform(click());

        //then
        verifyPageLayout(pageCount(), pageNum);
    }

}
