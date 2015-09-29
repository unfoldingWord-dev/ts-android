package com.door43.translationstudio;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.door43.tools.reporting.GlobalExceptionHandler;
import com.door43.translationstudio.newui.BaseActivity;
import com.door43.translationstudio.newui.home.HomeActivity;
import com.door43.translationstudio.tasks.InitializeLibraryTask;
import com.door43.translationstudio.tasks.LoadTargetLanguagesTask;
import com.door43.translationstudio.tasks.UpdateAppTask;
import com.door43.util.tasks.ManagedTask;
import com.door43.util.tasks.TaskManager;

import java.io.File;

/**
 * This activity initializes the app
 */
public class SplashScreenActivity extends BaseActivity implements ManagedTask.OnFinishedListener, ManagedTask.OnStartListener {
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
            mProgressTextView.setText(savedInstanceState.getString("message"));
        }

        // check if we crashed
        File dir = new File(getExternalCacheDir(), AppContext.context().STACKTRACE_DIR);
        String[] files = GlobalExceptionHandler.getStacktraces(dir);
        if (files.length > 0) {
            Intent intent = new Intent(this, CrashReporterActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if(!AppContext.isLoaded()) {
            // connect to tasks
            boolean isWorking = false;
            isWorking = connectToTask(LoadTargetLanguagesTask.TASK_ID) ? true : isWorking;
            isWorking = connectToTask(InitializeLibraryTask.TASK_ID) ? true : isWorking;
            isWorking = connectToTask(UpdateAppTask.TASK_ID) ? true : isWorking;

            // start new task
            if (!isWorking) {
                UpdateAppTask updateTask = new UpdateAppTask();
                updateTask.addOnFinishedListener(this);
                updateTask.addOnStartListener(this);
                TaskManager.addTask(updateTask, UpdateAppTask.TASK_ID);
            }
        } else {
            openMainActivity();
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
            if(!AppContext.getLibrary().exists()) {
                InitializeLibraryTask libraryTask = new InitializeLibraryTask();
                libraryTask.addOnFinishedListener(this);
                libraryTask.addOnStartListener(this);
                TaskManager.addTask(libraryTask, InitializeLibraryTask.TASK_ID);
            } else {
                // skip straight to loading languages if library is already initialized
                LoadTargetLanguagesTask langTask = new LoadTargetLanguagesTask();
                langTask.addOnFinishedListener(this);
                langTask.addOnStartListener(this);
                TaskManager.addTask(langTask, LoadTargetLanguagesTask.TASK_ID);
            }
        } else if(task instanceof InitializeLibraryTask) {
            LoadTargetLanguagesTask langTask = new LoadTargetLanguagesTask();
            langTask.addOnFinishedListener(this);
            langTask.addOnStartListener(this);
            TaskManager.addTask(langTask, LoadTargetLanguagesTask.TASK_ID);
        } else if(task instanceof LoadTargetLanguagesTask) {
            // Generate the ssh keys
            if(!AppContext.context().hasKeys()) {
                AppContext.context().generateKeys();
            }
            openMainActivity();
        }
    }

    private void openMainActivity() {
        AppContext.setLoaded(true);
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onStart(ManagedTask task) {
        if(task instanceof UpdateAppTask) {
            mProgressTextView.setText(R.string.updating_app);
        } else if(task instanceof InitializeLibraryTask) {
            mProgressTextView.setText(R.string.preparing_for_first_use);
        } else if(task instanceof LoadTargetLanguagesTask) {
            mProgressTextView.setText(R.string.loading_languages);
        }
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
        disconnectTask(InitializeLibraryTask.TASK_ID);
        disconnectTask(UpdateAppTask.TASK_ID);
        super.onDestroy();
    }
}
