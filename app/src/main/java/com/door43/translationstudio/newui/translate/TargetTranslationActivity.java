package com.door43.translationstudio.newui.translate;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.SeekBar;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.SharingActivity;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationViewMode;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.git.tasks.repo.CommitTask;
import com.door43.translationstudio.newui.ReportBugDialog;
import com.door43.translationstudio.newui.publish.PublishActivity;
import com.door43.translationstudio.util.AppContext;
import com.door43.widget.VerticalSeekBar;
import com.door43.widget.ViewUtil;

import java.security.InvalidParameterException;
import java.util.Timer;
import java.util.TimerTask;

public class TargetTranslationActivity extends AppCompatActivity implements ViewModeFragment.OnEventListener, FirstTabFragment.OnEventListener {

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
            mTranslator.setViewMode(targetTranslationId, TranslationViewMode.get(viewModeId));
        }

        // inject fragments
        if(findViewById(R.id.fragment_container) != null) {
            if(savedInstanceState != null) {
                mFragment = getFragmentManager().findFragmentById(R.id.fragment_container);
            } else {
                TranslationViewMode viewMode = mTranslator.getViewMode(mTargetTranslation.getId());
                if(viewMode == TranslationViewMode.READ) {
                    mFragment = new ReadModeFragment();
                } else if(viewMode == TranslationViewMode.CHUNK) {
                    mFragment = new ChunkModeFragment();
                } else if(viewMode == TranslationViewMode.REVIEW) {
                    mFragment = new ReviewModeFragment();
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
                if(progress < 0) {
                    position = seekBar.getMax();
                } else if(progress <= seekBar.getMax()) {
                    position = Math.abs(progress - seekBar.getMax());
                } else {
                    position = 0;
                }
                if(mFragment instanceof ViewModeFragment) {
                    ((ViewModeFragment)mFragment).onScrollProgressUpdate(position);
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
                        switch(item.getItemId()) {
                            case R.id.action_translations:
                                finish();
                                return true;
                            case R.id.action_publish:
                                Intent publishIntent = new Intent(TargetTranslationActivity.this, PublishActivity.class);
                                publishIntent.putExtra(PublishActivity.EXTRA_TARGET_TRANSLATION_ID, mTargetTranslation.getId());
                                startActivity(publishIntent);
                                return true;
                            case R.id.action_backup:
                                // TODO: need new ui
                                return true;
                            case R.id.action_share:
                                Intent shareIntent = new Intent(TargetTranslationActivity.this, SharingActivity.class);
                                startActivity(shareIntent);
                                return true;
                            case R.id.action_bug:
                                FragmentTransaction ft = getFragmentManager().beginTransaction();
                                Fragment prev = getFragmentManager().findFragmentByTag("bugDialog");
                                if (prev != null) {
                                    ft.remove(prev);
                                }
                                ft.addToBackStack(null);

                                ReportBugDialog dialog = new ReportBugDialog();
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
        ImageButton readButton = (ImageButton)findViewById(R.id.action_read);
        readButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mFragment instanceof ReadModeFragment == false) {
                    mTranslator.setViewMode(mTargetTranslation.getId(), TranslationViewMode.READ);
                    mFragment = new ReadModeFragment();
                    mFragment.setArguments(getIntent().getExtras());
                    getFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();
                    // TODO: animate
                    // TODO: udpate menu
                }
            }
        });

        ImageButton chunkButton = (ImageButton)findViewById(R.id.action_chunk);
        chunkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mFragment instanceof  ChunkModeFragment == false) {
                    mTranslator.setViewMode(mTargetTranslation.getId(), TranslationViewMode.CHUNK);
                    mFragment = new ChunkModeFragment();
                    mFragment.setArguments(getIntent().getExtras());
                    getFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();
                    // TODO: animate
                    // TODO: udpate menu
                }
            }
        });

        ImageButton reviewButton = (ImageButton)findViewById(R.id.action_review);
        reviewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mFragment instanceof  ReviewModeFragment == false) {
                    mTranslator.setViewMode(mTargetTranslation.getId(), TranslationViewMode.REVIEW);
                    mFragment = new ReviewModeFragment();
                    mFragment.setArguments(getIntent().getExtras());
                    getFragmentManager().beginTransaction().replace(R.id.fragment_container, mFragment).commit();
                    // TODO: animate
                    // TODO: udpate menu
                }
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
    public void onHasSourceTranslations() {
        TranslationViewMode viewMode = mTranslator.getViewMode(mTargetTranslation.getId());
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
