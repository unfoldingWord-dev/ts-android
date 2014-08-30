package com.door43.translationstudio;

import com.door43.translationstudio.panes.left.LeftPaneFragment;
import com.door43.translationstudio.panes.RightPaneFragment;
import com.door43.translationstudio.panes.TopPaneFragment;
import com.door43.translationstudio.util.TranslatorBaseActivity;
import com.slidinglayer.SlidingLayer;


import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

public class MainActivity extends TranslatorBaseActivity {
    // content panes
    private LeftPaneFragment mLeftPane;
    private RightPaneFragment mRightPane;
    private TopPaneFragment mTopPane;

    // sliding layers
    private SlidingLayer mLeftSlidingLayer;
    private SlidingLayer mRightSlidingLayer;
    private SlidingLayer mTopSlidingLayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayout centerPane = (LinearLayout)findViewById(R.id.centerPane);

        // close the side panes when the center content is clicked
        centerPane.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mLeftSlidingLayer != null) mLeftSlidingLayer.closeLayer(true);
                if(mRightSlidingLayer != null) mRightSlidingLayer.closeLayer(true);
                if(mTopSlidingLayer != null) mTopSlidingLayer.closeLayer(true);
            }
        });

        initTestData();
        initSlidingLayers();
        initPanes();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * Sets up the sliding effect between panes. Mostly just closing others when one opens
     */
    public void initSlidingLayers() {
        mTopSlidingLayer = (SlidingLayer)findViewById(R.id.topPane);
        mLeftSlidingLayer = (SlidingLayer)findViewById(R.id.leftPane);
        mRightSlidingLayer = (SlidingLayer)findViewById(R.id.rightPane);

        // set up pane grips
        final ImageButton leftGrip = (ImageButton)findViewById(R.id.buttonGripLeft);
        final ImageButton rightGrip = (ImageButton)findViewById(R.id.buttonGripRight);
        final ImageButton topGrip = (ImageButton)findViewById(R.id.buttonGripTop);

        topGrip.setColorFilter(getResources().getColor(R.color.green), PorterDuff.Mode.SRC_ATOP);
        rightGrip.setColorFilter(getResources().getColor(R.color.purple), PorterDuff.Mode.SRC_ATOP);
        leftGrip.setColorFilter(getResources().getColor(R.color.blue), PorterDuff.Mode.SRC_ATOP);

        topGrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTopSlidingLayer.openLayer(true);
            }
        });
        rightGrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRightSlidingLayer.openLayer(true);
            }
        });
        leftGrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLeftSlidingLayer.openLayer(true);
            }
        });

        // set up opening/closing
        mTopSlidingLayer.setOnInteractListener(new SlidingLayer.OnInteractListener() {
            @Override
            public void onOpen() {
                // bring self to front first to cover grip
                mTopSlidingLayer.bringToFront();

                mLeftSlidingLayer.closeLayer(true);
                mLeftSlidingLayer.bringToFront();
                mRightSlidingLayer.closeLayer(true);
                mRightSlidingLayer.bringToFront();
                leftGrip.bringToFront();
                rightGrip.bringToFront();
            }

            @Override
            public void onClose() {

            }

            @Override
            public void onOpened() {

            }

            @Override
            public void onClosed() {

            }
        });
        mLeftSlidingLayer.setOnInteractListener(new SlidingLayer.OnInteractListener() {
            @Override
            public void onOpen() {
                // bring self to front first to cover grip
                mLeftSlidingLayer.bringToFront();

                mTopSlidingLayer.closeLayer(true);
                mTopSlidingLayer.bringToFront();
                mRightSlidingLayer.closeLayer(true);
                mRightSlidingLayer.bringToFront();
                topGrip.bringToFront();
                rightGrip.bringToFront();
            }

            @Override
            public void onClose() {

            }

            @Override
            public void onOpened() {

            }

            @Override
            public void onClosed() {

            }
        });
        mRightSlidingLayer.setOnInteractListener(new SlidingLayer.OnInteractListener() {
            @Override
            public void onOpen() {
                // bring self to front first to cover grip
                mRightSlidingLayer.bringToFront();

                mLeftSlidingLayer.closeLayer(true);
                mLeftSlidingLayer.bringToFront();
                mTopSlidingLayer.closeLayer(true);
                mTopSlidingLayer.bringToFront();
                leftGrip.bringToFront();
                topGrip.bringToFront();
            }

            @Override
            public void onClose() {

            }

            @Override
            public void onOpened() {

            }

            @Override
            public void onClosed() {

            }
        });
    }

    /**
     * set up the content panes
     */
    private void initPanes() {
        mTopPane = new TopPaneFragment();
        mLeftPane = new LeftPaneFragment();
        mRightPane = new RightPaneFragment();


        getFragmentManager().beginTransaction().replace(R.id.topPaneContent, mTopPane).commit();
        getFragmentManager().beginTransaction().replace(R.id.leftPaneContent, mLeftPane).commit();
        getFragmentManager().beginTransaction().replace(R.id.rightPaneContent, mRightPane).commit();
    }

    private void initTestData() {

        app().getSharedProjectManager().add(new Project("Open Bible Stories", "Unfolding Word"));
        app().getSharedProjectManager().add(new Project("Bible Translation", "Some fun description"));
        app().getSharedProjectManager().add(new Project("Hello world", "oh hi"));
        app().getSharedProjectManager().add(new Project("Another project", "Unfolding Word"));
        app().getSharedProjectManager().add(new Project("One more project", "Some fun description"));
        app().getSharedProjectManager().add(new Project("Something", "oh hi"));
        app().getSharedProjectManager().add(new Project("Another Something", "oh hi"));
        app().getSharedProjectManager().add(new Project("One more something", "oh hi"));
        app().getSharedProjectManager().add(new Project("Bla bla bla", "oh hi"));
        app().getSharedProjectManager().add(new Project("Hi guys", "oh hi"));
        app().getSharedProjectManager().add(new Project("Hello!", "oh hi"));
        app().getSharedProjectManager().add(new Project("What?g", "oh hi"));

    }
}
