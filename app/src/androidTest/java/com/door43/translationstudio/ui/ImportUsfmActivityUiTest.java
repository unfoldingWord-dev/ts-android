package com.door43.translationstudio.ui;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.unfoldingword.tools.logger.Logger;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.Toolbar;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Profile;
import com.door43.translationstudio.core.Translator;
import com.door43.util.FileUtilities;


import java.io.File;
import java.io.InputStream;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class ImportUsfmActivityUiTest {

    private File mTestFile;
    private File mTempDir;
    private Context mTestContext;
    private String mTargetTranslationID;

    @Rule
    public ActivityTestRule<ImportUsfmActivity> mActivityRule = new ActivityTestRule<>(
            ImportUsfmActivity.class,
            true,    // initialTouchMode
            false);  // don't launchActivity yet

    @Before
    public void setUp() {
        mTestContext = InstrumentationRegistry.getContext();
        Logger.flush();
        if(App.getProfile() == null) { // make sure this is initialized
            App.setProfile(new Profile("testing"));
        }
    }

    @After
    public void tearDown() {
        removeTranslation();
        if(mTempDir != null) {
            FileUtilities.deleteQuietly(mTempDir);
            mTempDir = null;
            mTestFile = null;
        }
        finishActivity();
    }

    @Test
    public void markNoChapter() throws Exception {

        //given
        String testFile = "usfm/mrk_no_chapter.usfm";
        String book = "mrk";
        String language = "aa";
        initForImport(book, language);
        Intent intent = getIntentForTestFile(testFile);
        mActivityRule.launchActivity(intent);
        checkDisplayState(R.string.title_activity_import_usfm_language, true);
        onView(withText(language)).perform(click());
        boolean seen = waitWhileDisplayed(R.string.reading_usfm);

        //when
        matchSummaryDialog(R.string.title_import_usfm_error, book, false);
        rotateScreen();

        //then
        matchSummaryDialog(R.string.title_import_usfm_error, book, false);
        rotateScreen();
    }


    @Test
    public void markNoId() throws Exception {

        //given
        String testFile = "usfm/mrk_no_id.usfm";
        String book = "mrk";
        String language = "aa";
        initForImport(book, language);
        Intent intent = getIntentForTestFile(testFile);
        mActivityRule.launchActivity(intent);
        checkDisplayState(R.string.title_activity_import_usfm_language, true);
        onView(withText(language)).perform(click());
        boolean seen = waitWhileDisplayed(R.string.reading_usfm);
        thenShouldShowMissingBookNameDialog();
        rotateScreen();

        //when
        thenShouldShowMissingBookNameDialog();
        onView(withText(R.string.label_continue)).perform(click());
        clickOnViewText("Bible: NT");
        clickOnViewText("Mark");

        //then
        boolean seen2 = waitWhileDisplayed(R.string.reading_usfm);
        matchSummaryDialog(R.string.title_processing_usfm_summary, book, true);
        rotateScreen();
        matchSummaryDialog(R.string.title_processing_usfm_summary, book, true);
    }


    @Test
    public void judeValid() throws Exception {

        //given
        String testFile = "usfm/66-JUD.usfm";
        String book = "jud";
        String language = "aa";
        initForImport(book, language);
        Intent intent = getIntentForTestFile(testFile);
        mActivityRule.launchActivity(intent);
        checkDisplayState(R.string.title_activity_import_usfm_language, true);
        onView(withText(language)).perform(click());
        boolean seen = waitWhileDisplayed(R.string.reading_usfm);
        matchSummaryDialog(R.string.title_processing_usfm_summary, book, true);
        rotateScreen();
        matchSummaryDialog(R.string.title_processing_usfm_summary, book, true);

        //when
        onView(withText(R.string.label_continue)).perform(click());

        //then
        boolean seen2 = waitWhileDisplayed(R.string.importing_usfm);
        matchImportResultsDialog(true);
        rotateScreen();
        matchImportResultsDialog(true);
    }

    @Test
    public void judeNoVerses() throws Exception {

        //given
        String testFile = "usfm/jude.no_verses.usfm";
        String book = "jud";
        String language = "aa";
        initForImport(book, language);
        Intent intent = getIntentForTestFile(testFile);
        mActivityRule.launchActivity(intent);
        checkDisplayState(R.string.title_activity_import_usfm_language, true);

        //when
        onView(withText(language)).perform(click());
        boolean seen = waitWhileDisplayed(R.string.reading_usfm);

        //then
        matchSummaryDialog(R.string.title_import_usfm_error, book, false);
        rotateScreen();
        matchSummaryDialog(R.string.title_import_usfm_error, book, false);
        rotateScreen();
    }

    /**
     *
     */
    public void initForImport(String book, String language) {
        mTargetTranslationID = language +"_" + book +"_text_reg";
        removeTranslation();
    }

    /**
     *  cleanup old imports
     */
    public void removeTranslation() {
        if(mTargetTranslationID!=null)
        {
            Translator translator = App.getTranslator();
            translator.deleteTargetTranslation(mTargetTranslationID);
        }
    }

    /**
     * match expected values on summary dialog
     * @param title
     * @param book
     * @param noErrors
     */
    protected void matchSummaryDialog(int title, String book, boolean noErrors) {
        thenShouldHaveDialogTitle(title);
        if(book != null) {
            shouldHaveFoundBook(book);
        }
        checkForImportErrors(noErrors, title);
    }

    /**
     * wait until view with text is shown
     * @param matchText
     */
    protected void clickOnViewText(String matchText) {
        for(int i = 0; i < 20; i++) { // wait until dialog is displayed
            try {
                onView(withText(matchText)).check(matches(withText(matchText)));
                break;
            } catch (Exception e) {
            }
        }
        onView(withText(matchText)).check(matches(withText(matchText)));
        onView(withText(matchText)).perform(click());
    }

    /**
     * match expected values on summary dialog
     * @param success
     */
    protected void matchImportResultsDialog(boolean success) {
        int matchTitle = success ? R.string.title_import_usfm_results : R.string.title_import_usfm_error;
        int matchText = success ? R.string.import_usfm_success : R.string.import_usfm_failed;
        thenShouldHaveDialogTitle( matchTitle );
        onView((withText(matchText))).check(matches(isDisplayed()));
    }

    /**
     * check if dialog content shows no errors
     * @param noErrors
     */
    private void checkForImportErrors(boolean noErrors, int title) {
        String dialogTitle = App.context().getResources().getString(title);
        onView(withText(dialogTitle)).check(matches(isDisplayed()));
    }

    /**
     * make sure we found the book
     * @param book
     */
    private void shouldHaveFoundBook(String book) {
        String format = App.context().getResources().getString(R.string.found_book);
        String matchText = String.format(format, book);
        onView(withText(containsString(matchText))).check(matches(isDisplayed())); // dialog open
    }

    protected void thenShouldShowMissingBookNameDialog() {
        thenShouldHaveDialogTitle(R.string.title_activity_import_usfm_language);
    }

    protected void thenShouldHaveDialogTitle(int title) {
        String titleStr = App.context().getResources().getString(title);
//        for(int i = 0; i < 40; i++) { // wait until displayed
//            try {
//                onView(withId(R.id.dialog_title)).check(matches(withText(titleStr)));
//                break;
//            } catch (Exception e) {
//            }
//        }
        onView(withText(titleStr)).check(matches(isDisplayed())); // dialog displayed
    }

    /**
     * since progress dialog is async, then we idle while it is shown
     * @param resource
     * @return
     */
    private boolean waitWhileDisplayed(int resource) {
        String text = App.context().getResources().getString(resource);
        return waitWhileDisplayed(text);
    }

    /**
     * since progress dialog is async, then we idle while it is shown
     * @param text
     * @return
     */
    private boolean waitWhileDisplayed(String text) {
        boolean done = false;
        boolean viewSeen = false;
        int maxCount = 1000; // sanity limit
        for (int i = 0; (i < maxCount) && !done ; i++) {
            try {
                onView(withText(text)).check(matches(isCompletelyDisplayed()));
                viewSeen = true;
            } catch (Exception e) {
                done = true;
            }
        }
        if(!done) {
            String msg = "Max count reached";
        }
        return viewSeen;
    }

    /**
     * make sure text is displayed or not displayed in view
     * @param resource
     * @param displayed
     */
    private void checkDisplayState(int resource, boolean displayed) {
        String text = App.context().getResources().getString(resource);
        checkDisplayState(text, displayed);
    }

    /**
     * make sure text is displayed or not displayed in view
     * @param text
     * @param displayed
     */
    private void checkDisplayState(String text, boolean displayed) {
        ViewInteraction interaction = onView(withText(text));
        Matcher<View> displayState = isCompletelyDisplayed();
        if(!displayed) {
            displayState = not(displayState);
        }
        interaction.check(matches(displayState));
    }

    /**
     * generate intent for ImportUsfmActivity 
     * @param fileName
     * @return
     * @throws Exception
     */
    protected Intent getIntentForTestFile(String fileName) throws Exception {
        Intent intent = new Intent();
        Resources testRes = mTestContext.getResources();

        mTempDir = new File(App.context().getCacheDir(), System.currentTimeMillis() + "");
        mTempDir.mkdirs();

        InputStream usfmStream = mTestContext.getAssets().open(fileName);
        mTestFile = new File(mTempDir, "testFile.usfm");
        FileUtilities.copyInputStreamToFile(usfmStream, mTestFile);

        intent.putExtra(ImportUsfmActivity.EXTRA_USFM_IMPORT_FILE, mTestFile);
        return intent;
    }


    /**
     * force page orientation change
     */
    protected void rotateScreen() {
        Context context = InstrumentationRegistry.getTargetContext();
        int orientation
                = context.getResources().getConfiguration().orientation;

        Activity activity = mActivityRule.getActivity();
        activity.setRequestedOrientation(
                (orientation == Configuration.ORIENTATION_PORTRAIT) ?
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE :
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    /**
     *     finsih activity if still running
     */
    private void finishActivity() {
        Activity activity = mActivityRule.getActivity();
        if(activity != null) {
            activity.finish();
        }
    }

    /**
     * get interaction for toolbar with title
     * @param resource
     * @return
     */
    private static ViewInteraction matchToolbarTitle(
            int resource) {
        String title = App.context().getResources().getString(resource);
        return matchToolbarTitle(title);
    }

    /**
     * get interaction for toolbar with title
     * @param title
     * @return
     */
    private static ViewInteraction matchToolbarTitle(
            CharSequence title) {
        return onView(allOf(isAssignableFrom(Toolbar.class)))
                .check(matches(withToolbarTitle(is(title))));
    }

    /**
     * get interaction for toolbar with title
     * @param title
     * @return
     */
    private static ViewInteraction notMatchToolbarTitle(
            CharSequence title) {
        return onView(isAssignableFrom(Toolbar.class))
                .check(matches(not(withToolbarTitle(is(title)))));
    }

    /**
     * match toolbar
     * @param textMatcher
     * @return
     */
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
