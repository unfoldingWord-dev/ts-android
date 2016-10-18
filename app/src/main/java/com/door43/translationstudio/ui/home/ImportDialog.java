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
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.ui.ImportFileChooserActivity;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TranslationViewMode;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.ui.dialogs.DeviceNetworkAliasDialog;
import com.door43.translationstudio.ui.ImportUsfmActivity;
import com.door43.translationstudio.ui.dialogs.Door43LoginDialog;
import com.door43.translationstudio.ui.dialogs.ShareWithPeerDialog;
import com.door43.translationstudio.ui.translate.TargetTranslationActivity;
import com.door43.translationstudio.rendering.MergeConflictHandler;
import com.door43.util.SdUtils;
import com.door43.util.FileUtilities;
import com.door43.widget.ViewUtil;

import java.io.File;
import java.io.InputStream;

/**
 * Created by joel on 10/5/2015.
 */
public class ImportDialog extends DialogFragment {

    private static final int IMPORT_PROJECT_FROM_SD_REQUEST = 142;
    private static final int IMPORT_USFM_PROJECT_FROM_SD_REQUEST = 143;
    public static final String TAG = "importDialog";
    private static final String STATE_SETTING_DEVICE_ALIAS = "state_setting_device_alias";
    public static final String STATE_DIALOG_SHOWN = "state_dialog_shown";
    public static final String STATE_DIALOG_MESSAGE = "state_dialog_message";
    public static final String STATE_DIALOG_TRANSLATION_ID = "state_dialog_translationID";
    private boolean settingDeviceAlias = false;
    private boolean isDocumentFile = false;
    private eDialogShown mDialogShown = eDialogShown.NONE;
    private String mDialogMessage;
    private String mTargetTranslationID;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_import, container, false);

        final Button restoreDoor43Button = (Button)v.findViewById(R.id.restore_from_door43);
        Button importFromSDButton = (Button)v.findViewById(R.id.import_from_sd);
        Button importFromSDUsfmButton = (Button)v.findViewById(R.id.import_from_sd_usfm);
        Button importFromFriend = (Button)v.findViewById(R.id.import_from_friend);
        Button importDoor43Button = (Button)v.findViewById(R.id.import_from_door43);

        if(savedInstanceState != null) {
            // check if returning from device alias dialog
            settingDeviceAlias = savedInstanceState.getBoolean(STATE_SETTING_DEVICE_ALIAS, false);
            mDialogShown = eDialogShown.fromInt(savedInstanceState.getInt(STATE_DIALOG_SHOWN, eDialogShown.NONE.getValue()));
            mDialogMessage = savedInstanceState.getString(STATE_DIALOG_MESSAGE, null);
            mTargetTranslationID = savedInstanceState.getString(STATE_DIALOG_TRANSLATION_ID, null);
        }

        importDoor43Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // make sure we have a gogs user
                if(App.getProfile().gogsUser == null) {
                    Door43LoginDialog dialog = new Door43LoginDialog();
                    showDialogFragment(dialog, Door43LoginDialog.TAG);
                    return;
                }

                // open dialog for browsing repositories
                ImportFromDoor43Dialog dialog = new ImportFromDoor43Dialog();
                showDialogFragment(dialog, ImportFromDoor43Dialog.TAG);
            }
        });

        restoreDoor43Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // make sure we have a gogs user
                if(App.getProfile().gogsUser == null) {
                    Door43LoginDialog dialog = new Door43LoginDialog();
                    showDialogFragment(dialog, Door43LoginDialog.TAG);
                    return;
                }

                // open dialog for browsing repositories
                RestoreFromDoor43Dialog dialog = new RestoreFromDoor43Dialog();
                showDialogFragment(dialog, RestoreFromDoor43Dialog.TAG);
            }
        });
        importFromSDButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doImportFromSdCard(false);
            }
        });
        importFromSDUsfmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doImportFromSdCard(true);
            }
        });
        importFromFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

        restoreDialogs();
        return v;
    }

    /**
     * restore the dialogs that were displayed before rotation
     */
    private void restoreDialogs() {
        switch(mDialogShown) {
            case SHOW_IMPORT_RESULTS:
                showImportResults(mDialogMessage);
                break;

            case MERGE_CONFLICT:
                showMergeConflict(mTargetTranslationID);
                break;

            case NONE:
                break;

            default:
                Logger.e(TAG,"Unsupported restore dialog: " + mDialogShown.toString());
                break;
        }
    }

    private void doImportFromSdCard(boolean doingUsfmImport) {
        String typeStr = null;
        Intent intent = new Intent(getActivity(), ImportFileChooserActivity.class);
        isDocumentFile = SdUtils.isSdCardPresentLollipop();
        if(isDocumentFile) {
            typeStr = ImportFileChooserActivity.SD_CARD_TYPE;
        } else {
            typeStr = ImportFileChooserActivity.INTERNAL_TYPE;
        }

        intent.setType(typeStr);
        if(doingUsfmImport) {
            intent.putExtra(ImportFileChooserActivity.EXTRAS_ACCEPTED_EXTENSIONS, "usfm");
        }
        startActivityForResult(intent, doingUsfmImport ? IMPORT_USFM_PROJECT_FROM_SD_REQUEST : IMPORT_PROJECT_FROM_SD_REQUEST);
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
        if (requestCode == IMPORT_PROJECT_FROM_SD_REQUEST) {
            if ((resultCode == Activity.RESULT_OK) && (data != null)) {
                if (isDocumentFile) {
                    Uri uri = data.getData();
                    importUri(uri);
                } else {
                    File file = new File(data.getData().getPath());
                    importFile(file);
                }
            }
        } else if (requestCode == IMPORT_USFM_PROJECT_FROM_SD_REQUEST) {
            if ((resultCode == Activity.RESULT_OK) && (data != null)) {
                if (isDocumentFile) {
                    Uri uri = data.getData();
                    doUsfmImportUri(uri);
                } else {
                    String path = data.getData().getPath();
                    doUsfmImportFile(path);
                }
            }
        }
    }

    /**
     * import USFM uri with fallback to standard import if tstudio uri
     * @param uri
     */
    private void doUsfmImportUri(Uri uri) {
        String path = uri.toString();
        String ext = FileUtilities.getExtension(path).toLowerCase();
        ImportUsfmActivity.startActivityForUriImport(getActivity(), uri);
    }

    /**
     * import USFM file with fallback to standard import if tstudio file
     * @param path
     */
    private void doUsfmImportFile(String path) {
        File file = new File(path);
        String ext = FileUtilities.getExtension(path).toLowerCase();
        boolean tstudio = ext.equalsIgnoreCase(Translator.ARCHIVE_EXTENSION);
        if (tstudio) {
            importFile(file);
        } else {
            ImportUsfmActivity.startActivityForFileImport(getActivity(), file);
        }
    }

    /**
     * import selected file
     * @param file
     */
    private void importFile(File file) {
        if (FileUtilities.getExtension(file.getName()).toLowerCase().equals(Translator.ARCHIVE_EXTENSION)) {
            try {
                Logger.i(this.getClass().getName(), "Importing internal file: " + file.toString());
                final Translator translator = App.getTranslator();
                final Translator.ImportResults importResults = translator.importArchive(file);
                if(importResults.isSuccess() && importResults.mergeConflict) {
                    showMergeConflict(importResults.importedSlug);
                }
                else {
                    showImportResults(R.string.import_success, file.toString());
                }
            } catch (Exception e) {
                Logger.e(this.getClass().getName(), "Failed to import the archive", e);
                showImportResults(R.string.import_failed, file.toString());
            }

            // todo: terrible hack.
            ((HomeActivity) getActivity()).notifyDatasetChanged();
        } else {
            showImportResults(R.string.invalid_file, file.toString());
        }
    }

    /**
     * let user know there was a merge conflict
     * @param targetTranslationID
     */
    public void showMergeConflict(String targetTranslationID) {
        mDialogShown = eDialogShown.MERGE_CONFLICT;
        mTargetTranslationID = targetTranslationID;
        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                .setTitle(R.string.merge_conflict_title).setMessage(R.string.import_merge_conflict)
                .setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = eDialogShown.NONE;
                        doManualMerge();
                    }
                }).show();
    }

    /**
     * open review mode to let user resolve conflict
     */
    private void doManualMerge() {
        // ask parent activity to navigate to a new activity
        Intent intent = new Intent(getActivity(), TargetTranslationActivity.class);
        Bundle args = new Bundle();
        args.putString(App.EXTRA_TARGET_TRANSLATION_ID, mTargetTranslationID);

        MergeConflictHandler.CardLocation location = MergeConflictHandler.findFirstMergeConflict( mTargetTranslationID );
        if(location != null) {
            args.putString(App.EXTRA_CHAPTER_ID, location.chapterID);
            args.putString(App.EXTRA_FRAME_ID, location.frameID);
        }

        args.putInt(App.EXTRA_VIEW_MODE, TranslationViewMode.REVIEW.ordinal());
        intent.putExtras(args);
        startActivity(intent);
        dismiss();
    }

    /**
     * import selected uri
     * @param uri
     */
    private void importUri(Uri uri) {
        if(FileUtilities.getExtension(uri.getPath()).toLowerCase().equals(Translator.ARCHIVE_EXTENSION)) {
            try {
                Logger.i(this.getClass().getName(), "Importing SD card: " + uri);
                final InputStream in = App.context().getContentResolver().openInputStream(uri);
                final Translator translator = App.getTranslator();
                final Translator.ImportResults importResults = translator.importArchive(in);
                if(importResults.isSuccess() && importResults.mergeConflict) {
                    showMergeConflict(importResults.importedSlug); //
                }
                else {
                    showImportResults(R.string.import_success, SdUtils.getPathString(uri.toString()));
                }
            } catch (Exception e) {
                Logger.e(this.getClass().getName(), "Failed to import the archive", e);
                showImportResults(R.string.import_failed, SdUtils.getPathString(uri.toString()));
            }

            // todo: terrible hack.
            ((HomeActivity)getActivity()).notifyDatasetChanged();
        } else {
            showImportResults(R.string.invalid_file, uri.toString());
        }
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

    private void showImportResults(String message) {
        mDialogShown = eDialogShown.SHOW_IMPORT_RESULTS;
        mDialogMessage = message;
        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                .setTitle(R.string.import_from_sd)
                .setMessage(message)
                .setPositiveButton(R.string.dismiss, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = eDialogShown.NONE;
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

        super.onSaveInstanceState(out);
    }

    /**
     * for keeping track which dialog is being shown for orientation changes (not for DialogFragments)
     */
    public enum eDialogShown {
        NONE(0),
        SHOW_IMPORT_RESULTS(1),
        MERGE_CONFLICT(2);

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
