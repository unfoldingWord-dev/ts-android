package com.door43.translationstudio;

import android.support.v4.view.ViewPager;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by Gary S on 7/13/2015.
 */


import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.widget.TextView;

import com.door43.translationstudio.TermsActivity;
import com.door43.translationstudio.R;
import com.robotium.solo.Solo;

import java.util.ArrayList;

@SuppressWarnings("rawtypes")
public class RobotiumTests extends ActivityInstrumentationTestCase2<TermsActivity> {
    private Solo solo;
    private TermsActivity activity;
    //TODO: add log to file
    public RobotiumTests() {
        super(TermsActivity.class);
    }

    public void setUp() throws Exception {
        super.setUp();
        solo = new Solo(getInstrumentation());
        this.activity= getActivity();
    }

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
        super.tearDown();
    }
    public boolean waitAndClickText(String text,int match,int wait){
        boolean isFound = solo.waitForText(text,match,wait);
        solo.clickOnText(text);
        return isFound;
    }
    public boolean waitAndClickText(String text, int wait){
        return waitAndClickText(text,1,wait);
    }
    public boolean waitAndClickText(String text){
        return waitAndClickText(text,1,5000);
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
    /*
    Currently theses tests must be started with a fresh install of the ts-android apk.
    once yoou have navigated around the app remembers your choices. Well this makes for a great
    user experience it take time to develop smart tests that can detect the state of the UI and
    decide how to navigate.
    */
    public void test00SimpleStartToTranslate() {
        ArrayList<View> cvs;
        boolean isAgree = solo.waitForText("I Agree", 1, 5000);
        if(isAgree)solo.clickOnText("I Agree");
        boolean loadingTarget = solo.waitForText("Loading target", 1, 20000);
        boolean loadingProject = solo.waitForText("Loading project", 1, 20000);
        boolean isIndexing = solo.waitForText("Indexing", 1, 20000);

        boolean isOpen = solo.waitForText("Get more projects", 1, 25000);

        if(!isOpen){
            selectTab(0);
            isOpen = solo.waitForText("Get more projects", 1, 15000);
        }
        if(isOpen){
            boolean isNT = waitAndClickText("Bible: NT");
            boolean isMat = waitAndClickText("Matthew",15000);
            waitAndClickText("English");

            solo.waitForText("Loading", 1, 10000);
            solo.waitForText("Afaraf", 1, 25000);
            solo.clickInList(0);
            waitAndClickText("Chapter 1");
            waitAndClickText("1-3");
            TextView verseOne2Three = solo.getText("genealogy",true);
            String expectedString = "The book of the genealogy of Jesus Christ";
            String verse = verseOne2Three.getText().toString();
            Log.d("TSTEST",verse);
            boolean expected = verseOne2Three.getText().toString().contains(expectedString);
            //clickables: Open Bible Stories, bible-ot Bible-NT, Get more projects [CHAPTER & FRAME](are blank) Project returns to main view
            assertTrue(expected);
            //TODO: start really testing
        }else {
            cvs = solo.getCurrentViews();
            //TODO: query to find the visible view
            assertEquals(true,false);
        }

    }
    //TODO: add tests for feature / use cases found here: https://github.com/unfoldingWord-dev/ts-requirements/tree/master/features
    
}
