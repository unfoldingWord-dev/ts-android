package com.door43.translationstudio.newui.home;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.provider.DocumentFile;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.filebrowser.FileBrowserActivity;

import com.door43.translationstudio.newui.DeviceNetworkAliasDialog;
import com.door43.translationstudio.newui.ShareWithPeerDialog;
import com.door43.widget.ViewUtil;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.InputStream;

/**
 * Created by joel on 10/5/2015.
 */
public class ImportDialog extends DialogFragment {

    private static final int IMPORT_PROJECT_FROM_SD_REQUEST = 0;
    public static final String TAG = "importDialog";
    private static final String STATE_SETTING_DEVICE_ALIAS = "state_setting_device_alias";
    private boolean settingDeviceAlias = false;
    private boolean isDocumentFile = false;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_import, container, false);

        Button importCloudButton = (Button)v.findViewById(R.id.import_from_cloud);
        Button importFromSDButton = (Button)v.findViewById(R.id.import_from_sd);
        Button importFromFriend = (Button)v.findViewById(R.id.import_from_friend);

        if(savedInstanceState != null) {
            // check if returning from device alias dialog
            settingDeviceAlias = savedInstanceState.getBoolean(STATE_SETTING_DEVICE_ALIAS, false);
        }

        importCloudButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                Fragment prev = getFragmentManager().findFragmentByTag(ImportDialog.TAG);
                if (prev != null) {
                    ft.remove(prev);
                }
                ft.addToBackStack(null);

                RestoreFromCloudDialog dialog = new RestoreFromCloudDialog();
                dialog.show(ft, ImportDialog.TAG);
            }
        });
        importFromSDButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (AppContext.doWeNeedToRequestSdCardAccess()) {
                    final CustomAlertDialog dialog = CustomAlertDialog.Create(getActivity());
                    dialog.setTitle(R.string.enable_sd_card_access_title)
                            .setMessageHtml(R.string.enable_sd_card_access)
                            .setPositiveButton(R.string.confirm, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    AppContext.triggerStorageAccessFramework(getActivity());
                                }
                            })
                            .setNegativeButton(R.string.label_skip, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    doImportFromSdCard();
                                }
                            })
                            .show("approve-SD-access");
                } else {
                    doImportFromSdCard();
                }
            }
        });
        importFromFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: 11/18/2015 eventually we need to support bluetooth as well as an adhoc network
                if (AppContext.context().isNetworkAvailable()) {
                    if (AppContext.getDeviceNetworkAlias() == null) {
                        // get device alias
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        Fragment prev = getFragmentManager().findFragmentByTag(ImportDialog.TAG);
                        if (prev != null) {
                            ft.remove(prev);
                        }
                        ft.addToBackStack(null);

                        settingDeviceAlias = true;
                        DeviceNetworkAliasDialog dialog = new DeviceNetworkAliasDialog();
                        dialog.show(ft, ImportDialog.TAG);
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

        return v;
    }

    private void doImportFromSdCard() {
        String typeStr = null;
        Uri baseFolderURI = null;
        Intent intent = new Intent(getActivity(), FileBrowserActivity.class);
        isDocumentFile = AppContext.isSdCardPresentLollipop();

        if(isDocumentFile) {
            DocumentFile baseFolder = AppContext.sdCardMkdirs(null);
            String subFolder =  AppContext.searchFolderAndParentsForDocFile(baseFolder, Translator.ARCHIVE_EXTENSION);
            if(null == subFolder) {
                isDocumentFile = false;
            } else {
                String uriStr = AppContext.getSdCardAccessUriStr();
                intent.putExtra("Folder", subFolder);
                baseFolderURI = Uri.parse(uriStr);
                typeStr = FileBrowserActivity.DOC_FILE_TYPE;
            }
        }

        if(!isDocumentFile) {
            File path = AppContext.getPublicDownloadsDirectory();
            baseFolderURI = Uri.fromFile(path);
            typeStr = FileBrowserActivity.FILE_TYPE;
        }

        intent.setDataAndType(baseFolderURI, typeStr);
        startActivityForResult(intent, IMPORT_PROJECT_FROM_SD_REQUEST);
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
     * Displays the p2p dialog
     */
    private void showP2PDialog() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag(ImportDialog.TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        ShareWithPeerDialog dialog = new ShareWithPeerDialog();
        Bundle args = new Bundle();
        args.putInt(ShareWithPeerDialog.ARG_OPERATION_MODE, ShareWithPeerDialog.MODE_CLIENT);
        args.putString(ShareWithPeerDialog.ARG_DEVICE_ALIAS, AppContext.getDeviceNetworkAlias());
        dialog.setArguments(args);
        dialog.show(ft, ImportDialog.TAG);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == IMPORT_PROJECT_FROM_SD_REQUEST) {
            if(data != null) {
                if(isDocumentFile) {
                    Uri uri = data.getData();
                    if(FilenameUtils.getExtension(uri.getPath()).toLowerCase().equals(Translator.ARCHIVE_EXTENSION)) {
                        try {
                            InputStream in = AppContext.context().getContentResolver().openInputStream(uri);
                            AppContext.getTranslator().importArchive(in,uri.getPath());
                            // TODO: 12/17/2015 merge chunks .. loop
                            CustomAlertDialog.Create(getActivity())
                                    .setTitle(R.string.import_from_sd)
                                    .setMessage(R.string.success)
                                    .setNeutralButton(R.string.dismiss, null)
                                    .show("ImportSuccess");
                        } catch (Exception e) {
                            Logger.e(this.getClass().getName(), "Failed to import the archive", e);
                            Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.translation_import_failed, Snackbar.LENGTH_LONG);
                            ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                            snack.show();
                        }

                        // todo: terrible hack.
                        ((HomeActivity)getActivity()).notifyDatasetChanged();
                    } else {
                        Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.invalid_file, Snackbar.LENGTH_LONG);
                        ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                        snack.show();
                    }
                } else {
                    File file = new File(data.getData().getPath());
                    if (FilenameUtils.getExtension(file.getName()).toLowerCase().equals(Translator.ARCHIVE_EXTENSION)) {
                        try {
                            AppContext.getTranslator().importArchive(file);
                            // TODO: 12/17/2015 merge chunks .. loop
                            CustomAlertDialog.Create(getActivity())
                                    .setTitle(R.string.import_from_sd)
                                    .setMessage(R.string.success)
                                    .setNeutralButton(R.string.dismiss, null)
                                    .show("ImportSuccess");
                        } catch (Exception e) {
                            Logger.e(this.getClass().getName(), "Failed to import the archive", e);
                            Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.translation_import_failed, Snackbar.LENGTH_LONG);
                            ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                            snack.show();
                        }

                        // todo: terrible hack.
                        ((HomeActivity) getActivity()).notifyDatasetChanged();
                    } else {
                        Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.invalid_file, Snackbar.LENGTH_LONG);
                        ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                        snack.show();
                    }
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        out.putBoolean(STATE_SETTING_DEVICE_ALIAS, settingDeviceAlias);
        super.onSaveInstanceState(out);
    }
}
