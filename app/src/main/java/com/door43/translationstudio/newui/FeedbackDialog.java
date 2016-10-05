package com.door43.translationstudio.newui;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.tasks.CheckForLatestReleaseTask;
import com.door43.translationstudio.tasks.UploadBugReportTask;
import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.TaskManager;
import com.door43.widget.ViewUtil;

/**
 * Created by joel on 9/17/2015.
 */
public class FeedbackDialog extends DialogFragment implements ManagedTask.OnFinishedListener {

    private static final String STATE_LATEST_RELEASE = "latest_release";
    private static final String STATE_NOTES = "bug_notes";
    public static final String ARG_MESSAGE = "arg_message";
    private String mMessage = "";
    private CheckForLatestReleaseTask.Release mLatestRelease;
    private LinearLayout mLoadingLayout;
    private LinearLayout mFormLayout;
    private LinearLayout mControlsLayout;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_feedback, container, false);

        if(savedInstanceState == null) {
            Bundle args = getArguments();
            if (args != null) {
                mMessage = args.getString(ARG_MESSAGE, "");
            }
        }

        mLoadingLayout = (LinearLayout)v.findViewById(R.id.loadingLayout);
        mFormLayout = (LinearLayout)v.findViewById(R.id.formLayout);
        mControlsLayout = (LinearLayout)v.findViewById(R.id.controlsLayout);

        ImageView wifiIcon = (ImageView)v.findViewById(R.id.wifi_icon);
        ViewUtil.tintViewDrawable(wifiIcon, getActivity().getResources().getColor(R.color.dark_secondary_text));
        Button cancelButton = (Button)v.findViewById(R.id.cancelButton);
        Button confirmButton = (Button)v.findViewById(R.id.confirmButton);
        final EditText editText = (EditText)v.findViewById(R.id.editText);
        editText.setText(mMessage);
        editText.setSelection(editText.getText().length());

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(editText.getText().toString().isEmpty()) {
                    // requires text
                    notifyInputRequired();
                } else {
                    reportBug(editText.getText().toString().trim());
                }
            }
        });

        if(savedInstanceState != null) {
            mMessage = savedInstanceState.getString(STATE_NOTES, "");
            mLatestRelease = (CheckForLatestReleaseTask.Release)savedInstanceState.getSerializable(STATE_LATEST_RELEASE);
        }

        // connect to existing tasks
        CheckForLatestReleaseTask checkTask = (CheckForLatestReleaseTask) TaskManager.getTask(CheckForLatestReleaseTask.TASK_ID);
        UploadBugReportTask uploadTask = (UploadBugReportTask)TaskManager.getTask(UploadBugReportTask.TASK_ID);

        if(checkTask != null) {
            showLoadingUI();
            checkTask.addOnFinishedListener(this);
        } else if(uploadTask != null) {
            showLoadingUI();
            uploadTask.addOnFinishedListener(this);
        } else if(mLatestRelease != null) {
            showLoadingUI();
            notifyLatestRelease(mLatestRelease);
        }

        return v;
    }

    private void notifyInputRequired() {
        Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.input_required, Snackbar.LENGTH_SHORT);
        ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
        snack.show();
    }

    private void reportBug(String message) {
        mMessage = message;
        showLoadingUI();
        CheckForLatestReleaseTask task = new CheckForLatestReleaseTask();
        task.addOnFinishedListener(FeedbackDialog.this);
        TaskManager.addTask(task, CheckForLatestReleaseTask.TASK_ID);
    }

    private void showLoadingUI() {
        mFormLayout.setVisibility(View.GONE);
        mControlsLayout.setVisibility(View.GONE);
        mLoadingLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onTaskFinished(ManagedTask task) {
        TaskManager.clearTask(task);

        if(task instanceof CheckForLatestReleaseTask) {
            CheckForLatestReleaseTask.Release release = ((CheckForLatestReleaseTask)task).getLatestRelease();
            if(release != null) {
                mLatestRelease = release;
                Handler hand = new Handler(Looper.getMainLooper());
                hand.post(new Runnable() {
                    @Override
                    public void run() {
                        notifyLatestRelease(mLatestRelease);
                    }
                });
            } else {
                if(!mMessage.isEmpty()) {
                    UploadBugReportTask newTask = new UploadBugReportTask(mMessage);
                    newTask.addOnFinishedListener(FeedbackDialog.this);
                    TaskManager.addTask(newTask, UploadBugReportTask.TASK_ID);
                } else {
                    notifyInputRequired();
                    FeedbackDialog.this.dismiss();
                }
            }
        } else if(task instanceof UploadBugReportTask) {
            boolean success = ((UploadBugReportTask) task).isSuccess();
            Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), success ? R.string.success : R.string.upload_failed_label, Snackbar.LENGTH_LONG);
            ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
            snack.show();
            FeedbackDialog.this.dismiss();
        }
    }

    /**
     * Displays a dialog to the user telling them there is an apk update.
     * @param release
     */
    private void notifyLatestRelease(final CheckForLatestReleaseTask.Release release) {
        final Boolean isStoreVersion = App.isStoreVersion();

        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                .setTitle(R.string.apk_update_available)
                .setMessage(R.string.upload_report_or_download_latest_apk)
                .setNegativeButton(R.string.title_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mLatestRelease = null;
                        FeedbackDialog.this.dismiss();
                    }
                })
                .setNeutralButton(R.string.download_update, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (isStoreVersion) {
                            // open play store
                            final String appPackageName = getActivity().getPackageName();
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
                        FeedbackDialog.this.dismiss();
                    }
                })
                .setPositiveButton(R.string.label_continue, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!mMessage.isEmpty()) {
                            UploadBugReportTask newTask = new UploadBugReportTask(mMessage);
                            newTask.addOnFinishedListener(FeedbackDialog.this);
                            TaskManager.addTask(newTask, UploadBugReportTask.TASK_ID);
                        } else {
                            notifyInputRequired();
                            FeedbackDialog.this.dismiss();
                        }
                    }
                })
                .show();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if(mLatestRelease != null) {
            outState.putSerializable(STATE_LATEST_RELEASE, mLatestRelease);
        } else {
            outState.remove(STATE_LATEST_RELEASE);
        }
        outState.putString(STATE_NOTES, mMessage);
        super.onSaveInstanceState(outState);
    }

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
