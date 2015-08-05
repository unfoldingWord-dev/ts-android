package com.door43.translationstudio;

import android.graphics.PointF;
import android.graphics.Rect;
import android.support.v4.view.ViewPager;
import android.support.v7.internal.widget.TintButton;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.Display;
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



    //TODO: add tests for feature / use cases found here: https://github.com/unfoldingWord-dev/ts-requirements/tree/master/features

}
