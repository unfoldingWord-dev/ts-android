package com.door43.translationstudio.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.util.Log;
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
    private static final String STATE_STARTED = "started";
    private TextView mProgressTextView;
    private ProgressBar mProgressBar;
    private boolean silentStart = true;
    private boolean started = false;

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
            started = savedInstanceState.getBoolean(STATE_STARTED);
        }

        // check minimum requirements
        boolean checkHardware = App.getUserPreferences().getBoolean(SettingsActivity.KEY_PREF_CHECK_HARDWARE, true);
        if(checkHardware && !started) {
            int numProcessors = Runtime.getRuntime().availableProcessors();
            long maxMem = Runtime.getRuntime().maxMemory();

            long minMem = 100 * 1024 * 1024 * 10; // 100 MB
            if (numProcessors < 2 || maxMem < minMem) {
                silentStart = false;
                new AlertDialog.Builder(this, R.style.AppTheme_Dialog)
                        .setTitle(R.string.slow_device)
                        .setMessage(R.string.min_hardware_req_not_met)
                        .setCancelable(false)
                        .setNegativeButton(R.string.do_not_show_again, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SharedPreferences.Editor editor = App.getUserPreferences().edit();
                                editor.putBoolean(SettingsActivity.KEY_PREF_CHECK_HARDWARE, false);
                                editor.apply();
                                start();
                            }
                        })
                        .setPositiveButton(R.string.label_continue, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                start();
                            }
                        })
                        .show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(silentStart) {
            start();
        }
    }

    /**
     * Begins running tasks
     */
    private void start() {
        started = true;
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
            boolean isWorking = connectToTask(UpdateAppTask.TASK_ID);

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
        outState.putBoolean(STATE_STARTED, started);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        disconnectTask(UpdateAppTask.TASK_ID);
        super.onDestroy();
    }
}
