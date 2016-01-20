package com.door43.translationstudio.newui.draft;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationViewMode;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.newui.BaseActivity;
import com.door43.translationstudio.newui.translate.ViewModeFragment;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;


public class DraftActivity extends BaseActivity{
    public static final String TAG = "DraftActivity";
    public static final String EXTRA_TARGET_TRANSLATION_ID = "target_translation_id";
    private TargetTranslation mTargetTranslation;
    private List<SourceTranslation> mDraftTranslations = new ArrayList<>();
    private Translator mTranslator;
    private Library mLibrary;
    private RecyclerView mRecylerView;
    private LinearLayoutManager mLayoutManager;
    private DraftAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draft_preview);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mTranslator = AppContext.getTranslator();
        mLibrary = AppContext.getLibrary();

        // validate parameters
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            String targetTranslationId = extras.getString(EXTRA_TARGET_TRANSLATION_ID, null);
            mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
            if(mTargetTranslation != null) {
                mDraftTranslations = mLibrary.getDraftTranslations(mTargetTranslation.getProjectId(), mTargetTranslation.getTargetLanguageId());
            } else {
                throw new InvalidParameterException("a valid target translation id is required");
            }
        } else {
            throw new InvalidParameterException("This activity expects some arguments");
        }

        if(mDraftTranslations.size() == 0) {
            finish();
            return;
        }

        mRecylerView = (RecyclerView)findViewById(R.id.recycler_view);
        mLayoutManager = new LinearLayoutManager(this);
        mRecylerView.setLayoutManager(mLayoutManager);
        mRecylerView.setItemAnimator(new DefaultItemAnimator());
        // TODO: 1/20/2016 we need to place in the correct draft translation. Need to have tabs
        mAdapter = new DraftAdapter(this, mDraftTranslations.get(0));
        mRecylerView.setAdapter(mAdapter);

        FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CustomAlertDialog.Create(DraftActivity.this)
                        .setTitle(R.string.import_draft)
                        .setMessage(R.string.import_draft_confirmation)
                        .setNegativeButton(R.string.menu_cancel, null)
                        .setPositiveButton(R.string.label_import, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // // TODO: 1/20/2016 use the correct draft translation
                                // TODO: 1/20/2016 run this in a task
                                mTranslator.importDraftTranslation(mDraftTranslations.get(0), mLibrary);
                                finish();
                            }
                        })
                        .show("confirm_draft_import");
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}