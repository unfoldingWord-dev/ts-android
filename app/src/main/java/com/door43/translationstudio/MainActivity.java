package com.door43.translationstudio;

import com.door43.translationstudio.panes.left.LeftPaneFragment;
import com.door43.translationstudio.panes.RightPaneFragment;
import com.door43.translationstudio.panes.TopPaneFragment;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.TranslatorBaseActivity;
import com.slidinglayer.SlidingLayer;


import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends TranslatorBaseActivity {
    private MainActivity me = this;

    private static final String LANG_CODE = "en"; // TODO: this will eventually need to be managed dynamically by the project manager

    // content panes
    private LeftPaneFragment mLeftPane;
    private RightPaneFragment mRightPane;
    private TopPaneFragment mTopPane;
    private LinearLayout mCenterPane;

    // sliding layers
    private SlidingLayer mLeftSlidingLayer;
    private SlidingLayer mRightSlidingLayer;
    private SlidingLayer mTopSlidingLayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO: we should display a splash screen until the project manager has finished loading.
        // we should also not load the project manager when initialized but manually in the splash loader.
        // we should also generate the keys in the splash loader
        // Generate the ssh keys
        if(!app().hasKeys()) {
            app().generateKeys();
        }

        mCenterPane = (LinearLayout)findViewById(R.id.centerPane);

        initSlidingLayers();
        initPanes();

        // auto start with last selected frame
        String frameId = app().getActiveFrame();
        Integer chapterId = app().getActiveChapter();
        String projectSlug = app().getActiveProject();
        app().getSharedProjectManager().setSelectedProject(projectSlug);
        app().getSharedProjectManager().getSelectedProject().setSelectedChapter(chapterId);
        app().getSharedProjectManager().getSelectedProject().getSelectedChapter().setSelectedFrame(frameId);
        app().pauseAutoSave(true);
        reloadCenterPane();
        app().pauseAutoSave(false);
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
     * Returns the left pane fragment
     * @return
     */
    public LeftPaneFragment getLeftPane() {
        return mLeftPane;
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
                mTopPane.onOpen();

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
                mLeftPane.onOpen();

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
                mRightPane.onOpen();

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

        // close the side panes when the center content is clicked
        findViewById(R.id.inputText).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closePanes();
            }
        });
        TextView sourceText = ((TextView)findViewById(R.id.sourceText));
        sourceText.setMovementMethod(new ScrollingMovementMethod());
        sourceText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closePanes();
            }
        });
        final TextView helpText = (TextView)findViewById(R.id.helpTextView);

        // display help text when sourceText is empty.
        // TODO: enable/disable inputText as sourceText becomes available.
        sourceText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if(charSequence.length() > 0) {
                    helpText.setVisibility(View.GONE);
                } else {
                    helpText.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        // automatically save changes to inputText
        final EditText inputText = (EditText)findViewById(R.id.inputText);
        inputText.addTextChangedListener(new TextWatcher() {
            private Timer timer = new Timer();
            private final long DELAY = getResources().getInteger(R.integer.auto_save_delay); // in ms

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                timer.cancel();
                timer = new Timer();
                if(!app().pauseAutoSave()) {
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            // save the changes
                            me.save();
                        }
                    }, DELAY);
                }
            }
        });
    }

    public void closeTopPane() {
        if(mTopSlidingLayer != null) {
            mTopSlidingLayer.closeLayer(true);
        }
    }

    public void closeLeftPane() {
        if (mLeftSlidingLayer != null) {
            mLeftSlidingLayer.closeLayer(true);
        }
        app().pauseAutoSave(true);
        reloadCenterPane();
        app().pauseAutoSave(false);
    }

    /**
     * Updates the center pane with the selected source frame text and any existing translations
     */
    public void reloadCenterPane() {
        // load source text
        TextView sourceText = (TextView)mCenterPane.findViewById(R.id.sourceText);
        sourceText.setText(app().getSharedProjectManager().getSelectedProject().getSelectedChapter().getSelectedFrame().getText());

        // load translation
        Project p = app().getSharedProjectManager().getSelectedProject();
        String translation = app().getSharedTranslationManager().getTranslation(p.getSlug(), LANG_CODE, p.getSelectedChapter().getSelectedFrame().getChapterFrameId());
        EditText inputText = (EditText)mCenterPane.findViewById(R.id.inputText);
        inputText.setText(translation);

        // updates preferences so the app opens to the last opened frame
        // TODO: the auto opening of the frame when the app starts has not been implimented yet.
        app().setActiveProject(app().getSharedProjectManager().getSelectedProject().getSlug());
        app().setActiveChapter(app().getSharedProjectManager().getSelectedProject().getSelectedChapter().getId());
        app().setActiveFrame(app().getSharedProjectManager().getSelectedProject().getSelectedChapter().getSelectedFrame().getFrameId());
    }

    public void closeRightPane() {
        if(mRightSlidingLayer != null) {
            mRightSlidingLayer.closeLayer(true);
        }
    }

    /**
     * Closes all of the edge panes
     */
    public void closePanes() {
        if (mLeftSlidingLayer != null) mLeftSlidingLayer.closeLayer(true);
        if (mRightSlidingLayer != null) mRightSlidingLayer.closeLayer(true);
        if (mTopSlidingLayer != null) mTopSlidingLayer.closeLayer(true);
    }

    /**
     * Saves the translated content found in inputText
     */
    public void save() {
        if (!app().pauseAutoSave()) {
            Log.d("Save", "Performing auto save");
            // do not allow saves to stack up when saves are running slowly.
            app().pauseAutoSave(true);
            String inputTextValue = ((EditText) findViewById(R.id.inputText)).getText().toString();
            Project p = app().getSharedProjectManager().getSelectedProject();

            // TODO: we need a way to manage what language the translation is being made in. This is different than the source languages
            app().getSharedTranslationManager().save(inputTextValue, p.getSlug(), LANG_CODE, p.getSelectedChapter().getSelectedFrame().getChapterFrameId());
            app().pauseAutoSave(false);
        }
    }
}
