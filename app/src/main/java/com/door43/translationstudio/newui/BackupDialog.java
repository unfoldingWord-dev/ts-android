package com.door43.translationstudio.newui;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Project;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationViewMode;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.newui.translate.TargetTranslationActivity;
import com.door43.translationstudio.tasks.CreateRepositoryTask;
import com.door43.translationstudio.tasks.PullTargetTranslationTask;
import com.door43.translationstudio.tasks.PushTargetTranslationTask;
import com.door43.translationstudio.tasks.RegisterSSHKeysTask;
import com.door43.translationstudio.util.SdUtils;
import org.unfoldingword.tools.taskmanager.SimpleTaskWatcher;
import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.TaskManager;
import com.door43.widget.ViewUtil;

import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.merge.MergeStrategy;

import java.io.File;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.util.Map;

/**
 * Created by joel on 10/5/2015.
 */
public class BackupDialog extends DialogFragment implements SimpleTaskWatcher.OnFinishedListener {

    public static final String ARG_TARGET_TRANSLATION_ID = "target_translation_id";
    public static final String TAG = "backup-dialog";
    private static final String STATE_SETTING_DEVICE_ALIAS = "state_setting_device_alias";
    private TargetTranslation targetTranslation;
    private SimpleTaskWatcher taskWatcher;
    private boolean settingDeviceAlias = false;
    private Button mBackupToCloudButton = null;

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
            targetTranslation = App.getTranslator().getTargetTranslation(targetTranslationId);
            if(targetTranslation == null) {
                throw new InvalidParameterException("The target translation '" + targetTranslationId + "' is invalid");
            }
        } else {
            throw new InvalidParameterException("The target translation id was not specified");
        }

        targetTranslation.setDefaultContributor(App.getProfile().getNativeSpeaker());

        mBackupToCloudButton = (Button)v.findViewById(R.id.backup_to_cloud);
        Button backupToSDButton = (Button)v.findViewById(R.id.backup_to_sd);
        Button backupToAppButton = (Button)v.findViewById(R.id.backup_to_app);
        Button backupToDeviceButton = (Button)v.findViewById(R.id.backup_to_device);

        final String filename = targetTranslation.getId() + "." + Translator.ARCHIVE_EXTENSION;

        taskWatcher = new SimpleTaskWatcher(getActivity(), R.string.backup);
        taskWatcher.setOnFinishedListener(this);

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
                if(App.isNetworkAvailable()) {
                    if(App.getDeviceNetworkAlias() == null) {
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
        mBackupToCloudButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(App.isNetworkAvailable()) {
                    // make sure we have a gogs user
                    if(App.getProfile().gogsUser == null) {
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        Door43LoginDialog dialog = new Door43LoginDialog();
                        dialog.show(ft, Door43LoginDialog.TAG);
                        return;
                    }

                    PullTargetTranslationTask task = new PullTargetTranslationTask(targetTranslation);
                    taskWatcher.watch(task);
                    TaskManager.addTask(task, PullTargetTranslationTask.TASK_ID);
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
                    new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                        .setTitle(R.string.enable_sd_card_access_title)
                        .setMessage(Html.fromHtml(getString(R.string.enable_sd_card_access)))
                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SdUtils.triggerStorageAccessFramework(getActivity());
                            }
                        })
                        .setNegativeButton(R.string.label_skip, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                doSdCardBackup(filename);
                            }
                        })
                        .show();
                } else {
                    doSdCardBackup(filename);
                }
            }
        });

        backupToAppButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File exportFile = new File(App.getSharingDir(), filename);
                try {
                    App.getTranslator().exportArchive(targetTranslation, exportFile);
                } catch (Exception e) {
                    Logger.e(BackupDialog.class.getName(), "Failed to export the target translation " + targetTranslation.getId(), e);
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

        // connect to existing tasks
        PullTargetTranslationTask pullTask = (PullTargetTranslationTask)TaskManager.getTask(PullTargetTranslationTask.TASK_ID);
        RegisterSSHKeysTask keysTask = (RegisterSSHKeysTask)TaskManager.getTask(RegisterSSHKeysTask.TASK_ID);
        CreateRepositoryTask repoTask = (CreateRepositoryTask)TaskManager.getTask(CreateRepositoryTask.TASK_ID);
        PushTargetTranslationTask pushTask = (PushTargetTranslationTask)TaskManager.getTask(PushTargetTranslationTask.TASK_ID);

        if(pullTask != null) {
            taskWatcher.watch(pullTask);
        } else if (keysTask != null) {
            taskWatcher.watch(keysTask);
        } else if(repoTask != null) {
            taskWatcher.watch(repoTask);
        } else if(pushTask != null) {
            taskWatcher.watch(pushTask);
        }

        // attach to dialogs
        MergeConflictsDialog mergeConflictsDialog = (MergeConflictsDialog)getFragmentManager().findFragmentByTag(MergeConflictsDialog.TAG);
        if(mergeConflictsDialog != null) {
            attachMergeConflictListener(mergeConflictsDialog);
        }

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
                    out = App.context().getContentResolver().openOutputStream(sdCardFile.getUri());
                    App.getTranslator().exportArchive(targetTranslation, out, fileName);
                    success = true;
                }
            } else {
                File exportFile = new File(App.getPublicDownloadsDirectory(), fileName);
                filePath = exportFile.toString();
                App.getTranslator().exportArchive(targetTranslation, exportFile);
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
            Logger.e(BackupDialog.class.getName(), "Failed to export the target translation " + targetTranslation.getId(), e);
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
        new AlertDialog.Builder(getActivity(),R.style.AppTheme_Dialog)
                .setTitle(R.string.backup_to_sd)
                .setMessage(message)
                .setNeutralButton(R.string.dismiss, null)
                .show();
    }

    @Override
    public void onResume() {
        if(settingDeviceAlias && App.getDeviceNetworkAlias() != null) {
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
        args.putString(ShareWithPeerDialog.ARG_TARGET_TRANSLATION, targetTranslation.getId());
        args.putString(ShareWithPeerDialog.ARG_DEVICE_ALIAS, App.getDeviceNetworkAlias());
        dialog.setArguments(args);
        dialog.show(ft, BackupDialog.TAG);
    }

    @Override
    public void onFinished(ManagedTask task) {
        taskWatcher.stop();
        if(task instanceof PullTargetTranslationTask) {
            PullTargetTranslationTask.Status status = ((PullTargetTranslationTask)task).getStatus();
            //  TRICKY: we continue to push for unknown status in case the repo was just created (the missing branch is an error)
            // the pull task will catch any errors
            if(status == PullTargetTranslationTask.Status.UP_TO_DATE
                    || status == PullTargetTranslationTask.Status.UNKNOWN) {
                Logger.i(this.getClass().getName(), "Changes on the server were synced with " + targetTranslation.getId());

                PushTargetTranslationTask pushtask = new PushTargetTranslationTask(targetTranslation, false);
                taskWatcher.watch(pushtask);
                TaskManager.addTask(pushtask, PushTargetTranslationTask.TASK_ID);
            } else if(status == PullTargetTranslationTask.Status.AUTH_FAILURE) {
                Logger.i(this.getClass().getName(), "Authentication failed");
                // if we have already tried ask the user if they would like to try again
                if(App.hasSSHKeys()) {
                    showAuthFailure();
                    return;
                }

                RegisterSSHKeysTask keyTask = new RegisterSSHKeysTask(false);
                taskWatcher.watch(keyTask);
                TaskManager.addTask(keyTask, RegisterSSHKeysTask.TASK_ID);
            } else if(status == PullTargetTranslationTask.Status.NO_REMOTE_REPO) {
                Logger.i(this.getClass().getName(), "The repository " + targetTranslation.getId() + " could not be found");
                // create missing repo
                CreateRepositoryTask repoTask = new CreateRepositoryTask(targetTranslation);
                taskWatcher.watch(repoTask);
                TaskManager.addTask(repoTask, CreateRepositoryTask.TASK_ID);
            } else if(status == PullTargetTranslationTask.Status.MERGE_CONFLICTS) {
                Logger.i(this.getClass().getName(), "The server contains conflicting changes for " + targetTranslation.getId());
                notifyMergeConflicts(((PullTargetTranslationTask)task).getConflicts());
            } else {
                notifyBackupFailed(targetTranslation);
            }
        } else if(task instanceof RegisterSSHKeysTask) {
            if(((RegisterSSHKeysTask)task).isSuccess()) {
                Logger.i(this.getClass().getName(), "SSH keys were registered with the server");
                // try to push again
                PullTargetTranslationTask pullTask = new PullTargetTranslationTask(targetTranslation);
                taskWatcher.watch(pullTask);
                TaskManager.addTask(pullTask, PullTargetTranslationTask.TASK_ID);
            } else {
                notifyBackupFailed(targetTranslation);
            }
        } else if(task instanceof CreateRepositoryTask) {
            if(((CreateRepositoryTask)task).isSuccess()) {
                Logger.i(this.getClass().getName(), "A new repository " + targetTranslation.getId() + " was created on the server");
                PullTargetTranslationTask pullTask = new PullTargetTranslationTask(targetTranslation);
                taskWatcher.watch(pullTask);
                TaskManager.addTask(pullTask, PullTargetTranslationTask.TASK_ID);
            } else {
                notifyBackupFailed(targetTranslation);
            }
        } else if(task instanceof PushTargetTranslationTask) {
            PushTargetTranslationTask.Status status =((PushTargetTranslationTask)task).getStatus();
            final String message = ((PushTargetTranslationTask)task).getMessage();

            if(status == PushTargetTranslationTask.Status.OK) {
                Logger.i(this.getClass().getName(), "The target translation " + targetTranslation.getId() + " was pushed to the server");
                new AlertDialog.Builder(getActivity(),R.style.AppTheme_Dialog)
                        .setTitle(R.string.success)
                        .setMessage(R.string.project_uploaded)
                        .setPositiveButton(R.string.dismiss, null)
                        .setNeutralButton(R.string.label_details, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                                    .setTitle(R.string.project_uploaded)
                                    .setMessage(message)
                                    .setPositiveButton(R.string.dismiss, null)
                                    .show();
                            }
                        }).show();
            } else if(status == PushTargetTranslationTask.Status.AUTH_FAILURE) {
                Logger.i(this.getClass().getName(), "Authentication failed");
                showAuthFailure();
            } else {
                notifyBackupFailed(targetTranslation);
            }

        }
    }

    private void notifyMergeConflicts(Map<String, int[][]> conflicts) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        MergeConflictsDialog dialog = new MergeConflictsDialog();
        attachMergeConflictListener(dialog);
        dialog.show(ft, MergeConflictsDialog.TAG);
    }

    private void attachMergeConflictListener(MergeConflictsDialog dialog) {
        dialog.setOnClickListener(new MergeConflictsDialog.OnClickListener() {
            @Override
            public void onReview() {
                if(getActivity() instanceof  TargetTranslationActivity) {
                    ((TargetTranslationActivity) getActivity()).notifyDatasetChanged();
                    BackupDialog.this.dismiss();
                    // TODO: 4/20/16 it woulid be nice to navigate directly to the first conflict
                } else {
                    // ask parent activity to navigate to a new activity
                    Intent intent = new Intent(getActivity(), TargetTranslationActivity.class);
                    Bundle args = new Bundle();
                    args.putString(App.EXTRA_TARGET_TRANSLATION_ID, targetTranslation.getId());
                    // TODO: 4/20/16 it woulid be nice to navigate directly to the first conflict
//                args.putString(App.EXTRA_CHAPTER_ID, chapterId);
//                args.putString(App.EXTRA_FRAME_ID, frameId);
                    args.putString(App.EXTRA_VIEW_MODE, TranslationViewMode.REVIEW.toString());
                    intent.putExtras(args);
                    startActivity(intent);
                    getActivity().finish();
                }
            }

            @Override
            public void onKeepServer() {
                try {
                    Git git = targetTranslation.getRepo().getGit();
                    ResetCommand resetCommand = git.reset();
                    resetCommand.setMode(ResetCommand.ResetType.HARD)
                            .setRef("backup-master")
                            .call();

                    // try to pull again
                    PullTargetTranslationTask pullTask = new PullTargetTranslationTask(targetTranslation, MergeStrategy.THEIRS);
                    taskWatcher.watch(pullTask);
                    TaskManager.addTask(pullTask, PullTargetTranslationTask.TASK_ID);
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "Failed to keep server changes durring publish", e);
                    notifyBackupFailed(targetTranslation);
                }
            }

            @Override
            public void onKeepLocal() {
                try {
                    Git git = targetTranslation.getRepo().getGit();
                    ResetCommand resetCommand = git.reset();
                    resetCommand.setMode(ResetCommand.ResetType.HARD)
                            .setRef("backup-master")
                            .call();

                    // try to pull again
                    PullTargetTranslationTask pullTask = new PullTargetTranslationTask(targetTranslation, MergeStrategy.OURS);
                    taskWatcher.watch(pullTask);
                    TaskManager.addTask(pullTask, PullTargetTranslationTask.TASK_ID);
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "Failed to keep local changes durring publish", e);
                    notifyBackupFailed(targetTranslation);
                }
            }

            @Override
            public void onCancel() {
                try {
                    Git git = targetTranslation.getRepo().getGit();
                    ResetCommand resetCommand = git.reset();
                    resetCommand.setMode(ResetCommand.ResetType.HARD)
                            .setRef("backup-master")
                            .call();
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "Failed to restore local changes", e);
                }
                // TODO: 4/20/16 notify canceled
            }
        });
    }

    /**
     * Displays a dialog to the user indicating the publish failed.
     * Includes an option to submit a bug report
     * @param targetTranslation
     */
    private void notifyBackupFailed(final TargetTranslation targetTranslation) {
        final Project project = App.getLibrary().getProject(targetTranslation.getProjectId(), "en");
        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
        .setTitle(R.string.backup)
                .setMessage(R.string.upload_failed)
                .setPositiveButton(R.string.dismiss, null)
                .setNeutralButton(R.string.menu_bug, new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                         // open bug report dialog
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        Fragment prev = getFragmentManager().findFragmentByTag("bugDialog");
                        if (prev != null) {
                            ft.remove(prev);
                        }
                        ft.addToBackStack(null);

                        FeedbackDialog feedbackDialog = new FeedbackDialog();
                        Bundle args = new Bundle();
                        String message = "Failed to backup the translation of " +
                                project.name + " into " +
                                targetTranslation.getTargetLanguageName()
                                + ".\ntargetTranslation: " + targetTranslation.getId() +
                                "\n--------\n\n";
                        args.putString(FeedbackDialog.ARG_MESSAGE, message);
                        feedbackDialog.setArguments(args);
                        feedbackDialog.show(ft, "bugDialog");
                    }
                }).show();
    }

    public void showAuthFailure() {
        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                .setTitle(R.string.upload_failed)
                .setMessage(R.string.auth_failure_retry)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RegisterSSHKeysTask keyTask = new RegisterSSHKeysTask(true);
                        taskWatcher.watch(keyTask);
                        TaskManager.addTask(keyTask, RegisterSSHKeysTask.TASK_ID);
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        notifyBackupFailed(targetTranslation);
                    }
                }).show();
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        // remember if the device alias dialog is open
        out.putBoolean(STATE_SETTING_DEVICE_ALIAS, settingDeviceAlias);
        super.onSaveInstanceState(out);
    }

    @Override
    public void onDestroy() {
        if(taskWatcher != null) {
            taskWatcher.stop();
        }

        super.onDestroy();
    }
}
