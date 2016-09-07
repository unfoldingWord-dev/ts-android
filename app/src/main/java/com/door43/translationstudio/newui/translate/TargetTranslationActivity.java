package com.door43.translationstudio.newui.translate;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
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

import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationViewMode;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.newui.BackupDialog;
import com.door43.translationstudio.newui.FeedbackDialog;
import com.door43.translationstudio.newui.PrintDialog;
import com.door43.translationstudio.newui.draft.DraftActivity;
import com.door43.translationstudio.newui.publish.PublishActivity;
import com.door43.translationstudio.util.SdUtils;
import com.door43.widget.VerticalSeekBar;
import com.door43.widget.ViewUtil;
import com.door43.translationstudio.newui.BaseActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TargetTranslationActivity extends BaseActivity implements ViewModeFragment.OnEventListener, FirstTabFragment.OnEventListener, Spinner.OnItemSelectedListener {

    private static final String TAG = TargetTranslationActivity.class.getSimpleName();

    private static final long COMMIT_INTERVAL = 2 * 60 * 1000; // commit changes every 2 minutes
    public static final String STATE_SEARCH_ENABLED = "state_search_enabled";
    public static final int SEARCH_START_DELAY = 1000;
    public static final String STATE_SEARCH_TEXT = "state_search_text";
    public static final int TRANSLATION_SEARCH_TYPE = 1;
    private Fragment mFragment;
    private SeekBar mSeekBar;
    private ViewGroup mGraduations;
    private Translator mTranslator;
    private TargetTranslation mTargetTranslation;
    private Timer mCommitTimer = new Timer();
    private ImageButton mReadButton;
    private ImageButton mChunkButton;
    private ImageButton mReviewButton;
    private List<SourceTranslation> draftTranslations;
    private ImageButton mMoreButton;
    private boolean mSearchEnabled = false;
    private TextWatcher mSearchTextWatcher;
    private SearchTimerTask mSearchTimerTask;
    private Timer mSearchTimer;
    private String mSearchString;
    private ProgressBar mSearchingSpinner;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_target_translation_detail);

        mTranslator = App.getTranslator();

        // validate parameters
        Bundle args = getIntent().getExtras();
        final String targetTranslationId = args.getString(App.EXTRA_TARGET_TRANSLATION_ID, null);
        mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
        if (mTargetTranslation == null) {
            Logger.e(TAG ,"A valid target translation id is required. Received '" + targetTranslationId + "' but the translation could not be found");
            finish();
            return;
        }

        // open used source translations by default
        if(App.getOpenSourceTranslationIds(mTargetTranslation.getId()).length == 0) {
            String[] slugs = mTargetTranslation.getSourceTranslations();
            for (String slug : slugs) {
                SourceTranslation sourceTranslation = App.getLibrary().getSourceTranslation(slug);
                if(sourceTranslation != null) {
                    App.addOpenSourceTranslation(mTargetTranslation.getId(), sourceTranslation.getId());
                }
            }
        }

        // notify user that a draft translation exists the first time actvity starts
        if(savedInstanceState == null && draftIsAvailable() && !targetTranslationHasDraft()) {
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
        String viewModeId = args.getString(App.EXTRA_VIEW_MODE, null);
        if (viewModeId != null && TranslationViewMode.get(viewModeId) != null) {
            App.setLastViewMode(targetTranslationId, TranslationViewMode.get(viewModeId));
        }

        mSearchingSpinner = (ProgressBar) findViewById(R.id.search_progress);
        mReadButton = (ImageButton) findViewById(R.id.action_read);
        mChunkButton = (ImageButton) findViewById(R.id.action_chunk);
        mReviewButton = (ImageButton) findViewById(R.id.action_review);

        setupSidebarModeIcons();

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

        // set up menu items
        mGraduations = (ViewGroup) findViewById(R.id.action_seek_graduations);
        mSeekBar = (SeekBar) findViewById(R.id.action_seek);
        mSeekBar.setMax(100);
        mSeekBar.setProgress(computePositionFromProgress(0));
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int position;
                if (progress < 0) {
                    position = computePositionFromProgress(0);
                } else if (progress <= seekBar.getMax()) {
                    position = computePositionFromProgress(progress);
                } else {
                    position = 0;
                }

                // If this change was initiated by a click on a UI element (rather than as a result
                // of updates within the program), then update the view accordingly.
                if (mFragment instanceof ViewModeFragment && fromUser) {
                    ((ViewModeFragment) mFragment).onScrollProgressUpdate(position);
                }

                TargetTranslationActivity activity = (TargetTranslationActivity) seekBar.getContext();
                if (activity != null) {
                    activity.closeKeyboard();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mGraduations.animate().alpha(1.f);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mGraduations.animate().alpha(0.f);
            }
        });
        mMoreButton = (ImageButton) findViewById(R.id.action_more);
        buildMenu();

        mReadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeSearchBar();
                openTranslationMode(TranslationViewMode.READ, null);

                TargetTranslationActivity activity = (TargetTranslationActivity) v.getContext();
            }
        });

        mChunkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeSearchBar();
                openTranslationMode(TranslationViewMode.CHUNK, null);

                TargetTranslationActivity activity = (TargetTranslationActivity) v.getContext();
            }
        });

        mReviewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeSearchBar();
                openTranslationMode(TranslationViewMode.REVIEW, null);

                TargetTranslationActivity activity = (TargetTranslationActivity) v.getContext();
            }
        });

        if(savedInstanceState != null) {
            mSearchEnabled = savedInstanceState.getBoolean(STATE_SEARCH_ENABLED, false);
            mSearchString = savedInstanceState.getString(STATE_SEARCH_TEXT, null);
        }

        setSearchBarVisibility(mSearchEnabled);

        restartAutoCommitTimer();
    }

    public void onSaveInstanceState(Bundle out) {
        out.putBoolean(STATE_SEARCH_ENABLED, mSearchEnabled);
        String searchText = getSearchText();
        if( mSearchEnabled ) {
            out.putString(STATE_SEARCH_TEXT, searchText);
        }

        super.onSaveInstanceState(out);
    }

    private void buildMenu() {
        mMoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu moreMenu = new PopupMenu(TargetTranslationActivity.this, v);
                ViewUtil.forcePopupMenuIcons(moreMenu);
                moreMenu.getMenuInflater().inflate(R.menu.menu_target_translation_detail, moreMenu.getMenu());

                // display menu item for draft translations
                MenuItem draftsMenuItem = moreMenu.getMenu().findItem(R.id.action_drafts_available);
                draftsMenuItem.setVisible(draftIsAvailable() && !targetTranslationHasDraft());

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

    /**
     * hide search bar and clear search text
     */
    private void removeSearchBar() {
        setSearchBarVisibility(false);
        setSearchText(null);
        setSearchFilter(null); // clear search filter
    }

    /**
     * method to see if searching is supported
     */
    public boolean isSearchSupported() {
        if(mFragment != null) {
            return ((ViewModeFragment) mFragment).isSearchSupported();
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
                setSearchSpinner(false);
            }

            searchPane.setVisibility(visibility);
            mSearchEnabled = show;

            EditText edit = (EditText) searchPane.findViewById(R.id.search_text);
            if(edit != null) {
                if(mSearchTextWatcher != null) {
                    edit.removeTextChangedListener(mSearchTextWatcher); // remove old listener
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
                    edit.addTextChangedListener(mSearchTextWatcher);
                } else {
                    setSearchFilter(null); // clear search filter
                }

                if(mSearchString != null) { // restore after rotate
                    edit.setText(mSearchString);
                    if(show) {
                        setSearchFilter(mSearchString);
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

            Spinner type = (Spinner) searchPane.findViewById(R.id.search_type);
            if(type != null) {
                List<String> types = new ArrayList<String>();
                types.add(this.getResources().getString(R.string.search_source));
                types.add(this.getResources().getString(R.string.search_translation));
                ArrayAdapter<String> typesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, types);
                typesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                type.setAdapter(typesAdapter);

                type.setOnItemSelectedListener(this);
            }
        }
    }

    /**
     * sets the visible state of the search spinner
     * @param displayed
     */
    private void setSearchSpinner(boolean displayed) {
        if(mSearchingSpinner != null) {
            mSearchingSpinner.setVisibility(displayed ?  View.VISIBLE : View.GONE);
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

        setSearchFilter(getSearchText());  // do search with search string in edit control
    }

    /**
     * called if no search type is selected
     * @param parent
     */
    public void onNothingSelected(AdapterView<?> parent) {
        // do nothing
    }

    /**
     * get the type of search (position)
     */
    private int getSearchTypeSelection() {
        LinearLayout searchPane = (LinearLayout) findViewById(R.id.search_pane);
        if(searchPane != null) {

            Spinner type = (Spinner) searchPane.findViewById(R.id.search_type);
            if(type != null) {
                int pos = type.getSelectedItemPosition();
                if(pos >= 0) {
                    return pos;
                }
            }
        }

        return 0;
    }

    /**
     * get search text in search bar
     */
    private String getSearchText() {
        String text = null;

        LinearLayout searchPane = (LinearLayout) findViewById(R.id.search_pane);
        if(searchPane != null) {

            EditText edit = (EditText) searchPane.findViewById(R.id.search_text);
            if(edit != null) {
                text = edit.getText().toString();
            }
        }
        return text;
    }

    /**
     * set search text in search bar
     */
    private void setSearchText(String text) {

        if(text == null) {
            text = "";
        }

        LinearLayout searchPane = (LinearLayout) findViewById(R.id.search_pane);
        if(searchPane != null) {

            EditText edit = (EditText) searchPane.findViewById(R.id.search_text);
            if(edit != null) {
                edit.setText(text);
            }
        }
    }


    /**
     * do search with specific search string or clear search filter
     * @param search - if null or "" then search is cleared
     */
    public void setSearchFilter(final String search) {
        Log.d(TAG,"setSearchFilter: " + search);
        final String searchString = search; // save in case search string changes before search runs
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                if((mFragment != null) && (mFragment instanceof ViewModeFragment)) {
                    boolean searchTarget = getSearchTypeSelection() == TRANSLATION_SEARCH_TYPE;

                    ((ViewModeFragment) mFragment).setSearchFilter(searchString, searchTarget);
                }
             }
        });
    }

    /**
     * Checks if the target translation aleady has the best draft
     * @return
     */
    private boolean targetTranslationHasDraft() {
        return mTargetTranslation.getParentDraft() != null;
        // TODO: 1/20/2016 once users are forced to specify a resource they are translating into we'll use this to check
//        if(mTargetTranslation.getParentDraft() != null) {
//            // check for matching resource
//            if(mTargetTranslation.resourceSlug.equals(mTargetTranslation.getParentDraft().resourceSlug)) {
//                return true;
//            } else {
//                // check for second best
//                if (draftTranslations == null) {
//                    draftTranslations = App.getLibrary().getDraftTranslations(mTargetTranslation.getProjectId(), mTargetTranslation.getTargetLanguageId());
//                }
//                for (SourceTranslation st : draftTranslations) {
//                    if (st.resourceSlug.equals(mTargetTranslation.getParentDraft().resourceSlug)) {
//                        return true;
//                    }
//                }
//            }
//        }
//        return false;
    }

    /**
     * Checks if a draft is available
     *
     * @return
     */
    private boolean draftIsAvailable() {
        if(draftTranslations == null) {
            draftTranslations = App.getLibrary().getDraftTranslations(mTargetTranslation.getProjectId(), mTargetTranslation.getTargetLanguageId());
        }
        for(SourceTranslation st:draftTranslations) {
            if(App.getLibrary().sourceTranslationHasSource(st)) {
                return true;
            }
        }
        return false;
        // TODO: 1/20/2016 once users are forced to specify a resource they are translating into we'll use this to check
//        for(SourceTranslation st:draftTranslations) {
//            if(st.resourceSlug.equals(mTargetTranslation.resourceSlug) && App.getLibrary().sourceTranslationHasSource(st)) {
//                return true;
//            }
//        }
//        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        notifyDatasetChanged();
        buildMenu();
    }

    public void closeKeyboard() {
        if (mFragment instanceof ViewModeFragment) {
            ((ViewModeFragment) mFragment).closeKeyboard();
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
        mSeekBar.setProgress(computeProgressFromPosition(position));
        checkIfCursorStillOnScreen();
    }

    @Override
    public void onItemCountChanged(int itemCount, int progress) {
        mSeekBar.setMax(itemCount);
        mSeekBar.setProgress(itemCount - progress);
        closeKeyboard();
        setupGraduations();
    }

    private void setupGraduations() {
        final int numChapters = mSeekBar.getMax();
        TranslationViewMode viewMode = App.getLastViewMode(mTargetTranslation.getId());

        // Set up visibility of the graduation bar.
        // Display graduations evenly spaced by number of chapters (but not more than the number
        // of chapters that exist). As a special case, display nothing if there's only one chapter.
        // Also, show nothing unless we're in read mode, since the other modes are indexed by
        // frame, not by chapter, so displaying either frame numbers or chapter numbers would be
        // nonsensical.
        int numVisibleGraduations = Math.min(numChapters, mGraduations.getChildCount());
        if (numChapters < 2) {
            numVisibleGraduations = 0;
        }
        if (viewMode != TranslationViewMode.READ) {
            numVisibleGraduations = 0;
        }

        // Set up the visible chapters.
        for (int i = 0; i < numVisibleGraduations; ++i) {
            ViewGroup container = (ViewGroup) mGraduations.getChildAt(i);
            container.setVisibility(View.VISIBLE);
            TextView text = (TextView) container.getChildAt(1);

            // This calculation, full of fudge factors, has the following properties:
            //   - It starts at 1.
            //   - It ends with the last chapter.
            //   - It's evenly spaced in between.
            int label = 1 + i * (numChapters - 1) / (numVisibleGraduations - 1);

            text.setText(Integer.toString(label));
        }

        // Undisplay the invisible chapters.
        for (int i = numVisibleGraduations; i < mGraduations.getChildCount(); ++i) {
            mGraduations.getChildAt(i).setVisibility(View.GONE);
        }
    }

    private boolean displaySeekBarAsInverted() {
        return mSeekBar instanceof VerticalSeekBar;
    }

    private int computeProgressFromPosition(int position) {
        return displaySeekBarAsInverted() ? mSeekBar.getMax() - position : position;
    }

    private int computePositionFromProgress(int progress) {
        return displaySeekBarAsInverted() ? Math.abs(mSeekBar.getMax() - progress) : progress;
    }

    @Override
    public void onNoSourceTranslations(String targetTranslationId) {
        if (mFragment instanceof FirstTabFragment == false) {
            mFragment = new FirstTabFragment();
            mFragment.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();
            // TODO: animate
            // TODO: udpate menu
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

    @Override
    public void onSetBusyIndicator(boolean enable) {
        setSearchSpinner(enable);
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
        super.onDestroy();
    }

    /**
     * Causes the activity to tell the fragment it needs to reload
     */
    public void notifyDatasetChanged() {
        if (mFragment instanceof ViewModeFragment) {
            ((ViewModeFragment) mFragment).getAdapter().reload();
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
        mReviewButton.setImageResource(R.drawable.icon_check_inactive);
        mChunkButton.setImageResource(R.drawable.icon_frame_inactive);
        mReadButton.setImageResource(R.drawable.icon_study_inactive);

        // Clear the highlight background.
        //
        // This is more properly done with setBackground(), but that requires a higher API
        // level than this application's minimum. Equivalently use setBackgroundDrawable(),
        // which is deprecated, instead.
        mReviewButton.setBackgroundDrawable(null);
        mChunkButton.setBackgroundDrawable(null);
        mReadButton.setBackgroundDrawable(null);

        // For the active view, set the correct icon, and highlight the background.
        final int backgroundColor = getResources().getColor(R.color.primary_dark);
        switch (viewMode) {
            case READ:
                mReadButton.setImageResource(R.drawable.icon_study_active);
                mReadButton.setBackgroundColor(backgroundColor);
                break;
            case CHUNK:
                mChunkButton.setImageResource(R.drawable.icon_frame_active);
                mChunkButton.setBackgroundColor(backgroundColor);
                break;
            case REVIEW:
                mReviewButton.setImageResource(R.drawable.icon_check_active);
                mReviewButton.setBackgroundColor(backgroundColor);
                break;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SdUtils.REQUEST_CODE_STORAGE_ACCESS) {
            Uri treeUri = null;
            String msg = "";
            if (resultCode == Activity.RESULT_OK) {

                // Get Uri from Storage Access Framework.
                treeUri = data.getData();
                final int takeFlags = data.getFlags();
                boolean success = SdUtils.validateSdCardWriteAccess(treeUri, takeFlags);
                if (!success) {
                    String template = getResources().getString(R.string.access_failed);
                    msg = String.format(template, treeUri.toString());
                } else {
                    msg = getResources().getString(R.string.access_granted_backup);
                }
            } else {
                msg = getResources().getString(R.string.access_skipped);
            }
            new AlertDialog.Builder(this, R.style.AppTheme_Dialog)
                    .setTitle(R.string.access_title)
                    .setMessage(msg)
                    .setPositiveButton(R.string.label_ok, null)
                    .show();
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
             activity.setSearchFilter(searchString.toString());
        }
    }
}