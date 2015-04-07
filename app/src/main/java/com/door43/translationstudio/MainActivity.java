package com.door43.translationstudio;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ActionMenuView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.door43.translationstudio.dialogs.LanguageResourceDialog;
import com.door43.translationstudio.dialogs.NoteMarkerDialog;
import com.door43.translationstudio.dialogs.VerseMarkerDialog;
import com.door43.translationstudio.events.ChapterTranslationStatusChangedEvent;
import com.door43.translationstudio.events.FrameTranslationStatusChangedEvent;
import com.door43.translationstudio.events.OpenedChapterEvent;
import com.door43.translationstudio.events.OpenedFrameEvent;
import com.door43.translationstudio.events.OpenedProjectEvent;
import com.door43.translationstudio.events.SecurityKeysSubmittedEvent;
import com.door43.translationstudio.panes.left.LeftPaneFragment;
import com.door43.translationstudio.panes.right.RightPaneFragment;
import com.door43.translationstudio.projects.Chapter;
import com.door43.translationstudio.projects.Frame;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Resource;
import com.door43.translationstudio.projects.Translation;
import com.door43.translationstudio.rendering.DefaultRenderer;
import com.door43.translationstudio.rendering.KeyTermRenderer;
import com.door43.translationstudio.rendering.RenderingGroup;
import com.door43.translationstudio.rendering.SourceTextView;
import com.door43.translationstudio.rendering.USXRenderer;
import com.door43.translationstudio.spannables.NoteSpan;
import com.door43.translationstudio.spannables.Span;
import com.door43.translationstudio.spannables.TermSpan;
import com.door43.translationstudio.spannables.VerseSpan;
import com.door43.translationstudio.uploadwizard.UploadWizardActivity;
import com.door43.translationstudio.util.AnimationUtilities;
import com.door43.util.Logger;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.threads.ThreadableUI;
import com.door43.translationstudio.util.TranslatorBaseActivity;
import com.squareup.otto.Subscribe;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends TranslatorBaseActivity {
    private static final String STATE_SOURCE_SCROLL_Y = "source_text_scroll_y";
    private static final String STATE_SOURCE_SCROLL_X = "source_text_scroll_x";
    private final MainActivity me = this;

    // content panes
    private LeftPaneFragment mLeftPane;
    private RightPaneFragment mRightPane;
    private LinearLayout mCenterPane;
    private DrawerLayout mDrawerLayout;

    private GestureDetector mSourceGestureDetector;
    private GestureDetector mTranslationGestureDetector;
    private boolean mActivityIsInitializing;
    private ThreadableUI mHighlightTranslationThread;
    private ThreadableUI mHighlightSourceThread;

    // center view fields for caching
    private SourceTextView mSourceText;
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
    private BroadcastReceiver mMessageReceiver;
    private RenderingGroup mSourceRendering;
    private Span.OnClickListener mKeyTermClickListener;
    private Span.OnClickListener mSourceFootnoteListener;
    private static Toolbar mMainToolbar;
    private boolean mKeyboardIsOpen;
    private RenderingGroup mTranslationRendering;
    private Span.OnClickListener mVerseClickListener;
    private Span.OnClickListener mNoteClickListener;
    private boolean mTranslationEditTextIsFocused;
    private TextView mHelpText;
    private Dialog mFootnoteDialog;
    private int mPreviousRootViewHeight;
    private int mSourceScrollY = 0;
    private int mSourceScrollX = 0;
    private Button source;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //To view larger sections of Source
        source=(Button) findViewById(R.id.source);
        source.getBackground().setAlpha(64);

        source.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                PopupMenu popup = new PopupMenu(MainActivity.this, v);
                popup.getMenuInflater().inflate(R.menu.popup, popup.getMenu());


                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {

                        if(item.getItemId()==R.id.sourceText){

                            Dialog dialog=new Dialog(MainActivity.this);
                            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                            dialog.setContentView(R.layout.dialogfragment);


                            TextView text = (TextView)dialog.findViewById(R.id.dftext);
                            Frame a=AppContext.projectManager().getSelectedProject().getSelectedChapter().getPreviousFrame();
                            Frame b=AppContext.projectManager().getSelectedProject().getSelectedChapter().getSelectedFrame();
                            Frame c=AppContext.projectManager().getSelectedProject().getSelectedChapter().getNextFrame();

                            text.setText(new USXRenderer().render(a.getText()+"\n"+b.getText()+"\n"+c.getText()));

                            float typefaceSize = AppContext.typefaceSize();
                            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, typefaceSize);

                            dialog.show();

                        }

                       else if(item.getItemId()==R.id.targetText)
                        {
                            Dialog dialog=new Dialog(MainActivity.this);
                            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                            dialog.setContentView(R.layout.dialogfragment);

                            TextView text = (TextView)dialog.findViewById(R.id.dftext);
                            text.setText(mTranslationEditText.getText());

                            float typefaceSize = AppContext.typefaceSize();
                            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, typefaceSize);

                            dialog.show();
                        }
                        return true;
                    }
                });
                popup.show();

            }
        });


        // just in case something breaks while this is disabled we'll enable it again
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

        if(savedInstanceState != null) {
            mSourceScrollY = savedInstanceState.getInt(STATE_SOURCE_SCROLL_Y, 0);
            mSourceScrollX = savedInstanceState.getInt(STATE_SOURCE_SCROLL_X, 0);
        }

        // set up toolbars
        mMainToolbar = (Toolbar)findViewById(R.id.toolbar_main);
        mMainToolbar.setVisibility(View.VISIBLE);
        mMainToolbar.setTitle("");
        setSupportActionBar(mMainToolbar);
        getSupportActionBar().setHomeButtonEnabled(true);

        // listeners for the rendering engines
        mVerseClickListener = new Span.OnClickListener() {
            @Override
            public void onClick(View view, Span span, int start, int end) {
                updateVerseMarker((VerseSpan)span, start, end);
            }
        };
        mNoteClickListener = new Span.OnClickListener() {
            @Override
            public void onClick(View view, Span span, int start, int end) {
                updateNoteMarker((NoteSpan)span, start, end);
            }
        };
        mKeyTermClickListener = new Span.OnClickListener() {
            @Override
            public void onClick(View view, Span span, int start, int end) {
                showTermDetails(((TermSpan)span).getTermId());
            }
        };
        mSourceFootnoteListener = new Span.OnClickListener() {
            @Override
            public void onClick(View view, Span span, int start, int end) {
                showSourceFootnote((NoteSpan)span);
            }
        };

        mActivityIsInitializing = true;
        app().setMainActivity(this);

        mCenterPane = (LinearLayout)findViewById(R.id.centerPane);
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        mDrawerLayout.setScrimColor(getResources().getColor(R.color.scrim));

        initPanes();

        if(AppContext.projectManager().getSelectedProject() != null && AppContext.projectManager().getSelectedProject().getSelectedChapter() == null) {
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
                if(AppContext.projectManager().getSelectedProject() == null || AppContext.projectManager().getSelectedProject().getSelectedChapter() == null) {
                    openLeftDrawer();
                }
            }
            reloadCenterPane();
        }
        closeKeyboard();
    }


    /**
     * Display a footnote found in the source text
     * @param span
     */
    private void showSourceFootnote(NoteSpan span) {
        if(mFootnoteDialog != null) {
            if(mFootnoteDialog.isShowing()) {
                mFootnoteDialog.dismiss();
            }
            mFootnoteDialog = null;
        }
        mFootnoteDialog = new AlertDialog.Builder(this)
                .setMessage(span.getNotes())
                .show();
    }

    /**
     * Enables autosave
     */
    private void enableAutosave() {
        // cancel pending saves that got trapped after we disabled auto save
        AppContext.translationManager().stageTranslation(null, null);
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
        initFonts();
    }

    /**
     * Returns the left pane fragment
     * @return
     */
    public LeftPaneFragment getLeftPane() {
        return mLeftPane;
    }

    private void onKeyboardChanged(View root) {
        Rect r = new Rect();
        root.getWindowVisibleDisplayFrame(r);
        if(root.getHeight() < mPreviousRootViewHeight) {
            onKeyboardOpen(r);
        } else {
            onKeyboardClose(r);
        }
    }

    /**
     * Triggered when the keyboard opens
     * @param r the dimensions of the visible area
     */
    private void onKeyboardOpen(Rect r) {
        if(!mKeyboardIsOpen) {
            mKeyboardIsOpen = true;
            resizeRootView(r);

            // display the translation menu
            mMainToolbar.setBackgroundColor(getResources().getColor(R.color.light_blue));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            invalidateOptionsMenu();
        }
    }

    /**
     * Triggered when the keyboard closes
     * @param r the dimensions of the visible area
     */
    private void onKeyboardClose(Rect r) {
        if(mKeyboardIsOpen) {
            mKeyboardIsOpen = false;
            mTranslationEditText.clearFocus();
            resizeRootView(r);

            // display the main menu
            mMainToolbar.setBackgroundColor(getResources().getColor(R.color.green));
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            invalidateOptionsMenu();
        }
    }

    /**
     * Redraws the ui.
     * @param r
     */
    private void resizeRootView(Rect r) {
        ViewGroup.LayoutParams params = mDrawerLayout.getLayoutParams();
        int newHeight = r.bottom - getStatusBarHeight();
        if(newHeight != params.height) {
            params.height = newHeight;
            mDrawerLayout.setLayoutParams(params);
        }
    }

    /**
     * Initializes the fonts
     */
    private void initFonts() {
        if(mSourceText != null && mTranslationEditText != null) {
            // set up graphite fontface
            if (AppContext.projectManager().getSelectedProject() != null) {
                Project p = AppContext.projectManager().getSelectedProject();
                mTranslationEditText.setTypeface(AppContext.graphiteTypeface(p.getSelectedTargetLanguage()), 0);
                mSourceText.setTypeface(AppContext.graphiteTypeface(p.getSelectedSourceLanguage()), 0);
            }

            // set custom font size (sp)
            float typefaceSize = AppContext.typefaceSize();
            mTranslationEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, typefaceSize);
            mSourceText.setTextSize(TypedValue.COMPLEX_UNIT_SP, typefaceSize);
        }
    }

    /**
     * set up the content panes
     */
    private void initPanes() {
        mSourceText = (SourceTextView)mCenterPane.findViewById(R.id.sourceText);
        mSourceTitleText = (TextView)mCenterPane.findViewById(R.id.sourceTitleText);
        mSourceFrameNumText = (TextView)mCenterPane.findViewById(R.id.sourceFrameNumText);
        mTranslationTitleText = (TextView)mCenterPane.findViewById(R.id.translationTitleText);
        mNextFrameView = (ImageView)mCenterPane.findViewById(R.id.hasNextFrameImageView);
        mPreviousFrameView = (ImageView)mCenterPane.findViewById(R.id.hasPreviousFrameImageView);
        mTranslationEditText = (EditText)mCenterPane.findViewById(R.id.inputText);
        mTranslationProgressBar = (ProgressBar)mCenterPane.findViewById(R.id.translationProgressBar);
        mSourceProgressBar = (ProgressBar)mCenterPane.findViewById(R.id.sourceProgressBar);
        mHelpText = (TextView)findViewById(R.id.helpTextView);

        // set up resource switching
        mSourceTitleText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final Project p = AppContext.projectManager().getSelectedProject();
                if(p != null && p.getSelectedSourceLanguage().getResources().length > 1) {
                    // Create and show the dialog.
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    Fragment prev = getFragmentManager().findFragmentByTag("dialog");
                    if (prev != null) {
                        ft.remove(prev);
                    }
                    ft.addToBackStack(null);

                    app().closeToastMessage();

                    save();

                    LanguageResourceDialog newFragment = new LanguageResourceDialog();
                    Bundle args = new Bundle();
                    args.putString("projectId", p.getId());
                    newFragment.setArguments(args);
                    newFragment.setOnChooseListener(new LanguageResourceDialog.OnChooseListener() {
                        @Override
                        public void onChoose(Resource resource) {
                            if(p == null) return;
                            final ProgressDialog dialog = new ProgressDialog(MainActivity.this);
                            dialog.setMessage(getResources().getString(R.string.loading_project_chapters));
                            dialog.setCancelable(false);
                            dialog.setCanceledOnTouchOutside(false);
                            dialog.show();
                            p.getSelectedSourceLanguage().setSelectedResource(resource.getId());
                            new ThreadableUI(MainActivity.this) {

                                @Override
                                public void onStop() {

                                }

                                @Override
                                public void run() {
                                    // disable screen rotation so we don't break things
//                                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
                                    // reload the source so we get the correct language resource
                                    AppContext.projectManager().fetchProjectSource(p);
                                }

                                @Override
                                public void onPostExecute() {
                                    // refresh the ui
                                    dialog.dismiss();
                                    reloadCenterPane();
                                    mLeftPane.reloadFramesTab();
                                    mLeftPane.reloadChaptersTab();

                                    // re-enable screen rotation
//                                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                                }
                            }.start();
                        }
                    });
                    newFragment.show(ft, "dialog");
                }
            }
        });

        mTranslationEditText.setEnabled(false);

        // watch for the soft keyboard open and close
        final View rootView = ((ViewGroup)findViewById(android.R.id.content)).getChildAt(0);
        mPreviousRootViewHeight = 0;
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if(mPreviousRootViewHeight == 0) {
                    mPreviousRootViewHeight = rootView.getHeight();
                }
                onKeyboardChanged(rootView);
            }
        });

        // Register a listener for when the input method changes (e.g. the keyboard)
        IntentFilter filter = new IntentFilter(Intent.ACTION_INPUT_METHOD_CHANGED);
        mMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onKeyboardChanged(rootView);
            }
        };
        // TRICKY: we need to unregister this when the activity is destroyed
        registerReceiver(mMessageReceiver, filter);

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
                        int start = mTranslationEditText.getSelectionStart();
                        int end = mTranslationEditText.getSelectionEnd();
                        insertNoteMarker(start, end);
                        return true;
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
            // make both panes fill half the screen when in landscape mode.
            mRightPane.setLayoutWidth(size.x / 2);
            mLeftPane.setLayoutWidth(size.x / 2);
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
                        // check if this is a swipe
                        int maxSpanClickWiggle = 5;
                        if(Math.abs(mTranslationTextMotionDownX - x) > maxSpanClickWiggle || Math.abs(mTranslationTextMotionDownY - y) > maxSpanClickWiggle) {
                            return mTranslationGestureDetector.onTouchEvent(motionEvent);
                        }

                        // This is a click

                        // pass click to spans
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

        // display help text when sourceText is empty
        mSourceText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if(charSequence.length() > 0) {
//                    mHelpText.setVisibility(View.VISIBLE);
                    mTranslationEditText.setEnabled(true);
                } else {
//                    mHelpText.setVisibility(View.GONE);
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
                if (mAutosaveTimer != null) {
                    mAutosaveTimer.cancel();
                }
                if(mAutosaveEnabled) {
                    AppContext.translationManager().stageTranslation(mSelectedFrame, mTranslationEditText.getText());
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

        // detect when the translation text has focus
        mTranslationEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                invalidateOptionsMenu();
            }
        });

        // get notified when drawers open
        mDrawerLayout.setDrawerListener(new ActionBarDrawerToggle(this, mDrawerLayout, R.string.close, R.string.close) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                mTranslationEditText.setEnabled(true);
                // TODO: perhaps we could save the keyboard state and reopen it here.
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                mTranslationEditText.setEnabled(false);
                closeKeyboard();
            }
        });
    }

    public int getStatusBarHeight() {
        Rect r = new Rect();
        Window w = getWindow();
        w.getDecorView().getWindowVisibleDisplayFrame(r);
        return r.top;
    }

    /**
     * Parses the source text for key terms and generates clickable links.
     */
    private void renderSourceText() {
        if(mHighlightSourceThread != null) {
            mHighlightSourceThread.stop();
        }

        // this thread handles the fading animations
        mHighlightSourceThread = new ThreadableUI(MainActivity.this) {
            ThreadableUI renderThread;
            @Override
            public void onStop() {
                // kill children if this thread is stopped
                mHighlightSourceThread = null;
                if(renderThread != null) {
                    renderThread.stop();
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
                    public void onAnimationEnd(Animation animation) {}
                    public void onAnimationRepeat(Animation animation) {}
                });
                final Animation outProgress = new AlphaAnimation(1.0f, 0.0f);
                outProgress.setDuration(TEXT_FADE_SPEED);
                outProgress.setAnimationListener(new Animation.AnimationListener() {
                    public void onAnimationStart(Animation animation) {}
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mSourceProgressBar.setVisibility(View.INVISIBLE);
                    }
                    public void onAnimationRepeat(Animation animation) {}
                });

                // source text animations
                final Animation in = new AlphaAnimation(0.0f, 1.0f);
                in.setDuration(TEXT_FADE_SPEED);
                final Animation out = new AlphaAnimation(1.0f, 0.0f);
                out.setDuration(TEXT_FADE_SPEED);

                // build rendering engines
                mSourceRendering = new RenderingGroup();
                if(app().getUserPreferences().getBoolean(SettingsActivity.KEY_PREF_HIGHLIGHT_KEY_TERMS, Boolean.parseBoolean(getResources().getString(R.string.pref_default_highlight_key_terms)))) {
                    mSourceRendering.addEngine(new KeyTermRenderer(mSelectedFrame, mKeyTermClickListener));
                }
                if(mSelectedFrame.format == Frame.Format.USX) {
                    mSourceRendering.addEngine(new USXRenderer(null, mSourceFootnoteListener));
                } else {
                    mSourceRendering.addEngine(new DefaultRenderer());
                }
                mSourceRendering.init(mSelectedFrame.getText());

                in.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        mSourceText.setVisibility(View.VISIBLE);
                    }
                    public void onAnimationEnd(Animation animation) {}
                    public void onAnimationRepeat(Animation animation) {}
                });
                // begin task after animation
                out.setAnimationListener(new Animation.AnimationListener() {
                    public void onAnimationStart(Animation animation) {}
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        if(!isInterrupted()) {
                            mSourceText.setVisibility(View.INVISIBLE);
                            renderThread = new ThreadableUI(MainActivity.this) {
                                CharSequence output;
                                @Override
                                public void onStop() {
                                    mSourceRendering.stop();
                                }

                                @Override
                                public void run() {
                                    if(!isInterrupted()) {
                                        output = mSourceRendering.start();
                                    }
                                }

                                @Override
                                public void onPostExecute() {
                                    if(!isInterrupted()) {
                                        mSourceText.setText(output);
                                        mSourceText.clearAnimation();
                                        mSourceText.startAnimation(in);
                                        mSourceProgressBar.clearAnimation();
                                        mSourceProgressBar.startAnimation(outProgress);
                                        // scroll to top (or the last scroll position if screen rotation)
                                        mSourceText.scrollTo(mSourceScrollX, mSourceScrollY);
                                        mSourceScrollY = 0;
                                        mSourceScrollX = 0;
                                        mRightPane.reloadTermsTab();
                                    }
                                }
                            };
                            renderThread.start();
                        } else {
                            mSourceText.setAnimation(in);
                            mSourceProgressBar.clearAnimation();
                            mSourceProgressBar.setAnimation(outProgress);
                        }
                    }
                    public void onAnimationRepeat(Animation animation) {}
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
     * Begins or restarts parsing the note tags
     * @param text
     */
    private void renderTranslationText(final String text) {
        // disable the view so we don't save it
        mProcessingTranslation = true;
        if(mHighlightTranslationThread != null) {
            mHighlightTranslationThread.stop();
        }
        mHighlightTranslationThread = new ThreadableUI(MainActivity.this) {
            ThreadableUI renderThread;
            @Override
            public void onStop() {
                // kill children if this thread is stopped
                mHighlightTranslationThread = null;
                if(renderThread != null) {
                    renderThread.stop();
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
                    public void onAnimationEnd(Animation animation) {}
                    public void onAnimationRepeat(Animation animation) {}
                });
                final Animation outProgress = new AlphaAnimation(1.0f, 0.0f);
                outProgress.setDuration(TEXT_FADE_SPEED);
                outProgress.setAnimationListener(new Animation.AnimationListener() {
                    public void onAnimationStart(Animation animation) {}
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mTranslationProgressBar.setVisibility(View.INVISIBLE);
                    }
                    public void onAnimationRepeat(Animation animation) {}
                });

                // translation text animations
                final Animation in = new AlphaAnimation(0.0f, 1.0f);
                in.setDuration(TEXT_FADE_SPEED);
                final Animation out = new AlphaAnimation(1.0f, 0.0f);
                out.setDuration(TEXT_FADE_SPEED);

                // build rendering engines
                mTranslationRendering = new RenderingGroup();
                if(mSelectedFrame.format == Frame.Format.USX) {
                    mTranslationRendering.addEngine(new USXRenderer(mVerseClickListener, mNoteClickListener));
                } else {
                    mTranslationRendering.addEngine(new DefaultRenderer(mNoteClickListener));
                }
                mTranslationRendering.init(text);

                in.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        mTranslationEditText.setVisibility(View.VISIBLE);
                    }
                    public void onAnimationEnd(Animation animation) {}
                    public void onAnimationRepeat(Animation animation) {}
                });
                // execute task after animation
                out.setAnimationListener(new Animation.AnimationListener() {
                    public void onAnimationStart(Animation animation) {}
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        if(!isInterrupted()) {
//                            mTranslationEditText.setText("");
                            mTranslationEditText.setVisibility(View.INVISIBLE);
                            renderThread = new ThreadableUI(MainActivity.this) {
                                CharSequence output;
                                @Override
                                public void onStop() {
                                    mTranslationRendering.stop();
                                }

                                @Override
                                public void run() {
                                    if(!isInterrupted()) {
                                        output = mTranslationRendering.start();
                                    }
                                }

                                @Override
                                public void onPostExecute() {
                                    if(!isInterrupted()) {
                                        disableAutosave();
                                        mTranslationEditText.setText(output);
                                        enableAutosave();
                                        mTranslationEditText.clearAnimation();
                                        mTranslationEditText.startAnimation(in);
                                        mTranslationProgressBar.clearAnimation();
                                        mTranslationProgressBar.startAnimation(outProgress);
                                        // scroll to top
                                        mTranslationEditText.scrollTo(0, 0);
                                        mTranslationEditText.clearFocus();
                                        mProcessingTranslation = false;
                                    }
                                }
                            };
                            renderThread.start();
                        } else {
                            mTranslationEditText.setAnimation(in);
                            mTranslationProgressBar.clearAnimation();
                            mTranslationProgressBar.setAnimation(outProgress);
                        }
                    }
                    public void onAnimationRepeat(Animation animation) {}
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

    /**
     * closes the keyboard
     */
    public void closeKeyboard() {
        if(getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        } else {
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
    }

    /**
     * opens the keyboard
     */
    public void openKeyboard() {
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

//        if(getCurrentFocus() != null) {
//            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
//        } else {
//            MainActivity.this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
//        }
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

        Project p = AppContext.projectManager().getSelectedProject();
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
        // auto save is disabled to prevent accidentally saving into the wrong frame
        disableAutosave();
        invalidateOptionsMenu();
        // load the text
        final Project p = AppContext.projectManager().getSelectedProject();
        if(frameIsSelected()) {
            mRightPane.reloadNotesTab();

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

            renderTranslationText(translation.getText());

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

            if(mSelectedFrame.getText().length() > 0) {
                mHelpText.setVisibility(View.GONE);
            } else {
                mHelpText.setVisibility(View.VISIBLE);
            }

            // render the source text
            renderSourceText();

            // set correct text direction
            Language.Direction sourceDirection = p.getSelectedSourceLanguage().getDirection();
            if(sourceDirection == Language.Direction.RightToLeft) {
                if(Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    mSourceText.setTextDirection(View.TEXT_DIRECTION_RTL);
                    mTranslationEditText.setTextDirection(View.TEXT_DIRECTION_RTL);
                } else {
                    mSourceText.setGravity(Gravity.RIGHT);
                    mTranslationEditText.setGravity(Gravity.RIGHT);
                }
            } else {
                if(Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    mSourceText.setTextDirection(View.TEXT_DIRECTION_LTR);
                    mTranslationEditText.setTextDirection(View.TEXT_DIRECTION_LTR);
                } else {
                    mSourceText.setGravity(Gravity.LEFT);
                    mTranslationEditText.setGravity(Gravity.LEFT);
                }
            }

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
        Project selectedProject = AppContext.projectManager().getSelectedProject();
        return selectedProject != null && selectedProject.getSelectedChapter() != null && selectedProject.getSelectedChapter().getSelectedFrame() != null;
    }

    /**
     * Closes all the navigation drawers
     */
    public void closeDrawers() {
        mDrawerLayout.closeDrawers();
    }

    public void openLeftDrawer() {
        mDrawerLayout.closeDrawer(Gravity.RIGHT);
        mDrawerLayout.openDrawer(Gravity.LEFT);
    }

    public void openRightDrawer() {
        mDrawerLayout.closeDrawer(Gravity.LEFT);
        mDrawerLayout.openDrawer(Gravity.RIGHT);
    }

    /**
     * Saves the translation
     */
    public void save() {
        if (mAutosaveEnabled && !mProcessingTranslation && frameIsSelected() && AppContext.projectManager().getSelectedProject().hasChosenTargetLanguage()) {
            disableAutosave();
            AppContext.translationManager().commitTranslation();
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
     * Opens the bug reporter
     */
    public void openBugReporter() {
        Intent intent = new Intent(this, BugReporterActivity.class);
        startActivity(intent);
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
        if(AppContext.projectManager().getSelectedProject() != null) {
            mRightPane.showTerm(AppContext.projectManager().getSelectedProject().getTerm(term));
            openRightDrawer();
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    /**
     * Triggered by the translation manager after the security keys have been successfully submitted to the server
     * @param event
     */
    @Subscribe
    public void securityKeysSubmitted(SecurityKeysSubmittedEvent event) {
        if(app().isNetworkAvailable()) {
            AppContext.translationManager().syncSelectedProject();
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

    /**
     * Trigged by the navigator when a project is opened
     * @param event
     */
    @Subscribe
    public void onOpenedProject(OpenedProjectEvent event) {
        mLeftPane.reloadProjectsTab();
    }

    /**
     * Trigged by the navigator when a chapter is opened
     * @param event
     */
    @Subscribe
    public void onOpenedChapter(OpenedChapterEvent event) {
        mLeftPane.reloadChaptersTab();
    }

    /**
     * Trigged by the navigator when a frame is opened
     * @param event
     */
    @Subscribe
    public void onOpenedFrame(OpenedFrameEvent event) {
        mLeftPane.reloadFramesTab();
        reloadCenterPane();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        if(mKeyboardIsOpen) {
            inflater.inflate(R.menu.translation_actions, menu);
        } else {
            inflater.inflate(R.menu.main_activity_actions, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Project p = AppContext.projectManager().getSelectedProject();
        Boolean projectEnabled = p != null;
        if(mKeyboardIsOpen) {
            // translation menu
            boolean showUSXTools = mSelectedFrame != null && mSelectedFrame.format == Frame.Format.USX;
            menu.findItem(R.id.action_verse_marker).setVisible(showUSXTools);
        } else {
            // main menu
            boolean hasChapterSettings = false;
            boolean hasResources = false;
            if(projectEnabled) {
                Chapter c = p.getSelectedChapter();
                hasChapterSettings = c != null && c.hasChapterSettings();
                if(c != null) {
                    Frame f = c.getSelectedFrame();
                    if(f != null) {
                        hasResources = f.getImportantTerms().size() > 0 || f.getTranslationNotes() != null;
                    }
                }
            }

            menu.findItem(R.id.action_chapter_settings).setVisible(projectEnabled && hasChapterSettings);
            menu.findItem(R.id.action_project_settings).setVisible(projectEnabled);
            menu.findItem(R.id.action_sync).setVisible(projectEnabled);
            menu.findItem(R.id.action_resources).setVisible(projectEnabled && hasResources);

            if(!hasResources) {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.END);
            } else {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.END);
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case android.R.id.home:
                closeKeyboard();
                return true;
            case R.id.action_share:
                openSharing();
                return true;
            case R.id.action_sync:
                if(AppContext.projectManager().getSelectedProject() != null) {
                    openSyncing();
                } else {
                    app().showToastMessage(R.string.choose_a_project);
                }
                return true;
            case R.id.action_settings:
                openSettings();
                return true;
            case R.id.action_bug:
                openBugReporter();
                return true;
            case R.id.action_update:
                Intent intent = new Intent(this, GetMoreProjectsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_library:
                openLeftDrawer();
                return true;
            case R.id.action_resources:
                if(AppContext.projectManager().getSelectedProject() != null) {
                    openRightDrawer();
                } else {
                    app().showToastMessage(R.string.choose_a_project);
                }
                return true;
            case R.id.action_chapter_settings:
                if(AppContext.projectManager().getSelectedProject() != null && AppContext.projectManager().getSelectedProject().getSelectedChapter() != null) {
                    showChapterSettingsMenu();
                } else if(AppContext.projectManager().getSelectedProject() == null) {
                    app().showToastMessage(R.string.choose_a_project);
                } else {
                    app().showToastMessage(R.string.choose_a_chapter);
                }
                return true;
            case R.id.action_project_settings:
                if(AppContext.projectManager().getSelectedProject() != null) {
                    showProjectSettingsMenu();
                } else {
                    app().showToastMessage(R.string.choose_a_project);
                }
                return true;
            case R.id.action_verse_marker:
                insertVerseMarker( mTranslationEditText.getSelectionStart());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Inserts a verse marker on the given range in the translation text.
     * If a note already exists within the range no note will be added
     * @param start the start of the range
     * @param end the end of the range
     */
    private void insertNoteMarker(final int start, final int end) {
        CharSequence selection = mTranslationEditText.getText().subSequence(start, end);

        // do not allow notes to collide
        SpannedString[] conflictingSpans = mTranslationEditText.getText().getSpans(start, end, SpannedString.class);
        for(SpannedString s:conflictingSpans) {
            NoteSpan note = NoteSpan.parseNote(s.toString());
            if(note != null) {
                app().showToastMessage(R.string.notes_cannot_overlap);
                Logger.i(this.getClass().getName(), "Overlapping note spans is not allowed");
                return;
            }
            VerseSpan verse = VerseSpan.parseVerse(s.toString());
            if(verse != null) {
                app().showToastMessage("Notes accross multiple verses is not supported");
                Logger.i(this.getClass().getName(), "Notes accross multiple verses is not supported");
                return;
            }
        }

        // get notes from user
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog
        NoteMarkerDialog newFragment = new NoteMarkerDialog();
        Bundle args = new Bundle();
        args.putString("passage", selection.toString());
        newFragment.setArguments(args);
        newFragment.setOkListener(new NoteMarkerDialog.OnClickListener() {
            @Override
            public void onClick(CharSequence passage, CharSequence notes) {
                final NoteSpan note = NoteSpan.generateUserNote(passage, notes);
                note.setOnClickListener(new Span.OnClickListener() {
                    @Override
                    public void onClick(View view, Span span, int start, int end) {
                        updateNoteMarker((NoteSpan) span, start, end);
                    }
                });
                new Handler(getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mTranslationEditText.getText().replace(start, end, note.toCharSequence());
                    }
                });
            }
        });
        newFragment.show(ft, "dialog");
    }

    /**
     * Inserts a verse marker at the given position in the translation text
     * @param position the position within the translation text where the verse will be inserted
     */
    private void insertVerseMarker(final int position) {
        Project p = AppContext.projectManager().getSelectedProject();
        int defaultVerseNumber = 1;
        int endingVerse = 1;
        if(p != null) {
            if(p.getSelectedChapter() != null && p.getSelectedChapter().getSelectedFrame() != null) {
                defaultVerseNumber = p.getSelectedChapter().getSelectedFrame().getStartingVerseNumber();
                endingVerse = p.getSelectedChapter().getSelectedFrame().getEndingVerseNumber();
            }
        }
        final int minVerseNumber = defaultVerseNumber;
        final int maxVerseNumber = endingVerse;
        new ThreadableUI(this) {
            private int mSuggestedVerse = minVerseNumber;

            @Override
            public void onStop() {}

            @Override
            public void run() {
                // find next available verse number for suggestion
                int next = 0;
                VerseSpan previousVerse = null;
                for(int i = 0; i < position; i = next) {
                    if(isInterrupted()) return;
                    next = mTranslationEditText.getText().nextSpanTransition(i, position, SpannedString.class);
                    SpannedString[] verses = mTranslationEditText.getText().getSpans(i, next, SpannedString.class);
                    if(verses.length > 0) {
                        // TRICKY: not all spanned strings are verses so we need to check
                        VerseSpan potentialVerse = VerseSpan.parseVerse(verses[0].toString());
                        if(potentialVerse != null) {
                            previousVerse = potentialVerse;
                        }
                    }
                }
                if(previousVerse != null) {
                    mSuggestedVerse = previousVerse.getStartVerseNumber() + 1;
                }
            }

            @Override
            public void onPostExecute() {
                // get verse number from the user
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                Fragment prev = getFragmentManager().findFragmentByTag("dialog");
                if (prev != null) {
                    ft.remove(prev);
                }
                ft.addToBackStack(null);

                // Create and show the dialog
                VerseMarkerDialog newFragment = new VerseMarkerDialog();
                Bundle args = new Bundle();
                args.putInt("startVerse", mSuggestedVerse);
                args.putInt("maxVerse", maxVerseNumber);
                args.putInt("minVerse", minVerseNumber);
                newFragment.setArguments(args);
                newFragment.setOkListener(new VerseMarkerDialog.OnClickListener() {
                    @Override
                    public void onClick(int verse) {
                        final VerseSpan verseSpan = new VerseSpan(verse);
                        verseSpan.setOnClickListener(new Span.OnClickListener() {
                            @Override
                            public void onClick(View view, Span span, int start, int end) {
                                updateVerseMarker((VerseSpan)span, start, end);
                            }
                        });
                        // insert the verse
                        new Handler(getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                mTranslationEditText.getText().insert(position, verseSpan.toCharSequence());
                                // set selection to after verse marker
                                mTranslationEditText.setSelection(position + verseSpan.toCharSequence().length());
                                mTranslationEditText.requestFocus();
//                                openKeyboard();
                            }
                        });
                    }
                });
                newFragment.show(ft, "dialog");
            }
        }.start();
    }

    /**
     * Displays a dialog where users can edit the note text
     * @param note
     * @param spanStart
     * @param spanEnd
     */
    private void updateNoteMarker(NoteSpan note, final int spanStart, final int spanEnd) {
        // Create and show the dialog
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        NoteMarkerDialog newFragment = new NoteMarkerDialog();
        Bundle args = new Bundle();
        args.putCharSequence("passage", note.getPassage());
        args.putCharSequence("notes", note.getNotes());
        newFragment.setArguments(args);
        newFragment.setOkListener(new NoteMarkerDialog.OnClickListener() {
            @Override
            public void onClick(final CharSequence passage, CharSequence notes) {
                if(!TextUtils.isEmpty(notes)) {
                    // update the verse
                    final NoteSpan noteSpan = NoteSpan.generateUserNote(passage, notes);
                    noteSpan.setOnClickListener(new Span.OnClickListener() {
                        @Override
                        public void onClick(View view, Span span, int start, int end) {
                            updateNoteMarker((NoteSpan) span, start, end);
                        }
                    });
                    new Handler(getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            // replace the verse marker
                            int start = spanStart;
                            int end = spanEnd;
                            if (start == -1 || end == -1) {
                                Logger.e(this.getClass().getName(), "failed to update the note marker. The original span position could not be determined.");
                            } else {
                                CharSequence text = "";
                                // grab first part of text
                                if (start > 0) {
                                    text = mTranslationEditText.getText().subSequence(0, start);
                                }
                                // insert updated verse
                                text = TextUtils.concat(text, noteSpan.toCharSequence());
                                // concat last part of text
                                if (end < mTranslationEditText.getText().length()) {
                                    text = TextUtils.concat(text, mTranslationEditText.getText().subSequence(end, mTranslationEditText.getText().length()));
                                }
                                mTranslationEditText.setText(text);
                            }
                        }
                    });
                } else {
                    // delete the note
                    new Handler(getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            // replace the note marker
                            int start = spanStart;
                            int end = spanEnd;
                            if (start == -1 || end == -1) {
                                Logger.e(this.getClass().getName(), "failed to update the note marker. The original span position could not be determined.");
                            } else {
                                CharSequence text = "";
                                // grab first part of text
                                if (start > 0) {
                                    text = mTranslationEditText.getText().subSequence(0, start);
                                }
                                // add the passage
                                text = TextUtils.concat(text, passage);
                                // concat last part of text
                                if (end < mTranslationEditText.getText().length()) {
                                    text = TextUtils.concat(text, mTranslationEditText.getText().subSequence(end, mTranslationEditText.getText().length()));
                                }
                                mTranslationEditText.setText(text);
                            }
                        }
                    });
                }
            }
        });
        newFragment.show(ft, "dialog");
    }

    /**
     * Displays a dialog where users can edit the verse number
     * @param verse
     */
    private void updateVerseMarker(final VerseSpan verse, final int spanStart, final int spanEnd) {
        if(!frameIsSelected()) return;

        // Create and show the dialog
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        VerseMarkerDialog newFragment = new VerseMarkerDialog();
        Bundle args = new Bundle();
        Frame f =  AppContext.projectManager().getSelectedProject().getSelectedChapter().getSelectedFrame();
        args.putInt("startVerse", verse.getStartVerseNumber());
        args.putInt("maxVerse",f.getEndingVerseNumber());
        args.putInt("minVerse", f.getStartingVerseNumber());
        newFragment.setArguments(args);
        newFragment.setOkListener(new VerseMarkerDialog.OnClickListener() {
            @Override
            public void onClick(int verseNumber) {
                if(verseNumber > 0) {
                    // update the verse
                    final VerseSpan verseSpan = new VerseSpan(verseNumber);
                    verseSpan.setOnClickListener(new Span.OnClickListener() {
                        @Override
                        public void onClick(View view, Span span, int start, int end) {
                            updateVerseMarker((VerseSpan) span, start, end);
                        }
                    });
                    new Handler(getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            // replace the verse marker
                            int start = spanStart;
                            int end = spanEnd;
                            if (start == -1 || end == -1) {
                                Logger.e(this.getClass().getName(), "failed to update the verse marker. The original span position could not be determined.");
                            } else {
                                CharSequence text = "";
                                // grab first part of text
                                if (start > 0) {
                                    text = mTranslationEditText.getText().subSequence(0, start);
                                }
                                // insert updated verse
                                text = TextUtils.concat(text, verseSpan.toCharSequence());
                                // concat last part of text
                                if (end < mTranslationEditText.getText().length()) {
                                    text = TextUtils.concat(text, mTranslationEditText.getText().subSequence(end, mTranslationEditText.getText().length()));
                                }
                                mTranslationEditText.setText(text);
                                // set selection to after verse marker
                                mTranslationEditText.setSelection(start + verseSpan.toCharSequence().length());
                                mTranslationEditText.requestFocus();
//                                openKeyboard();
                            }
                        }
                    });
                } else {
                    // delete the verse
                    new Handler(getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            // replace the verse marker
                            int start = spanStart;
                            int end = spanEnd;
                            if (start == -1 || end == -1) {
                                Logger.e(this.getClass().getName(), "failed to update the verse marker. The original span position could not be determined.");
                            } else {
                                CharSequence text = "";
                                // grab first part of text
                                if (start > 0) {
                                    text = mTranslationEditText.getText().subSequence(0, start);
                                }
                                // concat last part of text
                                if (end < mTranslationEditText.getText().length()) {
                                    text = TextUtils.concat(text, mTranslationEditText.getText().subSequence(end, mTranslationEditText.getText().length()));
                                }
                                mTranslationEditText.setText(text);
                                // set selection to where the verse was
                                mTranslationEditText.setSelection(start);
                                mTranslationEditText.requestFocus();
//                                openKeyboard();
                            }
                        }
                    });
                }
            }
        });
        newFragment.show(ft, "dialog");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // TODO: we might be able to use getLayout().getLineVisibleEnd() for a more accurate scroll position.
        outState.putInt(STATE_SOURCE_SCROLL_Y, mSourceText.getScrollY());
        outState.putInt(STATE_SOURCE_SCROLL_X, mSourceText.getScrollX());
    }
}
