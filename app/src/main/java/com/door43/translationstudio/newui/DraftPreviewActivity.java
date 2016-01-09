package com.door43.translationstudio.newui;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.newui.translate.FirstTabFragment;
import com.door43.translationstudio.newui.translate.ReadModeFragment;
import com.door43.translationstudio.newui.translate.TargetTranslationActivity;
import com.door43.translationstudio.newui.translate.ViewModeFragment;

import java.security.InvalidParameterException;

public class DraftPreviewActivity extends BaseActivity {
    public static final int VERIFY_EDIT_OF_DRAFT = 142;
    public static final String EXTRA_SOURCE_TRANSLATION_ID = "extra_source_translation_id";
    private String mDraftTranslation;
    private TargetTranslation mTargetTranslation;
    private Fragment mFragment;
    private Translator mTranslator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draft_preview);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        mDraftTranslation = intent.getType();

        mTranslator = AppContext.getTranslator();

        // validate parameters
        Bundle args = getIntent().getExtras();
        final String sourceTranslationId = args.getString(EXTRA_SOURCE_TRANSLATION_ID, null);
        SourceTranslation sourceTranslation = SourceTranslation.simple(sourceTranslationId);
        String projectId = SourceTranslation.getProjectIdFromId(sourceTranslationId);
        String sourceLanguageId = SourceTranslation.getSourceLanguageIdFromId(sourceTranslationId);
        mTargetTranslation = AppContext.findExistingTargetTranslation(projectId, sourceLanguageId);
        if (mTargetTranslation == null) {
            throw new InvalidParameterException("a valid target translation id is required");
        }

        // TODO: 1/9/2016 - need to create draft preview fragment
        
        if(findViewById(R.id.fragment) != null) {
            if(savedInstanceState != null) {
                mFragment = getFragmentManager().findFragmentById(R.id.fragment);
            } else {
                mFragment = new ReadModeFragment();
                args.putString(TargetTranslationActivity.EXTRA_TARGET_TRANSLATION_ID,mTargetTranslation.getId());
                ((ReadModeFragment) mFragment).setArguments(args);
                getFragmentManager().beginTransaction().add(R.id.fragment, (ReadModeFragment) mFragment).commit();
            }
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                returnUserSelection(true);
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /**
     * notify calling activity that user has cancelled
     */
    private void returnUserSelection(boolean useDraft) {
        Intent intent = getIntent();
        intent.setData(null);
        intent.setType(mDraftTranslation);
        setResult(useDraft ? RESULT_OK : RESULT_CANCELED, intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                returnUserSelection(false);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
}