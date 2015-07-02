package com.door43.translationstudio;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.data.IndexStore;
import com.door43.util.tasks.GenericTaskWatcher;
import com.door43.translationstudio.tasks.ImportTranslationDraftTask;
import com.door43.translationstudio.tasks.IndexProjectsTask;
import com.door43.translationstudio.tasks.IndexResourceTask;
import com.door43.translationstudio.tasks.LoadChaptersTask;
import com.door43.translationstudio.tasks.LoadFramesTask;
import com.door43.translationstudio.translatonui.TranslatorFragment;
import com.door43.translationstudio.util.AppContext;
import com.door43.translationstudio.util.TranslatorBaseActivity;
import com.door43.util.tasks.ManagedTask;
import com.door43.util.tasks.TaskManager;


public class ProjectSettingsActivity extends TranslatorBaseActivity implements GenericTaskWatcher.OnFinishedListener {
    private static final int SOURCE_LANGUAGE_REQUEST = 0;
    private static final int TARGET_LANGUAGE_REQUEST = 1;
    private Button targetLanguageBtn;
    private Button sourceLanguageBtn;
    private Project mProject;
    private LinearLayout draftNoticeLayout;
    private AlertDialog mConfirmDialog;
    private final String STATE_SELECTED_SOURCE_ID = "selected_source_id";
    private final String STATE_SELECTED_TARGET_ID = "selected_target_id";
    private String mSelectedSourceLanguageId = null;
    private String mSelectedTargetLanguageId = null;
    private GenericTaskWatcher mTaskWatcher;
    private boolean mImportDraft = false; // flag to identify if the draft should be imported after loading the source

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mProject = AppContext.projectManager().getSelectedProject();
        if(mProject == null) {
            finish();
        } else {

            mTaskWatcher = new GenericTaskWatcher(this, R.string.loading);
            mTaskWatcher.setOnFinishedListener(this);

            if (savedInstanceState != null) {
                mSelectedSourceLanguageId = savedInstanceState.getString(STATE_SELECTED_SOURCE_ID, null);
                mSelectedTargetLanguageId = savedInstanceState.getString(STATE_SELECTED_TARGET_ID, null);
            }

            targetLanguageBtn = (Button) findViewById(R.id.switchTargetLanguageButton);
            sourceLanguageBtn = (Button) findViewById(R.id.switchSourceLanguageButton);

            draftNoticeLayout = (LinearLayout) findViewById(R.id.translationDraftNotice);

            Button editDraftButton = (Button) findViewById(R.id.editDraftButton);
            Button viewDraftButton = (Button) findViewById(R.id.viewDraftButton);

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
                                    mImportDraft = true;
                                    save();
                                }
                            })
                            .setNegativeButton(R.string.no, null)
                            .show();
                }
            });

            viewDraftButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AppContext.args.putBoolean(TranslatorFragment.ARGS_VIEW_TRANSLATION_DRAFT, true);
                    save();
                }
            });

            // hook up buttons
            targetLanguageBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent languageIntent = new Intent(ProjectSettingsActivity.this, LanguageSelectorActivity.class);
                    languageIntent.putExtra(LanguageSelectorActivity.EXTRAS_SOURCE_LANGUAGES, false);
                    startActivityForResult(languageIntent, TARGET_LANGUAGE_REQUEST);
                }
            });
            sourceLanguageBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent languageIntent = new Intent(ProjectSettingsActivity.this, LanguageSelectorActivity.class);
                    languageIntent.putExtra(LanguageSelectorActivity.EXTRAS_SOURCE_LANGUAGES, true);
                    startActivityForResult(languageIntent, SOURCE_LANGUAGE_REQUEST);
                }
            });
        }
    }

    public void onResume() {
        super.onResume();
        loadValues();

        mTaskWatcher.watch(IndexProjectsTask.TASK_ID);
        mTaskWatcher.watch(IndexResourceTask.TASK_ID);
        mTaskWatcher.watch(LoadChaptersTask.TASK_ID);
        mTaskWatcher.watch(LoadFramesTask.TASK_ID);
        mTaskWatcher.watch(ImportTranslationDraftTask.TASK_ID);
    }

    private void loadValues() {
        String sourceId = mProject.getSelectedSourceLanguage().getId();
        String targetId = mProject.getSelectedTargetLanguage().getId();
        if(mSelectedSourceLanguageId != null) {
            sourceId = mSelectedSourceLanguageId;
        }
        if(mSelectedTargetLanguageId != null) {
            targetId = mSelectedTargetLanguageId;
        }

        if(mProject.getSourceLanguageDraft(targetId) != null) {
            draftNoticeLayout.setVisibility(View.VISIBLE);
        } else {
            draftNoticeLayout.setVisibility(View.GONE);
        }

        targetLanguageBtn.setText(AppContext.projectManager().getLanguage(targetId).getName());
        sourceLanguageBtn.setText(mProject.getSourceLanguage(sourceId).getName());
        if (AppContext.projectManager().getLanguage(targetId).isTranslating(mProject)) {
            targetLanguageBtn.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_project_status_translating_light, 0);
        } else {
            targetLanguageBtn.setCompoundDrawables(null, null, null, null);
        }

        // set graphite fontface
        targetLanguageBtn.setTypeface(AppContext.graphiteTypeface(AppContext.projectManager().getLanguage(targetId)), 0);
        sourceLanguageBtn.setTypeface(AppContext.graphiteTypeface(mProject.getSourceLanguage(sourceId)), 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.project_settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(STATE_SELECTED_SOURCE_ID, mSelectedSourceLanguageId);
        outState.putString(STATE_SELECTED_TARGET_ID, mSelectedTargetLanguageId);

        super.onSaveInstanceState(outState);
    }

    private void save() {
        if(mSelectedSourceLanguageId != null) {
            if(!mProject.hasSelectedSourceLanguage() || !mProject.getSelectedSourceLanguage().getId().equals(mSelectedSourceLanguageId)) {
                mProject.setSelectedSourceLanguage(mSelectedSourceLanguageId);
                mProject.flush();
            }
        }
        if(mSelectedTargetLanguageId != null) {
            mProject.setSelectedTargetLanguage(mSelectedTargetLanguageId);
        }

        // load the changes
        if(!IndexStore.hasIndex(mProject)) {
            // index the project
            IndexProjectsTask task = new IndexProjectsTask(mProject);
            mTaskWatcher.watch(task);
            TaskManager.addTask(task, IndexProjectsTask.TASK_ID);
        } else if(!IndexStore.hasResourceIndex(mProject, mProject.getSelectedSourceLanguage(), mProject.getSelectedSourceLanguage().getSelectedResource())) {
            // index the resources
            IndexResourceTask task = new IndexResourceTask(mProject, mProject.getSelectedSourceLanguage(), mProject.getSelectedSourceLanguage().getSelectedResource());
            mTaskWatcher.watch(task);
            TaskManager.addTask(task, IndexResourceTask.TASK_ID);
        } else if(mProject.getChapters().length == 0) {
            // load the chapters
            LoadChaptersTask task = new LoadChaptersTask(mProject, mProject.getSelectedSourceLanguage(), mProject.getSelectedSourceLanguage().getSelectedResource());
            mTaskWatcher.watch(task);
            TaskManager.addTask(task, LoadChaptersTask.TASK_ID);
        } else if(mProject.getSelectedChapter() != null && mProject.getSelectedChapter().getFrames().length == 0) {
            // load the frames
            LoadFramesTask task = new LoadFramesTask(mProject, mProject.getSelectedSourceLanguage(), mProject.getSelectedSourceLanguage().getSelectedResource(), mProject.getSelectedChapter());
            mTaskWatcher.watch(task);
            TaskManager.addTask(task,LoadFramesTask.TASK_ID);
        } else if(mImportDraft){
            // import the draft
            ImportTranslationDraftTask task = new ImportTranslationDraftTask(mProject);
            mTaskWatcher.watch(task);
            TaskManager.addTask(task, ImportTranslationDraftTask.TASK_ID);
        } else {
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_dismiss_project_settings:
                save();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onDestroy() {
        mTaskWatcher.stop();
        if(mConfirmDialog != null) {
            mConfirmDialog.dismiss();
        }
        super.onDestroy();
    }

    @Override
    public void onFinished(ManagedTask task) {
        mTaskWatcher.stop();
        if(task instanceof IndexProjectsTask) {
            // index the resources
            IndexResourceTask newTask = new IndexResourceTask(mProject, mProject.getSelectedSourceLanguage(), mProject.getSelectedSourceLanguage().getSelectedResource());
            mTaskWatcher.watch(newTask);
            TaskManager.addTask(newTask, IndexResourceTask.TASK_ID);
        } else if(task instanceof IndexResourceTask) {
            // load chapters
            LoadChaptersTask newTask = new LoadChaptersTask(mProject, mProject.getSelectedSourceLanguage(), mProject.getSelectedSourceLanguage().getSelectedResource());
            mTaskWatcher.watch(newTask);
            TaskManager.addTask(newTask, LoadChaptersTask.TASK_ID);
        } else if(task instanceof LoadChaptersTask) {
            // load frames
            LoadFramesTask newTask = new LoadFramesTask(mProject, mProject.getSelectedSourceLanguage(), mProject.getSelectedSourceLanguage().getSelectedResource(), mProject.getSelectedChapter());
            mTaskWatcher.watch(newTask);
            TaskManager.addTask(newTask,LoadFramesTask.TASK_ID);
        }  else if(task instanceof LoadFramesTask) {
            // import the draft
            if(mImportDraft) {
                ImportTranslationDraftTask newTtask = new ImportTranslationDraftTask(mProject);
                mTaskWatcher.watch(newTtask);
                TaskManager.addTask(newTtask, ImportTranslationDraftTask.TASK_ID);
            } else {
                finish();
            }
        } else if(task instanceof ImportTranslationDraftTask) {
            finish();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == SOURCE_LANGUAGE_REQUEST) {
            if(resultCode == RESULT_OK) {
                mSelectedSourceLanguageId = data.getExtras().getString(LanguageSelectorActivity.EXTRAS_CHOSEN_LANGUAGE);
                loadValues();
            }
        } else if(requestCode == TARGET_LANGUAGE_REQUEST) {
            if(resultCode == RESULT_OK) {
                mSelectedTargetLanguageId = data.getExtras().getString(LanguageSelectorActivity.EXTRAS_CHOSEN_LANGUAGE);
                loadValues();
            }
        }
    }
}
