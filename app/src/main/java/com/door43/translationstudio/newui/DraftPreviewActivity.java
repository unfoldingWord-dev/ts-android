package com.door43.translationstudio.newui;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationViewMode;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.newui.translate.PreviewModeFragment;
import com.door43.translationstudio.newui.translate.ViewModeFragment;

import java.security.InvalidParameterException;


public class DraftPreviewActivity extends BaseActivity implements ViewModeFragment.OnEventListener {
    public static final String TAG = DraftPreviewActivity.class.toString();
    private String mDraftTranslation;
    private SourceTranslation mSourceTranslation;
    private PreviewModeFragment mFragment;
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
        final String sourceTranslationId = args.getString(AppContext.EXTRA_SOURCE_DRAFT_TRANSLATION_ID, null);
        mSourceTranslation = SourceTranslation.simple(sourceTranslationId);
        String projectId = SourceTranslation.getProjectIdFromId(sourceTranslationId);
        String sourceLanguageId = SourceTranslation.getSourceLanguageIdFromId(sourceTranslationId);
        TargetTranslation targetTranslation = AppContext.findExistingTargetTranslation(projectId, sourceLanguageId);
        if (targetTranslation == null) {
            throw new InvalidParameterException("a valid target translation id is required");
        }

        if(findViewById(R.id.fragment) != null) {
            if(savedInstanceState != null) {
                mFragment = (PreviewModeFragment) getFragmentManager().findFragmentById(R.id.fragment);
            } else {
                mFragment = new PreviewModeFragment();
                args.putString(AppContext.EXTRA_TARGET_TRANSLATION_ID,targetTranslation.getId());
                args.putString(AppContext.EXTRA_SOURCE_DRAFT_TRANSLATION_ID, mSourceTranslation.getId());
                mFragment.setArguments(args);
                getFragmentManager().beginTransaction().add(R.id.fragment, mFragment).commit();
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

    @Override
    public void onNoSourceTranslations(String targetTranslationId) {
        Logger.i(TAG, "onNoSourceTranslations");
    }

    public void onItemCountChanged(int itemCount, int progress) {
        Logger.i(TAG, "onItemCountChanged");
    }

    @Override
    public void onScrollProgress(int position) {
        Logger.i(TAG, "onScrollProgress");
    }

    @Override
    public void openTranslationMode(TranslationViewMode mode, Bundle extras) {
        Bundle fragmentExtras = new Bundle();
    }


}