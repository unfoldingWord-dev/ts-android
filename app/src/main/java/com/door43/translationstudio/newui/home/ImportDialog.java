package com.door43.translationstudio.newui.home;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.filebrowser.FileBrowserActivity;
import com.door43.translationstudio.newui.translate.TargetTranslationActivity;
import com.door43.widget.ViewUtil;

import org.apache.commons.io.FilenameUtils;

import java.io.File;

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

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_import, container, false);

        Button importCloudButton = (Button)v.findViewById(R.id.import_from_cloud);
        Button importFromSDButton = (Button)v.findViewById(R.id.import_from_sd);

        // todo: provide support for restoring from cloud
        importCloudButton.setVisibility(View.GONE);

        importCloudButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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
