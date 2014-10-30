package com.door43.translationstudio;

import com.door43.delegate.DelegateListener;
import com.door43.delegate.DelegateResponse;
import com.door43.translationstudio.dialogs.AdvancedSettingsDialog;
import com.door43.translationstudio.dialogs.InfoDialog;
import com.door43.translationstudio.events.LanguageModalDismissedEvent;
import com.door43.translationstudio.footnotes.Footnote;
import com.door43.translationstudio.panes.left.LeftPaneFragment;
import com.door43.translationstudio.panes.right.RightPaneFragment;
import com.door43.translationstudio.projects.Chapter;
import com.door43.translationstudio.projects.Frame;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Term;
import com.door43.translationstudio.projects.Translation;
import com.door43.translationstudio.translations.TranslationSyncResponse;
import com.door43.translationstudio.uploadwizard.UploadWizardActivity;
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
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ClickableSpan;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private TermsHighlighterTask mHighlighTask;

    // center view fields for caching
    private TextView mSourceText;
    private TextView mSourceTitleText;
    private TextView mSourceFrameNumText;
    private TextView mTranslationTitleText;
    private ImageView mNextFrameView;
    private ImageView mPreviousFrameView;
    private EditText mTranslationEditText;

    private static final int MENU_ITEM_FOOTNOTE = 1;

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

        // set custom commands for input text selection
        mTranslationEditText.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                menu.add(Menu.NONE, MENU_ITEM_FOOTNOTE, Menu.NONE, R.string.menu_footnote);
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                switch(menuItem.getItemId()) {
                    case MENU_ITEM_FOOTNOTE:
                        // TODO: handle footnotes
                        app().showToastMessage("creating footnote");

                        // load selection
                        String translationText = mTranslationEditText.getText().toString();
                        String selectionBefore = translationText.substring(0, mTranslationEditText.getSelectionStart());
                        String selectionAfter = translationText.substring(mTranslationEditText.getSelectionEnd(), mTranslationEditText.length());
                        final String selection = translationText.substring(mTranslationEditText.getSelectionStart(), mTranslationEditText.getSelectionEnd());

                        // TODO: we'll need to display a popup to get the footnote text from the user.

                        // generate clickable footnote
                        Footnote footnote = new Footnote(selection);

                        // add clickable footnote to the text
                        mTranslationEditText.setText(selectionBefore);
                        mTranslationEditText.append(footnote.toCharSequence());
                        mTranslationEditText.append(selectionAfter);

                        return false;
                    default:
                        app().showToastMessage(menuItem.getOrder()+"");
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

//        mTranslationEditText.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                // check if the cursor is on a footnote
//                String translationText = mTranslationEditText.getText().toString();
//                String selectionBefore = translationText.substring(0, mTranslationEditText.getSelectionStart());
//                String selectionAfter = translationText.substring(mTranslationEditText.getSelectionEnd(), mTranslationEditText.length());
//                String selection = translationText.substring(mTranslationEditText.getSelectionStart(), mTranslationEditText.getSelectionEnd());
//
//            }
//        });

        // make links in the translation text clickable
//        m = mTranslationEditText.getMovementMethod();
//        if ((m == null) || !(m instanceof ArrowKeyMovementMethod)) {
//            if (mTranslationEditText.getLinksClickable()) {
//                mTranslationEditText.setMovementMethod(ArrowKeyMovementMethod.getInstance());
//            }
//        }
//        mTranslationEditText.setFocusable(true);
//        mTranslationEditText.setClickable(true);

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
            mSourceFrameNumText.setText(getResources().getString(R.string.label_frame) + " " + (frameIndex + 1) + " of " + p.getSelectedChapter().numFrames());

            // set up task to highlight the source text key terms
            if(mHighlighTask != null && !mHighlighTask.isCancelled()) {
                mHighlighTask.cancel(true);
            }
            mHighlighTask = new TermsHighlighterTask(p.getTerms(), new OnHighlightProgress() {
                @Override
                public void onProgress(String result) {
                    String[] pieces = result.split("<a>");
                    mSourceText.setText("");
                    mSourceText.append(pieces[0]);
                    for(int i=1; i<pieces.length; i++) {
                        // get closing anchor
                        String[] linkChunks = pieces[i].split("</a>");
                        SpannableString link = new SpannableString(linkChunks[0]);
                        final String term = linkChunks[0];
                        ClickableSpan cs = new ClickableSpan() {
                            @Override
                            public void onClick(View widget) {
                                showTermDetails(term);
                            }
                        };
                        link.setSpan(cs, 0, term.length(), 0);
                        mSourceText.append(link);
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
            mHighlighTask.execute(frame.getText());

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
            String text = params[0];
            Vector<Boolean> indicies = new Vector<Boolean>();
            indicies.setSize(text.length());
            for(Term t:mTerms) {
                StringBuffer buf = new StringBuffer();
                Pattern p = Pattern.compile("(?i)\\b" + t.getName() + "\\b");
                Matcher m = p.matcher(text);
                while (m.find()) {
                    Log.d("Key Terms", t.getName());
                    String update = "";
                    for(int i = 0; i < indicies.size(); i ++) {
                        if(indicies.get(i) == null) {
                            update += "[__]";
                        } else {
                            update += "["+i+"]";
                        }
                    }
                    Log.d("UPDATE", "("+indicies.size()+" - actual: "+text.length()+")"+update);
                    if(indicies.size() < m.start() || (indicies.get(m.start()) == null && indicies.get(m.end()) == null)) {
                        // replace
                        String key = "<a>" + m.group() + "</a>";
                        int newKeyEnd = m.start() + key.length()-1;

                        // resize indicies vector
                        if(indicies.size() <= newKeyEnd ) {
                            indicies.setSize(newKeyEnd);
                        }

                        // TODO: IMPORTANT!! Our problem is that the m.start and end are not changing when we manage our indicies!!! This is why not everything is working.
                        // TODO: we need to virtually identify everything that is a valid key then make the changes so that we do not mix up our indicies with m.!!

                        // add new indicies for the anchor characters (7 total)
                        for(int i = m.start(); i < m.start() + 7; i ++) {
                            indicies.insertElementAt(true, i);
                        }

                        // lock indicies
                        for(int i = m.start(); i <= newKeyEnd; i ++) {
                            indicies.set(i, true);
                        }

                        Log.d("ADDING",key + "("+ key.length()+") added from "+m.start() + " to " + newKeyEnd);
                        m.appendReplacement(buf, key);
                    } else {
                        // do nothing. this is a key collision
                        String message = t.getName() + "("+ t.getName().length()+") start("+m.start()+")";
                        if(indicies.get(m.start()) != null) message = message + "[x]";
                        message = message + " end("+m.end()+")";
                        if(indicies.get(m.end()) != null) message = message + "[x]";
                        Log.d("COLLISION!", message);
                    }
                }
                m.appendTail(buf);
                text = buf.toString(); //text.replaceAll("(?i)\\b" + t.getName() + "\\b", "<a>" + t.getName() + "</a>");
            }
            publishProgress(text);
            return text;
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
}
