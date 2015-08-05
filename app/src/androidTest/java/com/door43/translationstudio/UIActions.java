package com.door43.translationstudio;

import android.graphics.Rect;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.widget.TextView;

import com.door43.translationstudio.TermsActivity;
import com.door43.translationstudio.R;
import com.robotium.solo.Solo;

/**
 * Created by Gary on 8/4/2015.
 */
public class UIActions extends ActivityInstrumentationTestCase2<TermsActivity> {
    private Solo solo;
    private TermsActivity activity;
    frame  frameInfo;
    public UIActions(Solo s) {
        super(TermsActivity.class);
        solo=s;
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
    public boolean waitForLoad(){
        boolean isAgree = solo.waitForText("I Agree", 1, 5000);
        if(isAgree)solo.clickOnText("I Agree");
        boolean loadingTarget = solo.waitForText("Loading target", 1, 20000);
        boolean loadingProject = solo.waitForText("Loading project", 1, 20000);
        boolean isIndexing = solo.waitForText("Indexing", 1, 20000);
        return isIndexing;
    }
    private void getFrameInfo(){
        TextView frameView = (TextView)solo.getView(R.id.sourceFrameNumText);
        if( frameInfo==null){
            frameInfo = new frame(frameView.getText().toString());
        }else{
            frameInfo.parse(frameView.getText().toString());
        }
    }
    public boolean nextFrame(){
        getFrameInfo();
        int startFrame = frameInfo.getCurrent();
        assertTrue("There are no more frames.", startFrame < frameInfo.getTotal());
        swipeToRight(R.id.inputText, 10);
        solo.sleep(2000);
        getFrameInfo();
        int newFrame =frameInfo.getCurrent();
        assertTrue("Failed to advance frame.", newFrame > startFrame);

        return true;
    }
    public int getFrame(){
        getFrameInfo();
        return frameInfo.getCurrent();
    }
    public boolean prevFrame(){
        getFrameInfo();
        int startFrame = frameInfo.getCurrent();
        assertTrue("There are no previous frames.", startFrame > 1);
        swipeToLeft(R.id.inputText, 10);
        solo.sleep(2000);
        getFrameInfo();
        int endFrame =frameInfo.getCurrent();
        assertTrue("Failed to revert frame.",endFrame < startFrame);
        return true;
    }
    private void swipeToLeft(int id, int stepCount) {
        View pView = solo.getView(id);
        int loc[] = new int[2];
        Rect rectSource = new Rect();
        pView.getWindowVisibleDisplayFrame(rectSource);
        pView.getLocationOnScreen(loc);
        solo.drag(loc[1],loc[0],rectSource.exactCenterY(),rectSource.exactCenterY(),stepCount);
    }
    private void swipeToRight(int id, int stepCount) {
        View pView = solo.getView(id);
        int loc[] = new int[2];
        Rect rectSource = new Rect();
        pView.getWindowVisibleDisplayFrame(rectSource);
        pView.getLocationOnScreen(loc);
        solo.drag(loc[0],loc[1],rectSource.exactCenterY(),rectSource.exactCenterY(),stepCount);
    }
    class frame {
        private int current;
        private int total;
        private String frameString;
        frame(String FrameString){
            frameString=FrameString;
            parse();
        }
        public int getCurrent(){return current;}
        public int getTotal(){return total;}
        public void parse(String FrameString){
            frameString=FrameString;
            parse();
        }
        private void parse(){
            if(frameString.isEmpty()){return;}
            String s[] = frameString.split(" ");
            current = Integer.parseInt(s[1]);
            total = Integer.parseInt(s[3]);
        }
    }
}
