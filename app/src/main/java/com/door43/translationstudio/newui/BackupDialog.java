package com.door43.translationstudio.newui;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;

import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.ExportUsfm;
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

import com.door43.util.FileUtilities;
import com.door43.widget.ViewUtil;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.merge.MergeStrategy;

import java.io.File;
import java.io.OutputStream;
import java.net.URL;
import java.security.InvalidParameterException;

/**
 * Created by joel on 10/5/2015.
 */
public class BackupDialog extends DialogFragment implements SimpleTaskWatcher.OnFinishedListener {

    public static final String TAG = BackupDialog.class.getName();
    public static final String ARG_TARGET_TRANSLATION_ID = "target_translation_id";
    private static final String STATE_SETTING_DEVICE_ALIAS = "state_setting_device_alias";
    public static final String STATE_DIALOG_SHOWN = "state_dialog_shown";
    public static final String STATE_DO_MERGE = "state_do_merge";
    public static final String STATE_ACCESS_FILE = "state_access_file";
    public static final String STATE_DIALOG_MESSAGE = "state_dialog_message";
//    public static final boolean FORCE_PUSH = true;
    private TargetTranslation targetTranslation;
    private SimpleTaskWatcher taskWatcher;
    private boolean settingDeviceAlias = false;
    private LinearLayout mBackupToCloudButton = null;
    private eDialogShown mDialogShown = eDialogShown.NONE;
    private String mAccessFile;
    private String mDialogMessage;
    private String mRemoteURL;

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

        mBackupToCloudButton = (LinearLayout)v.findViewById(R.id.backup_to_cloud);
        LinearLayout backupToSDButton = (LinearLayout)v.findViewById(R.id.backup_to_sd);
        Button backupToAppButton = (Button)v.findViewById(R.id.backup_to_app);
        Button backupToDeviceButton = (Button)v.findViewById(R.id.backup_to_device);
        LinearLayout exportToPDFButton = (LinearLayout)v.findViewById(R.id.export_to_pdf);
        LinearLayout exportToUsfmButton = (LinearLayout)v.findViewById(R.id.export_to_usfm);

        final String filename = targetTranslation.getId() + "." + Translator.ARCHIVE_EXTENSION;

        taskWatcher = new SimpleTaskWatcher(getActivity(), R.string.backup);
        taskWatcher.setOnFinishedListener(this);

        if(savedInstanceState != null) {
            // check if returning from device alias dialog
            settingDeviceAlias = savedInstanceState.getBoolean(STATE_SETTING_DEVICE_ALIAS, false);
            mDialogShown = eDialogShown.fromInt(savedInstanceState.getInt(STATE_DIALOG_SHOWN, eDialogShown.NONE.getValue()));
            mAccessFile = savedInstanceState.getString(STATE_ACCESS_FILE, null);
            mDialogMessage = savedInstanceState.getString(STATE_DIALOG_MESSAGE, null);
            restoreDialogs();
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
                showDeviceNetworkAliasDialog();
            }
        });

        // backup buttons
        mBackupToCloudButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(App.isNetworkAvailable()) {
                    // make sure we have a gogs user
                    if(App.getProfile().gogsUser == null) {
                        showDoor43LoginDialog();
                        return;
                    }

                    doPullTargetTranslationTask(targetTranslation, MergeStrategy.RECURSIVE);
                } else {
                    Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.internet_not_available, Snackbar.LENGTH_LONG);
                    ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                    snack.show();
                }
            }
        });

        exportToPDFButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PrintDialog printDialog = new PrintDialog();
                Bundle printArgs = new Bundle();
                printArgs.putString(PrintDialog.ARG_TARGET_TRANSLATION_ID, targetTranslation.getId());
                printDialog.setArguments(printArgs);
                showDialogFragment(printDialog, PrintDialog.TAG);
            }
        });

        backupToSDButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SdUtils.doWeNeedToRequestSdCardAccess()) {
                    showAccessRequestDialog(filename);
                } else {
                    doSdCardBackup(filename);
                }
            }
        });

        exportToUsfmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showExportToUsfmPrompt();
            }
        });

        backupToAppButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File exportFile = new File(App.getSharingDir(), filename);
                try {
                    App.getTranslator().exportArchive(targetTranslation, exportFile);
                } catch (Exception e) {
                    Logger.e(TAG, "Failed to export the target translation " + targetTranslation.getId(), e);
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

        return v;
    }

    /**
      * restore the dialogs that were displayed before rotation
      */
    private void restoreDialogs() {
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() { // wait for backup dialog to be drawn before showing popups
            @Override
            public void run() {
                switch (mDialogShown) {
                    case PUSH_REJECTED:
                        showPushRejection(targetTranslation);
                        break;

                    case AUTH_FAILURE:
                        showAuthFailure();
                        break;

                    case BACKUP_FAILED:
                        notifyBackupFailed(targetTranslation);
                        break;

                    case ACCESS_REQUEST:
                        showAccessRequestDialog(mAccessFile);
                        break;

                    case SHOW_BACKUP_RESULTS:
                        showBackupResults(mDialogMessage);
                        break;

                    case SHOW_PUSH_SUCCESS:
                        showPushSuccess(mDialogMessage);
                        break;

                    case MERGE_CONFLICT:
                        showMergeConflict(targetTranslation);
                        break;

                    case EXPORT_TO_USFM_PROMPT:
                        showExportToUsfmPrompt();
                        break;

                    case EXPORT_TO_USFM_RESULTS:
                        showUsfmExportResults(mDialogMessage);
                        break;

                    case NONE:
                        break;

                    default:
                        Logger.e(TAG, "Unsupported restore dialog: " + mDialogShown.toString());
                        break;
                }
            }
        });
    }

    /**
     * display confirmation prompt before USFM export
     */
    private void showExportToUsfmPrompt() {
        mDialogShown = eDialogShown.EXPORT_TO_USFM_PROMPT;
        ExportUsfm.saveToUsfmWithPrompt(getActivity(), targetTranslation, new ExportUsfm.OnResultsListener() {
            @Override
            public void onFinished(ExportUsfm.eResults result, String outputFilePath, String defaultMessage) {
                mDialogShown = eDialogShown.NONE;
                if(result != ExportUsfm.eResults.CANCELLED) {
                    showUsfmExportResults(defaultMessage);
                }
            }
        });
    }

    /**
     * show USFM export results
     * @param message
     */
    private void showUsfmExportResults(String message) {
        mDialogShown = eDialogShown.EXPORT_TO_USFM_RESULTS;
        mDialogMessage = message;

        ExportUsfm.showResults(getActivity(), message, new ExportUsfm.OnFinishedListener() {
            @Override
            public void onFinished() {
                mDialogShown = eDialogShown.NONE;
            }
        });
    }

    private void showDoor43LoginDialog() {
        Door43LoginDialog dialog = new Door43LoginDialog();
        showDialogFragment(dialog, Door43LoginDialog.TAG);
    }

    private void showDeviceNetworkAliasDialog() {
        if(App.isNetworkAvailable()) {
            if(App.getDeviceNetworkAlias() == null) {
                showDeviceNetworkAliasDialogSub();

            } else {
                showP2PDialog();
            }
        } else {
            Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.internet_not_available, Snackbar.LENGTH_LONG);
            ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
            snack.show();
        }
    }

    private void showDeviceNetworkAliasDialogSub() {
       settingDeviceAlias = true;
        DeviceNetworkAliasDialog dialog = new DeviceNetworkAliasDialog();
        showDialogFragment(dialog, "device-name-dialog");
    }

    private void showAccessRequestDialog(final String filename) {
        mDialogShown = eDialogShown.ACCESS_REQUEST;
        mAccessFile = filename;
        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                .setTitle(R.string.enable_sd_card_access_title)
                .setMessage(Html.fromHtml(getString(R.string.enable_sd_card_access)))
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = eDialogShown.NONE;
                        SdUtils.triggerStorageAccessFramework(getActivity());
                    }
                })
                .setNegativeButton(R.string.label_skip, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = eDialogShown.NONE;
                        doSdCardBackup(filename);
                    }
                })
                .show();
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
                        FileUtilities.closeQuietly(out);
                    }
                    sdCardFile.delete();
                } catch(Exception e2) {
                }
            }
            Logger.e(TAG, "Failed to export the target translation " + targetTranslation.getId(), e);
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
        showBackupResults(message);
    }

    private void showBackupResults(String message) {
        mDialogShown = eDialogShown.SHOW_BACKUP_RESULTS;
        mDialogMessage = message;
        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                .setTitle(R.string.backup_to_sd)
                .setMessage(message)
                .setNeutralButton(R.string.dismiss, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = eDialogShown.NONE;
                    }
                })
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
        ShareWithPeerDialog dialog = new ShareWithPeerDialog();
        Bundle args = new Bundle();
        args.putInt(ShareWithPeerDialog.ARG_OPERATION_MODE, ShareWithPeerDialog.MODE_SERVER);
        args.putString(ShareWithPeerDialog.ARG_TARGET_TRANSLATION, targetTranslation.getId());
        args.putString(ShareWithPeerDialog.ARG_DEVICE_ALIAS, App.getDeviceNetworkAlias());
        dialog.setArguments(args);
        showDialogFragment(dialog, "share-dialog");
    }

    /**
     * this is to fix old method which when called in onResume() would create a
     * second dialog overlaying the first.  The first was actually not removed.
     * Doing a commit after the remove() and starting a second FragmentTransaction
     * seems to fix the duplicate dialog bug.
     *
     * @param dialog
     * @param tag
     */
    private void showDialogFragment(android.app.DialogFragment dialog, String tag) {
        FragmentTransaction backupFt = getFragmentManager().beginTransaction();
        Fragment backupPrev = getFragmentManager().findFragmentByTag(tag);
        if (backupPrev != null) {
            backupFt.remove(backupPrev);
            backupFt.commit(); // apply the remove
            backupFt = getFragmentManager().beginTransaction(); // start a new transaction
        }
        backupFt.addToBackStack(null);

        dialog.show(backupFt, tag);
    }

    @Override
    public void onFinished(ManagedTask task) {
        taskWatcher.stop();
        if(task instanceof PullTargetTranslationTask) {
            PullTargetTranslationTask pullTask = (PullTargetTranslationTask) task;
//            mRemoteURL = pullTask.getSourceURL();
            PullTargetTranslationTask.Status status = pullTask.getStatus();
            //  TRICKY: we continue to push for unknown status in case the repo was just created (the missing branch is an error)
            // the pull task will catch any errors
            if(status == PullTargetTranslationTask.Status.UP_TO_DATE
                    || status == PullTargetTranslationTask.Status.UNKNOWN) {
                Logger.i(this.getClass().getName(), "Changes on the server were synced with " + targetTranslation.getId());

                PushTargetTranslationTask pushtask = new PushTargetTranslationTask(targetTranslation);
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
                showMergeConflict(targetTranslation);
            } else {
                notifyBackupFailed(targetTranslation);
            }
        } else if(task instanceof RegisterSSHKeysTask) {
            if(((RegisterSSHKeysTask)task).isSuccess()) {
                Logger.i(this.getClass().getName(), "SSH keys were registered with the server");
                // try to push again
                doPullTargetTranslationTask(targetTranslation, MergeStrategy.THEIRS);
            } else {
                notifyBackupFailed(targetTranslation);
            }
        } else if(task instanceof CreateRepositoryTask) {
            if(((CreateRepositoryTask)task).isSuccess()) {
                Logger.i(this.getClass().getName(), "A new repository " + targetTranslation.getId() + " was created on the server");
                doPullTargetTranslationTask(targetTranslation, MergeStrategy.THEIRS);
            } else {
                notifyBackupFailed(targetTranslation);
            }
        } else if(task instanceof PushTargetTranslationTask) {
            PushTargetTranslationTask.Status status =((PushTargetTranslationTask)task).getStatus();
            final String message = ((PushTargetTranslationTask)task).getMessage();

            if(status == PushTargetTranslationTask.Status.OK) {
                Logger.i(this.getClass().getName(), "The target translation " + targetTranslation.getId() + " was pushed to the server");
                showPushSuccess(message);
            } else if(status.isRejected()) {
                Logger.i(this.getClass().getName(), "Push Rejected");
                showPushRejection(targetTranslation);
            } else if(status == PushTargetTranslationTask.Status.AUTH_FAILURE) {
                Logger.i(this.getClass().getName(), "Authentication failed");
                showAuthFailure();
            } else {
                notifyBackupFailed(targetTranslation);
            }
        }
    }

    private void showPushSuccess(final String message) {
        mDialogShown = eDialogShown.SHOW_PUSH_SUCCESS;
        mDialogMessage = message;
        final Uri url = Uri.parse("https://door43.org/u/" + App.getProfile().gogsUser.getUsername() + "/" + targetTranslation.getId());
        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                .setTitle(R.string.success)
                .setMessage(R.string.project_uploaded)
                .setNegativeButton(R.string.dismiss, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = eDialogShown.NONE;
                    }
                })
                .setPositiveButton(R.string.view_online, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Intent.ACTION_VIEW, url));
                    }
                })
                .setNeutralButton(R.string.label_details, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                            .setTitle(R.string.project_uploaded)
                            .setMessage(message)
                            .setPositiveButton(R.string.view_online, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startActivity(new Intent(Intent.ACTION_VIEW, url));
                                }
                            })
                            .setNeutralButton(R.string.dismiss, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mDialogShown = eDialogShown.NONE;
                                }
                            })
                            .show();
                    }
                }).show();
    }

    /**
     * let user know there was a merge conflict
     * @param targetTranslation
     */
    private void showMergeConflict(final TargetTranslation targetTranslation) {
        mDialogShown = eDialogShown.MERGE_CONFLICT;
        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                .setTitle(R.string.backup_failed_title)
                .setMessage(R.string.backup_rejected)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = eDialogShown.NONE;
                        doManualMerge();
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = eDialogShown.NONE;
                        resetToMasterBackup(targetTranslation);
                        BackupDialog.this.dismiss();
                    }
                }).show();
    }

    /**
     * open review mode to let user resolve conflict
     */
    private void doManualMerge() {
        if(getActivity() instanceof TargetTranslationActivity) {
            ((TargetTranslationActivity) getActivity()).notifyDatasetChanged();
            BackupDialog.this.dismiss();
            // TODO: 4/20/16 it would be nice to navigate directly to the first conflict
        } else {
            // ask parent activity to navigate to a new activity
            Intent intent = new Intent(getActivity(), TargetTranslationActivity.class);
            Bundle args = new Bundle();
            args.putString(App.EXTRA_TARGET_TRANSLATION_ID, targetTranslation.getId());
            // TODO: 4/20/16 it would be nice to navigate directly to the first conflict
//                args.putString(App.EXTRA_CHAPTER_ID, chapterId);
//                args.putString(App.EXTRA_FRAME_ID, frameId);
            args.putString(App.EXTRA_VIEW_MODE, TranslationViewMode.REVIEW.toString());
            intent.putExtras(args);
            startActivity(intent);
            getActivity().finish();
        }
    }

    private void doPullTargetTranslationTask(TargetTranslation targetTranslation, MergeStrategy theirs) {
        PullTargetTranslationTask pullTask = new PullTargetTranslationTask(targetTranslation, theirs, null);
        taskWatcher.watch(pullTask);
        TaskManager.addTask(pullTask, PullTargetTranslationTask.TASK_ID);
    }

    /**
     * Displays a dialog to the user indicating the publish failed.
     * Includes an option to submit a bug report
     * @param targetTranslation
     */
    private void notifyBackupFailed(final TargetTranslation targetTranslation) {
        mDialogShown = eDialogShown.BACKUP_FAILED;
        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
        .setTitle(R.string.backup)
                .setMessage(R.string.upload_failed)
                .setPositiveButton(R.string.dismiss, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = eDialogShown.NONE;
                    }
                })
                .setNeutralButton(R.string.menu_bug, new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = eDialogShown.NONE;
                        showFeedbackDialog(targetTranslation);
                    }
                }).show();
    }

    private void showFeedbackDialog(TargetTranslation targetTranslation) {
        Project project = App.getLibrary().getProject(targetTranslation.getProjectId(), "en");

        // open bug report dialog
        FeedbackDialog feedbackDialog = new FeedbackDialog();
        Bundle args = new Bundle();
        String message = "Failed to backup the translation of " +
                project.name + " into " +
                targetTranslation.getTargetLanguageName()
                + ".\ntargetTranslation: " + targetTranslation.getId() +
                "\n--------\n\n";
        args.putString(FeedbackDialog.ARG_MESSAGE, message);
        feedbackDialog.setArguments(args);
        showDialogFragment(feedbackDialog, "feedback-dialog");
    }

    public void showPushRejection(final TargetTranslation targetTranslation) {
        mDialogShown = eDialogShown.PUSH_REJECTED;

        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                .setTitle(R.string.upload_failed)
                .setMessage(R.string.push_rejected)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = eDialogShown.NONE;
                        doManualMerge();
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = eDialogShown.NONE;
                        resetToMasterBackup(targetTranslation);
                        BackupDialog.this.dismiss();
                    }
                }).show();
    }

    private boolean resetToMasterBackup(TargetTranslation targetTranslation) {
        try { // restore state before the pull
            Git git = targetTranslation.getRepo().getGit();
            ResetCommand resetCommand = git.reset();
            resetCommand.setMode(ResetCommand.ResetType.HARD)
                    .setRef("backup-master")
                    .call();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void showAuthFailure() {
        mDialogShown = eDialogShown.AUTH_FAILURE;
        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                .setTitle(R.string.upload_failed)
                .setMessage(R.string.auth_failure_retry)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = eDialogShown.NONE;
                        RegisterSSHKeysTask keyTask = new RegisterSSHKeysTask(true);
                        taskWatcher.watch(keyTask);
                        TaskManager.addTask(keyTask, RegisterSSHKeysTask.TASK_ID);
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = eDialogShown.NONE;
                        notifyBackupFailed(targetTranslation);
                    }
                }).show();
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        // remember if the device alias dialog is open
        out.putBoolean(STATE_SETTING_DEVICE_ALIAS, settingDeviceAlias);
        out.putInt(STATE_DO_MERGE, mDialogShown.getValue());
        if(mAccessFile != null) {
            out.putString(STATE_ACCESS_FILE, mAccessFile);
        }
        out.putInt(STATE_DIALOG_SHOWN, mDialogShown.getValue());
        if(mDialogMessage != null) {
            out.putString(STATE_DIALOG_MESSAGE, mDialogMessage);
        }
        super.onSaveInstanceState(out);
    }

    @Override
    public void onDestroy() {
        if(taskWatcher != null) {
            taskWatcher.stop();
        }

        super.onDestroy();
    }

    /**
     * for keeping track which dialog is being shown for orientation changes (not for DialogFragments)
     */
    public enum eDialogShown {
        NONE(0),
        PUSH_REJECTED(1),
        AUTH_FAILURE(2),
        BACKUP_FAILED(3),
        ACCESS_REQUEST(4),
        SHOW_BACKUP_RESULTS(5),
        SHOW_PUSH_SUCCESS(6),
        MERGE_CONFLICT(7),
        EXPORT_TO_USFM_PROMPT(8),
        EXPORT_TO_USFM_RESULTS(9);

        private int value;

        eDialogShown(int Value) {
            this.value = Value;
        }

        public int getValue() {
            return value;
        }

        public static eDialogShown fromInt(int i) {
            for (eDialogShown b : eDialogShown.values()) {
                if (b.getValue() == i) {
                    return b;
                }
            }
            return null;
        }
    }
}
