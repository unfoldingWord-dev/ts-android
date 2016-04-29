package com.door43.translationstudio.newui.newlanguage;

import org.apache.commons.io.IOUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.support.v7.widget.Toolbar;

import com.door43.translationstudio.R;
import com.door43.translationstudio.tasks.UploadCrashReportTask;

import java.io.IOException;
import java.io.InputStream;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class NewLanguageActivityUiTest {

    private String mStringToBetyped;

    @Rule
    public ActivityTestRule<NewLanguageActivity> mActivityRule = new ActivityTestRule<>(
            NewLanguageActivity.class,
            true,    // initialTouchMode
            false);  // don't launchActivity yet

    @Before
    public void setUp() {
        // Specify a valid string.
        mStringToBetyped = "Espresso";
        UploadCrashReportTask.archiveErrorLogs();
    }

    @Test
    public void changeText_sameActivity() throws Exception {

        //given
        String fileName = "new_language/fullQuestionaire.json";
        Intent intent = getIntentForTestFile(fileName);
        int pageCount = 11;
        int pageNum = 1;

        //when
        mActivityRule.launchActivity(intent);

//        // Type text and then press the button.
//        ViewInteraction viewInteraction = onView(withId(R.id.fragment_container));
//        viewInteraction.perform(typeText(mStringToBetyped), closeSoftKeyboard());

        //then
        thenTitlePageCountShouldMatch(pageCount, pageNum);

        Activity currentActivity = mActivityRule.getActivity();
        currentActivity.recreate();
    }

    private void thenTitlePageCountShouldMatch(int pageCount, int pageNum) {
        Context appContext = InstrumentationRegistry.getTargetContext();
        String titleFormat = appContext.getResources().getString(R.string.new_lang_question_n);

        String title = String.format(titleFormat, pageNum, pageCount);
        matchToolbarTitle(title);
    }

    private Intent getIntentForTestFile(String fileName) throws IOException {
        Intent intent = new Intent();
        intent.putExtra(NewLanguageActivity.EXTRA_CALLING_ACTIVITY, NewLanguageActivity.ACTIVITY_HOME);

        Context testContext = InstrumentationRegistry.getContext();
        Resources testRes = testContext.getResources();

        InputStream usfmStream = testContext.getAssets().open(fileName);
        String questions = IOUtils.toString(usfmStream, "UTF-8");
        intent.putExtra(NewLanguageActivity.EXTRA_NEW_LANGUAGE_QUESTIONS_JSON,questions);
        return intent;
    }

    private static ViewInteraction matchToolbarTitle(
            CharSequence title) {
        return onView(isAssignableFrom(Toolbar.class))
                .check(matches(withToolbarTitle(is(title))));
    }

    private static Matcher<Object> withToolbarTitle(
            final Matcher<CharSequence> textMatcher) {
        return new BoundedMatcher<Object, Toolbar>(Toolbar.class) {
            @Override public boolean matchesSafely(Toolbar toolbar) {
                return textMatcher.matches(toolbar.getTitle());
            }
            @Override public void describeTo(Description description) {
                description.appendText("with toolbar title: ");
                textMatcher.describeTo(description);
            }
        };
    }
}