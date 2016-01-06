package com.door43.translationstudio.newui;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v4.provider.DocumentFile;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Project;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.tasks.UploadTargetTranslationTask;
import com.door43.translationstudio.util.SdUtils;
import com.door43.util.tasks.GenericTaskWatcher;
import com.door43.util.tasks.ManagedTask;
import com.door43.util.tasks.TaskManager;
import com.door43.widget.ViewUtil;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.OutputStream;
import java.security.InvalidParameterException;

/**
 * Created by joel on 10/5/2015.
 */
public class BackupDialog extends DialogFragment implements GenericTaskWatcher.OnFinishedListener {

    public static final String ARG_TARGET_TRANSLATION_ID = "target_translation_id";
    public static final String TAG = "backup-dialog";
    private static final String STATE_SETTING_DEVICE_ALIAS = "state_setting_device_alias";
    private TargetTranslation mTargetTranslation;
    private GenericTaskWatcher mTaskWatcher;
    private boolean settingDeviceAlias = false;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_backup, container, false);

        // get target translation to backup
        Bundle args = getArguments();
        if(args != null && args.containsKey(ARG_TARGET_TRANSLATION_ID)) {
            String targetTranslationId = args.getString(ARG_TARGET_TRANSLATION_ID, null);
            mTargetTranslation = AppContext.getTranslator().getTargetTranslation(targetTranslationId);
            if(mTargetTranslation == null) {
                throw new InvalidParameterException("The target translation '" + targetTranslationId + "' is invalid");
            }
        } else {
            throw new InvalidParameterException("The target translation id was not specified");
        }

        // TODO: 11/11/2015 check if at least one translator has been recorded on this target translation
        // if there are no translators the user must be presented with a form to enter a translator.

        Button backupToCloudButton = (Button)v.findViewById(R.id.backup_to_cloud);
        Button backupToSDButton = (Button)v.findViewById(R.id.backup_to_sd);
        Button backupToAppButton = (Button)v.findViewById(R.id.backup_to_app);
        Button backupToDeviceButton = (Button)v.findViewById(R.id.backup_to_device);

        final String filename = mTargetTranslation.getId() + "." + Translator.ARCHIVE_EXTENSION;

        mTaskWatcher = new GenericTaskWatcher(getActivity(), R.string.backup);
        mTaskWatcher.setOnFinishedListener(this);

        // connect to existing tasks
        UploadTargetTranslationTask task = (UploadTargetTranslationTask)TaskManager.getTask(UploadTargetTranslationTask.TASK_ID);
        if(task != null) {
            mTaskWatcher.watch(task);
        }

        if(savedInstanceState != null) {
            // check if returning from device alias dialog
            settingDeviceAlias = savedInstanceState.getBoolean(STATE_SETTING_DEVICE_ALIAS, false);
        }

        Button dismissButton = (Button)v.findViewById(R.id.dismiss_button);
        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        backupToDeviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: 11/18/2015 eventually we need to support bluetooth as well as an adhoc network
                if(AppContext.context().isNetworkAvailable()) {
                    if(AppContext.getDeviceNetworkAlias() == null) {
                        // get device alias
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        Fragment prev = getFragmentManager().findFragmentByTag(BackupDialog.TAG);
                        if (prev != null) {
                            ft.remove(prev);
                        }
                        ft.addToBackStack(null);

                        settingDeviceAlias = true;
                        DeviceNetworkAliasDialog dialog = new DeviceNetworkAliasDialog();
                        dialog.show(ft, BackupDialog.TAG);
                    } else {
                        showP2PDialog();
                    }
                } else {
                    Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.internet_not_available, Snackbar.LENGTH_LONG);
                    ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                    snack.show();
                }
            }
        });

        // backup buttons
        backupToCloudButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(AppContext.context().isNetworkAvailable()) {
                    try {
                        final Handler hand = new Handler(Looper.getMainLooper());
                        mTargetTranslation.setPublishable(false, new TargetTranslation.OnCommitListener() {
                            @Override
                            public void onCommit(boolean success) {
                                if(!success) {
                                    hand.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            notifyBackupFailed(mTargetTranslation);
                                        }
                                    });
                                } else {
                                    // begin upload
                                    UploadTargetTranslationTask task = new UploadTargetTranslationTask(mTargetTranslation);
                                    mTaskWatcher.watch(task);
                                    TaskManager.addTask(task, UploadTargetTranslationTask.TASK_ID);
                                }
                            }
                        });
                    } catch (Exception e) {
                        Logger.e(BackupDialog.class.getName(), "Failed to mark target translation " + mTargetTranslation.getId() + " as not publishable", e);
                        notifyBackupFailed(mTargetTranslation);
                        return;
                    }
                } else {
                    Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.internet_not_available, Snackbar.LENGTH_LONG);
                    ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                    snack.show();
                }
            }
        });

        backupToSDButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SdUtils.doWeNeedToRequestSdCardAccess()) {
                    final CustomAlertDialog dialog = CustomAlertDialog.Create(getActivity());
                    dialog.setTitle(R.string.enable_sd_card_access_title)
                            .setMessageHtml(R.string.enable_sd_card_access)
                            .setPositiveButton(R.string.confirm, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    SdUtils.triggerStorageAccessFramework(getActivity());
                                }
                            })
                            .setNegativeButton(R.string.label_skip, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    doSdCardBackup(filename);
                                }
                            })
                            .show("approve-SD-access");
                } else {
                    doSdCardBackup(filename);
                }
            }
        });

        backupToAppButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File exportFile = new File(AppContext.getSharingDir(), filename);
                try {
                    AppContext.getTranslator().exportArchive(mTargetTranslation, exportFile);
                } catch (Exception e) {
                    Logger.e(BackupDialog.class.getName(), "Failed to export the target translation " + mTargetTranslation.getId(), e);
                }
                if (exportFile.exists()) {
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

        return v;
    }

    /**
     * back up project - will try to write to SD card if available - otherwise will save to internal memory
     * @param filename
     */
    private void doSdCardBackup(String filename) {
        // TODO: 10/27/2015 have the user choose the file location
        String fileName = System.currentTimeMillis() / 1000L + "_" + filename;
        boolean success = false;
        boolean canWriteToSdCardBackupLollipop = false;
        DocumentFile baseFolder = null;
        String filePath = null;
        DocumentFile sdCardFile = null;
        OutputStream out = null;

        try {
            if(SdUtils.isSdCardPresentLollipop()) {
                baseFolder = SdUtils.sdCardMkdirs(SdUtils.DOWNLOAD_TRANSLATION_STUDIO_FOLDER);
                canWriteToSdCardBackupLollipop = baseFolder != null;
            }

            if (canWriteToSdCardBackupLollipop) { // default to writing to SD card if available
                filePath = SdUtils.getPathString(baseFolder);
                if (baseFolder.canWrite()) {
                    sdCardFile = baseFolder.createFile("image", fileName);
                    filePath = SdUtils.getPathString(sdCardFile);
                    out = AppContext.context().getContentResolver().openOutputStream(sdCardFile.getUri());
                    AppContext.getTranslator().exportArchive(mTargetTranslation, out, fileName);
                    success = true;
                }
            } else {
                File exportFile = new File(AppContext.getPublicDownloadsDirectory(), fileName);
                filePath = exportFile.toString();
                AppContext.getTranslator().exportArchive(mTargetTranslation, exportFile);
                success = exportFile.exists();

            }
        } catch (Exception e) {
            success = false;
            if(sdCardFile != null) {
                try {
                    if(null != out) {
                        IOUtils.closeQuietly(out);
                    }
                    sdCardFile.delete();
                } catch(Exception e2) {
                }
            }
            Logger.e(BackupDialog.class.getName(), "Failed to export the target translation " + mTargetTranslation.getId(), e);
        }

        if (success) {
            showBackupResults(R.string.backup_success, filePath);
        } else {
            showBackupResults(R.string.backup_failed, filePath);
        }
    }

    private void showBackupResults(final int textResId, final String filePath) {
        String message = getResources().getString(textResId);
        if(filePath != null) {
            message += "\n" + filePath;
        }
        CustomAlertDialog.Create(getActivity())
                .setTitle(R.string.backup_to_sd)
                .setMessage(message)
                .setNeutralButton(R.string.dismiss, null)
                .show("Backup");
    }

    @Override
    public void onResume() {
        if(settingDeviceAlias && AppContext.getDeviceNetworkAlias() != null) {
            settingDeviceAlias = false;
            showP2PDialog();
        }
        super.onResume();
    }

    /**
     * Displays the dialog for p2p sharing
     */
    private void showP2PDialog() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag(BackupDialog.TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        ShareWithPeerDialog dialog = new ShareWithPeerDialog();
        Bundle args = new Bundle();
        args.putInt(ShareWithPeerDialog.ARG_OPERATION_MODE, ShareWithPeerDialog.MODE_SERVER);
        args.putString(ShareWithPeerDialog.ARG_TARGET_TRANSLATION, mTargetTranslation.getId());
        args.putString(ShareWithPeerDialog.ARG_DEVICE_ALIAS, AppContext.getDeviceNetworkAlias());
        dialog.setArguments(args);
        dialog.show(ft, BackupDialog.TAG);
    }

    /**
     * Displays a dialog to the user indicating the publish failed.
     * Includes an option to submit a bug report
     * @param targetTranslation
     */
    private void notifyBackupFailed(final TargetTranslation targetTranslation) {
        final Project project = AppContext.getLibrary().getProject(targetTranslation.getProjectId(), "en");
        CustomAlertDialog.Create(getActivity())
                .setTitle(R.string.backup)
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
                }).show("BackupFailed");
    }

    @Override
    public void onFinished(ManagedTask task) {
        mTaskWatcher.stop();
        TaskManager.clearTask(task);
        if(((UploadTargetTranslationTask)task).uploadSucceeded()) {
            final String response = ((UploadTargetTranslationTask)task).getResponse();
            Handler hand = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    CustomAlertDialog.Create(getActivity())
                        .setTitle(R.string.success).setMessage(R.string.project_uploaded)
                            .setPositiveButton(R.string.dismiss, null)
                            .setNeutralButton(R.string.label_details, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    CustomAlertDialog.Create(getActivity())
                                        .setTitle(R.string.project_uploaded).setMessage(response).setPositiveButton(R.string.dismiss, null).show("UploadDetails");
                                }
                            }).show("UploadSuccess");
                }
            });
        } else {
            TargetTranslation targetTranslation = ((UploadTargetTranslationTask)task).getTargetTranslation();
            notifyBackupFailed(targetTranslation);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        // remember if the device alias dialog is open
        out.putBoolean(STATE_SETTING_DEVICE_ALIAS, settingDeviceAlias);
        super.onSaveInstanceState(out);
    }

    @Override
    public void onDestroy() {
        if(mTaskWatcher != null) {
            mTaskWatcher.stop();
        }

        super.onDestroy();
    }
}
