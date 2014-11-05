package com.door43.translationstudio;

import com.door43.delegate.DelegateListener;
import com.door43.delegate.DelegateResponse;
import com.door43.translationstudio.dialogs.AdvancedSettingsDialog;
import com.door43.translationstudio.dialogs.InfoDialog;
import com.door43.translationstudio.dialogs.PassageNoteDialog;
import com.door43.translationstudio.events.LanguageModalDismissedEvent;
import com.door43.translationstudio.projects.TranslationNote;
import com.door43.translationstudio.spannables.CustomMovementMethod;
import com.door43.translationstudio.spannables.CustomMultiAutoCompleteTextView;
import com.door43.translationstudio.spannables.FancySpan;
import com.door43.translationstudio.spannables.PassageNoteSpan;
import com.door43.translationstudio.spannables.TermSpan;
import com.door43.translationstudio.panes.left.LeftPaneFragment;
import com.door43.translationstudio.panes.right.RightPaneFragment;
import com.door43.translationstudio.projects.Chapter;
import com.door43.translationstudio.projects.Frame;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Term;
import com.door43.translationstudio.projects.Translation;
import com.door43.translationstudio.translations.TranslationSyncResponse;
import com.door43.translationstudio.uploadwizard.UploadWizardActivity;
import com.door43.translationstudio.util.PassageNoteEvent;
import com.door43.translationstudio.util.TranslatorBaseActivity;
import com.squareup.otto.Subscribe;


import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private TermsHighlighterTask mTermsTask;
    private PassageNotesHighlighterTask mPassageNoteTask;

    // center view fields for caching
    private TextView mSourceText;
    private TextView mSourceTitleText;
    private TextView mSourceFrameNumText;
    private TextView mTranslationTitleText;
    private ImageView mNextFrameView;
    private ImageView mPreviousFrameView;
    private CustomMultiAutoCompleteTextView mTranslationEditText;

    private static final int MENU_ITEM_PASSAGENOTE = 1;

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
        mTranslationEditText = (CustomMultiAutoCompleteTextView)mCenterPane.findViewById(R.id.inputText);

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

        // set custom commands for input text selection
        mTranslationEditText.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                // remove title
                actionMode.setTitle(null);
//                actionMode.setCustomView(null);

                // customize menu
                MenuInflater inflater = actionMode.getMenuInflater();
                inflater.inflate(R.menu.text_selection, menu);
                menu.removeItem(android.R.id.selectAll);
                menu.removeItem(android.R.id.paste);
                menu.removeItem(android.R.id.cut);
                menu.removeItem(android.R.id.copy);

                // force always show
                menu.findItem(android.R.id.selectAll).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                menu.findItem(android.R.id.cut).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                menu.findItem(android.R.id.copy).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                menu.findItem(R.id.action_notes).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
//                menu.add("Item " + (i + 1)).setIcon(android.R.drawable.sym_def_app_icon)
//                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                switch(menuItem.getItemId()) {
                    case R.id.action_notes:
                        // load selection
                        String translationText = mTranslationEditText.getText().toString();
                        String selectionBefore = translationText.substring(0, mTranslationEditText.getSelectionStart());
                        String selectionAfter = translationText.substring(mTranslationEditText.getSelectionEnd(), mTranslationEditText.length());
                        final String selection = translationText.substring(mTranslationEditText.getSelectionStart(), mTranslationEditText.getSelectionEnd());

                        // do not allow passage notes to collide
                        if(selection.split(PassageNoteSpan.REGEX_OPEN_TAG).length <= 1 && selection.split(PassageNoteSpan.REGEX_CLOSE_TAG).length <= 1) {
                            // convert to passage note tag
                            String taggedText = "";
                            taggedText += selectionBefore;
                            taggedText += PassageNoteSpan.generateTag(selection, "", false);
                            taggedText += selectionAfter;

                            // parse all passage note tags
                            parsePassageNoteTags(taggedText, true);
                        } else {
                            app().showToastMessage("Passage notes cannot overlap");
                        }
                        return false;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {

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

        // enable scrolling
        mSourceText.setMovementMethod(new ScrollingMovementMethod());

        // make links in the source text clickable
        MovementMethod m = mSourceText.getMovementMethod();
        if ((m == null) || !(m instanceof LinkMovementMethod)) {
            if (mSourceText.getLinksClickable()) {
                mSourceText.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
        mSourceText.setFocusable(true);

        // make links in the translation source clickable without losing selection capabilities.
        mTranslationEditText.setMovementMethod(new CustomMovementMethod());

        // display help text when sourceText is empty.
        final TextView helpText = (TextView)findViewById(R.id.helpTextView);
        mSourceText.addTextChangedListener(new TextWatcher() {
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
        mSourceText.setOnTouchListener(new View.OnTouchListener() {
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

    /**
     * Begins or restarts parsing the note tags.
     * @param text
     */
    private void parsePassageNoteTags(String text) {
        parsePassageNoteTags(text, false);
    }

    /**
     * Begins or restarts parsing the note tags
     * @param text
     */
    private void parsePassageNoteTags(String text, Boolean isNewNote) {
        if(mPassageNoteTask != null && !mPassageNoteTask.isCancelled()) {
            mPassageNoteTask.cancel(true);
        }
        mPassageNoteTask = new PassageNotesHighlighterTask(isNewNote);
        mPassageNoteTask.execute(text);
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

            // load the translation notes
            setTranslationNotes(frame.getTranslationNotes());

            // target translation
            Translation translation = frame.getTranslation();
            parsePassageNoteTags(translation.getText());
            // the translation text is initially loaded as html so so users do not see the raw code before notes are parsed.
            mTranslationEditText.setText(Html.fromHtml(translation.getText()));
            if(chapter.getTitleTranslation().getText().isEmpty()) {
                // display non-translated title
                mTranslationTitleText.setText(translation.getLanguage().getName() + ": [" + chapter.getTitle() + "]");
            } else {
                // display translated title
                mTranslationTitleText.setText(translation.getLanguage().getName() + ": " + chapter.getTitleTranslation().getText());
            }

            // be sure the terms highlighter task is stopped so it doesn't overwrite the new text.
            if(mTermsTask != null && !mTermsTask.isCancelled()) {
                mTermsTask.cancel(true);
            }

            // source translation
            mSourceTitleText.setText(p.getSelectedSourceLanguage().getName() + ": " + p.getSelectedChapter().getTitle());
            mSourceText.setText(frame.getText());
            mSourceFrameNumText.setText(getResources().getString(R.string.label_frame) + " " + (frameIndex + 1) + " of " + p.getSelectedChapter().numFrames());

            // set up task to highlight the source text key terms
            mTermsTask = new TermsHighlighterTask(p.getTerms(), new OnHighlightProgress() {
                @Override
                public void onProgress(String result) {
                    String[] pieces = result.split("<a>");
                    mSourceText.setText("");
                    mSourceText.append(pieces[0]);
                    for(int i=1; i<pieces.length; i++) {
                        // get closing anchor
                        String[] linkChunks = pieces[i].split("</a>");
                        TermSpan term  = new TermSpan(linkChunks[0], linkChunks[0], new FancySpan.OnClickListener() {
                            @Override
                            public void onClick(View view, String spanText, String spanId) {
                                showTermDetails(spanId);
                            }
                        });
                        mSourceText.append(term.toCharSequence());
                        try {
                            mSourceText.append(linkChunks[1]);
                        } catch(Exception e){}
                    }
                }

                @Override
                public void onSuccess(String result) {
                    // TODO: stop the loading indicator
                }
            });
            mTermsTask.execute(frame.getText());

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
    public void openSyncing(Boolean validate) {
        if(app().isNetworkAvailable()) {
            if(validate) {
                Intent intent = new Intent(this, UploadWizardActivity.class);
                startActivity(intent);
            } else {
                app().getSharedTranslationManager().syncSelectedProject();
            }
        } else {
            app().showToastMessage(R.string.internet_not_available);
        }
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
     * Displays the chapter settings
     */
    public void showChapterSettingsMenu() {
        Intent intent = new Intent(me, ChapterSettingActivity.class);
        startActivity(intent);
    }

    /**
     * Displays the project settings
     */
    public void showProjectSettingsMenu() {
        Intent intent = new Intent(me, ProjectSettingsActivity.class);
        startActivity(intent);
    }

    /**
     * Opens the resources panel and displays the term details
     * @param term
     */
    public void showTermDetails(String term) {
        openRightDrawer();
        mRightPane.showTerm(app().getSharedProjectManager().getSelectedProject().getTerm(term));
    }

    /**
     * OPens the resources panel and displays the translation notes
     * @param n
     */
    public void showTranslationNotes(TranslationNote n) {
        openRightDrawer();
        mRightPane.showNotes(n);
    }

    public void setTranslationNotes(TranslationNote n) {
        mRightPane.showNotes(n);
    }

    @Override
    public void onDelegateResponse(String id, DelegateResponse response) {
        if(TranslationSyncResponse.class == response.getClass()) {
            if (((TranslationSyncResponse)response).isSuccess()) {
                openSyncing(false);
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
                openSyncing(true);
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
            case R.id.action_chapter_settings:
                showChapterSettingsMenu();
                return true;
            case R.id.action_project_settings:
                showProjectSettingsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * A task to highlight key terms in the source text
     */
    private class TermsHighlighterTask extends AsyncTask<String, String, String> {
        private OnHighlightProgress mCallback;
        private List<Term> mTerms = new ArrayList<Term>();

        public TermsHighlighterTask(List<Term> terms, OnHighlightProgress callback) {
            mCallback = callback;
            mTerms = terms;
        }

        @Override
        protected String doInBackground(String... params) {
            String keyedText = params[0];
            Vector<Boolean> indicies = new Vector<Boolean>();
            indicies.setSize(keyedText.length());
            for(Term t:mTerms) {
                StringBuffer buf = new StringBuffer();
                Pattern p = Pattern.compile("(?i)\\b" + t.getName() + "\\b");
                // TRICKY: we need to run two matches at the same time in order to keep track of used indicies in the string
                Matcher matcherSourceText = p.matcher(params[0]);
                Matcher matcherKeyedText = p.matcher(keyedText);

                while (matcherSourceText.find() && matcherKeyedText.find()) {
                    // ensure the key term was found in an area of the string that does not overlap another key term.
                    if(indicies.get(matcherSourceText.start()) == null && indicies.get(matcherSourceText.end()) == null) {
                        String key = "<a>" + matcherSourceText.group() + "</a>";
//                        int newKeyEnd = matcherSourceText.start() + key.length()-1;

                        // lock indicies to prevent key term collisions
                        for(int i = matcherSourceText.start(); i <= matcherSourceText.end(); i ++) {
                            if(indicies.size() <= i) {
                                Log.d("test", "sd");
                            }
                            indicies.set(i, true);
                        }

                        // insert the key into the keyedText
                        matcherKeyedText.appendReplacement(buf, key);
                    } else {
                        // do nothing. this is a key collision
                        // e.g. the key term "life" collided with "eternal life".
                    }
                }
                matcherKeyedText.appendTail(buf);
                keyedText = buf.toString();
            }
            publishProgress(keyedText);
            return keyedText;
        }

        protected void onProgressUpdate(String... items) {
            mCallback.onProgress(items[0]);
        }

        protected void onPostExecute(String result) {
            mCallback.onSuccess(result);
        }
    }

    /**
     * An interface tfor the terms highlight task
     */
    private interface OnHighlightProgress {
        void onProgress(String result);
        void onSuccess(String result);
    }

    /**
     * A task to highlight passage notes in the translation text
     */
    private class PassageNotesHighlighterTask extends AsyncTask<String, String, CharSequence> {
        private Boolean mRequestEmptyDefinitions;

        public PassageNotesHighlighterTask(Boolean requestEmptyDefinitions) {
            mRequestEmptyDefinitions = requestEmptyDefinitions;
        }

        @Override
        protected CharSequence doInBackground(String... params) {
            PassageNoteSpan.reset();
            TextView notedResult = new TextView(me);
            PassageNoteSpan needsUpdate = null;
            Pattern p = Pattern.compile(PassageNoteSpan.REGEX_OPEN_TAG + "((?!" + PassageNoteSpan.REGEX_CLOSE_TAG + ").)*" + PassageNoteSpan.REGEX_CLOSE_TAG);
            Pattern defPattern = Pattern.compile("def=\"(((?!\").)*)\"");
            Matcher matcher = p.matcher(params[0]);
            int lastEnd = 0;
            while(matcher.find()) {
                if(matcher.start() > lastEnd) {
                    // add the last piece
                    notedResult.append(params[0].substring(lastEnd, matcher.start()));
                }
                lastEnd = matcher.end();

                // extract definition
                String data = matcher.group().substring(0, matcher.group().length() - PassageNoteSpan.REGEX_CLOSE_TAG.length());
                Matcher defMatcher = defPattern.matcher(data);
                String def = "";
                if(defMatcher.find()) {
                    def = defMatcher.group(1);
                }
                final String definition = def;

                // extract phrase
                String[] pieces = data.split(PassageNoteSpan.REGEX_OPEN_TAG);

                // check if footnote is set in the open tag
                Boolean isFootnote = false;
                if(data.substring(0, data.length() - pieces[1].length()).contains("footnote")) {
                    isFootnote = true;
                }
                final Boolean displayAsFootnote = isFootnote;

                // build passage note
                PassageNoteSpan note = new PassageNoteSpan(pieces[1], definition, isFootnote, new FancySpan.OnClickListener() {
                    @Override
                    public void onClick(View view, String spanText, String spanId) {
                        openPassageNoteDialog(spanText, definition, spanId, displayAsFootnote);
                    }
                });
                if(definition.isEmpty()) {
                    needsUpdate = note;
                }
                notedResult.append(note.toCharSequence());
            }
            if(lastEnd < params[0].length()) {
                notedResult.append(params[0].substring(lastEnd, params[0].length()));
            }

            // display a dialog to populate the empty definition.
            if(needsUpdate != null && mRequestEmptyDefinitions) {
                openPassageNoteDialog(needsUpdate.toString(), "", needsUpdate.getId()+"", needsUpdate.isFootnote());
            }
            return notedResult.getText();
        }

        protected void onPostExecute(CharSequence result) {
            mTranslationEditText.setText(result);
        }
    }

    /**
     * A task update passages notes
     */
    private class PassageNotesUpdaterTask extends AsyncTask<String, String, Void> {
        private Boolean mUpdate = false;
        private Boolean mIsFootnote = false;

        /**
         * Specifies if the passage note should be updated or removed
         * @param update
         */
        public PassageNotesUpdaterTask(Boolean update, Boolean isFootnote) {
            mUpdate = update;
            mIsFootnote = isFootnote;
        }

        @Override
        protected Void doInBackground(String... params) {
            String text = params[0];
            String spanId = params[1];
            String spanPassage = params[2];
            String spanPassageDefinition = params[3];

            TextView updatedResult = new TextView(me);

            Pattern p = Pattern.compile(PassageNoteSpan.regexOpenTagById(spanId) + "((?!" + PassageNoteSpan.REGEX_CLOSE_TAG + ").)*" + PassageNoteSpan.REGEX_CLOSE_TAG);
            Matcher matcher = p.matcher(text);
            if(matcher.find()) {
                updatedResult.append(text.substring(0, matcher.start()));
                if(mUpdate) {
                    // update passage note
                    updatedResult.append(PassageNoteSpan.generateTag(spanPassage, spanPassageDefinition, mIsFootnote));
                } else {
                    // remove passage note
                    String data = matcher.group().substring(0, matcher.group().length() - PassageNoteSpan.REGEX_CLOSE_TAG.length());
                    String[] pieces = data.split(PassageNoteSpan.REGEX_OPEN_TAG);
                    updatedResult.append(pieces[1]);
                }
                if(matcher.end() < text.length()) {
                    updatedResult.append(text.substring(matcher.end(), text.length()));
                }
                parsePassageNoteTags(updatedResult.getText().toString());
            }
            return null;
        }
    }

    /**
     * Displays a dialog for editing a passage note
     * @param passage
     * @param definition
     * @param id
     * @param isFootnote
     */
    public void openPassageNoteDialog(String passage, String definition, String id, Boolean isFootnote) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        app().closeToastMessage();

        // Create and show the dialog
        PassageNoteDialog newFragment = new PassageNoteDialog();
        Bundle args = new Bundle();
        args.putString("passage", passage);
        args.putString("note", definition);
        args.putBoolean("footnote", isFootnote);
        args.putString("id", id);
        newFragment.setArguments(args);
        newFragment.show(ft, "dialog");
    }

    @Subscribe
    public void passageNote(PassageNoteEvent event) {
        // close the dialog.
        event.getDialog().dismiss();
        PassageNotesUpdaterTask task;
        switch(event.getStatus()) {
            case OK:
                // update the passage note
                if(!event.getNote().isEmpty()) {
                    task = new PassageNotesUpdaterTask(true, event.getIsFootnote());

                } else {
                    // delete empty notes
                    task = new PassageNotesUpdaterTask(false, false);
                }
                task.execute(mTranslationEditText.getText().toString(),
                        event.getSpanId(),
                        event.getPassage(),
                        event.getNote());
                break;
            case DELETE:
                // remove the passage note
                task = new PassageNotesUpdaterTask(false, false);
                task.execute(mTranslationEditText.getText().toString(),
                        event.getSpanId(),
                        event.getPassage(),
                        event.getNote());
                break;
            case CANCEL:
            default:
                // do nothing
        }
    }
}
