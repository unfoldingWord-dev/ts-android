package com.door43.translationstudio.newui.translate;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.SeekBar;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationViewMode;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.git.tasks.repo.CommitTask;
import com.door43.translationstudio.newui.FeedbackDialog;
import com.door43.translationstudio.newui.publish.PublishActivity;
import com.door43.translationstudio.AppContext;
import com.door43.widget.VerticalSeekBar;
import com.door43.widget.ViewUtil;
import com.door43.translationstudio.newui.BaseActivity;

import java.security.InvalidParameterException;
import java.util.Timer;
import java.util.TimerTask;

public class TargetTranslationActivity extends BaseActivity implements ViewModeFragment.OnEventListener, FirstTabFragment.OnEventListener {

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

        TranslationViewMode viewMode = AppContext.getLastViewMode(mTargetTranslation.getId());

        // inject fragments
        if(findViewById(R.id.fragment_container) != null) {
            if(savedInstanceState != null) {
                mFragment = getFragmentManager().findFragmentById(R.id.fragment_container);
            } else {
                viewMode = AppContext.getLastViewMode(mTargetTranslation.getId());
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

        // highlight tabs
        switch (viewMode) {
            case READ:
                mReadButton.setImageResource(R.drawable.icon_study_active);
                break;
            case CHUNK:
                mChunkButton.setImageResource(R.drawable.icon_frame_active);
                break;
            case REVIEW:
                mReviewButton.setImageResource(R.drawable.ic_assignment_turned_in_white_24dp);
                break;
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
                                // TODO: need new ui
                                Snackbar snackBackup = Snackbar.make(findViewById(android.R.id.content), "Coming soon", Snackbar.LENGTH_SHORT);
                                ViewUtil.setSnackBarTextColor(snackBackup, getResources().getColor(R.color.light_primary_text));
                                snackBackup.show();
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
                openTranslationMode(TranslationViewMode.READ);
            }
        });

        mChunkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openTranslationMode(TranslationViewMode.CHUNK);
            }
        });

        mReviewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openTranslationMode(TranslationViewMode.REVIEW);
            }
        });

        // schedule translation commits
        mCommitTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                mTargetTranslation.commit(new CommitTask.OnAddComplete() {
                    @Override
                    public void success() {
                        Logger.i(TargetTranslationActivity.class.getName(), "Committed the latest translation for " + targetTranslationId);
                    }

                    @Override
                    public void error(Throwable e) {
                        Logger.e(TargetTranslationActivity.class.getName(), "Failed to commit the latest translation of " + targetTranslationId, e);
                    }
                });
            }
        }, COMMIT_INTERVAL, COMMIT_INTERVAL);
    }

    @Override
    public void onScrollProgress(int progress) {
        mSeekBar.setProgress(mSeekBar.getMax() - progress);
    }

    @Override
    public void onItemCountChanged(int itemCount, int progress) {
        mSeekBar.setMax(itemCount);
        mSeekBar.setProgress(itemCount - progress);
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
    public void openTranslationMode(TranslationViewMode mode) {
        // close the keyboard when switching between modes
        if(getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        } else {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }

        mReviewButton.setImageResource(R.drawable.ic_assignment_turned_in_inactive_24dp);
        mChunkButton.setImageResource(R.drawable.icon_frame_inactive);
        mReadButton.setImageResource(R.drawable.icon_study_inactive);

        switch (mode) {
            case READ:
                mReadButton.setImageResource(R.drawable.icon_study_active);
                AppContext.setLastViewMode(mTargetTranslation.getId(), TranslationViewMode.READ);
                if(mFragment instanceof ReadModeFragment == false) {
                    mFragment = new ReadModeFragment();
                    mFragment.setArguments(getIntent().getExtras());
                    getFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();
                    // TODO: animate
                    // TODO: udpate menu
                }
                break;
            case CHUNK:
                mChunkButton.setImageResource(R.drawable.icon_frame_active);
                AppContext.setLastViewMode(mTargetTranslation.getId(), TranslationViewMode.CHUNK);
                if(mFragment instanceof  ChunkModeFragment == false) {
                    mFragment = new ChunkModeFragment();
                    mFragment.setArguments(getIntent().getExtras());
                    getFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();
                    // TODO: animate
                    // TODO: udpate menu
                }
                break;
            case REVIEW:
                mReviewButton.setImageResource(R.drawable.ic_assignment_turned_in_white_24dp);
                AppContext.setLastViewMode(mTargetTranslation.getId(), TranslationViewMode.REVIEW);
                if(mFragment instanceof  ReviewModeFragment == false) {
                    mFragment = new ReviewModeFragment();
                    mFragment.setArguments(getIntent().getExtras());
                    getFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();
                    // TODO: animate
                    // TODO: udpate menu
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
        mTargetTranslation.commit(null);
        super.onDestroy();
    }
}
