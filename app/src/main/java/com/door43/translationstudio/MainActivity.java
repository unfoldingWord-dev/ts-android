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
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
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

    private GestureDetector mSourceGestureDetector;
    private GestureDetector mTranslationGestureDetector;
    private final float MIN_FLING_DISTANCE = 100;
    private final float MIN_FLING_VELOCITY = 10;
    private final float MIN_LOG_PRESS = 100;

    // center view fields for caching
    TextView mSourceText;
    TextView mSourceTitleText;
    TextView mSourceFrameNumText;
    TextView mTranslationTitleText;
    ImageView mNextFrameView;
    ImageView mPreviousFrameView;
    EditText mInputText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        app().getSharedTranslationManager().registerDelegateListener(this);

        mCenterPane = (LinearLayout)findViewById(R.id.centerPane);

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
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadCenterPane();
    }

    /**
     * Returns the left pane fragment
     * @return
     */
    public LeftPaneFragment getLeftPane() {
        return mLeftPane;
    }

    /**
     * set up the content panes
     */
    private void initPanes() {
        mLeftPane = new LeftPaneFragment();
        mRightPane = new RightPaneFragment();
        getFragmentManager().beginTransaction().replace(R.id.leftPaneContent, mLeftPane).commit();
        getFragmentManager().beginTransaction().replace(R.id.rightPaneContent, mRightPane).commit();

        // TODO: these should be getting an overlay color, but it's not working.
        ImageView nextFrameView = (ImageView)mCenterPane.findViewById(R.id.hasNextFrameImageView);
        ImageView previousFrameView = (ImageView)mCenterPane.findViewById(R.id.hasPreviousFrameImageView);
        nextFrameView.setColorFilter(getResources().getColor(R.color.blue), PorterDuff.Mode.SRC_ATOP);
        previousFrameView.setColorFilter(getResources().getColor(R.color.blue), PorterDuff.Mode.SRC_ATOP);

        // close the side panes when the center content is clicked
        final EditText translationText = (EditText)findViewById(R.id.inputText);
        // hide the input bottom border
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            translationText.setBackground(null);
        } else {
            translationText.setBackgroundDrawable(null);
        }
        translationText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeDrawers();
            }
        });

        TextView sourceText = ((TextView)findViewById(R.id.sourceText));
        sourceText.setMovementMethod(new ScrollingMovementMethod());
        sourceText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeDrawers();
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
        translationText.addTextChangedListener(new TextWatcher() {
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
        translationText.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return mTranslationGestureDetector.onTouchEvent(event);
            }
        });
    }

    private boolean handleFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
        Log.d("", "onFling: " + event1.toString()+event2.toString());
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
        mSourceText = (TextView)mCenterPane.findViewById(R.id.sourceText);
        mSourceTitleText = (TextView)mCenterPane.findViewById(R.id.sourceTitleText);
        mSourceFrameNumText = (TextView)mCenterPane.findViewById(R.id.sourceFrameNumText);
        mTranslationTitleText = (TextView)mCenterPane.findViewById(R.id.translationTitleText);
        mNextFrameView = (ImageView)mCenterPane.findViewById(R.id.hasNextFrameImageView);
        mPreviousFrameView = (ImageView)mCenterPane.findViewById(R.id.hasPreviousFrameImageView);
        mInputText = (EditText)mCenterPane.findViewById(R.id.inputText);

        Project p = app().getSharedProjectManager().getSelectedProject();
        if(frameIsSelected()) {
            int frameIndex = p.getSelectedChapter().getFrameIndex(p.getSelectedChapter().getSelectedFrame());
            Chapter chapter = p.getSelectedChapter();
            Frame frame = chapter.getSelectedFrame();

            // target translation
            Translation translation = frame.getTranslation();
            mInputText.setText(translation.getText());
            if(chapter.getTitleTranslation().getText() == "") {
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
            mInputText.setText("");
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
        ((DrawerLayout)findViewById(R.id.drawer_layout)).closeDrawers();
        app().pauseAutoSave(true);
        reloadCenterPane();
        app().pauseAutoSave(false);
    }

    public void openLeftDrawer() {
        ((DrawerLayout)findViewById(R.id.drawer_layout)).closeDrawer(Gravity.RIGHT);
        ((DrawerLayout)findViewById(R.id.drawer_layout)).openDrawer(Gravity.LEFT);
    }

    public void openRightDrawer() {
        ((DrawerLayout)findViewById(R.id.drawer_layout)).closeDrawer(Gravity.LEFT);
        ((DrawerLayout)findViewById(R.id.drawer_layout)).openDrawer(Gravity.RIGHT);
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
        app().showToastMessage("Sharing is not enabled yet.");
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
