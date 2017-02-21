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
import android.widget.Toast;

import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.core.MergeConflictsHandler;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.ui.filechooser.FileChooserActivity;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TranslationViewMode;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.ui.dialogs.DeviceNetworkAliasDialog;
import com.door43.translationstudio.ui.ImportUsfmActivity;
import com.door43.translationstudio.ui.dialogs.Door43LoginDialog;
import com.door43.translationstudio.ui.dialogs.ShareWithPeerDialog;
import com.door43.translationstudio.ui.translate.TargetTranslationActivity;
import com.door43.util.SdUtils;
import com.door43.util.FileUtilities;
import com.door43.widget.ViewUtil;

import java.io.File;
import java.io.InputStream;

/**
 * Created by joel on 10/5/2015.
 */
public class ImportDialog extends DialogFragment {

    private static final int IMPORT_TRANSLATION_REQUEST = 142;
    private static final int IMPORT_USFM_REQUEST = 143;
    private static final int IMPORT_RCONTAINER_REQUEST = 144;
    public static final String TAG = "importDialog";
    private static final String STATE_SETTING_DEVICE_ALIAS = "state_setting_device_alias";
    public static final String STATE_DIALOG_SHOWN = "state_dialog_shown";
    public static final String STATE_DIALOG_MESSAGE = "state_dialog_message";
    public static final String STATE_DIALOG_TRANSLATION_ID = "state_dialog_translationID";
    public static final String STATE_MERGE_OVERWRITE = "state_merge_overwrite";
    public static final String STATE_IMPORT_URL = "state_import_url";
    public static final String STATE_IMPORT_FILE = "state_import_file";
    private boolean settingDeviceAlias = false;
    private boolean isDocumentFile = false;
    private eDialogShown mDialogShown = eDialogShown.NONE;
    private String mDialogMessage;
    private String mTargetTranslationID;
    private boolean mMergeOverwrite = false;
    private File mImportFile;
    private Uri mImportUri;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_import, container, false);

        Button importFromSDButton = (Button)v.findViewById(R.id.import_target_translation);
        Button importFromSDUsfmButton = (Button)v.findViewById(R.id.import_usfm);
        Button importFromFriend = (Button)v.findViewById(R.id.import_from_device);
        Button importDoor43Button = (Button)v.findViewById(R.id.import_from_door43);
        Button importResourceContainerButton = (Button)v.findViewById(R.id.import_resource_container);

        v.findViewById(R.id.infoButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://help.door43.org/en/knowledgebase/9-translationstudio/docs/3-import-options"));
                startActivity(browserIntent);
            }
        });

        if(savedInstanceState != null) {
            // check if returning from device alias dialog
            settingDeviceAlias = savedInstanceState.getBoolean(STATE_SETTING_DEVICE_ALIAS, false);
            mDialogShown = eDialogShown.fromInt(savedInstanceState.getInt(STATE_DIALOG_SHOWN, eDialogShown.NONE.getValue()));
            mDialogMessage = savedInstanceState.getString(STATE_DIALOG_MESSAGE, null);
            mTargetTranslationID = savedInstanceState.getString(STATE_DIALOG_TRANSLATION_ID, null);
            mMergeOverwrite = savedInstanceState.getBoolean(STATE_MERGE_OVERWRITE, false);
            String path = savedInstanceState.getString(STATE_IMPORT_URL, null);
            mImportUri = (path != null) ? Uri.parse(path) : null;
            path = savedInstanceState.getString(STATE_IMPORT_FILE, null);
            mImportFile = (path != null) ? new File(path) : null;
        }

        importResourceContainerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), FileChooserActivity.class);
                Bundle args = new Bundle();
                args.putString(FileChooserActivity.EXTRA_MODE, FileChooserActivity.SelectionMode.DIRECTORY.name());
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
        importFromSDButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMergeOverwrite = false;
                doImportFromSdCard(false);
            }
        });
        importFromSDUsfmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMergeOverwrite = false;
                doImportFromSdCard(true);
            }
        });
        importFromFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMergeOverwrite = false;
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
                if (isDocumentFile) {
                    Uri uri = data.getData();
                    importUri(uri);
                } else {
                    File file = new File(data.getData().getPath());
                    importFile(file);
                }
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
                    e.printStackTrace();
                    Toast.makeText(getActivity(), R.string.not_a_source_text, Toast.LENGTH_SHORT).show();
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
     * Imports a resource container into the app
     * TODO: this should be performed in a task for better performance
     * @param dir
     */
    private void importResourceContainer(File dir) {
        try {
            ResourceContainer container = App.getLibrary().importResourceContainer(dir);
            new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                    .setTitle(R.string.success)
                    .setMessage(R.string.title_import_Success)
                    .setPositiveButton(R.string.dismiss, null)
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), R.string.could_not_import, Toast.LENGTH_SHORT).show();
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
    private void importFile(final File file) {
        mImportFile = file;
        mImportUri = null;
        if (FileUtilities.getExtension(file.getName()).toLowerCase().equals(Translator.ARCHIVE_EXTENSION)) {
            try {
                Logger.i(this.getClass().getName(), "Importing internal file: " + file.toString());
                final Translator translator = App.getTranslator();
                final Translator.ImportResults importResults = translator.importArchive(file, mMergeOverwrite);
                if(importResults.isSuccess() && importResults.mergeConflict) {
                    MergeConflictsHandler.backgroundTestForConflictedChunks(importResults.importedSlug, new MergeConflictsHandler.OnMergeConflictListener() {
                        @Override
                        public void onNoMergeConflict(String targetTranslationId) {
                            showImportResults(R.string.import_success, file.toString());
                        }

                        @Override
                        public void onMergeConflict(String targetTranslationId) {
                            showMergeConflict(importResults.importedSlug);
                        }
                    });
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
                .setTitle(R.string.merge_conflict_title).setMessage(R.string.import_merge_conflict_choices)
                .setPositiveButton(R.string.merge_projects_label, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = eDialogShown.NONE;
                        doManualMerge();
                    }
                })
                .setNeutralButton(R.string.title_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = eDialogShown.NONE;
                        resetToMasterBackup();
                    }
                })
                .setNegativeButton(R.string.overwrite_projects_label, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = eDialogShown.NONE;
                        resetToMasterBackup();

                        // re-import with overwrite
                        mMergeOverwrite = true;
                        if(mImportUri != null) {
                            importUri(mImportUri);
                        } else {
                            importFile(mImportFile);
                        }
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

    /**
     * import selected uri
     * @param uri
     */
    private void importUri(final Uri uri) {
        mImportUri = uri;
        mImportFile = null;
        if(FileUtilities.getExtension(uri.getPath()).toLowerCase().equals(Translator.ARCHIVE_EXTENSION)) {
            try {
                Logger.i(this.getClass().getName(), "Importing SD card: " + uri);
                final InputStream in = App.context().getContentResolver().openInputStream(uri);
                final Translator translator = App.getTranslator();
                final Translator.ImportResults importResults = translator.importArchive(in, mMergeOverwrite);
                if(importResults.isSuccess() && importResults.mergeConflict) {
                    MergeConflictsHandler.backgroundTestForConflictedChunks(importResults.importedSlug, new MergeConflictsHandler.OnMergeConflictListener() {
                        @Override
                        public void onNoMergeConflict(String targetTranslationId) {
                            showImportResults(R.string.import_success, SdUtils.getPathString(uri.toString()));
                        }

                        @Override
                        public void onMergeConflict(String targetTranslationId) {
                            showMergeConflict(importResults.importedSlug);
                        }
                    });
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
        out.putBoolean(STATE_MERGE_OVERWRITE, mMergeOverwrite);
        if(mImportUri != null) {
            out.putString(STATE_IMPORT_URL, mImportUri.toString());
        }
        if(mImportFile != null) {
            out.putString(STATE_IMPORT_FILE, mImportFile.toString());
        }

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
