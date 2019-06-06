package com.door43.translationstudio.ui.home;

import android.app.Activity;
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
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.logger.Logger;
import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.SimpleTaskWatcher;
import org.unfoldingword.tools.taskmanager.TaskManager;

import com.door43.translationstudio.App;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.tasks.ImportProjectFromUriTask;
import com.door43.translationstudio.ui.filechooser.FileChooserActivity;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TranslationViewMode;
import com.door43.translationstudio.ui.dialogs.DeviceNetworkAliasDialog;
import com.door43.translationstudio.ui.ImportUsfmActivity;
import com.door43.translationstudio.ui.dialogs.Door43LoginDialog;
import com.door43.translationstudio.ui.dialogs.ShareWithPeerDialog;
import com.door43.translationstudio.ui.translate.TargetTranslationActivity;
import com.door43.util.SdUtils;
import com.door43.widget.ViewUtil;

import java.io.File;

/**
 * Created by joel on 10/5/2015.
 */
public class ImportDialog extends DialogFragment implements SimpleTaskWatcher.OnFinishedListener {

    private static final int IMPORT_TRANSLATION_REQUEST = 142;
    private static final int IMPORT_USFM_REQUEST = 143;
    private static final int IMPORT_RCONTAINER_REQUEST = 144;
    public static final String TAG = "importDialog";
    private static final String STATE_SETTING_DEVICE_ALIAS = "state_setting_device_alias";
    public static final String STATE_DIALOG_SHOWN = "state_dialog_shown";
    public static final String STATE_DIALOG_MESSAGE = "state_dialog_message";
    public static final String STATE_DIALOG_TRANSLATION_ID = "state_dialog_translationID";
    public static final String STATE_MERGE_SELECTION = "state_merge_selection";
    public static final String STATE_MERGE_CONFLICT = "state_merge_conflict";
    public static final String STATE_IMPORT_URL = "state_import_url";
    private boolean settingDeviceAlias = false;
    private boolean isDocumentFile = false;
    private DialogShown mDialogShown = DialogShown.NONE;
    private String mDialogMessage;
    private String mTargetTranslationID;
    private Uri mImportUri;
    private SimpleTaskWatcher taskWatcher;
    private MergeOptions mMergeSelection = MergeOptions.NONE;
    private boolean mMergeConflicted = false;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_import, container, false);

        Button importLocalProjectButton = (Button)v.findViewById(R.id.import_target_translation);
        Button importLocalUsfmButton = (Button)v.findViewById(R.id.import_usfm);
        Button importFromFriend = (Button)v.findViewById(R.id.import_from_device);
        Button importDoor43Button = (Button)v.findViewById(R.id.import_from_door43);
        Button importResourceContainerButton = (Button)v.findViewById(R.id.import_resource_container);

        taskWatcher = new SimpleTaskWatcher(getActivity(), R.string.label_import);
        taskWatcher.setOnFinishedListener(this);

        v.findViewById(R.id.infoButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://forum.door43.org/t/ts-import-options"));
                startActivity(browserIntent);
            }
        });

        if(savedInstanceState != null) {
            // check if returning from device alias dialog
            settingDeviceAlias = savedInstanceState.getBoolean(STATE_SETTING_DEVICE_ALIAS, false);
            mDialogShown = DialogShown.fromInt(savedInstanceState.getInt(STATE_DIALOG_SHOWN, DialogShown.NONE.getValue()));
            mDialogMessage = savedInstanceState.getString(STATE_DIALOG_MESSAGE, null);
            mTargetTranslationID = savedInstanceState.getString(STATE_DIALOG_TRANSLATION_ID, null);
            mMergeConflicted = savedInstanceState.getBoolean(STATE_MERGE_CONFLICT, false);
            mMergeSelection = MergeOptions.fromInt(savedInstanceState.getInt(STATE_MERGE_SELECTION, MergeOptions.NONE.getValue()));
            String path = savedInstanceState.getString(STATE_IMPORT_URL, null);
            mImportUri = (path != null) ? Uri.parse(path) : null;
        }

        importResourceContainerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), FileChooserActivity.class);
                Bundle args = new Bundle();
                args.putString(FileChooserActivity.EXTRA_MODE, FileChooserActivity.SelectionMode.DIRECTORY.name());
                intent.putExtra(FileChooserActivity.EXTRA_READ_ACCESS, true);
                intent.putExtras(args);
                startActivityForResult(intent, IMPORT_RCONTAINER_REQUEST);
            }
        });

        importDoor43Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // make sure we have a gogs user
                if(App.getProfile() == null || App.getProfile().gogsUser == null) {
                    Door43LoginDialog dialog = new Door43LoginDialog();
                    showDialogFragment(dialog, Door43LoginDialog.TAG);
                    return;
                }

                // open dialog for browsing repositories
                ImportFromDoor43Dialog dialog = new ImportFromDoor43Dialog();
                showDialogFragment(dialog, ImportFromDoor43Dialog.TAG);
            }
        });
        importLocalProjectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMergeSelection = MergeOptions.NONE;
                doImportLocal(false);
            }
        });
        importLocalUsfmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMergeSelection = MergeOptions.NONE;
                doImportLocal(true);
            }
        });
        importFromFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMergeSelection = MergeOptions.NONE;
                // TODO: 11/18/2015 eventually we need to support bluetooth as well as an adhoc network
                if (App.isNetworkAvailable()) {
                    if (App.getDeviceNetworkAlias() == null) {
                        // get device alias
                        settingDeviceAlias = true;
                        DeviceNetworkAliasDialog dialog = new DeviceNetworkAliasDialog();
                        showDialogFragment(dialog, "device-name-dialog");
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

        Button dismissButton = (Button)v.findViewById(R.id.dismiss_button);
        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        // connect to existing tasks
        ImportProjectFromUriTask importProjectFromUriTask = (ImportProjectFromUriTask)TaskManager.getTask(ImportProjectFromUriTask.TASK_ID);
        if(importProjectFromUriTask != null) {
            taskWatcher.watch(importProjectFromUriTask);
        }

        restoreDialogs();
        return v;
    }

    /**
     * restore the dialogs that were displayed before rotation
     */
    private void restoreDialogs() {
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                switch(mDialogShown) {
                    case SHOW_IMPORT_RESULTS:
                        showImportResults(mDialogMessage);
                        break;

                    case MERGE_CONFLICT:
                        showMergeOverwritePrompt(mTargetTranslationID);
                        break;

                    case NONE:
                        break;

                    default:
                        Logger.e(TAG,"Unsupported restore dialog: " + mDialogShown.toString());
                        break;
                }
            }
        });
    }

    /**
     * do an import from local file system
     * @param doingUsfmImport
     */
    private void doImportLocal(boolean doingUsfmImport) {
        String typeStr = null;
        Intent intent = new Intent(getActivity(), FileChooserActivity.class);
        isDocumentFile = SdUtils.isSdCardPresentLollipop();
        if(isDocumentFile) {
            typeStr = FileChooserActivity.SD_CARD_TYPE;
        } else {
            typeStr = FileChooserActivity.INTERNAL_TYPE;
        }

        intent.setType(typeStr);
        if(doingUsfmImport) {
            intent.putExtra(FileChooserActivity.EXTRAS_ACCEPTED_EXTENSIONS, "usfm");
        }
        intent.putExtra(FileChooserActivity.EXTRA_READ_ACCESS, true);
        startActivityForResult(intent, doingUsfmImport ? IMPORT_USFM_REQUEST : IMPORT_TRANSLATION_REQUEST);
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
     * Displays the p2p dialog
     */
    private void showP2PDialog() {
        ShareWithPeerDialog dialog = new ShareWithPeerDialog();
        Bundle args = new Bundle();
        args.putInt(ShareWithPeerDialog.ARG_OPERATION_MODE, ShareWithPeerDialog.MODE_CLIENT);
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IMPORT_TRANSLATION_REQUEST) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                doProjectImport(data.getData());
            }
        } else if (requestCode == IMPORT_USFM_REQUEST) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                if (isDocumentFile) {
                    Uri uri = data.getData();
                    doUsfmImportUri(uri);
                } else {
                    String path = data.getData().getPath();
                    doUsfmImportFile(path);
                }
            }
        } else if (requestCode == IMPORT_RCONTAINER_REQUEST) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                Uri uri = data.getData();
                final File dir = new File(uri.getPath());
                if(!dir.isDirectory()) {
                    Toast.makeText(getActivity(), R.string.not_a_source_text, Toast.LENGTH_SHORT).show();
                    return;
                }

                // check that it's a valid container
                // TODO: 11/8/16 this should be done in a task for better performance
                ResourceContainer externalContainer;
                try {
                    externalContainer = ResourceContainer.load(dir);
                } catch (Exception e) {
                    Logger.e(TAG, "Could not import RC", e);
                    new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                            .setTitle(R.string.not_a_source_text)
                            .setMessage(e.getMessage())
                            .setPositiveButton(R.string.dismiss, null)
                            .show();
                    return;
                }

                // check if we already have it
                // TODO: 11/8/16 support screen rotation
                try {
                    App.getLibrary().open(externalContainer.slug);
                    new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                            .setTitle(R.string.confirm)
                            .setMessage(String.format(getResources().getString(R.string.overwrite_content), externalContainer.language.name + " - " + externalContainer.project.name + " - " + externalContainer.resource.name))
                            .setNegativeButton(R.string.menu_cancel, null)
                            .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    importResourceContainer(dir);
                                }
                            })
                            .show();
                } catch (Exception e) {
                    e.printStackTrace();
                    // no conflicts. import
                    importResourceContainer(dir);
                }
            }
        }
    }

    /**
     * start task to import a project
     * @param importUri
     */
    private void doProjectImport(Uri importUri) {
        mImportUri = importUri;
        ImportProjectFromUriTask importProjectFromUriTask = new ImportProjectFromUriTask(mImportUri, mMergeSelection == MergeOptions.OVERWRITE);
        taskWatcher.watch(importProjectFromUriTask);
        TaskManager.addTask(importProjectFromUriTask, ImportProjectFromUriTask.TASK_ID);
    }

    /**
     * Imports a resource container into the app
     * TODO: this should be performed in a task for better performance
     * @param dir
     */
    private void importResourceContainer(File dir) {
        try {
            ResourceContainer container = App.getLibrary().importResourceContainer(dir);
            new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                    .setTitle(R.string.success)
                    .setMessage(R.string.title_import_success)
                    .setPositiveButton(R.string.dismiss, null)
                    .show();
        } catch (Exception e) {
            Logger.e(TAG, "Could not import RC", e);
            new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                    .setTitle(R.string.could_not_import)
                    .setMessage(e.getMessage())
                    .setPositiveButton(R.string.dismiss, null)
                    .show();
        }
    }

    /**
     * import USFM uri
     * @param uri
     */
    private void doUsfmImportUri(Uri uri) {
       ImportUsfmActivity.startActivityForUriImport(getActivity(), uri);
    }

    /**
     * import USFM file
     * @param path
     */
    private void doUsfmImportFile(String path) {
        File file = new File(path);
        ImportUsfmActivity.startActivityForFileImport(getActivity(), file);
    }

    /**
     * let user know there was a merge conflict
     * @param targetTranslationID
     */
    public void showMergeOverwritePrompt(String targetTranslationID) {
        mDialogShown = DialogShown.MERGE_CONFLICT;
        mTargetTranslationID = targetTranslationID;
        int messageID = mMergeConflicted ? R.string.import_merge_conflict_project_name : R.string.import_project_already_exists;
        String message = getActivity().getString(messageID, targetTranslationID);
        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                .setTitle(R.string.merge_conflict_title)
                .setMessage(message)
                .setPositiveButton(R.string.merge_projects_label, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = DialogShown.NONE;
                        mMergeSelection = MergeOptions.OVERWRITE;
                        if(mMergeConflicted) {
                            doManualMerge();
                        } else {
                            showImportResults(R.string.title_import_success, null);
                        }
                    }
                })
                .setNeutralButton(R.string.title_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = DialogShown.NONE;
                        resetToMasterBackup();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.overwrite_projects_label, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = DialogShown.NONE;
                        resetToMasterBackup();

                        // re-import with overwrite
                        mMergeSelection = MergeOptions.OVERWRITE;
                        doProjectImport(mImportUri);
                    }
                }).show();
    }

    /**
     * restore original version
     */
    private void resetToMasterBackup() {
        TargetTranslation mTargetTranslation = App.getTranslator().getTargetTranslation(mTargetTranslationID);
        if(mTargetTranslation != null) {
            mTargetTranslation.resetToMasterBackup();
        }
    }

    /**
     * open review mode to let user resolve conflict
     */
    private void doManualMerge() {
        // ask parent activity to navigate to target translation review mode with merge filter on
        Intent intent = new Intent(getActivity(), TargetTranslationActivity.class);
        Bundle args = new Bundle();
        args.putString(App.EXTRA_TARGET_TRANSLATION_ID, mTargetTranslationID);
        args.putBoolean(App.EXTRA_START_WITH_MERGE_FILTER, true);
        args.putInt(App.EXTRA_VIEW_MODE, TranslationViewMode.REVIEW.ordinal());
        intent.putExtras(args);
        startActivity(intent);
        dismiss();
    }

    @Override
    public void onFinished(ManagedTask task) {
        taskWatcher.stop();
        if (task instanceof ImportProjectFromUriTask) {
            ImportProjectFromUriTask.ImportResults results = (ImportProjectFromUriTask.ImportResults) task.getResult();
            boolean success = results.success;
            mImportUri = results.filePath;
            mMergeConflicted = results.mergeConflict;
            if(success && results.alreadyExists && (mMergeSelection == MergeOptions.NONE)) {
                showMergeOverwritePrompt(results.importedSlug);
            } else if(success) {
                showImportResults(R.string.import_success, results.readablePath);
            } else if(results.invalidFileName) {
                showImportResults(R.string.invalid_file, results.readablePath);
            } else {
                showImportResults(R.string.import_failed, results.readablePath);
            }
        }

        // todo: terrible hack.
        ((HomeActivity)getActivity()).notifyDatasetChanged();
    }

    /**
     * show the import results to user
     * @param textResId
     * @param filePath
     */
    private void showImportResults(final int textResId, final String filePath) {
        String message = getResources().getString(textResId);
        if(filePath != null) {
            message += "\n" + filePath;
        }
        showImportResults(message);
    }

    /**
     * show the import results message to user
     * @param message
     */
    private void showImportResults(String message) {
        mDialogShown = DialogShown.SHOW_IMPORT_RESULTS;
        mDialogMessage = message;
        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                .setTitle(R.string.import_from_sd)
                .setMessage(message)
                .setPositiveButton(R.string.dismiss, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = DialogShown.NONE;
                    }
                })
                .show();
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        out.putBoolean(STATE_SETTING_DEVICE_ALIAS, settingDeviceAlias);
        out.putInt(STATE_DIALOG_SHOWN, mDialogShown.getValue());
        out.putString(STATE_DIALOG_MESSAGE, mDialogMessage);
        out.putString(STATE_DIALOG_TRANSLATION_ID, mTargetTranslationID);
        out.putInt(STATE_MERGE_SELECTION, mMergeSelection.getValue());
        out.putBoolean(STATE_MERGE_CONFLICT, mMergeConflicted);
        if(mImportUri != null) {
            out.putString(STATE_IMPORT_URL, mImportUri.toString());
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
    public enum DialogShown {
        NONE(0),
        SHOW_IMPORT_RESULTS(1),
        MERGE_CONFLICT(2);

        private int value;

        DialogShown(int Value) {
            this.value = Value;
        }

        public int getValue() {
            return value;
        }

        public static DialogShown fromInt(int i) {
            for (DialogShown b : DialogShown.values()) {
                if (b.getValue() == i) {
                    return b;
                }
            }
            return null;
        }
    }

    /**
     * for keeping track of user's merge selection
     */
    public enum MergeOptions {
        NONE(0),
        OVERWRITE(1),
        MERGE(2);

        private int value;

        MergeOptions(int Value) {
            this.value = Value;
        }

        public int getValue() {
            return value;
        }

        public static MergeOptions fromInt(int i) {
            for (MergeOptions b : MergeOptions.values()) {
                if (b.getValue() == i) {
                    return b;
                }
            }
            return null;
        }
    }
}
