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

        importCloudButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: display loading dialog
                new ThreadableUI(getActivity()) {
                    private List<String> targetTranslationIds = new ArrayList<>();
                    @Override
                    public void onStop() {

                    }

                    @Override
                    public void run() {
                        Channel channel = null;
                        try {
                            String[] userserver = AppContext.getUserString(SettingsActivity.KEY_PREF_GIT_SERVER, R.string.pref_default_git_server).split("@");
                            int port = Integer.parseInt(AppContext.getUserString(SettingsActivity.KEY_PREF_GIT_SERVER_PORT, R.string.pref_default_git_server_port));
                            channel = SSHSession.openSession(userserver[0], userserver[1], port);
                            OutputStream os = channel.getOutputStream();
                            InputStream is = channel.getInputStream();
                            os.write(0);
                            String response = Util.readStream(is);
                            if(response != null) {
                                String[] lines = response.split("\n");
                                for(String line:lines) {
                                    if(line.trim().indexOf("R") == 0 && line.contains(AppContext.udid())) {
                                        String[] parts = line.split("/");
                                        targetTranslationIds.add(parts[parts.length - 1]);
                                    }
                                }
                            }
                        } catch (JSchException e) {
                            e.printStackTrace();
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if(channel != null) {
                            channel.disconnect();
                        }
                    }

                    @Override
                    public void onPostExecute() {
                        if(targetTranslationIds.size() > 0) {
                            FragmentTransaction ft = getFragmentManager().beginTransaction();
                            Fragment prev = getFragmentManager().findFragmentByTag("restoreDialog");
                            if (prev != null) {
                                ft.remove(prev);
                            }
                            ft.addToBackStack(null);

                            RestoreFromCloudDialog dialog = new RestoreFromCloudDialog();
                            Bundle args = new Bundle();
                            args.putStringArray(RestoreFromCloudDialog.ARG_TARGET_TRANSLATIONS, targetTranslationIds.toArray(new String[targetTranslationIds.size()]));
                            dialog.setArguments(args);
                            dialog.show(ft, "restoreDialog");
                        } else {
                            new AlertDialog.Builder(getActivity())
                                    .setTitle(R.string.import_from_online)
                                    .setMessage(R.string.no_backups_online)
                                    .setNeutralButton(R.string.dismiss, null)
                                    .show();
                        }
                    }
                }.start();
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
