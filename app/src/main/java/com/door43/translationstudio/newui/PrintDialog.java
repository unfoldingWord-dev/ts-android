package com.door43.translationstudio.newui;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.SourceLanguage;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.core.Typography;
import com.door43.widget.ViewUtil;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.Locale;

/**
 * Created by joel on 11/16/2015.
 */
public class PrintDialog extends DialogFragment {

    public static final String ARG_TARGET_TRANSLATION_ID = "arg_target_translation_id";
    public static final String STATE_INCLUDE_IMAGES = "include_images";
    public static final String STATE_INCLUDE_INCOMPLETE = "include_incomplete";
    private Translator translator;
    private TargetTranslation mTargetTranslation;
    private Library library;
    private boolean includeImages = true;
    private boolean includeIncompleteFrames = true;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_print, container, false);

        translator = AppContext.getTranslator();
        library = AppContext.getLibrary();

        Bundle args = getArguments();
        if(args == null || !args.containsKey(ARG_TARGET_TRANSLATION_ID)) {
            throw new InvalidParameterException("The target translation id was not specified");
        } else {
            String targetTranslationId = args.getString(ARG_TARGET_TRANSLATION_ID, null);
            mTargetTranslation = translator.getTargetTranslation(targetTranslationId);
            if(mTargetTranslation == null) {
                throw new InvalidParameterException("The target translation '" + targetTranslationId + "' is invalid");
            }
        }

        if(savedInstanceState != null) {
            includeImages = savedInstanceState.getBoolean(STATE_INCLUDE_IMAGES, includeImages);
            includeIncompleteFrames = savedInstanceState.getBoolean(STATE_INCLUDE_INCOMPLETE, includeIncompleteFrames);
        }

        TextView projectTitle = (TextView)v.findViewById(R.id.project_title);
        SourceLanguage sourceLanguage = library.getPreferredSourceLanguage(mTargetTranslation.getProjectId(), Locale.getDefault().getLanguage());
        if(sourceLanguage != null) {
            projectTitle.setText(sourceLanguage.projectTitle + " - " + mTargetTranslation.getTargetLanguageName());
        } else {
            projectTitle.setText(mTargetTranslation.getProjectId() + " - " + mTargetTranslation.getTargetLanguageName());
        }

        final CheckBox includeImagesCheckBox = (CheckBox)v.findViewById(R.id.print_images);
        final CheckBox includeIncompleteCheckBox = (CheckBox)v.findViewById(R.id.print_incomplete_frames);
        includeImagesCheckBox.setEnabled(true);
        includeIncompleteCheckBox.setEnabled(true);
        includeImagesCheckBox.setChecked(includeImages);
        includeIncompleteCheckBox.setChecked(includeIncompleteFrames);

        Button cancelButton  = (Button)v.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        Button printButton  = (Button)v.findViewById(R.id.print_button);
        printButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                includeImages = includeImagesCheckBox.isChecked();
                includeIncompleteFrames = includeIncompleteCheckBox.isChecked();
                includeImagesCheckBox.setEnabled(false);
                includeIncompleteCheckBox.setEnabled(false);
                if(includeImages) {
                    // TODO: 11/16/2015 check if all the images have been downloaded for this project
                    print();
                } else {
                    print();
                }
            }
        });

        // TODO: 11/16/2015  attach to existing print process.
        // TODO: 11/16/2015 attach to existing download process (for images)
        // TODO: 11/16/2015 disable print button and check boxes if another process is running

        return v;
    }

    /**
     * Begins printing the translation
     */
    private void print() {
        // TODO: 11/16/2015 place the actual print operation within a task
        File exportFile = new File(AppContext.getSharingDir(), mTargetTranslation.getId() + ".pdf");
        try {
            SourceTranslation sourceTranslation = AppContext.getLibrary().getDefaultSourceTranslation(mTargetTranslation.getProjectId(), "en");
            this.translator.exportPdf(mTargetTranslation, sourceTranslation.getFormat(), Typography.getAssetPath(getActivity()), includeImages, includeIncompleteFrames, exportFile);
            if (exportFile.exists()) {
                Uri u = FileProvider.getUriForFile(getActivity(), "com.door43.translationstudio.fileprovider", exportFile);
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("application/pdf");
                i.putExtra(Intent.EXTRA_STREAM, u);
                startActivity(Intent.createChooser(i, "Print:"));
            } else {
                Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.translation_export_failed, Snackbar.LENGTH_LONG);
                ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                snack.show();
            }
        } catch (Exception e) {
            Logger.e(PrintDialog.class.getName(), "Failed to export as pdf " + mTargetTranslation.getId(), e);
            Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.translation_export_failed, Snackbar.LENGTH_LONG);
            ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
            snack.show();
        }
        // TODO: 11/16/2015 enable form controls again
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        out.putBoolean(STATE_INCLUDE_IMAGES, includeImages);
        out.putBoolean(STATE_INCLUDE_INCOMPLETE, includeIncompleteFrames);
        super.onSaveInstanceState(out);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        // TODO: 11/16/2015 stop any pending tasks
        super.onDismiss(dialog);
    }

    @Override
    public void onDestroy() {
        // TODO: 11/16/2015 disconnect from any pending tasks
        super.onDestroy();
    }
}
