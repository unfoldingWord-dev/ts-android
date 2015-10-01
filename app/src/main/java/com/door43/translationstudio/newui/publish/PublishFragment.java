package com.door43.translationstudio.newui.publish;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Project;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.newui.ReportBugDialog;
import com.door43.translationstudio.tasks.UploadTargetTranslationTask;
import com.door43.translationstudio.AppContext;
import com.door43.util.tasks.GenericTaskWatcher;
import com.door43.util.tasks.ManagedTask;
import com.door43.util.tasks.TaskManager;
import com.door43.widget.ViewUtil;

import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.security.InvalidParameterException;


/**
 * Created by joel on 9/20/2015.
 */
public class PublishFragment extends PublishStepFragment implements GenericTaskWatcher.OnFinishedListener {

    private static final String STATE_UPLOADED = "state_uploaded";
    private boolean mUploaded = false;
    private Button mUploadButton;
    private GenericTaskWatcher mTaskWatcher;
    private LinearLayout mUploadSuccess;
    private ProgressDialog mProgressDialog;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_publish_publish, container, false);

        HtmlTextView explanationView = (HtmlTextView)rootView.findViewById(R.id.explanation);
        explanationView.setHtmlFromString(getResources().getString(R.string.publishing_explanation), true);

        if(savedInstanceState != null) {
            mUploaded = savedInstanceState.getBoolean(STATE_UPLOADED, false);
        }

        Bundle args = getArguments();
        final String targetTranslationId = args.getString(PublishActivity.EXTRA_TARGET_TRANSLATION_ID);
        if (targetTranslationId == null) {
            throw new InvalidParameterException("a valid target translation id is required");
        }

        mTaskWatcher = new GenericTaskWatcher(getActivity(), R.string.uploading);
        mTaskWatcher.setOnFinishedListener(this);

        // receive uploaded status from activity (overrides save state from fragment)
        if(savedInstanceState == null) {
            mUploaded = args.getBoolean(ARG_PUBLISH_FINISHED, mUploaded);
        }

        final TargetTranslation targetTranslation = AppContext.getTranslator().getTargetTranslation(targetTranslationId);

        mUploadSuccess = (LinearLayout)rootView.findViewById(R.id.upload_success);
        mUploadButton = (Button)rootView.findViewById(R.id.upload_button);

        if(mUploaded) {
            mUploadButton.setVisibility(View.GONE);
            mUploadSuccess.setVisibility(View.VISIBLE);
        } else {
            mUploadButton.setVisibility(View.VISIBLE);
            mUploadSuccess.setVisibility(View.GONE);
        }

        // give the user some happy feedback in case they feel like clicking again
        mUploadSuccess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.success, Snackbar.LENGTH_SHORT);
                ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                snack.show();
            }
        });

        mUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(AppContext.context().isNetworkAvailable()) {
                    targetTranslation.setPublishable(true);
                    // begin upload
                    UploadTargetTranslationTask task = new UploadTargetTranslationTask(targetTranslation);
                    mTaskWatcher.watch(task);
                    TaskManager.addTask(task, UploadTargetTranslationTask.TASK_ID);
                    // TODO: display progress dialog
                } else {
                    Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.internet_not_available, Snackbar.LENGTH_LONG);
                    ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                    snack.show();
                }
            }
        });

        ImageView wifiIcon = (ImageView)rootView.findViewById(R.id.wifi_icon);
        ViewUtil.tintViewDrawable(wifiIcon, getResources().getColor(R.color.dark_secondary_text));

        UploadTargetTranslationTask task = (UploadTargetTranslationTask)TaskManager.getTask(UploadTargetTranslationTask.TASK_ID);
        if(task != null) {
            mTaskWatcher.watch(task);
            // TODO: display progress dialog
        }

        // export buttons
        Button exportToApp = (Button)rootView.findViewById(R.id.export_to_app);
        exportToApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), "Coming soon", Snackbar.LENGTH_SHORT);
                ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                snack.show();
            }
        });
        Button exportToSD = (Button)rootView.findViewById(R.id.export_to_sdcard);
        exportToSD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), "Coming soon", Snackbar.LENGTH_SHORT);
                ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                snack.show();
            }
        });
        Button exportToDevice = (Button)rootView.findViewById(R.id.export_to_device);
        exportToDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), "Coming soon", Snackbar.LENGTH_SHORT);
                ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                snack.show();
            }
        });
        return rootView;
    }

    @Override
    public void onFinished(ManagedTask task) {
        mTaskWatcher.stop();

        if(((UploadTargetTranslationTask)task).uploadSucceeded()) {
            final String response = ((UploadTargetTranslationTask)task).getResponse();
            mUploaded = true;
            // marks the publish step as done
            getListener().finishPublishing();
            Handler hand = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    if(mProgressDialog != null && mProgressDialog.isShowing()) {
                        mProgressDialog.dismiss();
                    }
                    mUploadButton.setVisibility(View.GONE);
                    mUploadSuccess.setVisibility(View.VISIBLE);

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(R.string.success).setMessage(R.string.git_push_success).setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //
                        }
                    }).setNeutralButton(R.string.label_details, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setTitle(R.string.git_push_success).setMessage(response).setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //
                                }
                            }).show();
                        }
                    }).show();
                }
            });
        } else {
            final TargetTranslation targetTranslation = ((UploadTargetTranslationTask)task).getTargetTranslation();
            final Project project = AppContext.getLibrary().getProject(targetTranslation.getProjectId(), "en");
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.success)
                    .setMessage(R.string.upload_failed)
                    .setPositiveButton(R.string.label_ok, null)
                    .setNeutralButton(R.string.menu_bug, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();

                            // open bug report dialog
                            FragmentTransaction ft = getFragmentManager().beginTransaction();
                            Fragment prev = getFragmentManager().findFragmentByTag("bugDialog");
                            if (prev != null) {
                                ft.remove(prev);
                            }
                            ft.addToBackStack(null);

                            ReportBugDialog dialog = new ReportBugDialog();
                            Bundle args = new Bundle();
                            String message = "Failed to publish the translation of " +
                                    project.name + " into " +
                                    targetTranslation.getTargetLanguageName()
                                    + ".\ntargetTranslation: " + targetTranslation.getId() +
                                    "\n--------\n\n";
                            args.putString(ReportBugDialog.ARG_MESSAGE, message);
                            dialog.setArguments(args);
                            dialog.show(ft, "bugDialog");
                        }
                    }).show();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        out.putBoolean(STATE_UPLOADED, mUploaded);
        super.onSaveInstanceState(out);
    }

    public void onDestroy() {
        mTaskWatcher.stop();
        super.onDestroy();
    }
}
