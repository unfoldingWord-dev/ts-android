package com.door43.translationstudio.newui.newlanguage;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.door43.translationstudio.R;
import com.door43.translationstudio.tasks.UploadCrashReportTask;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class NewLanguageActivityTest {

    private String mStringToBetyped;

    @Rule
    public ActivityTestRule<NewLanguageActivity> mActivityRule = new ActivityTestRule<>(
            NewLanguageActivity.class,
            true,    // initialTouchMode
            false);  // don't launchActivity yet

    @Before
    public void init() {
        // Specify a valid string.
        mStringToBetyped = "Espresso";
        UploadCrashReportTask.archiveErrorLogs();
    }

    @Test
    public void changeText_sameActivity() {
        Intent intent = new Intent();
        intent.putExtra(NewLanguageActivity.EXTRA_CALLING_ACTIVITY, NewLanguageActivity.ACTIVITY_HOME);

        mActivityRule.launchActivity(intent);

        // Type text and then press the button.
        ViewInteraction viewInteraction = onView(withId(R.id.fragment_container));
        viewInteraction.perform(typeText(mStringToBetyped), closeSoftKeyboard());
    }
}