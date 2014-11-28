package com.door43.translationstudio;

import com.door43.translationstudio.dialogs.AdvancedSettingsDialog;
import com.door43.translationstudio.dialogs.InfoDialog;
import com.door43.translationstudio.dialogs.NoteDialog;
import com.door43.translationstudio.events.ChapterTranslationStatusChangedEvent;
import com.door43.translationstudio.events.FrameTranslationStatusChangedEvent;
import com.door43.translationstudio.events.SecurityKeysSubmittedEvent;
import com.door43.translationstudio.spannables.CustomMovementMethod;
import com.door43.translationstudio.spannables.CustomMultiAutoCompleteTextView;
import com.door43.translationstudio.spannables.FancySpan;
import com.door43.translationstudio.spannables.NoteSpan;
import com.door43.translationstudio.spannables.TermSpan;
import com.door43.translationstudio.panes.left.LeftPaneFragment;
import com.door43.translationstudio.panes.right.RightPaneFragment;
import com.door43.translationstudio.projects.Chapter;
import com.door43.translationstudio.projects.Frame;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Term;
import com.door43.translationstudio.projects.Translation;
import com.door43.translationstudio.uploadwizard.UploadWizardActivity;
import com.door43.translationstudio.util.MainContext;
import com.door43.translationstudio.util.PassageNoteEvent;
import com.door43.translationstudio.util.TranslatorBaseActivity;
import com.squareup.otto.Subscribe;


import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ClickableSpan;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Display;
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
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
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

public class MainActivity extends TranslatorBaseActivity {
    private final MainActivity me = this;

    // content panes
    private LeftPaneFragment mLeftPane;
    private RightPaneFragment mRightPane;
    private LinearLayout mCenterPane;
    private DrawerLayout mRootView;

    private GestureDetector mSourceGestureDetector;
    private GestureDetector mTranslationGestureDetector;
    private int mActionBarHeight;
    private int mStatusBarHeight;
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
    private int mSourceTextMotionDownX = 0;
    private int mSourceTextMotionDownY = 0;
    private static final int TEXT_FADE_SPEED = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mActivityIsInitializing = true;
        app().setMainActivity(this);

        mCenterPane = (LinearLayout)findViewById(R.id.centerPane);
        mRootView = (DrawerLayout)findViewById(R.id.drawer_layout);
        mRootView.setScrimColor(getResources().getColor(R.color.scrim));

        initPanes();

        if(app().getSharedProjectManager().getSelectedProject() != null && app().getSharedProjectManager().getSelectedProject().getSelectedChapter() == null) {
            // the project contains no chapters for the current language
            Intent languageIntent = new Intent(me, LanguageSelectorActivity.class);
            languageIntent.putExtra("sourceLanguages", true);
            startActivity(languageIntent);
        }

        if(app().shouldShowWelcome()) {
            // perform any welcoming tasks here. This happens when the user first opens the app.
            app().setShouldShowWelcome(false);
            openLeftDrawer();
        } else {
            // open the drawer if the remembered chapter does not exist
            if(app().getUserPreferences().getBoolean(SettingsActivity.KEY_PREF_REMEMBER_POSITION, Boolean.parseBoolean(getResources().getString(R.string.pref_default_remember_position)))) {
                if(app().getSharedProjectManager().getSelectedProject() == null || app().getSharedProjectManager().getSelectedProject().getSelectedChapter() == null) {
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
            app().pauseAutoSave(true);
            reloadCenterPane();
            app().pauseAutoSave(false);
            mLeftPane.reloadFramesTab();
            mLeftPane.reloadChaptersTab();
            mLeftPane.reloadProjectsTab();
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
        params.height = r.bottom - mActionBarHeight - mStatusBarHeight;
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

        // set up custom fonts
        Typeface translationTypeface = app().getTranslationTypeface();
        FancySpan.setGlobalTypeface(translationTypeface);
        mTranslationEditText.setTypeface(translationTypeface);
        mSourceText.setTypeface(translationTypeface);

        // set custom font size (sp)
        int typefaceSize = Integer.parseInt(MainContext.getContext().getUserPreferences().getString(SettingsActivity.KEY_PREF_TYPEFACE_SIZE, MainContext.getContext().getResources().getString(R.string.pref_default_typeface_size)));
        FancySpan.setGlobalTypefaceSize(typefaceSize);
        mTranslationEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, typefaceSize);
        mSourceText.setTextSize(TypedValue.COMPLEX_UNIT_SP, typefaceSize);

        // get the actionbar height
        TypedValue tv = new TypedValue();
        mActionBarHeight = 0;
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
        {
            mActionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
        }

        // get the statusbar height
        mStatusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            mStatusBarHeight = getResources().getDimensionPixelSize(resourceId);
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
                        if(selection.split(NoteSpan.REGEX_OPEN_TAG).length <= 1 && selection.split(NoteSpan.REGEX_CLOSE_TAG).length <= 1) {
                            // convert to user note tag
                            String taggedText = "";
                            taggedText += selectionBefore;
                            taggedText += NoteSpan.generateTag(selection, "", NoteSpan.NoteType.UserNote);
                            taggedText += selectionAfter;

                            // parse all passage note tags
                            parsePassageNoteTags(taggedText, true);
                        } else {
                            app().showToastMessage(R.string.notes_cannot_overlap);
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

        // set up drawers
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mLeftPane = new LeftPaneFragment();
        mRightPane = new RightPaneFragment();
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            // make the right pane fill half the screen when in landscape mode.
            mRightPane.setLayoutWidth(size.x / 2);
        }
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
        mTranslationEditText.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return mTranslationGestureDetector.onTouchEvent(event);
            }
        });
        mTranslationEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (keyEvent != null && i == EditorInfo.IME_ACTION_DONE) {
//                    Log.i("keyboard", "done");
                }
                return false;
            }
        });

        // enable scrolling
        mSourceText.setMovementMethod(new ScrollingMovementMethod());

        // make links in the source text clickable
//        MovementMethod m = mSourceText.getMovementMethod();
//        if ((m == null) || !(m instanceof LinkMovementMethod)) {
//            if (mSourceText.getLinksClickable()) {
//                mSourceText.setMovementMethod(LinkMovementMethod.getInstance());
//            }
//        }
        mSourceText.setFocusable(true);
        /*
        * LinkMovementMethod disables parent gesture events for the spans.
        * So we manually enable the clicking event in order to support scrolling on top of spans.
        * http://stackoverflow.com/questions/7236840/android-textview-linkify-intercepts-with-parent-view-gestures
        * */
        mSourceText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                TextView widget = (TextView) view;
                Object text = widget.getText();
                if (text instanceof Spanned) {
                    Spannable buffer = (Spannable) text;

                    int action = motionEvent.getAction();
                    int x = (int) motionEvent.getX();
                    int y = (int) motionEvent.getY();

                    if(action == MotionEvent.ACTION_DOWN) {
                        mSourceTextMotionDownX = x;
                        mSourceTextMotionDownY = y;
                    } else if (action == MotionEvent.ACTION_UP) {
                        // don't click spans when dragging. we give a little wiggle room just in case
                        int maxSpanClickWiggle = 5;
                        if(Math.abs(mSourceTextMotionDownX - x) > maxSpanClickWiggle || Math.abs(mSourceTextMotionDownY - y) > maxSpanClickWiggle) {
                            return mSourceGestureDetector.onTouchEvent(motionEvent);
                        }

                        x -= widget.getTotalPaddingLeft();
                        y -= widget.getTotalPaddingTop();

                        x += widget.getScrollX();
                        y += widget.getScrollY();

                        Layout layout = widget.getLayout();
                        int line = layout.getLineForVertical(y);
                        int off = layout.getOffsetForHorizontal(line, x);
                        ClickableSpan[] link = buffer.getSpans(off, off,
                                ClickableSpan.class);

                        if (link.length != 0) {
                            if (action == MotionEvent.ACTION_UP) {
                                motionEvent.getX();
                                link[0].onClick(widget);
                            }
                            return mSourceGestureDetector.onTouchEvent(motionEvent);
                        }
                    }

                }

                return mSourceGestureDetector.onTouchEvent(motionEvent);
            }
        });


        // make links in the translation text clickable without losing selection capabilities.
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

        // get notified when drawers open
        mRootView.setDrawerListener(new ActionBarDrawerToggle(this, mRootView, R.drawable.ic_ab_back_holo_light_am, R.string.close, R.string.close) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                mTranslationEditText.setEnabled(true);
            }
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                mTranslationEditText.setEnabled(false);
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
    private void parsePassageNoteTags(final String text, Boolean isNewNote) {
        if(mPassageNoteTask != null && !mPassageNoteTask.isCancelled()) {
            mPassageNoteTask.cancel(true);
        }
        final Animation in = new AlphaAnimation(0.0f, 1.0f);
        in.setDuration(TEXT_FADE_SPEED);
        final Animation out = new AlphaAnimation(1.0f, 0.0f);
        out.setDuration(TEXT_FADE_SPEED);
        mPassageNoteTask = new PassageNotesHighlighterTask(isNewNote, new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                mTranslationEditText.setVisibility(View.VISIBLE);
                mTranslationEditText.startAnimation(in);
                return false;
            }
        });
        out.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                mTranslationEditText.setVisibility(View.INVISIBLE);
                mPassageNoteTask.execute(text);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTranslationEditText.startAnimation(out);
            }
        });
    }

    public void closeTranslationKeyboard() {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    private boolean handleFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
        // positive x distance moves right
        final double maxFlingAngle = 20;
        final float minFlingDistance = 50;
        final float minFlingVelocity = 20;
        float distanceX = event2.getX() - event1.getX();
        float distanceY = event2.getY() - event1.getY();

        // don't handle vertical swipes (division error)
        if(distanceX == 0) return false;

        double flingAngle = Math.toDegrees(Math.asin(Math.abs(distanceY/distanceX)));

        Project p = app().getSharedProjectManager().getSelectedProject();
        if(flingAngle <= maxFlingAngle && Math.abs(distanceX) >= minFlingDistance && Math.abs(velocityX) >= minFlingVelocity && p != null && p.getSelectedChapter() != null && p.getSelectedChapter().getSelectedFrame() != null) {
            // automatically save changes if the auto save did not have time to save
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
     * This method will cause a view to fade out after which it fires a callback where operations can be performed.
     * lastly it will fade back in.
     * TODO: this should be placed in a utility class
     */
    public static void fadeOutActionInAnimation(final View view, final Handler.Callback callback) {
        final Animation in = new AlphaAnimation(0.0f, 1.0f);
        in.setDuration(TEXT_FADE_SPEED);
        final Animation out = new AlphaAnimation(1.0f, 0.0f);
        out.setDuration(TEXT_FADE_SPEED);
        out.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                callback.handleMessage(null);
                view.startAnimation(in);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        view.startAnimation(out);
    }

    /**
     * Updates the center pane with the selected source frame text and any existing translations
     */
    public void reloadCenterPane() {
        invalidateOptionsMenu();
        // load the text
        final Project p = app().getSharedProjectManager().getSelectedProject();
        if(frameIsSelected()) {
            final int frameIndex = p.getSelectedChapter().getFrameIndex(p.getSelectedChapter().getSelectedFrame());
            final Chapter chapter = p.getSelectedChapter();
            final Frame frame = chapter.getSelectedFrame();

            // get the target language
            if(!p.hasChosenTargetLanguage()) {
                showProjectSettingsMenu();
            }

            // target translation
            final Translation translation = frame.getTranslation();


            parsePassageNoteTags(translation.getText());

            if(chapter.getTitleTranslation().getText().isEmpty()) {
                // display non-translated title
                fadeOutActionInAnimation(mTranslationTitleText, new Handler.Callback() {
                    @Override
                    public boolean handleMessage(Message message) {
                        mTranslationTitleText.setText(translation.getLanguage().getName() + ": [" + chapter.getTitle() + "]");
                        return false;
                    }
                });
            } else {
                // display translated title
                fadeOutActionInAnimation(mTranslationTitleText, new Handler.Callback() {
                    @Override
                    public boolean handleMessage(Message message) {
                        mTranslationTitleText.setText(translation.getLanguage().getName() + ": " + chapter.getTitleTranslation().getText());
                        return false;
                    }
                });
            }

            // source translation
            fadeOutActionInAnimation(mSourceTitleText, new Handler.Callback() {
                @Override
                public boolean handleMessage(Message message) {
                    mSourceTitleText.setText(p.getSelectedSourceLanguage().getName() + ": " + p.getSelectedChapter().getTitle());
                    return false;
                }
            });
            fadeOutActionInAnimation(mSourceFrameNumText, new Handler.Callback() {
                @Override
                public boolean handleMessage(Message message) {
                    mSourceFrameNumText.setText(getResources().getString(R.string.label_frame) + " " + (frameIndex + 1) + " " + getResources().getString(R.string.of) + " " + p.getSelectedChapter().numFrames());
                    return false;
                }
            });

            // be sure the terms highlighter task is stopped so it doesn't overwrite the new text.
            if(mTermsTask != null && !mTermsTask.isCancelled()) {
                mTermsTask.cancel(true);
            }

            // set up task to highlight the source text key terms. This also loads the important terms into the frame
            final Animation in = new AlphaAnimation(0.0f, 1.0f);
            in.setDuration(TEXT_FADE_SPEED);
            final Animation out = new AlphaAnimation(1.0f, 0.0f);
            out.setDuration(TEXT_FADE_SPEED);
            mTermsTask = new TermsHighlighterTask(p.getTerms(), new OnHighlightProgress() {
                @Override
                public void onSuccess(String result) {
                    if(app().getUserPreferences().getBoolean(SettingsActivity.KEY_PREF_HIGHLIGHT_KEY_TERMS, Boolean.parseBoolean(getResources().getString(R.string.pref_default_highlight_key_terms)))) {
                        final String textResult = result;
                        // load the highlighted text
                        String[] pieces = textResult.split("<a>");
                        mSourceText.setText("");
                        mSourceText.append(pieces[0]);
                        for (int i = 1; i < pieces.length; i++) {
                            // get closing anchor
                            String[] linkChunks = pieces[i].split("</a>");
                            TermSpan term = new TermSpan(linkChunks[0], linkChunks[0], new FancySpan.OnClickListener() {
                                @Override
                                public void onClick(View view, FancySpan span) {
                                    showTermDetails(span.getSpanId());
                                }
                            });
                            mSourceText.append(term.toCharSequence());
                            try {
                                mSourceText.append(linkChunks[1]);
                            } catch (Exception e) {
                            }
                        }
                    } else {
                        // just display the plain source text
                        mSourceText.setText(frame.getText());
                    }
                    mSourceText.setVisibility(View.VISIBLE);
                    mSourceText.startAnimation(in);
                    // scroll to top
                    mSourceText.scrollTo(0, 0);
                    mRightPane.reloadTerm();
                }
            });
            out.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mSourceText.setVisibility(View.INVISIBLE);
                    mTermsTask.execute(frame);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            mSourceText.startAnimation(out);

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
        if (!app().pauseAutoSave() && frameIsSelected() && app().getSharedProjectManager().getSelectedProject().hasChosenTargetLanguage()) {
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
        if(app().isNetworkAvailable()) {
            Intent intent = new Intent(this, UploadWizardActivity.class);
            startActivity(intent);
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
        if(app().getSharedProjectManager().getSelectedProject() != null) {
            openRightDrawer();
            mRightPane.showTerm(app().getSharedProjectManager().getSelectedProject().getTerm(term));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Triggered by the translation manager after the security keys have been successfully submitted to the server
     * @param event
     */
    @Subscribe
    public void securityKeysSubmitted(SecurityKeysSubmittedEvent event) {
        if(app().isNetworkAvailable()) {
            app().getSharedTranslationManager().syncSelectedProject();
        } else {
            app().showToastMessage(R.string.internet_not_available);
        }
    }

    /**
     * Triggered any time a frame is cleaned (deleted)
     * @param event
     */
    @Subscribe
    public void frameTranslationStatusChanged(FrameTranslationStatusChangedEvent event) {
        mLeftPane.reloadFramesTab();
    }

    /**
     * Triggered any time a chapter is cleaned (deleted)
     * @param event
     */
    @Subscribe
    public void chapterTranslationStatusChanged(ChapterTranslationStatusChangedEvent event) {
        mLeftPane.reloadChaptersTab();
        mLeftPane.reloadProjectsTab();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Boolean projectEnabled = app().getSharedProjectManager().getSelectedProject() != null;
        menu.findItem(R.id.action_chapter_settings).setVisible(projectEnabled);
        menu.findItem(R.id.action_project_settings).setVisible(projectEnabled);
        menu.findItem(R.id.action_share).setVisible(projectEnabled);
        menu.findItem(R.id.action_sync).setVisible(projectEnabled);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_share:
                if(app().getSharedProjectManager().getSelectedProject() != null) {
                    openSharing();
                } else {
                    app().showToastMessage(R.string.choose_a_project);
                }
                return true;
            case R.id.action_sync:
                if(app().getSharedProjectManager().getSelectedProject() != null) {
                    openSyncing();
                } else {
                    app().showToastMessage(R.string.choose_a_project);
                }
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
                if(app().getSharedProjectManager().getSelectedProject() != null) {
                    openRightDrawer();
                } else {
                    app().showToastMessage(R.string.choose_a_project);
                }
                return true;
            case R.id.action_chapter_settings:
                if(app().getSharedProjectManager().getSelectedProject() != null && app().getSharedProjectManager().getSelectedProject().getSelectedChapter() != null) {
                    showChapterSettingsMenu();
                } else if(app().getSharedProjectManager().getSelectedProject() == null) {
                    app().showToastMessage(R.string.choose_a_project);
                } else {
                    app().showToastMessage(R.string.choose_a_chapter);
                }
                return true;
            case R.id.action_project_settings:
                if(app().getSharedProjectManager().getSelectedProject() != null) {
                    showProjectSettingsMenu();
                } else {
                    app().showToastMessage(R.string.choose_a_project);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * A task to highlight key terms in the source text
     */
    private class TermsHighlighterTask extends AsyncTask<Frame, String, String> {
        private OnHighlightProgress mCallback;
        private List<Term> mTerms = new ArrayList<Term>();

        public TermsHighlighterTask(List<Term> terms, OnHighlightProgress callback) {
            mCallback = callback;
            mTerms = terms;
        }

        @Override
        protected String doInBackground(Frame... params) {
            String keyedText = params[0].getText();
            Vector<Boolean> indicies = new Vector<Boolean>();
            indicies.setSize(keyedText.length());
            for(Term t:mTerms) {
                StringBuffer buf = new StringBuffer();
                Pattern p = Pattern.compile("\\b" + t.getName() + "\\b");
                // TRICKY: we need to run two matches at the same time in order to keep track of used indicies in the string
                Matcher matcherSourceText = p.matcher(params[0].getText());
                Matcher matcherKeyedText = p.matcher(keyedText);

                while (matcherSourceText.find() && matcherKeyedText.find()) {
                    // ensure the key term was found in an area of the string that does not overlap another key term.
                    if(indicies.get(matcherSourceText.start()) == null && indicies.get(matcherSourceText.end()) == null) {
                        // build important terms list.
                        params[0].addImportantTerm(matcherSourceText.group());
                        // build the link
                        String key = "<a>" + matcherSourceText.group() + "</a>";
                        // lock indicies to prevent key term collisions
                        for(int i = matcherSourceText.start(); i <= matcherSourceText.end(); i ++) {
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
            return keyedText;
        }

        protected void onPostExecute(String result) {
            mCallback.onSuccess(result);
        }
    }

    /**
     * An interface tfor the terms highlight task
     */
    private interface OnHighlightProgress {
//        void onProgress(String result);
        void onSuccess(String result);
    }

    /**
     * A task to highlight passage notes in the translation text
     */
    private class PassageNotesHighlighterTask extends AsyncTask<String, String, CharSequence> {
        private Boolean mRequestEmptyDefinitions;
        private Handler.Callback mCallback;

        public PassageNotesHighlighterTask(Boolean requestEmptyDefinitions, Handler.Callback callback) {
            mRequestEmptyDefinitions = requestEmptyDefinitions;
            mCallback = callback;
        }

        @Override
        protected CharSequence doInBackground(String... params) {
            NoteSpan.reset();
            TextView notedResult = new TextView(me);
            NoteSpan needsUpdate = null;
            Pattern p = Pattern.compile(NoteSpan.REGEX_NOTE, Pattern.DOTALL);
            Matcher matcher = p.matcher(params[0]);
            int lastEnd = 0;
            while(matcher.find()) {
                if(matcher.start() > lastEnd) {
                    // add the last piece
                    notedResult.append(params[0].substring(lastEnd, matcher.start()));
                }
                lastEnd = matcher.end();

                NoteSpan note = NoteSpan.getInstanceFromXML(matcher.group());
                note.setOnClickListener(new FancySpan.OnClickListener() {
                    @Override
                    public void onClick(View view, FancySpan span) {
                        openPassageNoteDialog((NoteSpan)span);
                    }
                });
                if(note.getNoteText().isEmpty()) {
                    needsUpdate = note;
                }
                notedResult.append(note.toCharSequence());
            }
            if(lastEnd < params[0].length()) {
                notedResult.append(params[0].substring(lastEnd, params[0].length()));
            }

            // display a dialog to populate the empty note.
            if(needsUpdate != null && mRequestEmptyDefinitions) {
                openPassageNoteDialog(needsUpdate);
            }
            return notedResult.getText();
        }

        protected void onPostExecute(CharSequence result) {
            mTranslationEditText.setText(result);
            mTranslationEditText.setSelection(0);
            mCallback.handleMessage(null);
        }
    }

    /**
     * A task update passages notes
     */
    private class PassageNotesUpdaterTask extends AsyncTask<String, String, Void> {
        private Boolean mUpdate = false;
        private NoteSpan.NoteType mNoteType = NoteSpan.NoteType.UserNote;

        /**
         * Specifies if the passage note should be updated or removed
         * @param update
         */
        public PassageNotesUpdaterTask(Boolean update, NoteSpan.NoteType noteType) {
            mUpdate = update;
            mNoteType = noteType;
        }

        @Override
        protected Void doInBackground(String... params) {
            String text = params[0];
            String spanId = params[1];
            String spanPassage = params[2];
            String spanPassageDefinition = params[3];

            TextView updatedResult = new TextView(me);

            Pattern p = Pattern.compile(NoteSpan.regexNoteById(spanId), Pattern.DOTALL);
            Matcher matcher = p.matcher(text);
            if(matcher.find()) {
                updatedResult.append(text.substring(0, matcher.start()));
                if(mUpdate) {
                    // update the note
                    updatedResult.append(NoteSpan.generateTag(spanPassage, spanPassageDefinition, mNoteType));
                } else {
                    // remove the note
                    NoteSpan note = NoteSpan.getInstanceFromXML(matcher.group());
                    updatedResult.append(note.getSpanText());
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
     * @param note
     */
    public void openPassageNoteDialog(NoteSpan note) {

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        app().closeToastMessage();

        // Create and show the dialog
        NoteDialog newFragment = new NoteDialog();
        Bundle args = new Bundle();
        args.putString("passage", note.getSpanText());
        args.putString("note", note.getNoteText());
        args.putInt("noteType", note.getNoteType().ordinal());
        args.putString("id", note.getSpanId());
        newFragment.setArguments(args);
        newFragment.show(ft, "dialog");
    }

    /**
     * This is called when the passage note dialog is closed.
     * @param event
     */
    @Subscribe
    public void passageNote(PassageNoteEvent event) {
        // close the dialog.
        event.getDialog().dismiss();
        PassageNotesUpdaterTask task;
        switch(event.getStatus()) {
            case OK:
                // update the passage note
                if(!event.getNote().isEmpty()) {
                    task = new PassageNotesUpdaterTask(true, event.getNoteType());

                } else {
                    // delete empty notes
                    task = new PassageNotesUpdaterTask(false, event.getNoteType());
                }
                task.execute(mTranslationEditText.getText().toString(),
                        event.getSpanId(),
                        event.getPassage(),
                        event.getNote());
                break;
            case DELETE:
                // remove the passage note
                task = new PassageNotesUpdaterTask(false, event.getNoteType());
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
