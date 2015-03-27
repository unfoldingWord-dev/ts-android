package com.door43.translationstudio;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.tasks.ImportTranslationDraftTask;
import com.door43.translationstudio.util.AppContext;
import com.door43.translationstudio.util.TranslatorBaseActivity;
import com.door43.util.threads.ManagedTask;
import com.door43.util.threads.TaskManager;


public class ProjectSettingsActivity extends TranslatorBaseActivity implements ManagedTask.OnFinishedListener, ManagedTask.OnProgressListener, DialogInterface.OnCancelListener {
    private Button targetLanguageBtn;
    private Button sourceLanguageBtn;
    private Project mProject;
    private LinearLayout draftNoticeLayout;
    private AlertDialog mConfirmDialog;
    private int mImportDraftTaskId = -1;
    private ProgressDialog mLoadingDialog;
    private final String STATE_IMPORT_TASK_ID = "import_task_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(savedInstanceState != null) {
            mImportDraftTaskId = savedInstanceState.getInt(STATE_IMPORT_TASK_ID);
        }

        mProject = AppContext.projectManager().getSelectedProject();
        if(mProject == null || mProject.getSelectedChapter() == null) {
            if(mProject != null && mProject.getSelectedChapter() == null) {
                app().showToastMessage(R.string.missing_chapter); // the language does not contain any chapters
                Intent languageIntent = new Intent(ProjectSettingsActivity.this, LanguageSelectorActivity.class);
                languageIntent.putExtra("sourceLanguages", true);
                startActivity(languageIntent);
            } else {
                finish();
            }
        }

        targetLanguageBtn = (Button)findViewById(R.id.switchTargetLanguageButton);
        sourceLanguageBtn = (Button)findViewById(R.id.switchSourceLanguageButton);

        draftNoticeLayout = (LinearLayout)findViewById(R.id.translationDraftNotice);

        Button editDraftButton = (Button)findViewById(R.id.editDraftButton);
        Button viewDraftButton = (Button)findViewById(R.id.viewDraftButton);

        editDraftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: we could alternatively provide a merge option
                mConfirmDialog = new AlertDialog.Builder(ProjectSettingsActivity.this)
                        .setTitle(R.string.import_draft)
                        .setMessage(R.string.import_draft_confirmation)
                        .setIcon(R.drawable.ic_new_pencil_small)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                startImportDraftTask();
                            }
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
            }
        });

        viewDraftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: load the draft in read only mode
                AppContext.context().showToastMessage("Not implemented yet");
            }
        });


        // hook up buttons
        targetLanguageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent languageIntent = new Intent(ProjectSettingsActivity.this, LanguageSelectorActivity.class);
                languageIntent.putExtra("sourceLanguages", false);
                startActivity(languageIntent);
            }
        });
        sourceLanguageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent languageIntent = new Intent(ProjectSettingsActivity.this, LanguageSelectorActivity.class);
                languageIntent.putExtra("sourceLanguages", true);
                startActivity(languageIntent);
            }
        });

        loadValues();
    }

    public void onResume() {
        super.onResume();
        loadValues();
        connectImportDraftTask();
    }

    /**
     * connects to an existing import task
     * @return
     */
    private void connectImportDraftTask() {
        ImportTranslationDraftTask task = (ImportTranslationDraftTask)TaskManager.getTask(mImportDraftTaskId);
        if(task != null) {
            // connect to existing task
            task.setOnFinishedListener(this);
            task.setOnProgressListener(this);
            createLoadingDialog();
        }
    }

    /**
     * Begins importing a translation draft into the project
     */
    private void startImportDraftTask() {
        ImportTranslationDraftTask task = (ImportTranslationDraftTask)TaskManager.getTask(mImportDraftTaskId);
        if(task == null && mProject != null) {
            // create new task
            task = new ImportTranslationDraftTask(mProject);
            task.setOnFinishedListener(this);
            task.setOnProgressListener(this);
            mImportDraftTaskId = TaskManager.addTask(task);
            createLoadingDialog();
        }
    }

    private void createLoadingDialog() {
        if(mLoadingDialog == null) {
            mLoadingDialog = new ProgressDialog(this);
            mLoadingDialog.setCancelable(true);
            mLoadingDialog.setCanceledOnTouchOutside(false);
            mLoadingDialog.setOnCancelListener(this);
            mLoadingDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mLoadingDialog.setMax(100);
            mLoadingDialog.setTitle(R.string.loading);
            mLoadingDialog.show();
        }
    }

    private void loadValues() {
        if(mProject != null) {
            if(mProject.getSourceLanguageDraft(mProject.getSelectedTargetLanguage().getId()) != null) {
                draftNoticeLayout.setVisibility(View.VISIBLE);
            } else {
                draftNoticeLayout.setVisibility(View.GONE);
            }

            targetLanguageBtn.setText(mProject.getSelectedTargetLanguage().getName());
            sourceLanguageBtn.setText(mProject.getSelectedSourceLanguage().getName());
            if (mProject.getSelectedTargetLanguage().isTranslating(mProject)) {
                targetLanguageBtn.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_project_status_translating_light, 0);
            } else {
                targetLanguageBtn.setCompoundDrawables(null, null, null, null);
            }

            // set graphite fontface
            targetLanguageBtn.setTypeface(AppContext.graphiteTypeface(mProject.getSelectedTargetLanguage()), 0);
            sourceLanguageBtn.setTypeface(AppContext.graphiteTypeface(mProject.getSelectedSourceLanguage()), 0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.project_settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        ImportTranslationDraftTask task = (ImportTranslationDraftTask)TaskManager.getTask(mImportDraftTaskId);
        if(task != null) {
            outState.putInt(STATE_IMPORT_TASK_ID, mImportDraftTaskId);
            task.setOnFinishedListener(null);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_dismiss_project_settings:
                if(!mProject.hasChosenTargetLanguage()) {
                    mProject.setSelectedTargetLanguage(mProject.getSelectedTargetLanguage().getId());
                }
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onDestroy() {
        if(mConfirmDialog != null) {
            mConfirmDialog.dismiss();
        }
        if(mLoadingDialog != null) {
            mLoadingDialog.setOnCancelListener(null);
            mLoadingDialog.setOnDismissListener(null);
            mLoadingDialog.dismiss();
        }
        super.onDestroy();
    }

    @Override
    public void onFinished(ManagedTask task) {
        TaskManager.clearTask(mImportDraftTaskId);
        mImportDraftTaskId = -1;
        if(mLoadingDialog != null && mLoadingDialog.isShowing()) {
            mLoadingDialog.dismiss();
        }
        finish();
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        ImportTranslationDraftTask task = (ImportTranslationDraftTask)TaskManager.getTask(mImportDraftTaskId);
        if(task != null) {
            task.setOnFinishedListener(null);
            TaskManager.cancelTask(task);
            mImportDraftTaskId = -1;
        }
    }

    @Override
    public void onProgress(ManagedTask task, final double progress, final String message) {
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                if(mLoadingDialog != null) {
                    mLoadingDialog.setMessage(message);
                    if(progress == -1) {
                        mLoadingDialog.setIndeterminate(true);
                        mLoadingDialog.setProgress(mLoadingDialog.getMax());
                    } else {
                        mLoadingDialog.setIndeterminate(false);
                        mLoadingDialog.setProgress((int) Math.round(mLoadingDialog.getMax() * progress));
                    }
                }
            }
        });
    }
}
