package com.door43.translationstudio;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.door43.translationstudio.newui.BaseActivity;
import com.door43.translationstudio.tasks.CheckForLatestReleaseTask;
import com.door43.translationstudio.tasks.UploadCrashReportTask;

import org.unfoldingword.tools.logger.Logger;
import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.TaskManager;

public class CrashReporterActivity extends BaseActivity implements ManagedTask.OnFinishedListener {
    private Button mOkButton;
    private Button mCancelButton;
    private ProgressDialog mLoadingDialog;
    private EditText mCrashReportText;
    private static final String STATE_LATEST_RELEASE = "state_latest_release";
    private static final String STATE_NOTES = "state_notes";
    private String mNotes = "";
    private CheckForLatestReleaseTask.Release mLatestRelease = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash_reporter);

        mOkButton = (Button)findViewById(R.id.okButton);
        mCancelButton = (Button)findViewById(R.id.cancelButton);
        mCrashReportText = (EditText)findViewById(R.id.crashDescriptioneditText);

        mLoadingDialog = new ProgressDialog(CrashReporterActivity.this);
        mLoadingDialog.setCancelable(false);
        mLoadingDialog.setCanceledOnTouchOutside(false);

        mOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mNotes = mCrashReportText.getText().toString().trim();

                mLoadingDialog.setMessage(getResources().getString(R.string.loading));
                mLoadingDialog.show();

                CheckForLatestReleaseTask task = new CheckForLatestReleaseTask();
                task.addOnFinishedListener(CrashReporterActivity.this);
                TaskManager.addTask(task, CheckForLatestReleaseTask.TASK_ID);
            }
        });

        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Logger.flush();
                openSplash();
            }
        });


    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState != null) {
            mNotes = savedInstanceState.getString(STATE_NOTES, "");
            mLatestRelease = (CheckForLatestReleaseTask.Release)savedInstanceState.getSerializable(STATE_LATEST_RELEASE);
        }
        mCrashReportText.setText(mNotes);
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        CheckForLatestReleaseTask checkTask = (CheckForLatestReleaseTask) TaskManager.getTask(CheckForLatestReleaseTask.TASK_ID);
        UploadCrashReportTask uploadTask = (UploadCrashReportTask)TaskManager.getTask(UploadCrashReportTask.TASK_ID);

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

    /**
     * Displays a dialog to the user telling them there is an apk update.
     * @param release
     */
    private void notifyLatestRelease(final CheckForLatestReleaseTask.Release release) {
        new AlertDialog.Builder(this, R.style.AppTheme_Dialog)
                .setTitle(R.string.apk_update_available)
                .setMessage(R.string.upload_report_or_download_latest_apk)
                .setNegativeButton(R.string.title_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mLatestRelease = null;

                        Logger.flush();
                        openSplash();
                    }
                })
                .setNeutralButton(R.string.download_update, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Logger.flush();
                        getLatestAppVersion(CrashReporterActivity.this, mLatestRelease);
                        finish();
                    }
                })
                .setPositiveButton(R.string.label_continue, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mLoadingDialog.setMessage(getResources().getString(R.string.uploading));
                        mLoadingDialog.show();

                        UploadCrashReportTask newTask = new UploadCrashReportTask(mNotes);
                        newTask.addOnFinishedListener(CrashReporterActivity.this);
                        TaskManager.addTask(newTask, UploadCrashReportTask.TASK_ID);
                    }
                })
                .show();
    }

    private void openSplash() {
        Intent intent = new Intent(CrashReporterActivity.this, SplashScreenActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onTaskFinished(ManagedTask task) {
        TaskManager.clearTask(task);

        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                if (mLoadingDialog.isShowing()) {
                    mLoadingDialog.dismiss();
                }
            }
        });

        if(task instanceof CheckForLatestReleaseTask) {
            CheckForLatestReleaseTask.Release release = ((CheckForLatestReleaseTask)task).getLatestRelease();
            if(release != null) {
                // ask user if they would like to download updates
                mLatestRelease = release;
                hand.post(new Runnable() {
                    @Override
                    public void run() {
                        notifyLatestRelease(mLatestRelease);
                    }
                });
            } else {
                // upload crash report
                hand.post(new Runnable() {
                    @Override
                    public void run() {
                        mLoadingDialog.setMessage(getResources().getString(R.string.uploading));
                        mLoadingDialog.show();
                    }
                });

                UploadCrashReportTask newTask = new UploadCrashReportTask(mCrashReportText.getText().toString().trim());
                newTask.addOnFinishedListener(CrashReporterActivity.this);
                TaskManager.addTask(newTask, UploadCrashReportTask.TASK_ID);
            }
        } else if(task instanceof UploadCrashReportTask) {
            boolean success = ((UploadCrashReportTask) task).isSuccess();
            if(success) {
              openSplash();
            } else { // upload failed
                hand.post(new Runnable() {
                    @Override
                    public void run() {
                        new AlertDialog.Builder(CrashReporterActivity.this, R.style.AppTheme_Dialog)
                                .setTitle(R.string.upload_failed)
                                .setMessage(R.string.upload_crash_report_failed)
                                .setPositiveButton(R.string.label_ok, null)
                                .show();
                    }
                });
            }
        }
    }

    /**
     * download latest app version
     * @param activity
     * @param release
     * @return
     */
    public static boolean getLatestAppVersion(Activity activity, CheckForLatestReleaseTask.Release release) {
        if(release == null) {
            return false;
        }
        Boolean isStoreVersion = App.isStoreVersion();
        if (isStoreVersion) {
            // open play store
            final String appPackageName = activity.getPackageName();
            try {
                activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
            } catch (android.content.ActivityNotFoundException anfe) {
                activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
            }
        } else {
            // download from github
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(release.downloadUrl));
            activity.startActivity(browserIntent);
        }
        return true;
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
        UploadCrashReportTask uploadTask = (UploadCrashReportTask)TaskManager.getTask(UploadCrashReportTask.TASK_ID);
        if(uploadTask != null) {
            uploadTask.removeOnFinishedListener(this);
        }
        super.onDestroy();
    }
}
