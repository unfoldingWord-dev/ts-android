package com.door43.translationstudio.newui.translate;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.text.Layout;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.SeekBar;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationViewMode;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.newui.BackupDialog;
import com.door43.translationstudio.newui.FeedbackDialog;
import com.door43.translationstudio.newui.publish.PublishActivity;
import com.door43.translationstudio.AppContext;
import com.door43.widget.VerticalSeekBar;
import com.door43.widget.ViewUtil;
import com.door43.translationstudio.newui.BaseActivity;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.Timer;
import java.util.TimerTask;

public class TargetTranslationActivity extends BaseActivity implements ViewModeFragment.OnEventListener, FirstTabFragment.OnEventListener {

    private static final String TAG = TargetTranslationActivity.class.getSimpleName();

    public static final String EXTRA_TARGET_TRANSLATION_ID = "extra_target_translation_id";
    private static final long COMMIT_INTERVAL = 2 * 60 * 1000; // commit changes every 2 minutes
    public static final String EXTRA_CHAPTER_ID = "extra_chapter_id";
    public static final String EXTRA_FRAME_ID = "extra_frame_id";
    public static final String EXTRA_VIEW_MODE = "extra_view_mode_id";
    private Fragment mFragment;
    private VerticalSeekBar mSeekBar;
    private Translator mTranslator;
    private TargetTranslation mTargetTranslation;
    private Timer mCommitTimer = new Timer();
    private ImageButton mReadButton;
    private ImageButton mChunkButton;
    private ImageButton mReviewButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_target_translation_detail);

        mTranslator = AppContext.getTranslator();

        // validate parameters
        Bundle args = getIntent().getExtras();
        final String targetTranslationId = args.getString(TargetTranslationActivity.EXTRA_TARGET_TRANSLATION_ID, null);
        mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
        if(mTargetTranslation == null) {
            throw new InvalidParameterException("a valid target translation id is required");
        }

        // manual location settings
        String viewModeId = args.getString(TargetTranslationActivity.EXTRA_VIEW_MODE, null);
        if(viewModeId != null && TranslationViewMode.get(viewModeId) != null) {
            AppContext.setLastViewMode(targetTranslationId, TranslationViewMode.get(viewModeId));
        }

        mReadButton = (ImageButton)findViewById(R.id.action_read);
        mChunkButton = (ImageButton)findViewById(R.id.action_chunk);
        mReviewButton = (ImageButton)findViewById(R.id.action_review);

        setupSidebarModeIcons();

        // inject fragments
        if(findViewById(R.id.fragment_container) != null) {
            if(savedInstanceState != null) {
                mFragment = getFragmentManager().findFragmentById(R.id.fragment_container);
            } else {
                TranslationViewMode viewMode = AppContext.getLastViewMode(mTargetTranslation.getId());
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
        mSeekBar = (VerticalSeekBar)findViewById(R.id.action_seek);
        mSeekBar.setMax(100);
        mSeekBar.setProgress(mSeekBar.getMax());
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int position;
                if (progress < 0) {
                    position = seekBar.getMax();
                } else if (progress <= seekBar.getMax()) {
                    position = Math.abs(progress - seekBar.getMax());
                } else {
                    position = 0;
                }
                if (mFragment instanceof ViewModeFragment) {
                    ((ViewModeFragment) mFragment).onScrollProgressUpdate(position);
                }

                TargetTranslationActivity activity = (TargetTranslationActivity) seekBar.getContext();
                if(activity != null) {
                    activity.closeKeyboard();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        ImageButton moreButton = (ImageButton)findViewById(R.id.action_more);
        moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu moreMenu = new PopupMenu(TargetTranslationActivity.this, v);
                ViewUtil.forcePopupMenuIcons(moreMenu);
                moreMenu.getMenuInflater().inflate(R.menu.menu_target_translation_detail, moreMenu.getMenu());
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
                            case R.id.action_backup:
                                FragmentTransaction backupFt = getFragmentManager().beginTransaction();
                                Fragment backupPrev = getFragmentManager().findFragmentByTag("backupDialog");
                                if (backupPrev != null) {
                                    backupFt.remove(backupPrev);
                                }
                                backupFt.addToBackStack(null);

                                BackupDialog backupDialog = new BackupDialog();
                                Bundle args = new Bundle();
                                args.putString(BackupDialog.ARG_TARGET_TRANSLATION_ID, mTargetTranslation.getId());
                                backupDialog.setArguments(args);
                                backupDialog.show(backupFt, "backupDialog");
                                return true;
                            case R.id.action_print:
                                File exportFile = new File(AppContext.getSharingDir(), targetTranslationId + ".pdf");
                                try {
                                    SourceTranslation sourceTranslation = AppContext.getLibrary().getDefaultSourceTranslation(mTargetTranslation.getProjectId(), "en");
                                    mTranslator.exportPdf(mTargetTranslation, sourceTranslation.getFormat(), exportFile);
                                    if (exportFile.exists()) {
                                        Uri u = FileProvider.getUriForFile(TargetTranslationActivity.this, "com.door43.translationstudio.fileprovider", exportFile);
                                        Intent i = new Intent(Intent.ACTION_SEND);
                                        i.setType("application/pdf");
                                        i.putExtra(Intent.EXTRA_STREAM, u);
                                        startActivity(Intent.createChooser(i, "Print:"));
                                    } else {
                                        Snackbar snack = Snackbar.make(TargetTranslationActivity.this.findViewById(android.R.id.content), R.string.translation_export_failed, Snackbar.LENGTH_LONG);
                                        ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                                        snack.show();
                                    }
                                } catch (Exception e) {
                                    Logger.e(TargetTranslationActivity.class.getName(), "Failed to export as pdf " + targetTranslationId, e);
                                    Snackbar snack = Snackbar.make(TargetTranslationActivity.this.findViewById(android.R.id.content), R.string.translation_export_failed, Snackbar.LENGTH_LONG);
                                    ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                                    snack.show();
                                }
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

        // schedule translation commits
        mCommitTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    mTargetTranslation.commit();
                } catch (Exception e) {
                    Logger.e(TargetTranslationActivity.class.getName(), "Failed to commit the latest translation of " + targetTranslationId, e);
                }
            }
        }, COMMIT_INTERVAL, COMMIT_INTERVAL);
    }

    public void closeKeyboard() {
        if(mFragment instanceof ViewModeFragment) {
            ((ViewModeFragment) mFragment).closeKeyboard();
        }
    }

    public void checkIfCursorStillOnScreen() {

        Rect cursorPos = getCursorPositionOnScreen();
        if(cursorPos != null) {

            View scrollView = findViewById(R.id.fragment_container);
            if(scrollView != null) {

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

            if(focusedView instanceof  EditText) {

                // getting relative cursor position
                EditText editText = (EditText) focusedView;
                int pos = editText.getSelectionStart();
                Layout layout = editText.getLayout();
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

        return null;
    }

    @Override
    public void onScrollProgress(int progress) {
        mSeekBar.setProgress(mSeekBar.getMax() - progress);
        Log.d(TAG, "onScrollProgress: " + progress);
        checkIfCursorStillOnScreen();
    }

    @Override
    public void onItemCountChanged(int itemCount, int progress) {
        mSeekBar.setMax(itemCount);
        mSeekBar.setProgress(itemCount - progress);
        closeKeyboard();
    }

    @Override
    public void onNoSourceTranslations(String targetTranslationId) {
        if(mFragment instanceof FirstTabFragment == false) {
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
        if(extras != null) {
            fragmentExtras.putAll(extras);
        }

        // close the keyboard when switching between modes
        if(getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        } else {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }

        AppContext.setLastViewMode(mTargetTranslation.getId(), mode);
        setupSidebarModeIcons();

        switch (mode) {
            case READ:
                if(mFragment instanceof ReadModeFragment == false) {
                    mFragment = new ReadModeFragment();
                    mFragment.setArguments(fragmentExtras);

                    getFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();
                    // TODO: animate
                    // TODO: update menu
                }
                break;
            case CHUNK:
                if(mFragment instanceof  ChunkModeFragment == false) {
                    mFragment = new ChunkModeFragment();
                    mFragment.setArguments(fragmentExtras);

                    getFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();
                    // TODO: animate
                    // TODO: update menu
                }
                break;
            case REVIEW:
                if(mFragment instanceof  ReviewModeFragment == false) {
                    mFragment = new ReviewModeFragment();
                    mFragment.setArguments(fragmentExtras);

                    getFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();
                    // TODO: animate
                    // TODO: update menu
                }
                break;
        }

    }

    @Override
    public void onHasSourceTranslations() {
        TranslationViewMode viewMode = AppContext.getLastViewMode(mTargetTranslation.getId());
        if(viewMode == TranslationViewMode.READ) {
            mFragment = new ReadModeFragment();
        } else if(viewMode == TranslationViewMode.CHUNK) {
            mFragment = new ChunkModeFragment();
        } else if(viewMode == TranslationViewMode.REVIEW) {
            mFragment = new ReviewModeFragment();
        }
        mFragment.setArguments(getIntent().getExtras());
        getFragmentManager().beginTransaction().replace(R.id.fragment_container, (Fragment) mFragment).commit();
        // TODO: animate
        // TODO: update menu
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if(mFragment instanceof ViewModeFragment) {
            if(!((ViewModeFragment)mFragment).onTouchEvent(event)) {
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
        if(mFragment instanceof ViewModeFragment) {
            ((ViewModeFragment)mFragment).getAdapter().reload();
        }
    }

    /**
     * Updates the visual state of all the sidebar icons to match the application's current mode.
     *
     * Call this when creating the activity or when changing modes.
     */
    private void setupSidebarModeIcons() {
        TranslationViewMode viewMode = AppContext.getLastViewMode(mTargetTranslation.getId());

        // Set the non-highlighted icons by default.
        mReviewButton.setImageResource(R.drawable.ic_assignment_turned_in_inactive_24dp);
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
                mReviewButton.setImageResource(R.drawable.ic_assignment_turned_in_white_24dp);
                mReviewButton.setBackgroundColor(backgroundColor);
                break;
        }
    }
}
