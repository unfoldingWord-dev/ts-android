package com.door43.translationstudio.newui.draft;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draft_preview);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
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

        // TODO: 1/19/2016 display draft translation with tabs if needed
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