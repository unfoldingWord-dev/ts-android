package com.door43.translationstudio;

import com.door43.delegate.DelegateListener;
import com.door43.delegate.DelegateResponse;
import com.door43.translationstudio.dialogs.AdvancedSettingsDialog;
import com.door43.translationstudio.dialogs.InfoDialog;
import com.door43.translationstudio.events.LanguageModalDismissedEvent;
import com.door43.translationstudio.panes.left.LeftPaneFragment;
import com.door43.translationstudio.panes.right.RightPaneFragment;
import com.door43.translationstudio.projects.Chapter;
import com.door43.translationstudio.projects.Frame;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Translation;
import com.door43.translationstudio.translations.TranslationSyncResponse;
import com.door43.translationstudio.util.TranslatorBaseActivity;
import com.squareup.otto.Subscribe;


import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends TranslatorBaseActivity implements DelegateListener {
    private final MainActivity me = this;

    private static final String LANG_CODE = "en"; // TODO: this will eventually need to be managed dynamically by the project manager

    // content panes
    private LeftPaneFragment mLeftPane;
    private RightPaneFragment mRightPane;
    private LinearLayout mCenterPane;
    private DrawerLayout mRootView;

    private GestureDetector mSourceGestureDetector;
    private GestureDetector mTranslationGestureDetector;
    private final float MIN_FLING_DISTANCE = 100;
    private final float MIN_FLING_VELOCITY = 10;
    private final float MIN_LOG_PRESS = 100;
    private int mActionBarHeight;
    private boolean mActivityIsInitializing;

    // center view fields for caching
    TextView mSourceText;
    TextView mSourceTitleText;
    TextView mSourceFrameNumText;
    TextView mTranslationTitleText;
    ImageView mNextFrameView;
    ImageView mPreviousFrameView;
    EditText mTranslationEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mActivityIsInitializing = true;

        app().getSharedTranslationManager().registerDelegateListener(this);

        mCenterPane = (LinearLayout)findViewById(R.id.centerPane);
        mRootView = (DrawerLayout)findViewById(R.id.drawer_layout);

        initPanes();

        if(app().getSharedProjectManager().getSelectedProject() != null && app().getSharedProjectManager().getSelectedProject().getSelectedChapter() == null) {
            // the project contains no chapters for the current language
            Intent languageIntent = new Intent(me, LanguageSelectorActivity.class);
            languageIntent.putExtra("sourceLanguages", true);
            startActivity(languageIntent);
        }

        if(app().shouldShowWelcome()) {
            // perform any welcoming tasks here
            app().setShouldShowWelcome(false);
            openLeftDrawer();
        } else {
            // open the drawer if the remembered chapter does not exist
            if(app().getUserPreferences().getBoolean(SettingsActivity.KEY_PREF_REMEMBER_POSITION, Boolean.parseBoolean(getResources().getString(R.string.pref_default_remember_position)))) {
                if(app().getSharedProjectManager().getSelectedProject().getSelectedChapter() == null) {
                    openLeftDrawer();
                }
            }
            app().pauseAutoSave(true);
            reloadCenterPane();
            app().pauseAutoSave(false);
        }
        closeTranslationKeyboard();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        // save any changes to the frame
        save();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!mActivityIsInitializing) {
            reloadCenterPane();
        } else {
            // don't reload the center pane the first time the app starts.
            mActivityIsInitializing = false;
        }
    }

    /**
     * Returns the left pane fragment
     * @return
     */
    public LeftPaneFragment getLeftPane() {
        return mLeftPane;
    }

    /**
     * Triggered when the keyboard opens
     * @param r the dimensions of the visible area
     */
    private void onKeyboardOpen(Rect r) {
        resizeRootView(r);
    }

    /**
     * Triggered when the keyboard closes
     * @param r the dimensions of the visible area
     */
    private void onKeyboardClose(Rect r) {
        mTranslationEditText.clearFocus();
        resizeRootView(r);
    }

    private void resizeRootView(Rect r) {
        ViewGroup.LayoutParams params = mRootView.getLayoutParams();
        params.height = r.bottom - mActionBarHeight;
        mRootView.setLayoutParams(params);
    }

    /**
     * set up the content panes
     */
    private void initPanes() {
        mSourceText = (TextView)mCenterPane.findViewById(R.id.sourceText);
        mSourceTitleText = (TextView)mCenterPane.findViewById(R.id.sourceTitleText);
        mSourceFrameNumText = (TextView)mCenterPane.findViewById(R.id.sourceFrameNumText);
        mTranslationTitleText = (TextView)mCenterPane.findViewById(R.id.translationTitleText);
        mNextFrameView = (ImageView)mCenterPane.findViewById(R.id.hasNextFrameImageView);
        mPreviousFrameView = (ImageView)mCenterPane.findViewById(R.id.hasPreviousFrameImageView);
        mTranslationEditText = (EditText)mCenterPane.findViewById(R.id.inputText);

        mTranslationEditText.setEnabled(false);

        // calculate actionbar height
        TypedValue tv = new TypedValue();
        mActionBarHeight = 0;
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
        {
            mActionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
        }

        // hack to watch for the soft keyboard open and close
        final View activityRootView = ((ViewGroup)findViewById(android.R.id.content)).getChildAt(0);
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            private boolean mWasOpened;
            private final Rect mRect= new Rect();

            @Override
            public void onGlobalLayout() {
                activityRootView.getWindowVisibleDisplayFrame(mRect);
                int heightDiff = activityRootView.getRootView().getHeight() - (mRect.bottom - mRect.top);
                boolean isOpen = heightDiff > 100;
                if (isOpen == mWasOpened) {
                    return;
                }
                mWasOpened = isOpen;
                if(isOpen) {
                    onKeyboardOpen(mRect);
                } else {
                    onKeyboardClose(mRect);
                }
            }
        });

        mLeftPane = new LeftPaneFragment();
        mRightPane = new RightPaneFragment();
        getFragmentManager().beginTransaction().replace(R.id.leftPaneContent, mLeftPane).commit();
        getFragmentManager().beginTransaction().replace(R.id.rightPaneContent, mRightPane).commit();

        // TODO: these should be getting an overlay color, but it's not working.
        ImageView nextFrameView = (ImageView)mCenterPane.findViewById(R.id.hasNextFrameImageView);
        ImageView previousFrameView = (ImageView)mCenterPane.findViewById(R.id.hasPreviousFrameImageView);
        nextFrameView.setColorFilter(getResources().getColor(R.color.blue), PorterDuff.Mode.SRC_ATOP);
        previousFrameView.setColorFilter(getResources().getColor(R.color.blue), PorterDuff.Mode.SRC_ATOP);

        // hide the input bottom border
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mTranslationEditText.setBackground(null);
        } else {
            mTranslationEditText.setBackgroundDrawable(null);
        }

        TextView sourceText = ((TextView)findViewById(R.id.sourceText));
        sourceText.setMovementMethod(new ScrollingMovementMethod());

        // display help text when sourceText is empty.
        final TextView helpText = (TextView)findViewById(R.id.helpTextView);
        sourceText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if(charSequence.length() > 0) {
                    helpText.setVisibility(View.GONE);
                    mTranslationEditText.setEnabled(true);
                } else {
                    helpText.setVisibility(View.VISIBLE);
                    mTranslationEditText.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        mSourceText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mSourceText.getText().length() == 0) {
                    openLeftDrawer();
                }
            }
        });

        // automatically save changes to inputText
        mTranslationEditText.addTextChangedListener(new TextWatcher() {
            private Timer timer = new Timer();

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                int saveDelay = Integer.parseInt(app().getUserPreferences().getString(SettingsActivity.KEY_PREF_AUTOSAVE, getResources().getString(R.string.pref_default_autosave)));
                timer.cancel();
                if (saveDelay != -1) {
                    timer = new Timer();
                    if (!app().pauseAutoSave()) {
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                // save the changes
                                me.save();
                            }
                        }, saveDelay);
                    }
                }
            }
        });

        // detect gestures
        mSourceGestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
                return handleFling(event1, event2, velocityX, velocityY);
            }
        });
        mTranslationGestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
                return handleFling(event1, event2, velocityX, velocityY);
            }
        });

        // hook up gesture detectors
        sourceText.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return mSourceGestureDetector.onTouchEvent(event);
            }
        });
        mTranslationEditText.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return mTranslationGestureDetector.onTouchEvent(event);
            }
        });
        mTranslationEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (keyEvent != null && i == EditorInfo.IME_ACTION_DONE) {
                    Log.i("keyboard", "done");
                }
                return false;
            }
        });
    }

    public void closeTranslationKeyboard() {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    private boolean handleFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
//        Log.d("", "onFling: " + event1.toString()+event2.toString());
        // positive distance moves right
        Float distanceX = event2.getX() - event1.getX();
        Project p = app().getSharedProjectManager().getSelectedProject();
        if(Math.abs(distanceX) >= MIN_FLING_DISTANCE && Math.abs(velocityX) >= MIN_FLING_VELOCITY && p != null && p.getSelectedChapter() != null && p.getSelectedChapter().getSelectedFrame() != null) {
            // automatically save changes if the auto save did not have time to save
            // TODO: this should only occure if there were actual changes
            save();
            Frame f;
            if(distanceX > 0) {
                f = p.getSelectedChapter().getPreviousFrame();
            } else {
                f = p.getSelectedChapter().getNextFrame();
            }
            if(f != null) {
                p.getSelectedChapter().setSelectedFrame(f.getId());
                mLeftPane.selectTab(mLeftPane.getSelectedTabIndex());
            }
            app().pauseAutoSave(true);
            reloadCenterPane();
            app().pauseAutoSave(false);
            return true;
        } else {
            // TODO: might be cool to perform some simple animation to indicate you can swipe.
            return false;
        }
    }

    /**
     * Updates the center pane with the selected source frame text and any existing translations
     */
    public void reloadCenterPane() {
        // load source text

        Project p = app().getSharedProjectManager().getSelectedProject();
        if(frameIsSelected()) {
            int frameIndex = p.getSelectedChapter().getFrameIndex(p.getSelectedChapter().getSelectedFrame());
            Chapter chapter = p.getSelectedChapter();
            Frame frame = chapter.getSelectedFrame();

            // target translation
            Translation translation = frame.getTranslation();
            mTranslationEditText.setText(translation.getText());
            if(chapter.getTitleTranslation().getText().isEmpty()) {
                // display non-translated title
                mTranslationTitleText.setText(translation.getLanguage().getName() + ": [" + chapter.getTitle() + "]");
            } else {
                // display translated title
                mTranslationTitleText.setText(translation.getLanguage().getName() + ": " + chapter.getTitleTranslation().getText());
            }

            // source translation
            mSourceTitleText.setText(p.getSelectedSourceLanguage().getName() + ": " + p.getSelectedChapter().getTitle());
            mSourceText.setText(frame.getText());
            mSourceFrameNumText.setText((frameIndex + 1) + " of " + p.getSelectedChapter().numFrames());

            // navigation indicators
            if(p.getSelectedChapter().numFrames() > frameIndex + 1) {
                mNextFrameView.setVisibility(View.VISIBLE);
            } else {
                mNextFrameView.setVisibility(View.INVISIBLE);
            }
            if(0 < frameIndex) {
                mPreviousFrameView.setVisibility(View.VISIBLE);
            } else {
                mPreviousFrameView.setVisibility(View.INVISIBLE);
            }

            // updates preferences so the app opens to the last opened frame
            app().setActiveProject(p.getId());
            app().setActiveChapter(p.getSelectedChapter().getId());
            app().setActiveFrame(frame.getId());
        } else {
            mTranslationEditText.setText("");
            mTranslationTitleText.setText("");
            mSourceTitleText.setText("");
            mSourceText.setText("");
            mSourceFrameNumText.setText("");
            mNextFrameView.setVisibility(View.INVISIBLE);
            mPreviousFrameView.setVisibility(View.INVISIBLE);

            // nothing was selected so open the project selector
            openLeftDrawer();
        }
    }

    // Checks if a frame has been selected in the app
    public boolean frameIsSelected() {
        Project selectedProject = app().getSharedProjectManager().getSelectedProject();
        return selectedProject != null && selectedProject.getSelectedChapter() != null && selectedProject.getSelectedChapter().getSelectedFrame() != null;
    }

    /**
     * Closes all the navigation drawers
     */
    public void closeDrawers() {
        mRootView.closeDrawers();
        app().pauseAutoSave(true);
        reloadCenterPane();
        app().pauseAutoSave(false);
    }

    public void openLeftDrawer() {
        mRootView.closeDrawer(Gravity.RIGHT);
        mRootView.openDrawer(Gravity.LEFT);
    }

    public void openRightDrawer() {
        mRootView.closeDrawer(Gravity.LEFT);
        mRootView.openDrawer(Gravity.RIGHT);
    }

    /**
     * Saves the translated content found in inputText
     */
    public void save() {
        if (!app().pauseAutoSave() && frameIsSelected()) {
            // do not allow saves to stack up when saves are running slowly.
            app().pauseAutoSave(true);
            String inputTextValue = ((EditText) findViewById(R.id.inputText)).getText().toString();
            Frame f = app().getSharedProjectManager().getSelectedProject().getSelectedChapter().getSelectedFrame();
            f.setTranslation(inputTextValue);
            f.save();
            app().pauseAutoSave(false);
        }
    }

    /**
     * Opens the user settings activity
     */
    public void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    /**
     * Begins syncing the selected project
     */
    public void openSyncing() {
        app().getSharedTranslationManager().syncSelectedProject();
    }

    /**
     * Opens the sharing and export activity
     */
    public void openSharing() {
        Intent intent = new Intent(this, SharingActivity.class);
        startActivity(intent);
    }

    /**
     * opens the advanced settings dialog
     */
    public void openAdvancedSettings() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        app().closeToastMessage();
        // Create and show the dialog.
        AdvancedSettingsDialog newFragment = new AdvancedSettingsDialog();

        newFragment.show(ft, "dialog");
    }

    /**
     * opens the app info dialog
     */
    public void openInfo() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        app().closeToastMessage();
        // Create and show the dialog.
        InfoDialog newFragment = new InfoDialog();

        newFragment.show(ft, "dialog");
    }

    /**
     * Displays the translation contextual menu
     */
    public void showTranslationMenu() {
        Intent chapterSettingsIntent = new Intent(me, ChapterSettingActivity.class);
        startActivity(chapterSettingsIntent);
    }

    @Override
    public void onDelegateResponse(String id, DelegateResponse response) {
        if(TranslationSyncResponse.class == response.getClass()) {
            if (((TranslationSyncResponse)response).isSuccess()) {
                openSyncing();
            } else {
                // error
            }
        }
    }

    @Override
    public void onDestroy() {
        app().getSharedTranslationManager().removeDelegateListener(this);
        super.onDestroy();
    }

    @Subscribe
    public void modalDismissed(LanguageModalDismissedEvent event) {
        reloadCenterPane();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_share:
                openSharing();
                return true;
            case R.id.action_sync:
                openSyncing();
                return true;
            case R.id.action_settings:
                openSettings();
                return true;
            case R.id.action_info:
                openInfo();
                return true;
            case R.id.action_library:
                openLeftDrawer();
                return true;
            case R.id.action_resources:
                openRightDrawer();
                return true;
            case R.id.action_translation_settings:
                showTranslationMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
