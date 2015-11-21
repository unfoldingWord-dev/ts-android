package com.door43.translationstudio.newui.home;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.core.Util;
import com.door43.translationstudio.filebrowser.FileBrowserActivity;
import com.door43.translationstudio.git.SSHSession;
import com.door43.translationstudio.newui.ShareWithPeerDialog;
import com.door43.util.tasks.ThreadableUI;
import com.door43.widget.ViewUtil;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSchException;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 10/5/2015.
 */
public class ImportDialog extends DialogFragment {

    private static final int IMPORT_PROJECT_FROM_SD_REQUEST = 0;
    public static final String TAG = "importDialog";

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
                File path = AppContext.getPublicDownloadsDirectory();
                Intent intent = new Intent(getActivity(), FileBrowserActivity.class);
                intent.setDataAndType(Uri.fromFile(path), "file/*");
                startActivityForResult(intent, IMPORT_PROJECT_FROM_SD_REQUEST);
            }
        });
        importFromFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: 11/18/2015 eventually we need to support bluetooth as well as an adhoc network
                if(AppContext.context().isNetworkAvailable()) {
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    Fragment prev = getFragmentManager().findFragmentByTag(ImportDialog.TAG);
                    if (prev != null) {
                        ft.remove(prev);
                    }
                    ft.addToBackStack(null);

                    ShareWithPeerDialog dialog = new ShareWithPeerDialog();
                    Bundle args = new Bundle();
                    args.putInt(ShareWithPeerDialog.ARG_OPERATION_MODE, ShareWithPeerDialog.MODE_CLIENT);
                    dialog.setArguments(args);
                    dialog.show(ft, ImportDialog.TAG);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == IMPORT_PROJECT_FROM_SD_REQUEST) {
            if(data != null) {
                File file = new File(data.getData().getPath());
                if(FilenameUtils.getExtension(file.getName()).toLowerCase().equals(Translator.ARCHIVE_EXTENSION)) {
                    try {
                        AppContext.getTranslator().importArchive(file);
                        new AlertDialog.Builder(getActivity())
                                .setTitle(R.string.import_from_sd)
                                .setMessage(R.string.success)
                                .setNeutralButton(R.string.dismiss, null)
                                .show();
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
            }
        }
    }
}
