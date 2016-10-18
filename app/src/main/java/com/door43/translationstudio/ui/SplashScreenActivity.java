package com.door43.translationstudio.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.tasks.UpdateAppTask;

import org.unfoldingword.tools.logger.Logger;
import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.TaskManager;

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
    }

    @Override
    public void onResume() {
        super.onResume();

        if(!waitingForPermissions()) {
            // check if we crashed
            File[] files = Logger.listStacktraces();
            if (files.length > 0) {
                Intent intent = new Intent(this, CrashReporterActivity.class);
                startActivity(intent);
                finish();
                return;
            }

            // connect to tasks
            boolean isWorking = false;
            isWorking = connectToTask(UpdateAppTask.TASK_ID) ? true : isWorking;

            // start new task
            if (!isWorking) {
                UpdateAppTask updateTask = new UpdateAppTask(App.context());
                updateTask.addOnFinishedListener(this);
                updateTask.addOnStartListener(this);
                TaskManager.addTask(updateTask, UpdateAppTask.TASK_ID);
            }
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
    public void onTaskFinished(final ManagedTask task) {
        TaskManager.clearTask(task);
        disconnectTask(task);
        openMainActivity();
    }

    private void openMainActivity() {
        Intent intent = new Intent(this, ProfileActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onTaskStart(final ManagedTask task) {
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                mProgressTextView.setText(R.string.updating_app);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("message", mProgressTextView.getText().toString());
        outState.putInt("progress", mProgressBar.getProgress());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        disconnectTask(UpdateAppTask.TASK_ID);
        super.onDestroy();
    }
}
