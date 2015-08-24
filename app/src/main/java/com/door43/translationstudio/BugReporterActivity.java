package com.door43.translationstudio;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.tasks.CheckForLatestReleaseTask;
import com.door43.translationstudio.tasks.UploadBugReportTask;
import com.door43.translationstudio.util.TranslatorBaseActivity;
import com.door43.util.tasks.ManagedTask;
import com.door43.util.tasks.TaskManager;

/**
 * Created by joel on 1/14/2015.
 */
public class BugReporterActivity extends TranslatorBaseActivity implements ManagedTask.OnFinishedListener {
    private static final String STATE_LATEST_RELEASE = "state_latest_release";
    private static final String STATE_NOTES = "staet_notes";
    private Button mOkButton;
    private Button mCancelButton;
    private ProgressDialog mLoadingDialog;
    private EditText mCrashReportText;
    private String mNotes = "";
    private CheckForLatestReleaseTask.Release mLatestRelease = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash_reporter);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mOkButton = (Button)findViewById(R.id.okButton);
        mOkButton.setBackgroundResource(R.color.gray);
        mCancelButton = (Button)findViewById(R.id.cancelButton);
        mCrashReportText = (EditText)findViewById(R.id.crashDescriptioneditText);
        mCrashReportText.setHint(R.string.bug_report);

        mLoadingDialog = new ProgressDialog(BugReporterActivity.this);
        mLoadingDialog.setCancelable(false);
        mLoadingDialog.setCanceledOnTouchOutside(false);

        mCrashReportText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(mCrashReportText.getText().toString().isEmpty()) {
                    mOkButton.setBackgroundResource(R.color.gray);
                } else {
                    mOkButton.setBackgroundResource(R.color.accent);
                }
            }
        });
        mOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mCrashReportText.getText().toString().isEmpty()) {
                    mNotes = mCrashReportText.getText().toString().trim();

                    mLoadingDialog.setMessage(getResources().getString(R.string.loading));
                    mLoadingDialog.show();

                    CheckForLatestReleaseTask task = new CheckForLatestReleaseTask();
                    task.addOnFinishedListener(BugReporterActivity.this);
                    TaskManager.addTask(task, CheckForLatestReleaseTask.TASK_ID);
                } else {
                    Toast toast = Toast.makeText(BugReporterActivity.this, R.string.please_enter_text, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.TOP, 0, 0);
                    toast.show();
                }
            }
        });
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState != null) {
            mNotes = savedInstanceState.getString(STATE_NOTES, "");
            mLatestRelease = (CheckForLatestReleaseTask.Release)savedInstanceState.getSerializable(STATE_LATEST_RELEASE);
        }

        if(TextUtils.isEmpty(mNotes)) {
            mOkButton.setBackgroundResource(R.color.gray);
        } else {
            mOkButton.setBackgroundResource(R.color.accent);
            mCrashReportText.setText(mNotes);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        CheckForLatestReleaseTask checkTask = (CheckForLatestReleaseTask) TaskManager.getTask(CheckForLatestReleaseTask.TASK_ID);
        UploadBugReportTask uploadTask = (UploadBugReportTask)TaskManager.getTask(UploadBugReportTask.TASK_ID);

        if(checkTask != null) {
            mLoadingDialog.setMessage(getResources().getString(R.string.loading));
            mLoadingDialog.show();
            checkTask.addOnFinishedListener(this);
        } else if(uploadTask != null) {
            mLoadingDialog.setMessage(getResources().getString(R.string.uploading));
            mLoadingDialog.show();
            uploadTask.addOnFinishedListener(this);
        } else if(mLatestRelease != null) {
            notifyLatestRelease(mLatestRelease);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onFinished(ManagedTask task) {
        TaskManager.clearTask(task);

        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                if(mLoadingDialog.isShowing()) {
                    mLoadingDialog.dismiss();
                }
            }
        });

        if(task.getClass().getName().equals(CheckForLatestReleaseTask.class.getName())) {
            CheckForLatestReleaseTask.Release release = ((CheckForLatestReleaseTask)task).getLatestRelease();
            if(release != null) {
                mLatestRelease = release;
                hand.post(new Runnable() {
                    @Override
                    public void run() {
                        notifyLatestRelease(mLatestRelease);
                    }
                });
            } else {
                hand.post(new Runnable() {
                    @Override
                    public void run() {
                        mLoadingDialog.setMessage(getResources().getString(R.string.uploading));
                        mLoadingDialog.show();
                    }
                });

                UploadBugReportTask newTask = new UploadBugReportTask(mNotes);
                newTask.addOnFinishedListener(BugReporterActivity.this);
                TaskManager.addTask(newTask, UploadBugReportTask.TASK_ID);
            }
        } else if(task.getClass().getName().equals(UploadBugReportTask.class.getName())) {
            finish();
        }
    }

    /**
     * Displays a dialog to the user telling them there is an apk update.
     * @param release
     */
    private void notifyLatestRelease(final CheckForLatestReleaseTask.Release release) {
        final Boolean isStoreVersion = ((MainApplication)getApplication()).isStoreVersion();

        new AlertDialog.Builder(this)
                .setTitle(R.string.apk_update_available)
                .setMessage(R.string.upload_report_or_download_latest_apk)
                .setNegativeButton(R.string.title_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mLatestRelease = null;
                        BugReporterActivity.this.finish();
                    }
                })
                .setNeutralButton(R.string.download_update, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(isStoreVersion) {
                            // open play store
                            final String appPackageName = getPackageName();
                            try {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                            } catch (android.content.ActivityNotFoundException anfe) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                            }
                        } else {
                            // download from github
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(release.downloadUrl));
                            startActivity(browserIntent);
                        }
                        dialog.dismiss();
                        BugReporterActivity.this.finish();
                    }
                })
                .setPositiveButton(R.string.label_continue, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mLoadingDialog.setMessage(getResources().getString(R.string.uploading));
                        mLoadingDialog.show();

                        UploadBugReportTask newTask = new UploadBugReportTask(mNotes);
                        newTask.addOnFinishedListener(BugReporterActivity.this);
                        TaskManager.addTask(newTask, UploadBugReportTask.TASK_ID);
                        dialog.dismiss();
                    }
                })
                .setCancelable(false)
                .create()
                .show();
        Logger.i(BugReporterActivity.class.getName(), "A new release is available: " + release.name);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if(mLatestRelease != null) {
            outState.putSerializable(STATE_LATEST_RELEASE, mLatestRelease);
        } else {
            outState.remove(STATE_LATEST_RELEASE);
        }
        outState.putString(STATE_NOTES, mCrashReportText.getText().toString().trim());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        // disconnect listeners
        CheckForLatestReleaseTask checkTask = (CheckForLatestReleaseTask) TaskManager.getTask(CheckForLatestReleaseTask.TASK_ID);
        if(checkTask != null) {
            checkTask.removeOnFinishedListener(this);
        }
        UploadBugReportTask uploadTask = (UploadBugReportTask)TaskManager.getTask(UploadBugReportTask.TASK_ID);
        if(uploadTask != null) {
            uploadTask.removeOnFinishedListener(this);
        }

        super.onDestroy();
    }
}