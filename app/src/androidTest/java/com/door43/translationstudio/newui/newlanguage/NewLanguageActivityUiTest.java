package com.door43.translationstudio.newui.newlanguage;

import org.apache.commons.io.IOUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
import android.view.View;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.NewLanguageQuestion;
import com.door43.translationstudio.newui.newtranslation.NewTargetTranslationActivity;
import com.door43.translationstudio.tasks.UploadCrashReportTask;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withHint;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class NewLanguageActivityUiTest {

    private String mStringToBetyped;
    private String mQuestions;
    Context mTestContext;
    List<List<NewLanguageQuestion>> mQuestionPages;
    long mQuestionnaireID;
    Context mAppContext;
    Field mf_resultCode;
    Field mf_resultData;


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
        mTestContext = InstrumentationRegistry.getContext();
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
        captureResultData();
        thenTitlePageCountShouldMatch(pageCount, pageNum);
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
        assertTrue("The result code is not ok. ", resultCode == Activity.RESULT_OK);
        JSONArray answers = jsonData.getJSONArray("answers");
        assertTrue("The result data is empty. ", answers != null);
        verifyAnswers(answers, expectedBoolean);
        currentActivity.finish();
    }

    @Test
    public void fillPageBoolean() throws Exception {

        //given
        String fileName = "new_language/fullQuestionaire.json";
        Intent intent = getIntentForTestFile(fileName);
        int pageCount = 11;
        int pageNum = 0;
        boolean hideKeyboard = false;
        boolean requiredOnly = false;
        boolean valueForBooleans = false;
        boolean doNext = true;
        mActivityRule.launchActivity(intent);
        thenTitlePageCountShouldMatch(pageCount, pageNum);
        fillPage(pageNum, true, false, false, hideKeyboard);
        thenTitlePageCountShouldMatch(pageCount, pageNum + 1);

        //when
        fillPage(pageNum + 1, doNext, requiredOnly, valueForBooleans, hideKeyboard);

        //then
        thenTitlePageCountShouldMatch(pageCount, pageNum + 2);
    }


    @Test
    public void fillPageRequired() throws Exception {

        //given
        String fileName = "new_language/fullQuestionaire.json";
        Intent intent = getIntentForTestFile(fileName);
        int pageCount = 11;
        int pageNum = 0;
        boolean hideKeyboard = false;
        boolean requiredOnly = true;
        boolean valueForBooleans = true;
        boolean doNext = true;
        mActivityRule.launchActivity(intent);
        thenTitlePageCountShouldMatch(pageCount, pageNum);

        //when
        fillPage(pageNum, doNext, requiredOnly, valueForBooleans, hideKeyboard);

        //then
        thenShouldHaveMissingAnswerDialog();
    }

    @Test
    public void fillPageNotRequired() throws Exception {

        //given
        String fileName = "new_language/fullQuestionaire.json";
        Intent intent = getIntentForTestFile(fileName);
        int pageCount = 11;
        int pageNum = 0;
        boolean hideKeyboard = false;
        boolean requiredOnly = false;
        boolean valueForBooleans = false;
        boolean doNext = true;
        mActivityRule.launchActivity(intent);
        thenTitlePageCountShouldMatch(pageCount, pageNum);

        //when
        fillPage(pageNum, doNext, requiredOnly, valueForBooleans, hideKeyboard);

        //then
        int pageNumExpected = 1;
        thenTitlePageCountShouldMatch(pageCount, pageNumExpected);
    }


    @Test
    public void requiredAnswerContinue() throws Exception {

        //given
        String fileName = "new_language/fullQuestionaire.json";
        Intent intent = getIntentForTestFile(fileName);
        int pageCount = 11;
        int pageNum = 0;
        mActivityRule.launchActivity(intent);
        thenTitlePageCountShouldMatch(pageCount, pageNum);

        //when
//        currentActivity.recreate();
        onView(withId(R.id.next_button)).perform(click());
        thenShouldHaveRequiredAnswerDialog();
        onView(withId(R.id.positiveButton)).perform(click());

        //then
        thenTitlePageCountShouldMatch(pageCount, pageNum);
    }

    @Test
    public void missingAnswerContinue() throws Exception {

        //given
        String fileName = "new_language/fullQuestionaire.json";
        Intent intent = getIntentForTestFile(fileName);
        int pageCount = 11;
        int pageNum = 0;
        boolean hideKeyboard = true;
        mActivityRule.launchActivity(intent);
        thenTitlePageCountShouldMatch(pageCount, pageNum);
        addEditText(0, 0, "language_name", hideKeyboard);

        //when
//        currentActivity.recreate();
        onView(withId(R.id.next_button)).perform(click());
        thenShouldHaveMissingAnswerDialog();
        onView(withId(R.id.positiveButton)).perform(click());

        //then
        int pageNumExpected = 1;
        thenTitlePageCountShouldMatch(pageCount, pageNumExpected);
    }

    @Test
    public void missingAnswerCancel() throws Exception {

        //given
        String fileName = "new_language/fullQuestionaire.json";
        Intent intent = getIntentForTestFile(fileName);
        int pageCount = 11;
        int pageNum = 0;
        boolean hideKeyboard = true;
        mActivityRule.launchActivity(intent);
        thenTitlePageCountShouldMatch(pageCount, pageNum);
        addEditText(0, 0, "language_name", hideKeyboard);

        //when
//        currentActivity.recreate();
        onView(withId(R.id.next_button)).perform(click());
        thenShouldHaveMissingAnswerDialog();
        onView(withId(R.id.negativeButton)).perform(click());

        //then
        thenTitlePageCountShouldMatch(pageCount, pageNum);
    }

    private void verifyAnswers(JSONArray answers, boolean expectedAnswer) throws Exception {
        for (int i = 0; i < answers.length(); i++) {
            JSONObject answer = answers.getJSONObject(i);
            int id = answer.getInt("question_id");
            String answerText =  answer.getString("answer");

            NewLanguageQuestion newLanguageQuestion = findQuestion(id);

            String expected;
            if(newLanguageQuestion.type == NewLanguageQuestion.QuestionType.INPUT_TYPE_STRING) {
                expected = generateAnswerForQuestion(id);
            } else {
                expected = expectedAnswer ? NewLanguageQuestion.TRUE_STR : NewLanguageQuestion.FALSE_STR;
            }
            assertEquals("Question " + id + " mismatch", expected, answerText);
        }
    }

    private JSONObject getJsonData(NewLanguageActivity currentActivity, String key) throws NoSuchFieldException, IllegalAccessException, JSONException {
        Intent resultData = getResultData(currentActivity);
        String jsonDataStr = resultData.getStringExtra(key);
        return new JSONObject(jsonDataStr);
    }

    private void captureResultData() throws NoSuchFieldException {
        mf_resultData = Activity.class.getDeclaredField("mResultData"); //NoSuchFieldException
        mf_resultData.setAccessible(true);
        mf_resultCode = Activity.class.getDeclaredField("mResultCode"); //NoSuchFieldException
        mf_resultCode.setAccessible(true);
    }

    private void clearResultData() throws NoSuchFieldException {
        mf_resultData.setAccessible(false);
        mf_resultData = null;
        mf_resultCode.setAccessible(false);
        mf_resultCode = null;
    }

    private Intent getResultData(NewLanguageActivity currentActivity) throws NoSuchFieldException, IllegalAccessException {
        return (Intent) mf_resultData.get(currentActivity);
    }

    private int getResultCode(NewLanguageActivity currentActivity) throws NoSuchFieldException, IllegalAccessException {
        return mf_resultCode.getInt(currentActivity);
    }

    private void thenShouldHaveRequiredAnswerDialog() {
        onView(withId(R.id.dialog_title)).check(matches(withText(R.string.invalid_entry_title)));
        String warning = mAppContext.getResources().getString(R.string.answer_required_for);
        String[] lines = warning.split("\n");
        warning = lines[0];
        onView(withId(R.id.dialog_content)).check(matches(withText(startsWith(warning))));
    }

//    public static Matcher<View> withPartialText(final String text){
//        return new TypeSafeMatcher<View>() {
//
//            @Override
//            protected boolean matchesSafely(View item) {
//                if(!(item instanceof TextView)) {
//                    return false;
//                }
//
//                String foundText = ((TextView) item).getText().toString();
//                if(null ==foundText) {
//                    return false;
//                }
//
//                int pos = foundText.indexOf(text);
//                return pos >= 0;
//            }
//
//            @Override
//            public void describeTo(Description description) {
//                description.appendText("Value expected is wrong");
//            }
//        };
//    }


    private void thenShouldHaveMissingAnswerDialog() {
        onView(withId(R.id.dialog_title)).check(matches(withText(R.string.answers_missing_title)));
        onView(withId(R.id.dialog_content)).check(matches(withText(R.string.answers_missing_continue)));
    }

    private void fillAllPages(boolean doDone, boolean requiredOnly, boolean valueForBooleans, boolean hideKeyboard) {
        int pageCount = mQuestionPages.size();

        for (int i = 0; i < pageCount - 1; i++) {

            fillPage(i, true, requiredOnly, valueForBooleans, hideKeyboard);
            thenTitlePageCountShouldMatch( pageCount, i + 1);
        }

        fillPage(pageCount - 1, false, requiredOnly, valueForBooleans, hideKeyboard);

        if(doDone) {
            onView(withId(R.id.done_button)).perform(click());
        }
    }

    private void fillPage(int pageNum, boolean doNext, boolean requiredOnly, boolean valueForBooleans, boolean hideKeyboard) {
        List<NewLanguageQuestion> questions = mQuestionPages.get(pageNum);

        for (int i = 0; i < questions.size(); i++) {

            NewLanguageQuestion question = questions.get(i);
            if(!requiredOnly || question.required) {
                if(question.type == NewLanguageQuestion.QuestionType.INPUT_TYPE_BOOLEAN) {
                    boolean value = valueForBooleans;
                    setBoolean(pageNum, i, value);
                } else {
                    String text = generateAnswerForQuestion(question.id);
                    addEditText(pageNum, i, text, hideKeyboard);
                }
            }
        }

        if(doNext) {
            onView(withId(R.id.next_button)).perform(click());
        }
    }

    private String generateAnswerForQuestion(long questionId) {
        return String.format("A-%d", questionId);
    }

    private boolean generateAnswerForBoolean(int questionNum) {
        return questionNum % 2 == 1;
    }

    // TODO: 5/1/16 check initial value
    private void addEditText(int pageNum, int questionNum, String newText, boolean hideKeyboard) {
        NewLanguageQuestion question = mQuestionPages.get(pageNum).get(questionNum);
        String questionText = question.question;
        ViewInteraction interaction = onView(allOf(withId(R.id.edit_text), hasSibling(withText(questionText))));
        interaction.perform(scrollTo());
        interaction.perform( typeText(newText));
//        interaction.check(matches(withHint(question.helpText))); // doesn't seem to work on second question
        if(hideKeyboard) {
            interaction.perform(closeSoftKeyboard());
        }
        interaction.check(matches(withText(newText)));
    }

    // TODO: 5/1/16 check initial value
    private void setBoolean(int pageNum, int questionNum, boolean value) {
        String questionText = mQuestionPages.get(pageNum).get(questionNum).question;
        Matcher<View> parent = allOf(withClassName(endsWith("RadioGroup")), hasSibling(withText(questionText)));

        int resource = value ? R.id.radio_button_yes : R.id.radio_button_no;
        int oppositeResource = !value ? R.id.radio_button_yes : R.id.radio_button_no;
        ViewInteraction interaction = onView(allOf(withId(resource), withParent(parent)));
        interaction.perform(scrollTo(), click());
        interaction.check(matches(isChecked()));
        onView(allOf(withId(oppositeResource), withParent(parent))).check(matches(not(isChecked())));
    }

    private void getQuestionPages() throws Exception {
        mQuestionPages = new ArrayList<>();

        JSONObject questionnaire = (new NewLanguageAPI()).readQuestionnaire(mTestContext, mQuestions, "en"); // TODO: 4/25/16 get actual language
        mQuestionnaireID = NewLanguageActivity.getQuestionnaireID(questionnaire);
        NewLanguageActivity.getQuestionPages(mQuestionPages, questionnaire);
    }

    private NewLanguageQuestion findQuestion(long id) throws Exception {
        for (List<NewLanguageQuestion> questionPage : mQuestionPages) {
            for (NewLanguageQuestion newLanguageQuestion : questionPage) {
                if(newLanguageQuestion.id == id) {
                    return newLanguageQuestion;
                }
            }
        }
        return null;
    }

    private void thenTitlePageCountShouldMatch(int pageCount, int pageNum) {
        mAppContext = InstrumentationRegistry.getTargetContext();
        String titleFormat = mAppContext.getResources().getString(R.string.new_lang_question_n);

        String title = String.format(titleFormat, pageNum + 1, pageCount);
        matchToolbarTitle(title);
    }

    private Intent getIntentForTestFile(String fileName) throws Exception {
        Intent intent = new Intent();
        intent.putExtra(NewLanguageActivity.EXTRA_CALLING_ACTIVITY, NewLanguageActivity.ACTIVITY_HOME);

        Resources testRes = mTestContext.getResources();

        InputStream usfmStream = mTestContext.getAssets().open(fileName);
        mQuestions = IOUtils.toString(usfmStream, "UTF-8");
        intent.putExtra(NewLanguageActivity.EXTRA_NEW_LANGUAGE_QUESTIONS_JSON, mQuestions);

        getQuestionPages();
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