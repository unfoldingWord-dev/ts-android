package com.door43.translationstudio;

import com.door43.translationstudio.dialogs.AdvancedSettingsDialog;
import com.door43.translationstudio.dialogs.InfoDialog;
import com.door43.translationstudio.dialogs.LanguageResourceDialog;
import com.door43.translationstudio.dialogs.MetaProjectDialog;
import com.door43.translationstudio.dialogs.NoteDialog;
import com.door43.translationstudio.events.ChapterTranslationStatusChangedEvent;
import com.door43.translationstudio.events.FrameTranslationStatusChangedEvent;
import com.door43.translationstudio.events.LanguageResourceSelectedEvent;
import com.door43.translationstudio.events.SecurityKeysSubmittedEvent;
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
import com.door43.translationstudio.util.AnimationUtilities;
import com.door43.translationstudio.util.MainContext;
import com.door43.translationstudio.util.PassageNoteEvent;
import com.door43.translationstudio.util.ThreadableUI;
import com.door43.translationstudio.util.TranslatorBaseActivity;
import com.squareup.otto.Subscribe;


import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
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
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
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
import android.widget.ProgressBar;
import android.widget.TextView;

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
    private ThreadableUI mHighlightTranslationThread;
    private ThreadableUI mHighlightSourceThread;

    // center view fields for caching
    private TextView mSourceText;
    private TextView mSourceTitleText;
    private TextView mSourceFrameNumText;
    private TextView mTranslationTitleText;
    private ImageView mNextFrameView;
    private ImageView mPreviousFrameView;
    private EditText mTranslationEditText;
    private int mSourceTextMotionDownX = 0;
    private int mSourceTextMotionDownY = 0;
    private static final int TEXT_FADE_SPEED = 100;
    private int mTranslationTextMotionDownX = 0;
    private int mTranslationTextMotionDownY = 0;
    private ProgressBar mTranslationProgressBar;
    private ProgressBar mSourceProgressBar;
    private Timer mAutosaveTimer;
    private boolean mAutosaveEnabled;
    private boolean mProcessingTranslation;
    private Frame mSelectedFrame;
//    private ImageView mChangeResourceBtnIcon;

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
            reloadCenterPane();
        }
        closeTranslationKeyboard();
    }

    /**
     * Enables autosave
     */
    private void enableAutosave() {
        mAutosaveEnabled = true;
    }

    /**
     * Disables autosave and cancels any pending timers
     */
    private void disableAutosave() {
        mAutosaveEnabled = false;
        if(mAutosaveTimer != null) {
            mAutosaveTimer.cancel();
            mAutosaveTimer = null;
        }
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
//        mChangeResourceBtnIcon = (ImageView)mCenterPane.findViewById(R.id.changeResourceBtn);
        mSourceFrameNumText = (TextView)mCenterPane.findViewById(R.id.sourceFrameNumText);
        mTranslationTitleText = (TextView)mCenterPane.findViewById(R.id.translationTitleText);
        mNextFrameView = (ImageView)mCenterPane.findViewById(R.id.hasNextFrameImageView);
        mPreviousFrameView = (ImageView)mCenterPane.findViewById(R.id.hasPreviousFrameImageView);
        mTranslationEditText = (EditText)mCenterPane.findViewById(R.id.inputText);
        mTranslationProgressBar = (ProgressBar)mCenterPane.findViewById(R.id.translationProgressBar);
        mSourceProgressBar = (ProgressBar)mCenterPane.findViewById(R.id.sourceProgressBar);


        mSourceTitleText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Project p = app().getSharedProjectManager().getSelectedProject();
                if(p != null && p.getSelectedSourceLanguage().getResources().length > 1) {
                    // Create and show the dialog.
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    Fragment prev = getFragmentManager().findFragmentByTag("dialog");
                    if (prev != null) {
                        ft.remove(prev);
                    }
                    ft.addToBackStack(null);

                    app().closeToastMessage();

                    LanguageResourceDialog newFragment = new LanguageResourceDialog();
                    Bundle args = new Bundle();
                    args.putString("projectId", p.getId());
                    newFragment.setArguments(args);
                    newFragment.show(ft, "dialog");
                }
            }
        });

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
                // remove actionbar title
                actionMode.setTitle(null);

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
                            highlightTranslationSpans(taggedText, true);
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

        // TODO: do we need this?
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
                        ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class);

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
        mTranslationEditText.setOnTouchListener(new View.OnTouchListener() {
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
                        mTranslationTextMotionDownX = x;
                        mTranslationTextMotionDownY = y;
                    } else if (action == MotionEvent.ACTION_UP) {
                        // don't click spans when dragging. we give a little wiggle room just in case
                        int maxSpanClickWiggle = 5;
                        if(Math.abs(mTranslationTextMotionDownX - x) > maxSpanClickWiggle || Math.abs(mTranslationTextMotionDownY - y) > maxSpanClickWiggle) {
                            return mTranslationGestureDetector.onTouchEvent(motionEvent);
                        }

                        x -= widget.getTotalPaddingLeft();
                        y -= widget.getTotalPaddingTop();

                        x += widget.getScrollX();
                        y += widget.getScrollY();

                        Layout layout = widget.getLayout();
                        int line = layout.getLineForVertical(y);
                        int off = layout.getOffsetForHorizontal(line, x);
                        ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class);

                        if (link.length != 0) {
                            if (action == MotionEvent.ACTION_UP) {
                                link[0].onClick(widget);
                            }
                            return mTranslationGestureDetector.onTouchEvent(motionEvent);
                        }
                    }

                }

                return mTranslationGestureDetector.onTouchEvent(motionEvent);
            }
        });

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
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                int saveDelay = Integer.parseInt(app().getUserPreferences().getString(SettingsActivity.KEY_PREF_AUTOSAVE, getResources().getString(R.string.pref_default_autosave)));
                if(mAutosaveTimer != null) {
                    mAutosaveTimer.cancel();
                }
                if (saveDelay != -1) {
                    mAutosaveTimer = new Timer();
                    if (mAutosaveEnabled) {
                        mAutosaveTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                // save the changes
                                MainActivity.this.save();
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
     * Parses the source text for key terms and generates clickable links.
     * @param frame The frame that contains the source text to parse
     */
    private void highlightSourceSpans(final Frame frame) {
        if(mHighlightSourceThread != null) {
            mHighlightSourceThread.stop();
        }
        // this thread handles the fading animations
        mHighlightSourceThread = new ThreadableUI(MainActivity.this) {
            private ThreadableUI mTaskThread;

            @Override
            public void onStop() {
                // kill children if this thread is stopped
                mHighlightSourceThread = null;
                if(mTaskThread != null) {
                    mTaskThread.stop();
                }
            }

            @Override
            public void run() {
                // progress bar animations
                final Animation inProgress = new AlphaAnimation(0.0f, 1.0f);
                inProgress.setDuration(TEXT_FADE_SPEED);
                inProgress.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        mSourceProgressBar.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                final Animation outProgress = new AlphaAnimation(1.0f, 0.0f);
                outProgress.setDuration(TEXT_FADE_SPEED);
                outProgress.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mSourceProgressBar.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });

                // source text animations
                final Animation in = new AlphaAnimation(0.0f, 1.0f);
                in.setDuration(TEXT_FADE_SPEED);
                final Animation out = new AlphaAnimation(1.0f, 0.0f);
                out.setDuration(TEXT_FADE_SPEED);

                // This thread handles the actual parsing
                mTaskThread = new ThreadableUI(MainActivity.this) {
                    private String mKeyedText = frame.getText();
                    private List<Term> mTerms = frame.getChapter().getProject().getTerms();

                    @Override
                    public void onStop() {
                        mTaskThread = null;
                        // the thread was killed
                        mSourceText.clearAnimation();
                        mSourceText.startAnimation(in);
                        mSourceProgressBar.clearAnimation();
                        mSourceProgressBar.startAnimation(outProgress);
                        // scroll to top
                        mSourceText.scrollTo(0, 0);
                        mRightPane.reloadTerm();
                    }

                    @Override
                    public void run() {
//                        String keyedText = params[0].getText();
                        Vector<Boolean> indicies = new Vector<Boolean>();
                        indicies.setSize(mKeyedText.length());
                        for(Term t:mTerms) {
                            if(isInterrupted()) return;
                            StringBuffer buf = new StringBuffer();
                            Pattern p = Pattern.compile("\\b" + t.getName() + "\\b");
                            // TRICKY: we need to run two matches at the same time in order to keep track of used indicies in the string
                            Matcher matcherSourceText = p.matcher(frame.getText());
                            Matcher matcherKeyedText = p.matcher(mKeyedText);

                            while (matcherSourceText.find() && matcherKeyedText.find() && !isInterrupted()) {
                                // ensure the key term was found in an area of the string that does not overlap another key term.
                                if(indicies.get(matcherSourceText.start()) == null && indicies.get(matcherSourceText.end()) == null) {
                                    // build important terms list.
                                    frame.addImportantTerm(matcherSourceText.group());
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
                            mKeyedText = buf.toString();
                        }
                    }

                    @Override
                    public void onPostExecute() {
                        if(isInterrupted()) return;
                        if(mKeyedText != null && app().getUserPreferences().getBoolean(SettingsActivity.KEY_PREF_HIGHLIGHT_KEY_TERMS, Boolean.parseBoolean(getResources().getString(R.string.pref_default_highlight_key_terms)))) {
//                            final String textResult = mKeyedText;
                            // load the highlighted text
                            String[] pieces = mKeyedText.split("<a>");
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
                        onStop();
                    }
                };
                in.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        mSourceText.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                // begin task after animation
                out.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        if(!isInterrupted()) {
                            mSourceText.setVisibility(View.INVISIBLE);
                            mTaskThread.start();
                        } else {
                            mSourceText.setAnimation(in);
                            mSourceProgressBar.clearAnimation();
                            mSourceProgressBar.setAnimation(outProgress);
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });

                if(!isInterrupted()) {
                    // begin animations
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // clear old animations in case they haven't finished yet
                            mSourceText.clearAnimation();
                            mSourceProgressBar.clearAnimation();

                            mSourceText.startAnimation(out);
                            mSourceProgressBar.startAnimation(inProgress);
                        }
                    });
                }
            }

            @Override
            public void onPostExecute() {
                mHighlightSourceThread = null;
            }
        };
        mHighlightSourceThread.start();
    }

    /**
     * Begins or restarts parsing the note tags.
     * @param text
     */
    private void highlightTranslationSpans(String text) {
        highlightTranslationSpans(text, false);
    }

    /**
     * Begins or restarts parsing the note tags
     * @param text
     */
    private void highlightTranslationSpans(final String text, final Boolean isNewNote) {
        // disable the view so we don't save it
        mProcessingTranslation = true;
        if(mHighlightTranslationThread != null) {
            mHighlightTranslationThread.stop();
        }
        mHighlightTranslationThread = new ThreadableUI(MainActivity.this) {
            private ThreadableUI mTaskThread;

            @Override
            public void onStop() {
                mHighlightTranslationThread = null;
                // kill children if this thread is stopped
                if(mTaskThread != null) {
                    mTaskThread.stop();
                }
            }

            @Override
            public void run() {
                // progress bar animations
                final Animation inProgress = new AlphaAnimation(0.0f, 1.0f);
                inProgress.setDuration(TEXT_FADE_SPEED);
                inProgress.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        mTranslationProgressBar.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                final Animation outProgress = new AlphaAnimation(1.0f, 0.0f);
                outProgress.setDuration(TEXT_FADE_SPEED);
                outProgress.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mTranslationProgressBar.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });

                // translation animations
                final Animation in = new AlphaAnimation(0.0f, 1.0f);
                in.setDuration(TEXT_FADE_SPEED);
                final Animation out = new AlphaAnimation(1.0f, 0.0f);
                out.setDuration(TEXT_FADE_SPEED);

                mTaskThread = new ThreadableUI(MainActivity.this) {
                    private TextView mNotedResult;

                    @Override
                    public void onStop() {
                        mTaskThread = null;
                        mTranslationEditText.clearAnimation();
                        mTranslationEditText.startAnimation(in);
                        mTranslationProgressBar.clearAnimation();
                        mTranslationProgressBar.startAnimation(outProgress);
                    }

                    @Override
                    public void run() {
                        NoteSpan.reset();
                        mNotedResult = new TextView(MainActivity.this);
                        NoteSpan needsUpdate = null;
                        Pattern p = Pattern.compile(NoteSpan.REGEX_NOTE, Pattern.DOTALL);
                        Matcher matcher = p.matcher(text);
                        int lastEnd = 0;
                        while(matcher.find() && !Thread.currentThread().isInterrupted()) {
                            if(matcher.start() > lastEnd) {
                                // add the last piece
                                mNotedResult.append(text.substring(lastEnd, matcher.start()));
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
                            mNotedResult.append(note.toCharSequence());
                        }
                        if(Thread.currentThread().isInterrupted())  return;
                        if(lastEnd < text.length()) {
                            // add the last bit of text to the view
                            String remainingText = text.substring(lastEnd, text.length());
                            mNotedResult.append(remainingText);
                        } else if(text.length() > 0) {
                            // TRICKY: adding a line break at the end makes is easier to type after the spannable
                            mNotedResult.append("\n");
                        }

                        // display a dialog to populate the empty note.
                        if(needsUpdate != null && isNewNote) {
                            openPassageNoteDialog(needsUpdate);
                        }
                    }

                    @Override
                    public void onPostExecute() {
                        if(Thread.currentThread().isInterrupted()) return;
                        if(mNotedResult.getText() != null) {
                            disableAutosave();
                            mTranslationEditText.setText(mNotedResult.getText());
                            mTranslationEditText.setSelection(0);
                            enableAutosave();
                        }
                        onStop();
                        // re-enable the view so we can save it
                        mProcessingTranslation = false;
                    }
                };

                in.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        mTranslationEditText.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                // execute stask after animation
                out.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        if(!isInterrupted()) {
                            mTranslationEditText.setText("");
                            mTranslationEditText.setVisibility(View.INVISIBLE);
                            mTaskThread.start();
                        } else {
                            mTranslationEditText.setAnimation(in);
                            mTranslationProgressBar.clearAnimation();
                            mTranslationProgressBar.setAnimation(outProgress);
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });

                if(!isInterrupted()) {
                    // begin animations
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // clear animations in case they haven't finished yet
                            mTranslationEditText.clearAnimation();
                            mTranslationProgressBar.clearAnimation();

                            mTranslationEditText.startAnimation(out);
                            mTranslationProgressBar.startAnimation(inProgress);
                        }
                    });
                }
            }

            @Override
            public void onPostExecute() {
                mHighlightTranslationThread = null;
            }
        };
        mHighlightTranslationThread.start();
    }

    public void closeTranslationKeyboard() {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    /**
     * This handles a horizontal fling that a user may make to move between frames
     * @param event1
     * @param event2
     * @param velocityX
     * @param velocityY
     * @return
     */
    private boolean handleFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
        // positive x distance moves right
        final double maxFlingAngle = 30; // this restricts the angle at which you must swipe in order to be considered a fling.
        final float minFlingDistance = 50; // the minimum distance you must swipe
        final float minFlingVelocity = 20; // the minimum speed of the swipe
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
            reloadCenterPane();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Updates the center pane with the selected source frame text and any existing translations
     */
    public void reloadCenterPane() {
        // auto save is disabled to prevent accidently saving into the wrong frame
        disableAutosave();
        invalidateOptionsMenu();
        // load the text
        final Project p = app().getSharedProjectManager().getSelectedProject();
        if(frameIsSelected()) {
            mSelectedFrame = p.getSelectedChapter().getSelectedFrame();
            mTranslationEditText.setEnabled(true);
            final int frameIndex = p.getSelectedChapter().getFrameIndex(mSelectedFrame);
            final Chapter chapter = p.getSelectedChapter();

            // get the target language
            if(!p.hasChosenTargetLanguage()) {
                showProjectSettingsMenu();
            }

            // target translation
            final Translation translation = mSelectedFrame.getTranslation();

            highlightTranslationSpans(translation.getText());

            if(chapter.getTitleTranslation().getText().isEmpty()) {
                // display non-translated title
                AnimationUtilities.fadeOutIn(mTranslationTitleText, new Handler.Callback() {
                    @Override
                    public boolean handleMessage(Message message) {
                        mTranslationTitleText.setText(translation.getLanguage().getName() + ": [" + chapter.getTitle() + "]");
                        return false;
                    }
                });
            } else {
                // display translated title
                AnimationUtilities.fadeOutIn(mTranslationTitleText, new Handler.Callback() {
                    @Override
                    public boolean handleMessage(Message message) {
                        mTranslationTitleText.setText(translation.getLanguage().getName() + ": " + chapter.getTitleTranslation().getText());
                        return false;
                    }
                });
            }

            // source translation
            AnimationUtilities.fadeOutIn(mSourceTitleText, new Handler.Callback() {
                @Override
                public boolean handleMessage(Message message) {
                    if(p.getSelectedSourceLanguage().getResources().length > 1) {
                        String languageName = p.getSelectedSourceLanguage().getName() + " - ";
                        String resourceName = p.getSelectedSourceLanguage().getSelectedResource().getName();
                        String chapterName = ": " + chapter.getTitle();
                        SpannableStringBuilder span = new SpannableStringBuilder(languageName + resourceName + chapterName);
                        span.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.blue)), languageName.length(), languageName.length() + resourceName.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        mSourceTitleText.setText(span);
                    } else {
                        mSourceTitleText.setText(p.getSelectedSourceLanguage().getName() + ": " + chapter.getTitle());
                    }
                    return false;
                }
            });
            AnimationUtilities.fadeOutIn(mSourceFrameNumText, new Handler.Callback() {
                @Override
                public boolean handleMessage(Message message) {
                    mSourceFrameNumText.setText(String.format(getResources().getString(R.string.currently_viewed_frame_index), (frameIndex + 1), chapter.numFrames()));
                    return false;
                }
            });

            highlightSourceSpans(mSelectedFrame);

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
//            app().setActiveChapter(p.getSelectedChapter().getId());
//            app().setActiveFrame(mSelectedFrame.getId());
        } else {
            // stop all text processing and clear everything out.
            if(mHighlightSourceThread != null) {
                mHighlightSourceThread.stop();
            }
            if(mHighlightTranslationThread != null) {
                mHighlightTranslationThread.stop();
            }
            mSelectedFrame = null;
            mTranslationEditText.setText("");
            mTranslationEditText.setEnabled(false);
            mTranslationTitleText.setText("");
            mSourceTitleText.setText("");
            mSourceText.setText("");
            mSourceFrameNumText.setText("");
            mNextFrameView.setVisibility(View.INVISIBLE);
            mPreviousFrameView.setVisibility(View.INVISIBLE);

            // nothing was selected so open the project selector
            openLeftDrawer();
        }
        enableAutosave();
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
        if (mAutosaveEnabled && !mProcessingTranslation && frameIsSelected() && app().getSharedProjectManager().getSelectedProject().hasChosenTargetLanguage()) {
            disableAutosave();
            String inputTextValue = ((EditText) findViewById(R.id.inputText)).getText().toString();
            Frame f = app().getSharedProjectManager().getSelectedProject().getSelectedChapter().getSelectedFrame();
            f.setTranslation(inputTextValue);
            f.save();
            enableAutosave();
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

    @Subscribe
    public void languageResourceSelectionChanged(LanguageResourceSelectedEvent event) {
        final Project p = app().getSharedProjectManager().getSelectedProject();
        p.getSelectedSourceLanguage().setSelectedResource(event.getResource().getId());
        new ThreadableUI(this) {

            @Override
            public void onStop() {

            }

            @Override
            public void run() {
                // reload the source so we get the correct language resource
                app().getSharedProjectManager().fetchProjectSource(p);
            }

            @Override
            public void onPostExecute() {
                // refresh the ui
                reloadCenterPane();
            }
        }.start();
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
        menu.findItem(R.id.action_resources).setVisible(projectEnabled);
        Boolean advancedSettingsEnabled = app().getUserPreferences().getBoolean(SettingsActivity.KEY_PREF_ADVANCED_SETTINGS, Boolean.parseBoolean(getResources().getString(R.string.pref_default_advanced_settings)));
        menu.findItem(R.id.action_info).setVisible(advancedSettingsEnabled);
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
            case R.id.action_update:
                // TODO: we need to display a better ui and progress indicator. Probably a dialog that users can cancel to stop the download.
                final Thread t = new Thread() {
                    @Override
                    public void run() {
                        app().showProgressDialog(R.string.downloading_updates);

                        // check for updates to current projects
                        int numProjects = app().getSharedProjectManager().numProjects();
                        for (int i = 0; i < numProjects; i ++) {
                            app().getSharedProjectManager().downloadProjectUpdates(app().getSharedProjectManager().getProject(i));
                        }

                        // check for new projects to download
                        app().getSharedProjectManager().downloadNewProjects();

                        app().closeProgressDialog();
                        app().showToastMessage(R.string.project_updates_downloaded);

                        // reload the center pane
                        Handler mainHandler = new Handler(getMainLooper());
                        Runnable myRunnable = new Runnable() {
                            @Override
                            public void run() {
                                reloadCenterPane();
                            }
                        };
                        mainHandler.post(myRunnable);

                    }
                };

                new AlertDialog.Builder(this)
                        .setMessage(R.string.update_confirmation)
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                t.start();
                            }
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
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
                highlightTranslationSpans(updatedResult.getText().toString());
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
