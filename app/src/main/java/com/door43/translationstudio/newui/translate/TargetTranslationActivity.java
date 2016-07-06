package com.door43.translationstudio.newui.translate;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.text.Layout;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.SeekBar;
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

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import it.moondroid.seekbarhint.library.SeekBarHint;

public class TargetTranslationActivity extends BaseActivity implements ViewModeFragment.OnEventListener, FirstTabFragment.OnEventListener {

    private static final String TAG = TargetTranslationActivity.class.getSimpleName();

    private static final long COMMIT_INTERVAL = 2 * 60 * 1000; // commit changes every 2 minutes
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
    private boolean enableGrids = false;
    private int seekbarMultiplier = 1; // allows for more granularity in setting position if cards are few

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
        if(enableGrids) {
            mGraduations = (ViewGroup) findViewById(R.id.action_seek_graduations);
        }
        mSeekBar = (SeekBar) findViewById(R.id.action_seek);
        mSeekBar.setMax(100 * seekbarMultiplier);
        mSeekBar.setProgress(computePositionFromProgress(0));
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int correctedProgress = correctProgress(progress);
                correctedProgress = limitRange(correctedProgress, 0, mSeekBar.getMax() - 1);
                int position = correctedProgress / seekbarMultiplier;
                int percentage = 0;

                if(seekbarMultiplier > 1) { // if we need some granularity, calculate fractional amount
                    int fractional = correctedProgress - position * seekbarMultiplier;
                    if(fractional != 0) {
                        percentage = 100 * fractional / seekbarMultiplier;
                    }
                }

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

        mMoreButton = (ImageButton) findViewById(R.id.action_more);
        buildMenu();

        mReadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openTranslationMode(TranslationViewMode.READ, null);

                TargetTranslationActivity activity = (TargetTranslationActivity) v.getContext();
            }
        });

        mChunkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openTranslationMode(TranslationViewMode.CHUNK, null);

                TargetTranslationActivity activity = (TargetTranslationActivity) v.getContext();
            }
        });

        mReviewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openTranslationMode(TranslationViewMode.REVIEW, null);

                TargetTranslationActivity activity = (TargetTranslationActivity) v.getContext();
            }
        });

        restartAutoCommitTimer();
    }

    private int limitRange(int value, int min, int max) {
        int newValue = value;
        if(newValue < min) {
            newValue = min;
        }
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
        int chapter = position + 1; // chapters start at 1
        String formattedText = String.format(" %02d ", chapter);
        return formattedText;
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
                        }
                        return false;
                    }
                });
                moreMenu.show();
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
        final int minimumStepSize = 300;

        if(itemCount < 1) { // sanity check
            itemCount = 0;
        }

        if(itemCount < minimumStepSize) {  // increase step size if number of cards is small, this gives more granularity in positioning
            seekbarMultiplier = (int) (minimumStepSize / itemCount) + 1;
        } else {
            seekbarMultiplier = 1;
        }

        mSeekBar.setMax(itemCount * seekbarMultiplier);
        mSeekBar.setProgress((itemCount - progress) * seekbarMultiplier);
        closeKeyboard();
        setupGraduations();
    }

    private void setupGraduations() {
        if(enableGrids) {
            final int numCards = mSeekBar.getMax() / seekbarMultiplier;

            String maxChapterStr = getChapterID(numCards - 1);
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
                String chapter = getChapterID(position);
                text.setText(chapter);
            }

            // Undisplay the invisible chapters.
            for (int i = numVisibleGraduations; i < mGraduations.getChildCount(); ++i) {
                mGraduations.getChildAt(i).setVisibility(View.GONE);
            }
        }
    }

    /**
     * get the chapter ID for the position
     * @param position
     * @return
     */
    private String getChapterID(int position) {
        if( (mFragment != null) && (mFragment instanceof ViewModeFragment)) {
            return ((ViewModeFragment) mFragment).getChapterID(position);
        }

        String chapterID = Integer.toString(position + 1);
        return chapterID;
    }


    private boolean displaySeekBarAsInverted() {
        return mSeekBar instanceof VerticalSeekBar;
    }

    private int computeProgressFromPosition(int position) {
        int correctedProgress = correctProgress(position * seekbarMultiplier);
        int progress = limitRange(correctedProgress, 0, mSeekBar.getMax());
        return progress;
    }

    private int computePositionFromProgress(int progress) {
        int correctedProgress = correctProgress(progress);
        correctedProgress = limitRange(correctedProgress, 0, mSeekBar.getMax() - 1);
        int position = correctedProgress / seekbarMultiplier;
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
}