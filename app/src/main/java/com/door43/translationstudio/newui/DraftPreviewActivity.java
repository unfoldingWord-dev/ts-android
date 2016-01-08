package com.door43.translationstudio.newui;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.door43.translationstudio.R;

public class DraftPreviewActivity extends BaseActivity {
    public static final int VERIFY_EDIT_OF_DRAFT = 142;
    private String mDraftTranslation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draft_preview);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        mDraftTranslation = intent.getType();

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