package com.door43.translationstudio.ui.translate;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.ui.SettingsActivity;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationViewMode;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.ui.dialogs.BackupDialog;
import com.door43.translationstudio.ui.dialogs.FeedbackDialog;
import com.door43.translationstudio.ui.dialogs.PrintDialog;
import com.door43.translationstudio.ui.draft.DraftActivity;
import com.door43.translationstudio.ui.filechooser.FileChooserActivity;
import com.door43.translationstudio.ui.publish.PublishActivity;
import com.door43.translationstudio.ui.translate.review.SearchSubject;
import com.door43.util.SdUtils;
import com.door43.widget.VerticalSeekBar;
import com.door43.widget.VerticalSeekBarHint;
import com.door43.widget.ViewUtil;
import com.door43.translationstudio.ui.BaseActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import it.moondroid.seekbarhint.library.SeekBarHint;

public class TargetTranslationActivity extends BaseActivity implements ViewModeFragment.OnEventListener, FirstTabFragment.OnEventListener, Spinner.OnItemSelectedListener {

    private static final String TAG = "TranslationActivity";

    private static final long COMMIT_INTERVAL = 2 * 60 * 1000; // commit changes every 2 minutes
    public static final int SEARCH_START_DELAY = 1000;
    public static final String STATE_SEARCH_ENABLED = "state_search_enabled";
    public static final String STATE_SEARCH_TEXT = "state_search_text";
    public static final String STATE_HAVE_MERGE_CONFLICT = "state_have_merge_conflict";
    public static final String STATE_MERGE_CONFLICT_FILTER_ENABLED = "state_merge_conflict_filter_enabled";
    public static final String STATE_MERGE_CONFLICT_SUMMARY_DISPLAYED = "state_merge_conflict_summary_displayed";
    public static final String SEARCH_SOURCE = "search_source";
    public static final String STATE_SEARCH_AT_END = "state_search_at_end";
    public static final String STATE_SEARCH_AT_START = "state_search_at_start";
    public static final String STATE_SEARCH_FOUND_CHUNKS = "state_search_found_chunks";
    public static final int RESULT_DO_UPDATE = 42;
    private Fragment mFragment;
    private SeekBar mSeekBar;
    private ViewGroup mGraduations;
    private Translator mTranslator;
    private TargetTranslation mTargetTranslation;
    private Timer mCommitTimer = new Timer();
    private ImageButton mReadButton;
    private ImageButton mChunkButton;
    private ImageButton mReviewButton;
    private ImageButton mMoreButton;
    private boolean mSearchEnabled = false;
    private TextWatcher mSearchTextWatcher;
    private SearchTimerTask mSearchTimerTask;
    private Timer mSearchTimer;
    private String mSearchString;
    private ProgressBar mSearchingSpinner;
    private EditText mSearchEditText;

    private boolean mEnableGrids = false;
    private int mSeekbarMultiplier = 1; // allows for more granularity in setting position if cards are few
    private int mOldItemCount = 1; // so we can update the seekbar maximum when item count has changed
    private ImageButton mMergeConflict;
    private boolean mHaveMergeConflict = false;
    private boolean mMergeConflictFilterEnabled = false;
    private ImageButton mDownSearch;
    private ImageButton mUpSearch;
    private TextView mFoundText;
    private int mFoundTextFormat;
    private boolean mSearchAtEnd = false;
    private boolean mSearchAtStart = false;
    private int mNumberOfChunkMatches = 0;
    private boolean mSearchResumed = false;
    private boolean mShowConflictSummary = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_target_translation_detail);

        mTranslator = App.getTranslator();

        // validate parameters
        Bundle args = getIntent().getExtras();
        final String targetTranslationId = args.getString(App.EXTRA_TARGET_TRANSLATION_ID, null);
        mMergeConflictFilterEnabled = args.getBoolean(App.EXTRA_START_WITH_MERGE_FILTER, false);
        mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
        if (mTargetTranslation == null) {
            Logger.e(TAG ,"A valid target translation id is required. Received '" + targetTranslationId + "' but the translation could not be found");
            finish();
            return;
        }

        if(savedInstanceState == null) {
            // reset cached values
            ViewModeFragment.reset();
        }

        // open used source translations by default
        if(App.getOpenSourceTranslations(mTargetTranslation.getId()).length == 0) {
            String[] resourceContainerSlugs = mTargetTranslation.getSourceTranslations();
            for (String slug : resourceContainerSlugs) {
                App.addOpenSourceTranslation(mTargetTranslation.getId(), slug);
            }
        }

        // notify user that a draft translation exists the first time activity starts
        if(savedInstanceState == null && draftIsAvailable() && mTargetTranslation.numTranslated() == 0) {
            Snackbar snack = Snackbar.make(findViewById(android.R.id.content), R.string.draft_translation_exists, Snackbar.LENGTH_LONG)
                    .setAction(R.string.preview, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(TargetTranslationActivity.this, DraftActivity.class);
                            intent.putExtra(DraftActivity.EXTRA_TARGET_TRANSLATION_ID, mTargetTranslation.getId());
                            startActivity(intent);
                        }
                    });
            ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
            snack.show();
        }

        // manual location settings
        int modeIndex = args.getInt(App.EXTRA_VIEW_MODE, -1);
        if (modeIndex > 0 && modeIndex < TranslationViewMode.values().length) {
            App.setLastViewMode(targetTranslationId, TranslationViewMode.values()[modeIndex]);
        }

        mSearchingSpinner = (ProgressBar) findViewById(R.id.search_progress);
        mSearchEditText = (EditText) findViewById(R.id.search_text);
        mReadButton = (ImageButton) findViewById(R.id.action_read);
        mChunkButton = (ImageButton) findViewById(R.id.action_chunk);
        mReviewButton = (ImageButton) findViewById(R.id.action_review);
        mMergeConflict = (ImageButton) findViewById(R.id.warn_merge_conflict);

        mDownSearch = (ImageButton) findViewById(R.id.down_search);
        mDownSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveSearch(true);
            }
        });
        mUpSearch = (ImageButton) findViewById(R.id.up_search);
        mUpSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveSearch(false);
            }
        });

        mFoundText = (TextView) findViewById(R.id.found);
        mFoundTextFormat = R.string.found_in_chunks;

        // inject fragments
        if (findViewById(R.id.fragment_container) != null) {
            if (savedInstanceState != null) {
                mFragment = getFragmentManager().findFragmentById(R.id.fragment_container);
            } else {
                TranslationViewMode viewMode = App.getLastViewMode(mTargetTranslation.getId());
                switch (viewMode) {
                    case READ:
                        mFragment = new ReadModeFragment();
                        break;
                    case CHUNK:
                        mFragment = new ChunkModeFragment();
                        break;
                    case REVIEW:
                        mFragment = new ReviewModeFragment();
                        break;
                }
                mFragment.setArguments(getIntent().getExtras());
                getFragmentManager().beginTransaction().add(R.id.fragment_container, mFragment).commit();
                // TODO: animate
                // TODO: udpate menu
            }
        }

        setUpSeekBar();

        // set up menu items
        mMoreButton = (ImageButton) findViewById(R.id.action_more);
        buildMenu();

        mMergeConflict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMergeConflictFilterEnabled = !mMergeConflictFilterEnabled; // toggle filter state
                openTranslationMode(TranslationViewMode.REVIEW, null); // make sure we are in review mode
                setMergeConflictFilter(mMergeConflictFilterEnabled, mMergeConflictFilterEnabled); // update displayed state
            }
        });

        mReadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeSearchBar();
                openTranslationMode(TranslationViewMode.READ, null);
            }
        });

        mChunkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeSearchBar();
                openTranslationMode(TranslationViewMode.CHUNK, null);
            }
        });

        mReviewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeSearchBar();
                mMergeConflictFilterEnabled = false;
                setMergeConflictFilter(mMergeConflictFilterEnabled, false);
                openTranslationMode(TranslationViewMode.REVIEW, null);
            }
        });

        if(savedInstanceState != null) {
            mSearchEnabled = savedInstanceState.getBoolean(STATE_SEARCH_ENABLED, false);
            mSearchResumed = mSearchEnabled;
            mSearchAtEnd = savedInstanceState.getBoolean(STATE_SEARCH_AT_END, false);
            mSearchAtStart = savedInstanceState.getBoolean(STATE_SEARCH_AT_START, false);
            mNumberOfChunkMatches = savedInstanceState.getInt(STATE_SEARCH_FOUND_CHUNKS, 0);
            mSearchString = savedInstanceState.getString(STATE_SEARCH_TEXT, null);
            mHaveMergeConflict = savedInstanceState.getBoolean(STATE_HAVE_MERGE_CONFLICT, false);
            mMergeConflictFilterEnabled = savedInstanceState.getBoolean(STATE_MERGE_CONFLICT_FILTER_ENABLED, false);
            mShowConflictSummary = savedInstanceState.getBoolean(STATE_MERGE_CONFLICT_SUMMARY_DISPLAYED, false);
        } else {
            mShowConflictSummary = mMergeConflictFilterEnabled;
        }

        setupSidebarModeIcons();
        setSearchBarVisibility(mSearchEnabled);
        if(mSearchEnabled) {
            setSearchSpinner(true, mNumberOfChunkMatches, mSearchAtEnd, mSearchAtStart); // restore initial state
        }

        restartAutoCommitTimer();
    }

    /**
     * enable/disable merge conflict filter in adapter
     * @param enableFilter
     * @param forceMergeConflict - if true, then will initialize have merge conflict flag to true
     */
    private void setMergeConflictFilter(final boolean enableFilter, final boolean forceMergeConflict) {
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                mMergeConflictFilterEnabled = enableFilter;
                if(mFragment instanceof ViewModeFragment) {
                    ViewModeFragment viewModeFragment = (ViewModeFragment) TargetTranslationActivity.this.mFragment;
                    viewModeFragment.setShowMergeSummary(mShowConflictSummary);
                    viewModeFragment.setMergeConflictFilter(enableFilter, forceMergeConflict);
                }
                onEnableMergeConflict(mHaveMergeConflict, mMergeConflictFilterEnabled);
            }
        });
    }

    @Override
    /**
     * called by adapter to set state for merge conflict icon
     */
    public void onEnableMergeConflict(boolean showConflicted, boolean active) {
        mHaveMergeConflict = showConflicted;
        mMergeConflictFilterEnabled = active;
        if(mMergeConflict != null) {
            mMergeConflict.setVisibility(showConflicted ? View.VISIBLE : View.GONE);
            if(mMergeConflictFilterEnabled) {
                mMergeConflict.setImageResource(R.drawable.ic_warning_white_24dp);
                final int highlightedColor = getResources().getColor(R.color.primary_dark);
                mMergeConflict.setBackgroundColor(highlightedColor);
            } else {
                mMergeConflict.setImageResource(R.drawable.ic_warning_inactive_24dp);
                mMergeConflict.setBackgroundDrawable(null); // clear any previous background highlighting
            }
        }
    }

    private void setUpSeekBar() {
        if(mEnableGrids) {
            mGraduations = (ViewGroup) findViewById(R.id.action_seek_graduations);
        }
        mSeekBar = (SeekBar) findViewById(R.id.action_seek);
        mSeekBar.setMax(100);
        mSeekBar.setProgress(computePositionFromProgress(0));
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                progress = handleItemCountIfChanged(progress);
                int correctedProgress = correctProgress(progress);
                correctedProgress = limitRange(correctedProgress, 0, mSeekBar.getMax() - 1);
                int position = correctedProgress / mSeekbarMultiplier;
                int percentage = 0;

                if(mSeekbarMultiplier > 1) { // if we need some granularity, calculate fractional amount
                    int fractional = correctedProgress - position * mSeekbarMultiplier;
                    if(fractional != 0) {
                        percentage = 100 * fractional / mSeekbarMultiplier;
                    }
                }

                // TODO: 2/16/17 record position

                // If this change was initiated by a click on a UI element (rather than as a result
                // of updates within the program), then update the view accordingly.
                if (mFragment instanceof ViewModeFragment && fromUser) {
                    ((ViewModeFragment) mFragment).onScrollProgressUpdate(position, percentage);
                }

                TargetTranslationActivity activity = (TargetTranslationActivity) seekBar.getContext();
                if (activity != null) {
                    activity.closeKeyboard();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if(mGraduations != null) {
                    mGraduations.animate().alpha(1.f);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(mGraduations != null) {
                    mGraduations.animate().alpha(0.f);
                }
            }
        });

        if(mSeekBar instanceof SeekBarHint) {
            ((SeekBarHint) mSeekBar).setOnProgressChangeListener(new SeekBarHint.OnSeekBarHintProgressChangeListener() {
                @Override
                public String onHintTextChanged(SeekBarHint seekBarHint, int progress) {
                    return getFormattedChapter(progress);
                }
            });
        }

        if(mSeekBar instanceof VerticalSeekBarHint) {
            ((VerticalSeekBarHint) mSeekBar).setOnProgressChangeListener(new VerticalSeekBarHint.OnSeekBarHintProgressChangeListener() {
                @Override
                public String onHintTextChanged(VerticalSeekBarHint seekBarHint, int progress) {
                    return getFormattedChapter(progress);
                }
            });
        }
    }

    /**
     * clips value to within range min to max
     * @param value
     * @param min
     * @param max
     * @return
     */
    private int limitRange(int value, int min, int max) {
        int newValue = value;
        if(newValue < min) {
            newValue = min;
        } else
        if(newValue > max) {
            newValue = max;
        }
        return newValue;
    }

    /**
     * get chapter string to display
     * @param progress
     * @return
     */
    private String getFormattedChapter(int progress) {
        int position = computePositionFromProgress(progress);
        String chapter = getChapterSlug(position);
        String displayedText = " " + chapter + " ";
        return displayedText;
    }

    public void onSaveInstanceState(Bundle out) {
        out.putBoolean(STATE_SEARCH_ENABLED, mSearchEnabled);
        String searchText = getFilterText();
        if( mSearchEnabled ) {
            out.putString(STATE_SEARCH_TEXT, searchText);
            out.putBoolean(STATE_SEARCH_AT_END, mSearchAtEnd);
            out.putBoolean(STATE_SEARCH_AT_START, mSearchAtStart);
            out.putInt(STATE_SEARCH_FOUND_CHUNKS, mNumberOfChunkMatches);
        }
        out.putBoolean(STATE_HAVE_MERGE_CONFLICT, mHaveMergeConflict);
        out.putBoolean(STATE_MERGE_CONFLICT_FILTER_ENABLED, mMergeConflictFilterEnabled);
        if(mFragment instanceof ViewModeFragment) {
            out.putBoolean(STATE_MERGE_CONFLICT_SUMMARY_DISPLAYED, ((ViewModeFragment) mFragment).ismMergeConflictSummaryDisplayed());
        }
        super.onSaveInstanceState(out);
    }

    private void buildMenu() {
        if(mMoreButton != null) {
            mMoreButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu moreMenu = new PopupMenu(TargetTranslationActivity.this, v);
                    ViewUtil.forcePopupMenuIcons(moreMenu);
                    moreMenu.getMenuInflater().inflate(R.menu.menu_target_translation_detail, moreMenu.getMenu());

                    // display menu item for draft translations
                    MenuItem draftsMenuItem = moreMenu.getMenu().findItem(R.id.action_drafts_available);
                    draftsMenuItem.setVisible(draftIsAvailable());

                    MenuItem searchMenuItem = moreMenu.getMenu().findItem(R.id.action_search);
                    boolean searchSupported = isSearchSupported();
                    searchMenuItem.setVisible(searchSupported);

                    moreMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.action_translations:
                                    finish();
                                    return true;
                                case R.id.action_publish:
                                    Intent publishIntent = new Intent(TargetTranslationActivity.this, PublishActivity.class);
                                    publishIntent.putExtra(PublishActivity.EXTRA_TARGET_TRANSLATION_ID, mTargetTranslation.getId());
                                    publishIntent.putExtra(PublishActivity.EXTRA_CALLING_ACTIVITY, PublishActivity.ACTIVITY_TRANSLATION);
                                    startActivity(publishIntent);
                                    // TRICKY: we may move back and forth between the publisher and translation activites
                                    // so we finish to avoid filling the stack.
                                    finish();
                                    return true;
                                case R.id.action_drafts_available:
                                    Intent intent = new Intent(TargetTranslationActivity.this, DraftActivity.class);
                                    intent.putExtra(DraftActivity.EXTRA_TARGET_TRANSLATION_ID, mTargetTranslation.getId());
                                    startActivity(intent);
                                    return true;
                                case R.id.action_backup:
                                    FragmentTransaction backupFt = getFragmentManager().beginTransaction();
                                    Fragment backupPrev = getFragmentManager().findFragmentByTag(BackupDialog.TAG);
                                    if (backupPrev != null) {
                                        backupFt.remove(backupPrev);
                                    }
                                    backupFt.addToBackStack(null);

                                    BackupDialog backupDialog = new BackupDialog();
                                    Bundle args = new Bundle();
                                    args.putString(BackupDialog.ARG_TARGET_TRANSLATION_ID, mTargetTranslation.getId());
                                    backupDialog.setArguments(args);
                                    backupDialog.show(backupFt, BackupDialog.TAG);
                                    return true;
                                case R.id.action_print:
                                    FragmentTransaction printFt = getFragmentManager().beginTransaction();
                                    Fragment printPrev = getFragmentManager().findFragmentByTag("printDialog");
                                    if (printPrev != null) {
                                        printFt.remove(printPrev);
                                    }
                                    printFt.addToBackStack(null);

                                    PrintDialog printDialog = new PrintDialog();
                                    Bundle printArgs = new Bundle();
                                    printArgs.putString(PrintDialog.ARG_TARGET_TRANSLATION_ID, mTargetTranslation.getId());
                                    printDialog.setArguments(printArgs);
                                    printDialog.show(printFt, "printDialog");
                                    return true;
                                case R.id.action_feedback:
                                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                                    Fragment prev = getFragmentManager().findFragmentByTag("bugDialog");
                                    if (prev != null) {
                                        ft.remove(prev);
                                    }
                                    ft.addToBackStack(null);

                                    FeedbackDialog dialog = new FeedbackDialog();
                                    dialog.show(ft, "bugDialog");
                                    return true;
                                case R.id.action_settings:
                                    Intent settingsIntent = new Intent(TargetTranslationActivity.this, SettingsActivity.class);
                                    startActivity(settingsIntent);
                                    return true;
                                case R.id.action_search:
                                    setSearchBarVisibility(true);
                                    return true;
                            }
                            return false;
                        }
                    });
                    moreMenu.show();
                }
            });
        }
    }

    /**
     * hide search bar and clear search text
     */
    private void removeSearchBar() {
        setSearchBarVisibility(false);
        setFilterText(null);
        filter(null); // clear search filter
    }

    /**
     * method to see if searching is supported
     */
    public boolean isSearchSupported() {
        if(mFragment instanceof ViewModeFragment) {
            return ((ViewModeFragment) mFragment).hasFilter();
        }
        return false;
    }

    /**
     * change state of search bar
     * @param show - if true set visible
     */
    private void setSearchBarVisibility(boolean show) {
        // toggle search bar
        LinearLayout searchPane = (LinearLayout) findViewById(R.id.search_pane);
        if(searchPane != null) {

            int visibility = View.GONE;
            if(show) {
                visibility = View.VISIBLE;
            } else {
                setSearchSpinner(false, 0, true, true);
            }

            searchPane.setVisibility(visibility);
            mSearchEnabled = show;

            if(mSearchEditText != null) {
                if(mSearchTextWatcher != null) {
                    mSearchEditText.removeTextChangedListener(mSearchTextWatcher); // remove old listener
                    mSearchTextWatcher = null;
                }

                if(show) {
                    mSearchTextWatcher = new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {

                        }

                        @Override
                        public void afterTextChanged(Editable s) {

                            if(mSearchTimer != null) {
                                mSearchTimer.cancel();
                            }

                            mSearchTimer = new Timer();
                            mSearchTimerTask = new SearchTimerTask(TargetTranslationActivity.this, s);
                            mSearchTimer.schedule(mSearchTimerTask, SEARCH_START_DELAY);
                        }
                    };

                    mSearchEditText.addTextChangedListener(mSearchTextWatcher);
                    if(mSearchResumed) {
                        // we don't have a way to reliably determine the state of the soft keyboard
                        //   so we don't initially show the keyboard on resume.  This should be less
                        //   annoying than always popping up the keyboard on resume
                        mSearchResumed = false;
                    } else {
                        setFocusOnTextSearchEdit();
                    }
                } else {
                    filter(null); // clear search filter
                    App.closeKeyboard(TargetTranslationActivity.this);
                }

                if(mSearchString != null) { // restore after rotate
                    mSearchEditText.setText(mSearchString);
                    if(show) {
                        filter(mSearchString);
                    }
                    mSearchString = null;
                }
            }

            ImageButton close = (ImageButton) searchPane.findViewById(R.id.close_search);
            if(close != null) {

                close.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        removeSearchBar();
                    }
                });
            }

            Spinner searchType = (Spinner) searchPane.findViewById(R.id.search_type);
            if(searchType != null) {
                List<String> types = new ArrayList<String>();
                types.add(this.getResources().getString(R.string.search_source));
                types.add(this.getResources().getString(R.string.search_translation));
                ArrayAdapter<String> typesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, types);
                typesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                searchType.setAdapter(typesAdapter);

                // restore last search type
                String lastSearchSourceStr = App.getUserString(SEARCH_SOURCE, SearchSubject.SOURCE.name().toUpperCase());
                SearchSubject lastSearchSource = SearchSubject.SOURCE;
                try {
                    lastSearchSource = SearchSubject.valueOf(lastSearchSourceStr.toUpperCase());
                } catch(Exception e) {
                    e.printStackTrace();
                }
                searchType.setSelection(lastSearchSource.ordinal());

                searchType.setOnItemSelectedListener(this);
            }
        }
    }

    /**
     * this seems crazy that we have to do a delay within a delay, but it is the only thing that works
     *   to bring up keyboard.  Guessing that it is because there is so much redrawing that is
     *   happening on bringing up the search bar.
     */
    @Deprecated
    private void setFocusOnTextSearchEdit() {
        if(mSearchEditText != null) {
            Handler hand = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    mSearchEditText.setFocusableInTouchMode(true);
                    mSearchEditText.requestFocus();

                    Handler hand = new Handler(Looper.getMainLooper());
                    hand.post(new Runnable() {
                        @Override
                        public void run() {
                            App.showKeyboard(TargetTranslationActivity.this, mSearchEditText, true);
                        }
                    });
                }
            });
        }
    }

    /**
     * notify listener of search state changes
     * @param doingSearch - search is currently processing
     * @param numberOfChunkMatches - number of chunks that have the search string
     * @param atEnd - we are at last search item
     * @param atStart - we are at first search item
     */
    private void setSearchSpinner(boolean doingSearch, int numberOfChunkMatches, boolean atEnd, boolean atStart) {
        mSearchAtEnd = atEnd;
        mSearchAtStart = atStart;
        mNumberOfChunkMatches = numberOfChunkMatches;
        if(mSearchingSpinner != null) {
            mSearchingSpinner.setVisibility(doingSearch ? View.VISIBLE : View.GONE);

            boolean showSearchNavigation = !doingSearch && (numberOfChunkMatches > 0);
            int searchVisibility = showSearchNavigation ? View.VISIBLE : View.INVISIBLE;
            mDownSearch.setVisibility(atEnd ? View.INVISIBLE : searchVisibility);
            mUpSearch.setVisibility(atStart ? View.INVISIBLE : searchVisibility);

            String msg = getResources().getString(mFoundTextFormat, numberOfChunkMatches);
            mFoundText.setVisibility( !doingSearch ? View.VISIBLE : View.INVISIBLE);
            mFoundText.setText(msg);
        }
    }

    /**
     * called if search type is changed
     * @param parent
     * @param view
     * @param pos
     * @param id
     */
    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {

        filter(getFilterText());  // do search with search string in edit control
    }

    /**
     * called if no search type is selected
     * @param parent
     */
    public void onNothingSelected(AdapterView<?> parent) {
        // do nothing
    }

    /**
     * get the type of search
     */
    private SearchSubject getFilterSubject() {
        LinearLayout searchPane = (LinearLayout) findViewById(R.id.search_pane);
        if(searchPane != null) {

            Spinner type = (Spinner) searchPane.findViewById(R.id.search_type);
            if(type != null) {
                int pos = type.getSelectedItemPosition();
                if(pos == 0) {
                    return SearchSubject.SOURCE;
                }
            }
        }
        return SearchSubject.TARGET;
    }

    /**
     * get search text in search bar
     */
    private String getFilterText() {
        String text = null;

        LinearLayout searchPane = (LinearLayout) findViewById(R.id.search_pane);
        if(searchPane != null) {
            if(mSearchEditText != null) {
                text = mSearchEditText.getText().toString();
            }
        }
        return text;
    }

    /**
     * set search text in search bar
     */
    private void setFilterText(String text) {

        if(text == null) {
            text = "";
        }

        LinearLayout searchPane = (LinearLayout) findViewById(R.id.search_pane);
        if(searchPane != null) {
            if(mSearchEditText != null) {
                mSearchEditText.setText(text);
            }
        }
    }


    /**
     * Filters the list, currently it just marks chunks with text
     * @param constraint
     */
    public void filter(final String constraint) {
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                if((mFragment != null) && (mFragment instanceof ViewModeFragment)) {

                    // preserve current search type
                    SearchSubject subject = getFilterSubject();
                    App.setUserString(SEARCH_SOURCE, subject.name().toUpperCase());
                    if(constraint != null) {
                        ((ViewModeFragment) mFragment).filter(constraint, subject);
                    }
                }
             }
        });
    }

    /**
     * move to next/previous search item
     * @param next if true then find next, otherwise will find previous
     */
    public void moveSearch(final boolean next) {
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                if((mFragment != null) && (mFragment instanceof ViewModeFragment)) {
                    ((ViewModeFragment)mFragment).onMoveSearch(next);
                }
            }
        });
    }

    /**
     * Checks if a draft is available
     *
     * @return
     */
    private boolean draftIsAvailable() {
        List<Translation> draftTranslations = App.getLibrary().index().findTranslations(mTargetTranslation.getTargetLanguage().slug, mTargetTranslation.getProjectId(), null, "book", null, 0, -1);
        return draftTranslations.size() > 0;
    }

    @Override
    public void onResume() {
        super.onResume();
        notifyDatasetChanged();
        buildMenu();
        setMergeConflictFilter(mMergeConflictFilterEnabled, mMergeConflictFilterEnabled); // restore last state
    }

    @Override
    public void onPause() {
        super.onPause();

        if(mFragment instanceof ViewModeFragment) {
            mShowConflictSummary = ((ViewModeFragment) mFragment).ismMergeConflictSummaryDisplayed(); // update current state
        }
    }

    public void closeKeyboard() {
        if (mFragment instanceof ViewModeFragment) {
            boolean enteringSearchText = mSearchEnabled && (mSearchEditText != null) && (mSearchEditText.hasFocus());
            if(!enteringSearchText) { // we don't want to close keyboard if we are entering search text
                ((ViewModeFragment) mFragment).closeKeyboard();
            }
        }
    }

    public void checkIfCursorStillOnScreen() {

        Rect cursorPos = getCursorPositionOnScreen();
        if (cursorPos != null) {

            View scrollView = findViewById(R.id.fragment_container);
            if (scrollView != null) {

                Boolean visible = true;

                Rect scrollBounds = new Rect();
                scrollView.getHitRect(scrollBounds);

                if (cursorPos.top < scrollBounds.top) {
                    visible = false;
                }
                else if (cursorPos.bottom > scrollBounds.bottom) {
                    visible = false;
                }

                if (!visible) {
                    closeKeyboard();
                }
            }
        }
    }

    public Rect getCursorPositionOnScreen() {

        View focusedView = (View) getCurrentFocus();
        if (focusedView != null) {

            // get view position on screen
            int[] l = new int[2];
            focusedView.getLocationOnScreen(l);
            int focusedViewX = l[0];
            int focusedViewY = l[1];

            if (focusedView instanceof EditText) {

                // getting relative cursor position
                EditText editText = (EditText) focusedView;
                int pos = editText.getSelectionStart();
                Layout layout = editText.getLayout();
                if (layout != null) {
                    int line = layout.getLineForOffset(pos);
                    int baseline = layout.getLineBaseline(line);
                    int ascent = layout.getLineAscent(line);

                    // convert relative positions to absolute position
                    int x = focusedViewX + (int) layout.getPrimaryHorizontal(pos);
                    int bottomY = focusedViewY + baseline;
                    int y = bottomY + ascent;

                    return new Rect(x, y, x, bottomY); // ignore width of cursor for now
                }
            }
        }

        return null;
    }

    @Override
    public void onScrollProgress(int position) {
//        position = handleItemCountIfChanged(position);
        // TODO: 2/16/17 record scroll position
        int progress = computeProgressFromPosition(position);
        Log.d(TAG, "onScrollProgress: position=" + position + ", mapped to progressbar=" + progress);
        mSeekBar.setProgress(progress);
        checkIfCursorStillOnScreen();
    }

    @Override
    public void onDataSetChanged(int count) {
        int initialMax = mSeekBar.getMax();
        int initialProgress = mSeekBar.getProgress();

        count = setSeekbarMax(count);
        int newMax = mSeekBar.getMax();
        if(initialMax != newMax) { // if seekbar maximum has changed
            // adjust proportionally
            int newProgress = newMax * initialProgress / initialMax;
            mSeekBar.setProgress(newProgress);
        }
        closeKeyboard();
        setupGraduations();
    }

    /**
     * get number of items in adapter
     * @return
     */
    private int getItemCount() {
        if((mFragment != null) && (mFragment instanceof ViewModeFragment)) {
            return ((ViewModeFragment)mFragment).getItemCount();
        }
        return 0;
    }

    /**
     * sets seekbar maximum based on item count, and add granularity if item count is small
     * @param itemCount
     * @return
     */
    private int setSeekbarMax(int itemCount) {
        final int minimumSteps = 300;

        Log.i(TAG,"setSeekbarMax: itemCount=" + itemCount);

        if(itemCount < 1) { // sanity check
            itemCount = 1;
        }

        if(itemCount < minimumSteps) {  // increase step size if number of cards is small, this gives more granularity in positioning
            mSeekbarMultiplier = (int) (minimumSteps / itemCount) + 1;
        } else {
            mSeekbarMultiplier = 1;
        }

        int newMax = itemCount * mSeekbarMultiplier;
        int oldMax = mSeekBar.getMax();
        if(newMax != oldMax) {
            Log.i(TAG,"setSeekbarMax: oldMax=" + oldMax + ", newMax=" + newMax + ", mSeekbarMultiplier=" + mSeekbarMultiplier);
            mSeekBar.setMax(newMax);
            mOldItemCount = itemCount;
        } else {
            Log.i(TAG, "setSeekbarMax: max unchanged=" + oldMax);
        }
        return itemCount;
    }

    /**
     * initialize text on graduations if enabled
     */
    private void setupGraduations() {
        if(mEnableGrids) {
            final int numCards = mSeekBar.getMax() / mSeekbarMultiplier;

            String maxChapterStr = getChapterSlug(numCards - 1);
            int maxChapter = Integer.valueOf(maxChapterStr);

            // Set up visibility of the graduation bar.
            // Display graduations evenly spaced by number of chapters (but not more than the number
            // of chapters that exist). As a special case, display nothing if there's only one chapter.
            // Also, show nothing unless we're in read mode, since the other modes are indexed by
            // frame, not by chapter, so displaying either frame numbers or chapter numbers would be
            // nonsensical.
            int numVisibleGraduations = Math.min(numCards, mGraduations.getChildCount());

            if ((maxChapter > 0) && (maxChapter < numVisibleGraduations)) {
                numVisibleGraduations = maxChapter;
            }

            if (numVisibleGraduations < 2) {
                numVisibleGraduations = 0;
            }

            // Set up the visible chapters.
            for (int i = 0; i < numVisibleGraduations; ++i) {
                ViewGroup container = (ViewGroup) mGraduations.getChildAt(i);
                container.setVisibility(View.VISIBLE);
                TextView text = (TextView) container.getChildAt(1);

                int position = i * (numCards - 1) / (numVisibleGraduations - 1);
                String chapter = getChapterSlug(position);
                text.setText(chapter);
            }

            // Undisplay the invisible chapters.
            for (int i = numVisibleGraduations; i < mGraduations.getChildCount(); ++i) {
                mGraduations.getChildAt(i).setVisibility(View.GONE);
            }
        }
    }

    /**
     * get the chapter slug for the position
     * @param position
     * @return
     */
    private String getChapterSlug(int position) {
        if( (mFragment != null) && (mFragment instanceof ViewModeFragment)) {
            return ((ViewModeFragment) mFragment).getChapterSlug(position);
        }
        return Integer.toString(position + 1);
    }

    /**
     * user has selected to update sources
     */
    public void onUpdateSources() {
        setResult(RESULT_DO_UPDATE);
        finish();
    }

    private boolean displaySeekBarAsInverted() {
        return mSeekBar instanceof VerticalSeekBar;
    }

    private int computeProgressFromPosition(int position) {
        int correctedProgress = correctProgress(position * mSeekbarMultiplier);
        int progress = limitRange(correctedProgress, 0, mSeekBar.getMax());
        return progress;
    }

    private int computePositionFromProgress(int progress) {
        int correctedProgress = correctProgress(progress);
        correctedProgress = limitRange(correctedProgress, 0, mSeekBar.getMax() - 1);
        int position = correctedProgress / mSeekbarMultiplier;
        return position;
    }

    /**
     * if seekbar is inverted, this will correct the progress
     * @param progress
     * @return
     */
    private int correctProgress(int progress) {
        return displaySeekBarAsInverted() ? mSeekBar.getMax() - progress : progress;
    }

    @Override
    public void onNoSourceTranslations(String targetTranslationId) {
        if (mFragment instanceof FirstTabFragment == false) {
            mFragment = new FirstTabFragment();
            mFragment.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();
            buildMenu();
        }
    }

    @Override
    public void openTranslationMode(TranslationViewMode mode, Bundle extras) {
        Bundle fragmentExtras = new Bundle();
        fragmentExtras.putAll(getIntent().getExtras());
        if (extras != null) {
            fragmentExtras.putAll(extras);
        }

        // close the keyboard when switching between modes
        if (getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        } else {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }

        App.setLastViewMode(mTargetTranslation.getId(), mode);
        setupSidebarModeIcons();

        switch (mode) {
            case READ:
                if (mFragment instanceof ReadModeFragment == false) {
                    mFragment = new ReadModeFragment();
                    mFragment.setArguments(fragmentExtras);

                    getFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();
                    // TODO: animate
                    // TODO: update menu
                }
                break;
            case CHUNK:
                if (mFragment instanceof ChunkModeFragment == false) {
                    mFragment = new ChunkModeFragment();
                    mFragment.setArguments(fragmentExtras);

                    getFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();
                    // TODO: animate
                    // TODO: update menu
                }
                break;
            case REVIEW:
                if (mFragment instanceof ReviewModeFragment == false) {
                    mFragment = new ReviewModeFragment();
                    mFragment.setArguments(fragmentExtras);

                    getFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();
                    // TODO: animate
                    // TODO: update menu
                }
                break;
        }
    }

    /**
     * Restart scheduled translation commits
     */
    public void restartAutoCommitTimer() {
        mCommitTimer.cancel();
        mCommitTimer = new Timer();
        mCommitTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(mTargetTranslation != null) {
                    try {
                        mTargetTranslation.commit();
                    } catch (Exception e) {
                        Logger.e(TargetTranslationActivity.class.getName(), "Failed to commit the latest translation of " + mTargetTranslation.getId(), e);
                    }
                } else {
                    Logger.w(TAG, "cannot auto commit target translation. The target translation is null.");
                    mCommitTimer.cancel();
                }
            }
        }, COMMIT_INTERVAL, COMMIT_INTERVAL);
    }

    /**
     * callback on search state changes
     * @param doingSearch - search is currently processing
     * @param numberOfChunkMatches - number of chunks that have the search string
     * @param atEnd - we are at last search item highlighted
     * @param atStart - we are at first search item highlighted
     */
    @Override
    public void onSearching(boolean doingSearch, int numberOfChunkMatches, boolean atEnd, boolean atStart) {
        setSearchSpinner(doingSearch, numberOfChunkMatches, atEnd, atStart);
    }

    @Override
    public void onHasSourceTranslations() {
        TranslationViewMode viewMode = App.getLastViewMode(mTargetTranslation.getId());
        if (viewMode == TranslationViewMode.READ) {
            mFragment = new ReadModeFragment();
        } else if (viewMode == TranslationViewMode.CHUNK) {
            mFragment = new ChunkModeFragment();
        } else if (viewMode == TranslationViewMode.REVIEW) {
            mFragment = new ReviewModeFragment();
        }
        mFragment.setArguments(getIntent().getExtras());
        getFragmentManager().beginTransaction().replace(R.id.fragment_container, (Fragment) mFragment).commit();
        // TODO: animate
        // TODO: update menu
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (mFragment instanceof ViewModeFragment) {
            if (!((ViewModeFragment) mFragment).onTouchEvent(event)) {
                return super.dispatchTouchEvent(event);
            } else {
                return true;
            }
        } else {
            return super.dispatchTouchEvent(event);
        }
    }

    @Override
    public void onDestroy() {
        mCommitTimer.cancel();
        try {
            mTargetTranslation.commit();
        } catch (Exception e) {
            Logger.e(this.getClass().getName(), "Failed to commit changes before closing translation", e);
        }
        App.closeKeyboard(TargetTranslationActivity.this);
        super.onDestroy();
    }

    /**
     * Causes the activity to tell the fragment it needs to reload
     */
    public void notifyDatasetChanged() {
        if (mFragment instanceof ViewModeFragment && ((ViewModeFragment) mFragment).getAdapter() != null) {
            ((ViewModeFragment) mFragment).getAdapter().triggerNotifyDataSetChanged();
        }
    }

    /**
     * Causes the activity to tell the fragment that everything needs to be redrawn
     */
    public void redrawTarget() {
        if (mFragment instanceof ViewModeFragment) {
            ((ViewModeFragment) mFragment).onResume();
        }
    }

    /**
     * Updates the visual state of all the sidebar icons to match the application's current mode.
     *
     * Call this when creating the activity or when changing modes.
     */
    private void setupSidebarModeIcons() {
        TranslationViewMode viewMode = App.getLastViewMode(mTargetTranslation.getId());

        // Set the non-highlighted icons by default.
        mReviewButton.setImageResource(R.drawable.ic_view_week_inactive_24dp);
        mChunkButton.setImageResource(R.drawable.ic_content_copy_inactive_24dp);
        mReadButton.setImageResource(R.drawable.ic_subject_inactive_24dp);

        // Clear the highlight background.
        //
        // This is more properly done with setBackground(), but that requires a higher API
        // level than this application's minimum. Equivalently use setBackgroundDrawable(),
        // which is deprecated, instead.
        mReviewButton.setBackgroundDrawable(null);
        mChunkButton.setBackgroundDrawable(null);
        mReadButton.setBackgroundDrawable(null);

        // For the active view, set the correct icon, and highlight the background.
        final int highlightedColor = getResources().getColor(R.color.primary_dark);
        switch (viewMode) {
            case READ:
                mReadButton.setImageResource(R.drawable.ic_subject_white_24dp);
                mReadButton.setBackgroundColor(highlightedColor);
                break;
            case CHUNK:
                mChunkButton.setImageResource(R.drawable.ic_content_copy_white_24dp);
                mChunkButton.setBackgroundColor(highlightedColor);
                break;
            case REVIEW:
                if(mMergeConflictFilterEnabled) {
                    mMergeConflict.setBackgroundColor(highlightedColor); // highlight background of the conflict icon
                    onEnableMergeConflict(true, true);
                } else {
                    mReviewButton.setBackgroundColor(highlightedColor);
                    mReviewButton.setImageResource(R.drawable.ic_view_week_white_24dp);
                }
                break;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SdUtils.REQUEST_CODE_STORAGE_ACCESS) {
            FileChooserActivity.showSdCardAccessResults(this, resultCode, data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private class SearchTimerTask extends TimerTask {

        private TargetTranslationActivity activity;
        private Editable searchString;

        public SearchTimerTask(TargetTranslationActivity activity, Editable searchString) {
            this.activity = activity;
            this.searchString = searchString;
        }

        @Override
        public void run() {
             activity.filter(searchString.toString());
        }
    }
}