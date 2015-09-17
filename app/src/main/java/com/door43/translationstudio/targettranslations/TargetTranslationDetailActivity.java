package com.door43.translationstudio.targettranslations;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.SeekBar;

import com.door43.translationstudio.BugReporterActivity;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.SharingActivity;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationViewMode;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.util.AppContext;
import com.door43.widget.VerticalSeekBar;
import com.door43.widget.ViewUtil;

import java.security.InvalidParameterException;

public class TargetTranslationDetailActivity extends AppCompatActivity implements TargetTranslationDetailFragmentListener {

    public static final String EXTRA_TARGET_TRANSLATION_ID = "extra_target_translation_id";
    private TargetTranslationDetailActivityListener mFragment;
    private VerticalSeekBar mSeekBar;
    private Translator mTranslator;
    private TargetTranslation mTargetTranslation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_target_translation_detail);

        mTranslator = AppContext.getTranslator();

        // validate parameters
        Bundle args = getIntent().getExtras();
        String targetTranslationId = args.getString(TargetTranslationDetailActivity.EXTRA_TARGET_TRANSLATION_ID, null);
        mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
        if(mTargetTranslation == null) {
            throw new InvalidParameterException("a valid target translation id is required");
        }

        // inject fragments
        if(findViewById(R.id.fragment_container) != null) {
            if(savedInstanceState != null) {
                mFragment = (TargetTranslationDetailActivityListener)getFragmentManager().findFragmentById(R.id.fragment_container);
            } else {
                TranslationViewMode viewMode = mTranslator.getViewMode(mTargetTranslation.getId());
                if(viewMode == TranslationViewMode.READ) {
                    mFragment = new ReadModeFragment();
                    ((Fragment)mFragment).setArguments(getIntent().getExtras());
                } else if(viewMode == TranslationViewMode.CHUNK) {
                    mFragment = new ChunkModeFragment();
                    ((ChunkModeFragment) mFragment).setArguments(getIntent().getExtras());
                } else if(viewMode == TranslationViewMode.REVIEW) {
                    // TODO: set up fragment
                }

                getFragmentManager().beginTransaction().add(R.id.fragment_container, (Fragment) mFragment).commit();
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
                mFragment.onScrollProgressUpdate(position);
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
                PopupMenu moreMenu = new PopupMenu(TargetTranslationDetailActivity.this, v);
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
                                // TODO: need new ui
                                return true;
                            case R.id.action_backup:
                                // TODO: need new ui
                                return true;
                            case R.id.action_share:
                                Intent shareIntent = new Intent(TargetTranslationDetailActivity.this, SharingActivity.class);
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
                                Intent settingsIntent = new Intent(TargetTranslationDetailActivity.this, SettingsActivity.class);
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
                    ((Fragment) mFragment).setArguments(getIntent().getExtras());
                    getFragmentManager().beginTransaction().replace(R.id.fragment_container, (Fragment) mFragment).commit();
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
                    ((Fragment) mFragment).setArguments(getIntent().getExtras());
                    getFragmentManager().beginTransaction().replace(R.id.fragment_container, (Fragment) mFragment).commit();
                    // TODO: animate
                    // TODO: udpate menu
                }
            }
        });

        ImageButton reviewButton = (ImageButton)findViewById(R.id.action_review);
        reviewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                mTranslator.setViewMode(mTargetTranslation.getId(), TranslationViewMode.REVIEW);
                // TODO: swap fragments
                // TODO: udpate menu
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_target_translation_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
        mFragment = new FirstTabFragment();
        ((FirstTabFragment) mFragment).setArguments(getIntent().getExtras());
        getFragmentManager().beginTransaction().replace(R.id.fragment_container, (Fragment) mFragment).commit();
        // TODO: animate
        // TODO: udpate menu
    }

    @Override
    public void invalidateViewMode() {
        TranslationViewMode viewMode = mTranslator.getViewMode(mTargetTranslation.getId());
        if(viewMode == TranslationViewMode.READ) {
            mFragment = new ReadModeFragment();
            ((Fragment)mFragment).setArguments(getIntent().getExtras());
        } else if(viewMode == TranslationViewMode.CHUNK) {
            mFragment = new ChunkModeFragment();
            ((ChunkModeFragment) mFragment).setArguments(getIntent().getExtras());
        } else if(viewMode == TranslationViewMode.REVIEW) {
            // TODO: set up fragment
        }

        getFragmentManager().beginTransaction().replace(R.id.fragment_container, (Fragment) mFragment).commit();
        // TODO: animate
        // TODO: update menu
    }
}
