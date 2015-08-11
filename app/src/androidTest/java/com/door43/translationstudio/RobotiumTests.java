package com.door43.translationstudio;

import android.graphics.PointF;
import android.graphics.Rect;
import android.support.v4.view.ViewPager;
import android.support.v7.internal.widget.TintButton;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Time;
import java.util.ArrayList;

/**
 * Created by Gary S on 7/13/2015.
 */


import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.door43.translationstudio.TermsActivity;
import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Frame;
import com.door43.util.ClearableEditText;
import com.robotium.solo.Solo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SuppressWarnings("rawtypes")
public class RobotiumTests extends ActivityInstrumentationTestCase2<TermsActivity> {
    private Solo solo;
    private TermsActivity activity;
    private UIActions act;
    //TODO: add log to file
    public RobotiumTests() {
        super(TermsActivity.class);
    }

    public void setUp() throws Exception {
        super.setUp();
        solo = new Solo(getInstrumentation());
        this.activity= getActivity();
        try {
            act = new UIActions(solo);
        } catch (Throwable e) {
            e.printStackTrace();
            return;
        }
    }

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
        super.tearDown();
    }

    private void selectTab(int tabIndex){
        if(tabIndex>1){
            solo.scrollToSide(tabIndex);
        }
        final ViewPager vp = (ViewPager)solo.getView(R.id.leftViewPager);

        final int tab = tabIndex;
        activity.runOnUiThread(new Runnable() {
            public void run() {

                vp.setCurrentItem(tab);
                vp.invalidate();
            }
        });
        int curItem = vp.getCurrentItem();
    }

    private List<File> getListFiles(File parentDir) {
        ArrayList<File> inFiles = new ArrayList<File>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                inFiles.addAll(getListFiles(file));
            } else {
                if(file.getName().endsWith(".txt")){
                    inFiles.add(file);
                }
            }
        }
        return inFiles;
    }
    /*
    Currently theses tests must be started with a fresh install of the ts-android apk.
    once yoou have navigated around the app remembers your choices. Well this makes for a great
    user experience it take time to develop smart tests that can detect the state of the UI and
    decide how to navigate.
    */

    public void test00SimpleStartToTranslate() {
        act.waitForLoad();
        boolean isOpen = solo.waitForText("Get more projects", 1, 25000);

        if(!isOpen){
            selectTab(0);
            isOpen = solo.waitForText("Get more projects", 1, 15000);
        }
        if(isOpen){
            boolean isNT =act.waitAndClickText("Bible: NT");
            boolean isMat = act.waitAndClickText("Matthew", 15000);
            act.waitAndClickText("English");

            solo.waitForText("Loading", 1, 10000);
            solo.waitForText("Afaraf", 1, 25000);
            solo.clickInList(0);
            act.waitAndClickText("Chapter 1");
            act.waitAndClickText("1-3");
            TextView verseOne2Three = solo.getText("genealogy",true);
            String expectedString = "The book of the genealogy of Jesus Christ";
            String verse = verseOne2Three.getText().toString();
            Log.d("TSTEST",verse);
            boolean expected = verseOne2Three.getText().toString().contains(expectedString);
            //clickables: Open Bible Stories, bible-ot Bible-NT, Get more projects [CHAPTER & FRAME](are blank) Project returns to main view
            assertTrue(expected);
            //TODO: start really testing
        }else {
            //cvs = solo.getCurrentViews();
            //TODO: query to find the visible view
            assertEquals(true,false);
        }
    }
    /*
    Feature: Auto save
    The app will automatically save translations

    Background:
    Given I have selected a frame
    And I am viewing the main activity
    */
    /*
    Scenario: Save after a few moments
    Given I have entered some text into the translation field
    When I am inactive for a few moments
    Then I want my changes to be saved
    */
    public void test01AutoSaveByTime(){
        //  /data/data/com.translationstudio.androidapp/files/git/uw-mat-aa/01/04.txt
        // save code for frames is in frame.java
        //text files start with two digit of first verse in frame example: frame 4-6 would be 04.txt
        act.waitForLoad();
        //ArrayList<TextView> tvs = solo.getCurrentViews(TextView.class);
        //com.door43.util.ClearableEditText{424443e8 GFED..CL ......I. 0,0-0,0 #7f0a00cf app:id/translationNoteEditText}
        int title = R.id.translationNoteReferenceEditText;

        //ArrayList<EditText> etViews = solo.getCurrentViews(EditText.class);
        EditText et = (EditText)solo.getView(R.id.inputText);

        assertNotNull("Could not find id.inputText", et);
        String inText = "Test input text";
        solo.enterText(et,inText);
        solo.sleep(10000);
        //List<File> files = getListFiles(new File("/data/data/com.translationstudio.androidapp/files/git/uw-mat-aa/01/"));
        String path = "/data/data/com.translationstudio.androidapp/files/git/uw-mat-aa/01/01.txt";
        try{
            FileReader fr = new FileReader(path);
            BufferedReader br = new BufferedReader(fr);
            String s = br.readLine();

            assertTrue(s + " does not match " + inText,s.equals(inText));
            return;
        }catch(Exception ex){
            assertTrue("Failed to find saved text: " + ex.getMessage(),false);
            return;
        }
    }
    /*
    Scenario: Save when changing frames
    Given I have entered some text into the translation field
    When I open a different frame
    Then I want my changes to be saved
    */
    public void _test02AutoSaveByFrameChange(){
        assertTrue(false);
    }
    /*
    Scenario: Save when leaving activity
    Given I have entered some text into the translation field
    When I leave the main activity
    Then I want my changes to be saved
    */
    public void _test03AutoSaveByLeaving(){
        assertTrue(false);
    }
    /*
    Scenario: Save to external sd card
    Given I have chosen to save the translations to the sd card.
    When the auto save is executed
    Then I want my changes to be saved to the external sd card.
    */
    //Need a device that supports writing to external
    public void _test04AutoSaveExternal(){
        assertTrue(false);
    }
    /*
    Feature: Blind draft mode
    Two translation modes will be available to translators.
    The default mode has the source on the left and the translation on the right.
    The blind draft mode will only display the translation.

            Background:
    Given I have selected a frame
    And I am viewing the main activity
    And I have opened the contextual menu

    Scenario: Enable blind draft mode
    Given the blind draft mode is disabled
    When I click on the "toggle blind draft mode" button
    Then I want to see the blind draft mode

    Scenario: Disable blind draft mode
    Given the blind draft mode is enabled
    When I click on the "toggle blind draft mode" button
    Then I want to see the default mode
    */
    public void test05EnableDisableBlindDraftMode()
    {
        act.waitForLoad();
        int title = R.id.translationNoteReferenceEditText;
        ArrayList<View> cvs;
        //ArrayList<EditText> etViews = solo.getCurrentViews(EditText.class);
        boolean isChapter = solo.waitForText("Chapter", 1, 30000);
        boolean isFrame = solo.waitForText("Frame", 1, 30000);
        boolean isTranslation = solo.waitForText("Afaraf: [Chapter 1]", 1, 30000);
        EditText et = (EditText)solo.getView(R.id.inputText);

        TextView tTitle = (TextView)solo.getView(R.id.translationTitleText);
        isTranslation= tTitle.getText().toString().endsWith("[Chapter 1]");
        assertTrue("Failed to find translation view.", isTranslation);
        assertNotNull("Could not find id.inputText", et);
        View pView = solo.getView(R.id.sourceTitleText);
        ArrayList<Button> btns = solo.getCurrentViews(Button.class);
        View cButton = solo.getView(R.id.contextual_menu_btn);
        assertTrue("Failed to find contextual menu button.", cButton.getVisibility() == View.VISIBLE);
        solo.clickOnView(cButton);
        solo.sleep(1000);
        solo.clickOnMenuItem("Toggle blind draft");
        solo.sleep(1000);
        ArrayList<TextView> textViews = solo.getCurrentViews(TextView.class);
        assertFalse("Unexpectedly found source view", textViews.contains(pView));
        solo.clickOnView(cButton);
        solo.sleep(1000);
        solo.clickOnMenuItem("Toggle blind draft");
        solo.sleep(1000);
        pView = solo.getView(R.id.sourceTitleText);
        boolean isSourceView= solo.waitForView(R.id.sourceTitleText, 1, 5000);
        assertTrue(isSourceView);
    }
    /*
    Feature: Frame swipe navigation
  Translations will be able to quickly move between frames by swiping left or right.

  Background:
    Given I have selected a frame

  Scenario: Swipe to next frame
    Given I am viewing the main activity
    When I swipe from right to left
    Then I want to see the next frame

  Scenario: Swipe to previous frame
    Given I am viewing the main activity
    When I swipe from left to right
    Then I want to see the previous frame

  Scenario: Device back button
    Given I am viewing the main activity
    When I click on the device back button
    Then I want to return to the previously selected frame
     */
    public void test06TestFrameSwipeNavigation(){
        act.waitForLoad();
        int title = R.id.translationNoteReferenceEditText;
        ArrayList<View> cvs;
        //ArrayList<EditText> etViews = solo.getCurrentViews(EditText.class);
        boolean isChapter = solo.waitForText("Chapter", 1, 30000);
        boolean isFrame = solo.waitForText("Frame", 1, 30000);
        boolean isTranslation = solo.waitForText("Afaraf: [Chapter 1]", 1, 30000);
        EditText et = (EditText)solo.getView(R.id.inputText);

        TextView tTitle = (TextView)solo.getView(R.id.translationTitleText);
        isTranslation= tTitle.getText().toString().endsWith("[Chapter 1]");
        assertTrue("Failed to find translation view.", isTranslation);
        assertNotNull("Could not find id.inputText", et);
        ArrayList<TextView> tvs = solo.getCurrentViews(TextView.class);
        View pView = solo.getView(R.id.readSourceTranslation);
        act.nextFrame();
        solo.sleep(9000);//TODO: need to figure out how to waIT FOR SPINNER STOP
        act.prevFrame();
        assertTrue(true);
        //TODO: navigate to last frame then back
    }

/*
Feature: Bug report
  Translators will be able to submit bug reports to developers when they encounter problems while using the app

  Background:
    Given I am viewing the main activity

  Scenario:
    Given I have found a bug in the app
    When I click on the bug report button from the menu
    Then I want to see the bug report form

  Scenario:
    Given I have opened the bug report form
    When I click on the submit button
      And I have not entered any notes
      And there there are no error messages in the log
    Then I want to be prompted to describe what the problem is.

  Scenario:
    Given I have opened the bug report form
    When I click on the submit button
      And I have entered less than three words.
    Then I want to be prompted to provide more detail about the problem.
 */
    public void test07ReportBug(){
        act.waitForLoad();
        boolean isChapter = solo.waitForText("Chapter", 1, 30000);
        boolean isFrame = solo.waitForText("Frame", 1, 30000);
        boolean isTranslation = solo.waitForText("Afaraf: [Chapter 1]", 1, 30000);
        act.clickOnOverflowButton();
        solo.sleep(1000);
        solo.clickOnMenuItem("Report Bug");
        solo.sleep(3000);
        EditText etBug = (EditText)solo.getView(R.id.crashDescriptioneditText);
        solo.enterText(etBug,"Robotium test07ReportBug()- this is not a real bug.");
        solo.clickOnButton("Cancel");
        isTranslation = solo.waitForText("Afaraf: [Chapter 1]", 1, 30000);
        act.clickOnOverflowButton();
        solo.sleep(1000);
        solo.clickOnMenuItem("Report Bug");
        solo.sleep(3000);
        etBug = (EditText)solo.getView(R.id.crashDescriptioneditText);
        solo.enterText(etBug, "Robotium test07ReportBug()- this is not a real bug.");
        solo.clickOnButton("Upload");
        boolean success = solo.waitForText("Success",1,5000);
        assertTrue("Failed to find success toast.",success);
    }
    /*
    Feature: Publish and Backup
  Translators will be able to upload their translation to the server and optionally mark it as being ready

  Scenario: View upload interface
    Given I am viewing the main activity
    When I click on the upload menu item
    Then I want to view the upload interface

  Scenario: Choose project to upload
    Given I am viewing the upload interface
    And at least one project has some translations in it
    When I click on the project button
    Then I want to view a list of projects to upload

  Scenario: Choose project language to upload
    Given I am viewing the list of projects to upload
    And the project has translations in more than one language
    When I click on a project
    Then I want to view a list of available translation languages for that project

  Scenario: Verify translation
    Given I am viewing the upload interface
    And the project translation has been marked as complete
    When click on the continue button
    Then I want tests to be ran on my translation to make sure it is ready

  Scenario: Review verification results
    Given verification tests have been ran on my translation
    When I click on a verification result
    Then I want to view notes about the result (if available)

  Scenario: View Checking Questions
    Given my translation has been successfully verified
    When I click on the continue button
    Then I want to view a list of checking questions to be reviewed

  Scenario: Accept Checking Question
    Given I am viewing checking questions for my translation
    When I click on a checking question
    Then I want to see the answer to the question
    And I want the question to be marked as accepted
    And I want to see links to related passages
    And I want the question to remain accepted unless clicked again or the translation changes

  Scenario: Provide contact information
    Given my project has been analyzed for possible errors
    And the checking questions have been accepted
    And I have not already entered my contact information
    When I click continue
    Then I want to enter my contact information

  Scenario: Publish translation
    Given my project has been analyzed for possible errors
    And the translation is marked as complete
    And I have accepted the checking questions for my translation
    And I have entered my contact infromation
    When I click on the upload button
    Then I want my translation to be uploaded to the server

  Scenario: Backup
    Given the translation has not been marked as complete
    When I click on the upload button
    Then I want my translation to be uploaded to the server

  Scenario: Indicate publish status
    Given I have marked my translation as complete
    And I have not completed the publish process
    When I view the publish and backup interface
    Then I want to see a notice that my changes have not been published to the server yet
     */
    public void test08PublishAndBackup(){
        assertTrue(true);
    }

    //TODO: add tests for feature / use cases found here: https://github.com/unfoldingWord-dev/ts-requirements/tree/master/features

}
