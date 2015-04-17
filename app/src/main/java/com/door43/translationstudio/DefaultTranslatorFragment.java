package com.door43.translationstudio;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
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
import android.util.Pair;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.door43.translationstudio.dialogs.LanguageResourceDialog;
import com.door43.translationstudio.dialogs.NoteMarkerDialog;
import com.door43.translationstudio.dialogs.VerseMarkerDialog;
import com.door43.translationstudio.projects.Chapter;
import com.door43.translationstudio.projects.Frame;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Resource;
import com.door43.translationstudio.projects.Translation;
import com.door43.translationstudio.projects.TranslationManager;
import com.door43.translationstudio.rendering.DefaultRenderer;
import com.door43.translationstudio.rendering.KeyTermRenderer;
import com.door43.translationstudio.rendering.RenderingGroup;
import com.door43.translationstudio.rendering.SourceTextView;
import com.door43.translationstudio.rendering.USXRenderer;
import com.door43.translationstudio.spannables.NoteSpan;
import com.door43.translationstudio.spannables.Span;
import com.door43.translationstudio.spannables.TermSpan;
import com.door43.translationstudio.spannables.VerseSpan;
import com.door43.translationstudio.util.AnimationUtilities;
import com.door43.translationstudio.util.AppContext;
import com.door43.translationstudio.util.TranslatorBaseFragment;
import com.door43.util.Logger;
import com.door43.util.StringUtilities;
import com.door43.util.threads.ThreadableUI;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by joel on 4/17/2015.
 */
public class DefaultTranslatorFragment extends TranslatorBaseFragment implements TranslatorFragmentInterface {

    private static final int TEXT_FADE_SPEED = 100;
    private static final String STATE_SOURCE_SCROLL_Y = "source_text_scroll_y";
    private static final String STATE_SOURCE_SCROLL_X = "source_text_scroll_x";
    private SourceTextView mSourceText;
    private TextView mSourceTitleText;
    private TextView mSourceFrameNumText;
    private TextView mTranslationTitleText;
    private ImageView mNextFrameView;
    private ImageView mPreviousFrameView;
    private EditText mTranslationEditText;
    private ProgressBar mTranslationProgressBar;
    private ProgressBar mSourceProgressBar;
    private TextView mHelpText;
    private TextWatcher mTranslationChangedListener;
    private Timer mAutosaveTimer;
    private boolean mAutosaveEnabled;
    private GestureDetector mSourceGestureDetector;
    private GestureDetector mTranslationGestureDetector;
    private int mSourceTextMotionDownX = 0;
    private int mSourceTextMotionDownY = 0;
    private int mTranslationTextMotionDownX = 0;
    private int mTranslationTextMotionDownY = 0;
    private Frame mSelectedFrame;
    private Activity mActivity;
    private Span.OnClickListener mVerseClickListener;
    private Span.OnClickListener mNoteClickListener;
    private Span.OnClickListener mKeyTermClickListener;
    private Span.OnClickListener mSourceFootnoteListener;
    private boolean mActivityIsInitializing;
    private AlertDialog mFootnoteDialog;
    private ThreadableUI mHighlightSourceThread;
    private RenderingGroup mSourceRendering;
    private int mSourceScrollX = 0;
    private int mSourceScrollY = 0;
    private ThreadableUI mHighlightTranslationThread;
    private RenderingGroup mTranslationRendering;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_default_translator, container, false);

        setHasOptionsMenu(true);

        mSourceText = (SourceTextView)rootView.findViewById(R.id.readSourceTranslation);
        mSourceTitleText = (TextView)rootView.findViewById(R.id.sourceTitleText);
        mSourceFrameNumText = (TextView)rootView.findViewById(R.id.sourceFrameNumText);
        mTranslationTitleText = (TextView)rootView.findViewById(R.id.translationTitleText);
        mNextFrameView = (ImageView)rootView.findViewById(R.id.hasNextFrameImageView);
        mPreviousFrameView = (ImageView)rootView.findViewById(R.id.hasPreviousFrameImageView);
        mTranslationEditText = (EditText)rootView.findViewById(R.id.inputText);
        mTranslationProgressBar = (ProgressBar)rootView.findViewById(R.id.translationProgressBar);
        mSourceProgressBar = (ProgressBar)rootView.findViewById(R.id.sourceProgressBar);
        mHelpText = (TextView)rootView.findViewById(R.id.helpTextView);

        if(savedInstanceState != null) {
            mSourceScrollY = savedInstanceState.getInt(STATE_SOURCE_SCROLL_Y, 0);
            mSourceScrollX = savedInstanceState.getInt(STATE_SOURCE_SCROLL_X, 0);
        }

        initResourceSwitching();
        mTranslationEditText.setEnabled(false);
        initTextSelectionMenu();
        initTranslationChangedWatcher();
        mTranslationEditText.addTextChangedListener(mTranslationChangedListener);
        ImageView nextFrameView = (ImageView)rootView.findViewById(R.id.hasNextFrameImageView);
        ImageView previousFrameView = (ImageView)rootView.findViewById(R.id.hasPreviousFrameImageView);
        nextFrameView.setColorFilter(getResources().getColor(R.color.blue), PorterDuff.Mode.SRC_ATOP);
        previousFrameView.setColorFilter(getResources().getColor(R.color.blue), PorterDuff.Mode.SRC_ATOP);
        mSourceText.setMovementMethod(new ScrollingMovementMethod());
        mSourceText.setFocusable(true);
        initLinks();
        mSourceText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if (charSequence.length() > 0) {
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
                if (mSourceText.getText().length() == 0) {
                    ((TranslatorActivityInterface) mActivity).openLibraryDrawer();
                }
            }
        });
        initAutoSave();
        mTranslationEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                mActivity.invalidateOptionsMenu();
            }
        });

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
                updateNoteMarker((NoteSpan) span, start, end);
            }
        };
        mKeyTermClickListener = new Span.OnClickListener() {
            @Override
            public void onClick(View view, Span span, int start, int end) {
                showTermDetails(((TermSpan) span).getTermId());
            }
        };
        mSourceFootnoteListener = new Span.OnClickListener() {
            @Override
            public void onClick(View view, Span span, int start, int end) {
                showSourceFootnote((NoteSpan) span);
            }
        };
        mActivityIsInitializing = true;
        initFonts();


        return rootView;
    }

    /**
     * Opens the resources panel and displays the term details
     * @param term
     */
    public void showTermDetails(String term) {
        if(AppContext.projectManager().getSelectedProject() != null) {
//            mRightPane.showTerm(AppContext.projectManager().getSelectedProject().getTerm(term));
            ((TranslatorActivityInterface)mActivity).openResourcesDrawer();
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

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if(activity instanceof TranslatorActivityInterface) {
            mActivity = activity;
        } else {
            throw new ClassCastException("The activity must implement the TranslatorActivityInterface interface");
        }
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
        mFootnoteDialog = new AlertDialog.Builder(mActivity)
                .setMessage(span.getNotes())
                .show();
    }

    private void initAutoSave() {
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
                if (mAutosaveEnabled) {
                    AppContext.translationManager().stageTranslation(mSelectedFrame, mTranslationEditText.getText());
                }
                if (saveDelay != -1) {
                    mAutosaveTimer = new Timer();
                    if (mAutosaveEnabled) {
                        mAutosaveTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                // save the changes
                                ((TranslatorActivityInterface) mActivity).save();
                            }
                        }, saveDelay);
                    }
                }
            }
        });
    }

    /**
     * LinkMovementMethod disables parent gesture events for the spans.
     * So we manually enable the clicking event in order to support scrolling on top of spans.
     * http://stackoverflow.com/questions/7236840/android-textview-linkify-intercepts-with-parent-view-gestures
     */
    private void initLinks() {
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

                    if (action == MotionEvent.ACTION_DOWN) {
                        mTranslationTextMotionDownX = x;
                        mTranslationTextMotionDownY = y;
                    } else if (action == MotionEvent.ACTION_UP) {
                        // check if this is a swipe
                        int maxSpanClickWiggle = 5;
                        if (Math.abs(mTranslationTextMotionDownX - x) > maxSpanClickWiggle || Math.abs(mTranslationTextMotionDownY - y) > maxSpanClickWiggle) {
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
    }

    private void initTranslationChangedWatcher() {
        mTranslationChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // handle saves
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
                                ((TranslatorActivityInterface)mActivity).save();
                            }
                        }, saveDelay);
                    }
                }

                // handle rendering
                // TRICKY: anything worth rendering will need to change by at least two characters
                // so we don't re-render when the user is typing.
                // <a></a> <-- at least 7 characters are required to create a tag for rendering.
                int minDeviation = 7;
                // catch additions greater than the min deviation
                if(count - before > minDeviation) {
                    renderTranslationText(TranslationManager.compileTranslation((Editable)s));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
    }

    private void initTextSelectionMenu() {
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
                int start = mTranslationEditText.getSelectionStart();
                int end = mTranslationEditText.getSelectionEnd();
                switch (menuItem.getItemId()) {
                    case R.id.action_notes:
                        insertNoteMarker(start, end);
                        return true;
                    case android.R.id.copy:
                        Pair copyRange = StringUtilities.expandSelectionForSpans(mTranslationEditText.getText(), start, end);
                        mTranslationEditText.setSelection((int) copyRange.first, (int) copyRange.second);
                        String copyString = TranslationManager.compileTranslation((Editable) mTranslationEditText.getText().subSequence((int) copyRange.first, (int) copyRange.second));
                        StringUtilities.copyToClipboard(mActivity, copyString);
                        return true;
                    case android.R.id.cut:
                        Pair cutRange = StringUtilities.expandSelectionForSpans(mTranslationEditText.getText(), start, end);
                        String cutString = TranslationManager.compileTranslation((Editable) mTranslationEditText.getText().subSequence((int) cutRange.first, (int) cutRange.second));
                        StringUtilities.copyToClipboard(mActivity, cutString);
                        CharSequence preceedingText = mTranslationEditText.getText().subSequence(0, (int) cutRange.first);
                        CharSequence trailingText = mTranslationEditText.getText().subSequence((int) cutRange.second, mTranslationEditText.getText().length());
                        mTranslationEditText.setText(TextUtils.concat(preceedingText, trailingText));
                        mTranslationEditText.setSelection((int) cutRange.first);
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
            }
        });
    }

    private void initResourceSwitching() {
        mSourceTitleText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final Project p = AppContext.projectManager().getSelectedProject();
                if (p != null && p.getSelectedSourceLanguage().getResources().length > 1) {
                    // Create and show the dialog.
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    Fragment prev = getFragmentManager().findFragmentByTag("dialog");
                    if (prev != null) {
                        ft.remove(prev);
                    }
                    ft.addToBackStack(null);

                    app().closeToastMessage();

                    ((TranslatorActivityInterface) mActivity).save();

                    LanguageResourceDialog newFragment = new LanguageResourceDialog();
                    Bundle args = new Bundle();
                    args.putString("projectId", p.getId());
                    newFragment.setArguments(args);
                    newFragment.setOnChooseListener(new LanguageResourceDialog.OnChooseListener() {
                        @Override
                        public void onChoose(Resource resource) {
                            if (p == null) return;
                            final ProgressDialog dialog = new ProgressDialog(mActivity);
                            dialog.setMessage(getResources().getString(R.string.loading_project_chapters));
                            dialog.setCancelable(false);
                            dialog.setCanceledOnTouchOutside(false);
                            dialog.show();
                            p.getSelectedSourceLanguage().setSelectedResource(resource.getId());
                            new ThreadableUI(mActivity) {

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
                                    ((TranslatorActivityInterface) mActivity).refreshLibraryDrawer();
                                }
                            }.start();
                        }
                    });
                    newFragment.show(ft, "dialog");
                }
            }
        });
    }

    /**
     * Parses the source text for key terms and generates clickable links.
     */
    private void renderSourceText() {
        if(mHighlightSourceThread != null) {
            mHighlightSourceThread.stop();
        }

        // this thread handles the fading animations
        mHighlightSourceThread = new ThreadableUI(mActivity) {
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
                            renderThread = new ThreadableUI(mActivity) {
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
                                        ((TranslatorActivityInterface)mActivity).refreshResourcesDrawer();
//                                        mRightPane.reloadTermsTab();
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
                    Handler handle = new Handler(Looper.getMainLooper());
                    handle.post(new Runnable() {
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
        if(mHighlightTranslationThread != null) {
            mHighlightTranslationThread.stop();
        }
        mHighlightTranslationThread = new ThreadableUI(mActivity) {
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
                            renderThread = new ThreadableUI(mActivity) {
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
                                        int scrollX = mTranslationEditText.getScrollX();
                                        int scrollY = mTranslationEditText.getScrollX();
                                        int selection = mTranslationEditText.getSelectionStart();
                                        mTranslationEditText.removeTextChangedListener(mTranslationChangedListener);
                                        mTranslationEditText.setText(TextUtils.concat(output, "\n"));
                                        mTranslationEditText.addTextChangedListener(mTranslationChangedListener);
                                        mTranslationEditText.clearAnimation();
                                        mTranslationEditText.startAnimation(in);
                                        mTranslationProgressBar.clearAnimation();
                                        mTranslationProgressBar.startAnimation(outProgress);
                                        // preserve selection and scroll
                                        mTranslationEditText.scrollTo(scrollX, scrollY);
                                        if(selection > mTranslationEditText.length()) {
                                            selection = mTranslationEditText.length();
                                        }
                                        mTranslationEditText.setSelection(selection);
                                        mTranslationEditText.clearFocus();
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
                    Handler handle = new Handler(Looper.getMainLooper());
                    handle.post(new Runnable() {
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
            ((TranslatorActivityInterface)mActivity).save();
            Frame f;
            if(distanceX > 0) {
                f = p.getSelectedChapter().getPreviousFrame();
            } else {
                f = p.getSelectedChapter().getNextFrame();
            }
            if(f != null) {
                p.getSelectedChapter().setSelectedFrame(f.getId());
//                mLeftPane.selectTab(mLeftPane.getSelectedTabIndex());
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
        mActivity.invalidateOptionsMenu();
        // load the text
        final Project p = AppContext.projectManager().getSelectedProject();
        if(frameIsSelected()) {
            ((TranslatorActivityInterface)mActivity).refreshResourcesDrawer();
//            mRightPane.reloadNotesTab();

            mSelectedFrame = p.getSelectedChapter().getSelectedFrame();
            mTranslationEditText.setEnabled(true);
            final int frameIndex = p.getSelectedChapter().getFrameIndex(mSelectedFrame);
            final Chapter chapter = p.getSelectedChapter();

            // get the target language
            if(!p.hasChosenTargetLanguage()) {
                ((TranslatorActivityInterface)mActivity).showProjectSettingsMenu();
            }

            // target translation
            final Translation translation = mSelectedFrame.getTranslation();

            mTranslationEditText.scrollTo(0, 0);
            mTranslationEditText.setSelection(0);
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
            ((TranslatorActivityInterface)mActivity).openLibraryDrawer();
        }
        enableAutosave();
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

    // Checks if a frame has been selected in the app
    public boolean frameIsSelected() {
        Project selectedProject = AppContext.projectManager().getSelectedProject();
        return selectedProject != null && selectedProject.getSelectedChapter() != null && selectedProject.getSelectedChapter().getSelectedFrame() != null;
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
                new Handler(Looper.getMainLooper()).post(new Runnable() {
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
        new ThreadableUI(mActivity) {
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
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
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
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
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
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
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
        args.putInt("maxVerse", f.getEndingVerseNumber());
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
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
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
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
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
    public void reload() {
        reloadCenterPane();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        Project p = AppContext.projectManager().getSelectedProject();
        Boolean projectEnabled = p != null;
        if(((TranslatorActivityInterface)mActivity).keyboardIsOpen()) {
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

            if(menu.findItem(R.id.action_chapter_settings) != null) {
                menu.findItem(R.id.action_chapter_settings).setVisible(projectEnabled && hasChapterSettings);
            }
            if(menu.findItem(R.id.action_project_settings) != null) {
                menu.findItem(R.id.action_project_settings).setVisible(projectEnabled);
            }
            if(menu.findItem(R.id.action_sync) != null) {
                menu.findItem(R.id.action_sync).setVisible(projectEnabled);
            }
            if(menu.findItem(R.id.action_resources) != null) {
                menu.findItem(R.id.action_resources).setVisible(projectEnabled && hasResources);
            }

//            if(!hasResources) {
//                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.END);
//            } else {
//                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.END);
//            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu items for use in the action bar
        if(((TranslatorActivityInterface)mActivity).keyboardIsOpen()) {
            inflater.inflate(R.menu.translation_actions, menu);
        } else {
            inflater.inflate(R.menu.main_activity_actions, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_verse_marker:
                insertVerseMarker( mTranslationEditText.getSelectionStart());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // TODO: we might be able to use getLayout().getLineVisibleEnd() for a more accurate scroll position.
        outState.putInt(STATE_SOURCE_SCROLL_Y, mSourceText.getScrollY());
        outState.putInt(STATE_SOURCE_SCROLL_X, mSourceText.getScrollX());
    }
}
