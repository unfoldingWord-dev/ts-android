package com.door43.translationstudio.ui.draft;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.ui.BaseActivity;
import com.door43.translationstudio.tasks.ImportDraftTask;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.taskmanager.SimpleTaskWatcher;
import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.TaskManager;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;


public class DraftActivity extends BaseActivity implements SimpleTaskWatcher.OnFinishedListener {
    public static final String TAG = "DraftActivity";
    public static final String EXTRA_TARGET_TRANSLATION_ID = "target_translation_id";
    private TargetTranslation mTargetTranslation;
    private Translator mTranslator;
    private Door43Client mLibrary;
    private RecyclerView mRecylerView;
    private LinearLayoutManager mLayoutManager;
    private DraftAdapter mAdapter;
    private SimpleTaskWatcher taskWatcher;
    private Translation mDraftTranslation;
    private ResourceContainer mSourceContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draft_preview);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mTranslator = App.getTranslator();
        mLibrary = App.getLibrary();

        // validate parameters
        List<Translation> draftTranslations = new ArrayList<>();
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            String targetTranslationId = extras.getString(EXTRA_TARGET_TRANSLATION_ID, null);
            mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
            if(mTargetTranslation != null) {
                draftTranslations = mLibrary.index().findTranslations(mTargetTranslation.getTargetLanguage().slug, mTargetTranslation.getProjectId(), null, "book", null, 0, -1);
            } else {
                throw new InvalidParameterException("a valid target translation id is required");
            }
        } else {
            throw new InvalidParameterException("This activity expects some arguments");
        }

        if(draftTranslations.size() == 0) {
            finish();
            return;
        }

        // TODO: 10/7/16 display translations in tabs like in the translate modes so they can choose which one to import
        mDraftTranslation = draftTranslations.get(0);

        mRecylerView = (RecyclerView)findViewById(R.id.recycler_view);
        mLayoutManager = new LinearLayoutManager(this);
        mRecylerView.setLayoutManager(mLayoutManager);
        mRecylerView.setItemAnimator(new DefaultItemAnimator());
        // TODO: 10/6/16 we need to open the container in a task for better performance
        ResourceContainer rc;
        try {
            rc = mLibrary.open(mDraftTranslation.resourceContainerSlug);
        } catch (Exception e) {
            e.printStackTrace();
            finish();
            return;
        }
        mSourceContainer = rc;
        mAdapter = new DraftAdapter(this, mSourceContainer);
        mRecylerView.setAdapter(mAdapter);

        taskWatcher = new SimpleTaskWatcher(this, R.string.loading);
        taskWatcher.setOnFinishedListener(this);

        FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(DraftActivity.this,R.style.AppTheme_Dialog)
                        .setTitle(R.string.import_draft)
                        .setMessage(R.string.import_draft_confirmation)
                        .setNegativeButton(R.string.menu_cancel, null)
                        .setPositiveButton(R.string.label_import, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // // TODO: 1/20/2016 use the draft from the selected tab
                                ImportDraftTask task = new ImportDraftTask(mSourceContainer);
                                taskWatcher.watch(task);
                                TaskManager.addTask(task, ImportDraftTask.TASK_ID);
                            }
                        }).show();
            }
        });

        // re-attach to tasks
        ManagedTask task = TaskManager.getTask(ImportDraftTask.TASK_ID);
        if(task != null) {
            taskWatcher.watch(task);
        }
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

    @Override
    public void onFinished(ManagedTask task) {
        taskWatcher.stop();
        TaskManager.clearTask(task);
        TargetTranslation targetTranslation = ((ImportDraftTask) task).getTargetTranslation();
        if(targetTranslation != null) {
            finish();
        } else {
            new AlertDialog.Builder(this, R.style.AppTheme_Dialog)
                    .setTitle(R.string.error)
                    .setMessage(R.string.translation_import_failed)
                    .setNeutralButton(R.string.dismiss, null)
                    .show();
        }
    }
    @Override
    public void onDestroy() {
        if(taskWatcher != null) taskWatcher.stop();

        super.onDestroy();
    }
}