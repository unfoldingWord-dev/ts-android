package com.door43.translationstudio;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.door43.tools.reporting.GlobalExceptionHandler;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.data.IndexStore;
import com.door43.translationstudio.tasks.IndexProjectsTask;
import com.door43.translationstudio.tasks.IndexResourceTask;
import com.door43.translationstudio.tasks.LoadChaptersTask;
import com.door43.translationstudio.tasks.LoadFramesTask;
import com.door43.translationstudio.tasks.LoadProjectsTask;
import com.door43.translationstudio.tasks.LoadTargetLanguagesTask;
import com.door43.translationstudio.tasks.LoadTermsTask;
import com.door43.translationstudio.tasks.UpdateAppTask;
import com.door43.translationstudio.util.AppContext;
import com.door43.translationstudio.util.TranslatorBaseActivity;
import com.door43.util.tasks.ManagedTask;
import com.door43.util.tasks.TaskManager;

import java.io.File;

/**
 * This activity initializes the app
 */
public class SplashScreenActivity extends TranslatorBaseActivity implements ManagedTask.OnProgressListener, ManagedTask.OnFinishedListener, ManagedTask.OnStartListener {
    private TextView mProgressTextView;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mProgressTextView = (TextView)findViewById(R.id.loadingText);
        mProgressBar = (ProgressBar)findViewById(R.id.loadingBar);
        mProgressBar.setMax(100);
        mProgressBar.setIndeterminate(true);
        if(savedInstanceState != null) {
            int savedProgress = savedInstanceState.getInt("progress");
            if(savedProgress == -1) {
                mProgressBar.setIndeterminate(true);
                mProgressBar.setMax(mProgressBar.getMax());
            } else {
                mProgressBar.setIndeterminate(false);
                mProgressBar.setProgress(savedProgress);
            }
            mProgressTextView.setText(savedInstanceState.getString("message"));
        } else {
            mProgressBar.setProgress(0);
        }

        // check if we crashed
        File dir = new File(getExternalCacheDir(), app().STACKTRACE_DIR);
        String[] files = GlobalExceptionHandler.getStacktraces(dir);
        if (files.length > 0) {
            Intent intent = new Intent(this, CrashReporterActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // begin loading things
        if(!AppContext.isLoaded()) {
            boolean isWorking = false;
            isWorking = connectToTask(LoadTargetLanguagesTask.TASK_ID) ? true: isWorking;
            isWorking = connectToTask(LoadProjectsTask.TASK_ID) ? true: isWorking;
            isWorking = connectToTask(UpdateAppTask.TASK_ID) ? true: isWorking;
            isWorking = connectToTask(IndexProjectsTask.TASK_ID) ? true: isWorking;
            isWorking = connectToTask(IndexResourceTask.TASK_ID) ? true: isWorking;
            isWorking = connectToTask(LoadTermsTask.TASK_ID) ? true: isWorking;
            isWorking = connectToTask(LoadChaptersTask.TASK_ID) ? true: isWorking;
            isWorking = connectToTask(LoadFramesTask.TASK_ID) ? true: isWorking;

            // start new task
            if(!isWorking) {
                UpdateAppTask updateTask = new UpdateAppTask();
                updateTask.addOnProgressListener(this);
                updateTask.addOnFinishedListener(this);
                updateTask.addOnStartListener(this);
                TaskManager.addTask(updateTask, UpdateAppTask.TASK_ID);
            }
        } else {
            // index projects
            indexProjects();
        }
    }

    /**
     * Connect to an existing task
     * @param id
     * @return true if the task exists
     */
    private boolean connectToTask(String id) {
        ManagedTask t = TaskManager.getTask(id);
        if(t != null) {
            t.addOnProgressListener(this);
            t.addOnFinishedListener(this);
            t.addOnStartListener(this);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Disconnects this activity from a task
     * @param t
     */
    private void disconnectTask(ManagedTask t) {
        if(t != null) {
            t.removeOnProgressListener(this);
            t.removeOnStartListener(this);
            t.removeOnFinishedListener(this);
        }
    }

    /**
     * Disconnects this activity from a task
     * @param id
     */
    private void disconnectTask(String id) {
        ManagedTask t = TaskManager.getTask(id);
        disconnectTask(t);
    }

    @Override
    public void onProgress(final ManagedTask task, final double progress, final String message) {
        if(!task.isFinished()) {
            Handler hand = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    mProgressBar.setMax(task.maxProgress());
                    if (progress == -1) {
                        mProgressBar.setIndeterminate(true);
                        mProgressBar.setProgress(task.maxProgress());
                    } else {
                        mProgressBar.setIndeterminate(false);
                        mProgressBar.setProgress((int) Math.ceil(progress * task.maxProgress()));
                    }
                    if(task instanceof IndexProjectsTask || task instanceof IndexResourceTask) {
                        mProgressTextView.setText(R.string.indexing);
                    } else {
                        mProgressTextView.setText(message);
                    }
                }
            });
        }
    }

    /**
     * Builds an index of all the projects
     */
    private void indexProjects() {
        IndexProjectsTask newTask = new IndexProjectsTask(AppContext.projectManager().getProjects());
        newTask.addOnFinishedListener(this);
        newTask.addOnStartListener(this);
        newTask.addOnProgressListener(this);
        TaskManager.addTask(newTask, newTask.TASK_ID);
    }

    /**
     * Loads the project terms
     * @param p
     */
    private void loadTerms(Project p) {
        LoadTermsTask newTask = new LoadTermsTask(p, p.getSelectedSourceLanguage(), p.getSelectedSourceLanguage().getSelectedResource());
        newTask.addOnProgressListener(this);
        newTask.addOnStartListener(this);
        newTask.addOnFinishedListener(this);
        TaskManager.addTask(newTask, LoadTermsTask.TASK_ID);
    }

    @Override
    public void onFinished(final ManagedTask task) {
        TaskManager.clearTask(task);
        disconnectTask(task);

        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setIndeterminate(true);
                mProgressBar.setProgress(task.maxProgress());
            }
        });

        if(task instanceof UpdateAppTask) {
            LoadTargetLanguagesTask langTask = new LoadTargetLanguagesTask();
            langTask.addOnProgressListener(this);
            langTask.addOnFinishedListener(this);
            langTask.addOnStartListener(this);
            TaskManager.addTask(langTask, LoadTargetLanguagesTask.TASK_ID);
        } else if(task instanceof LoadTargetLanguagesTask) {
            LoadProjectsTask newTask = new LoadProjectsTask();
            newTask.addOnFinishedListener(this);
            newTask.addOnProgressListener(this);
            newTask.addOnStartListener(this);
            TaskManager.addTask(newTask, newTask.TASK_ID);
        } else if(task instanceof LoadProjectsTask) {
            // Generate the ssh keys
            if(!AppContext.context().hasKeys()) {
                AppContext.context().generateKeys();
            }
            indexProjects();
        } else if(task instanceof IndexProjectsTask) {
            // load the selected project if enabled
            if (app().getUserPreferences().getBoolean(SettingsActivity.KEY_PREF_REMEMBER_POSITION, Boolean.parseBoolean(getResources().getString(R.string.pref_default_remember_position)))) {
                Project p = AppContext.projectManager().getSelectedProject();
                if (p != null && p.hasSelectedSourceLanguage()) {
                    if (!IndexStore.hasIndex(p)) {
                        // index the resources
                        IndexResourceTask newTask = new IndexResourceTask(p, p.getSelectedSourceLanguage(), p.getSelectedSourceLanguage().getSelectedResource());
                        newTask.addOnProgressListener(this);
                        newTask.addOnStartListener(this);
                        newTask.addOnFinishedListener(this);
                        TaskManager.addTask(newTask, IndexResourceTask.TASK_ID);
                    } else {
                        loadTerms(p);
                    }
                    return;
                }
            }
            openMainActivity();
        } else if(task instanceof IndexResourceTask) {
            loadTerms(AppContext.projectManager().getSelectedProject());
        } else if(task instanceof LoadTermsTask) {
            // load chapters
            Project p = AppContext.projectManager().getSelectedProject();
            LoadChaptersTask newTask = new LoadChaptersTask(p, p.getSelectedSourceLanguage(), p.getSelectedSourceLanguage().getSelectedResource());
            newTask.addOnProgressListener(this);
            newTask.addOnStartListener(this);
            newTask.addOnFinishedListener(this);
            TaskManager.addTask(newTask, LoadChaptersTask.TASK_ID);
        } else if(task instanceof LoadChaptersTask) {
            // load frames
            Project p = AppContext.projectManager().getSelectedProject();
            if(p.getSelectedChapter() != null) {
                LoadFramesTask newTask  = new LoadFramesTask(p, p.getSelectedSourceLanguage(), p.getSelectedSourceLanguage().getSelectedResource(), p.getSelectedChapter());
                newTask.addOnProgressListener(this);
                newTask.addOnStartListener(this);
                newTask.addOnFinishedListener(this);
                TaskManager.addTask(newTask, LoadFramesTask.TASK_ID);
            } else {
                openMainActivity();
            }
        } else if(task instanceof LoadFramesTask) {
            openMainActivity();
        }
    }

    private void openMainActivity() {
        AppContext.setLoaded(true);
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onStart(ManagedTask task) {

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("message", mProgressTextView.getText().toString());
        outState.putInt("progress", mProgressBar.getProgress());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        disconnectTask(LoadTargetLanguagesTask.TASK_ID);
        disconnectTask(LoadProjectsTask.TASK_ID);
        disconnectTask(UpdateAppTask.TASK_ID);
        disconnectTask(IndexProjectsTask.TASK_ID);
        disconnectTask(IndexResourceTask.TASK_ID);
        disconnectTask(LoadTermsTask.TASK_ID);
        disconnectTask(LoadChaptersTask.TASK_ID);
        disconnectTask(LoadFramesTask.TASK_ID);
        super.onDestroy();
    }
}
