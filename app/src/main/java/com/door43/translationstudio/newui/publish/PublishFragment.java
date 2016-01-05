package com.door43.translationstudio.newui.publish;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Project;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.newui.FeedbackDialog;
import com.door43.translationstudio.tasks.UploadTargetTranslationTask;
import com.door43.translationstudio.AppContext;
import com.door43.util.tasks.GenericTaskWatcher;
import com.door43.util.tasks.ManagedTask;
import com.door43.util.tasks.TaskManager;
import com.door43.widget.ViewUtil;

import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.io.File;
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

        mTaskWatcher = new GenericTaskWatcher(getActivity(), R.string.publish);
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
                    try {
                        final Handler hand = new Handler(Looper.getMainLooper());
                        targetTranslation.setPublishable(true, new TargetTranslation.OnCommitListener() {
                            @Override
                            public void onCommit(boolean success) {
                                if(!success) {
                                    hand.post(new Runnable() {
                                        @Override
                                        public void run() {
                                        notifyPublishFailed(targetTranslation);
                                        }
                                    });
                                } else {
                                    targetTranslation.setPublishTag(null); //TODO: move to completion
                                    // begin upload
                                    UploadTargetTranslationTask task = new UploadTargetTranslationTask(targetTranslation);
                                    mTaskWatcher.watch(task);
                                    TaskManager.addTask(task, UploadTargetTranslationTask.TASK_ID);
                                }
                            }
                        });
                    } catch (Exception e) {
                        Logger.e(PublishFragment.class.getName(), "Failed to mark target translation " + targetTranslation.getId() + " as publishable", e);
                        notifyPublishFailed(targetTranslation);
                        return;
                    }
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
        }

        final String filename = targetTranslation.getId() + ".zip";

        // export buttons
        Button exportToApp = (Button)rootView.findViewById(R.id.backup_to_app);
        exportToApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File exportFile = new File(AppContext.getSharingDir(), filename);
                try {
                    AppContext.getTranslator().exportDokuWiki(targetTranslation, exportFile);
                } catch (Exception e) {
                    Logger.e(PublishFragment.class.getName(), "Failed to export the target translation " + targetTranslation.getId(), e);
                }
                if(exportFile.exists()) {
                    Uri u = FileProvider.getUriForFile(getActivity(), "com.door43.translationstudio.fileprovider", exportFile);
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("application/zip");
                    i.putExtra(Intent.EXTRA_STREAM, u);
                    startActivity(Intent.createChooser(i, "Email:"));
                } else {
                    Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.translation_export_failed, Snackbar.LENGTH_LONG);
                    ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                    snack.show();
                }
            }
        });
        Button exportToSD = (Button)rootView.findViewById(R.id.export_to_sdcard);
        exportToSD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: 10/27/2015 have the user choose where to save the file
                File exportFile = new File(AppContext.getPublicDownloadsDirectory(), System.currentTimeMillis() / 1000L + "_" + filename);
                try {
                    AppContext.getTranslator().exportDokuWiki(targetTranslation, exportFile);
                } catch (Exception e) {
                    Logger.e(PublishFragment.class.getName(), "Failed to export the target translation " + targetTranslation.getId(), e);
                }
                if(exportFile.exists()) {
                    Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.success, Snackbar.LENGTH_LONG);
                    ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                    snack.show();
                } else {
                    Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.translation_export_failed, Snackbar.LENGTH_LONG);
                    ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                    snack.show();
                }
            }
        });
        Button exportToDevice = (Button)rootView.findViewById(R.id.backup_to_device);
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
                    mUploadButton.setVisibility(View.GONE);
                    mUploadSuccess.setVisibility(View.VISIBLE);

                    CustomAlertDialog.Create(getActivity())
                        .setTitle(R.string.success).setMessage(R.string.project_uploaded).setPositiveButton(R.string.dismiss, null)
                        .setNeutralButton(R.string.label_details, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                CustomAlertDialog.Create(getActivity())
                                    .setTitle(R.string.project_uploaded).setMessage(response).setPositiveButton(R.string.dismiss, null).show("PubDetails");
                            }
                        }).show("PubFinished");
                }
            });
        } else {
            TargetTranslation targetTranslation = ((UploadTargetTranslationTask)task).getTargetTranslation();
            notifyPublishFailed(targetTranslation);
        }
    }

    /**
     * Displays a dialog to the user indicating the publish failed.
     * Includes an option to submit a bug report
     * @param targetTranslation
     */
    private void notifyPublishFailed(final TargetTranslation targetTranslation) {
        final Project project = AppContext.getLibrary().getProject(targetTranslation.getProjectId(), "en");
        CustomAlertDialog.Create(getActivity())
                .setTitle(R.string.publish)
                .setMessage(R.string.upload_failed)
                .setPositiveButton(R.string.dismiss, null)
                .setNeutralButton(R.string.menu_bug, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        // open bug report dialog
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        Fragment prev = getFragmentManager().findFragmentByTag("bugDialog");
                        if (prev != null) {
                            ft.remove(prev);
                        }
                        ft.addToBackStack(null);

                        FeedbackDialog dialog = new FeedbackDialog();
                        Bundle args = new Bundle();
                        String message = "Failed to publish the translation of " +
                                project.name + " into " +
                                targetTranslation.getTargetLanguageName()
                                + ".\ntargetTranslation: " + targetTranslation.getId() +
                                "\n--------\n\n";
                        args.putString(FeedbackDialog.ARG_MESSAGE, message);
                        dialog.setArguments(args);
                        dialog.show(ft, "bugDialog");
                    }
                }).show("PublishFail");
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        out.putBoolean(STATE_UPLOADED, mUploaded);
        super.onSaveInstanceState(out);
    }

    @Override
    public void onDestroy() {
        mTaskWatcher.stop();
        super.onDestroy();
    }
}
