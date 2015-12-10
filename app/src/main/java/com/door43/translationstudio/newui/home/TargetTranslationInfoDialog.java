package com.door43.translationstudio.newui.home;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
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
import android.widget.ImageButton;
import android.widget.TextView;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.SourceLanguage;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.core.Typography;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.newui.PrintDialog;
import com.door43.translationstudio.newui.publish.PublishActivity;
import com.door43.translationstudio.newui.BackupDialog;
import com.door43.translationstudio.user.Profile;
import com.door43.translationstudio.user.ProfileManager;
import com.door43.util.tasks.ThreadableUI;
import com.door43.widget.ViewUtil;

import java.io.File;
import java.util.Locale;

/**
 * Displays detailed information about a target translation
 */
public class TargetTranslationInfoDialog extends DialogFragment {

    public static final String ARG_TARGET_TRANSLATION_ID = "arg_target_translation_id";
    private TargetTranslation mTargetTranslation;
    private Translator mTranslator;
    private OnDeleteListener mListener;
    private int mTranslationProgress = 0;
    private boolean mTranslationProgressWasCalculated = false;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_target_translation_info, container, false);

        mTranslator = AppContext.getTranslator();
        Bundle args = getArguments();
        if(args == null || !args.containsKey(ARG_TARGET_TRANSLATION_ID)) {
            dismiss();
        } else {
            String targetTranslationId = args.getString(ARG_TARGET_TRANSLATION_ID, null);
            mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
        }

        final Library library = AppContext.getLibrary();

        TextView title = (TextView)v.findViewById(R.id.title);
        TextView projectTitle = (TextView)v.findViewById(R.id.project_title);
        SourceLanguage sourceLanguage = library.getPreferredSourceLanguage(mTargetTranslation.getProjectId(), Locale.getDefault().getLanguage());
        if(sourceLanguage != null) {
            title.setText(sourceLanguage.projectTitle + " - " + mTargetTranslation.getTargetLanguageName());
            projectTitle.setText(sourceLanguage.projectTitle + " (" + mTargetTranslation.getProjectId() + ")");
        } else {
            title.setText(mTargetTranslation.getProjectId() + " - " + mTargetTranslation.getTargetLanguageName());
            projectTitle.setText(mTargetTranslation.getProjectId());
        }

        TextView languageTitle = (TextView)v.findViewById(R.id.language_title);
        languageTitle.setText(mTargetTranslation.getTargetLanguageName() + " (" + mTargetTranslation.getTargetLanguageId() + ")");

        final TextView progressView = (TextView)v.findViewById(R.id.progress);
        final ThreadableUI task = new ThreadableUI(getActivity()) {
            private int progress = 0;

            @Override
            public void onStop() {

            }

            @Override
            public void run() {
                progress = Math.round(library.getTranslationProgress(mTargetTranslation) * 100);
            }

            @Override
            public void onPostExecute() {
                if (!isInterrupted()) {
                    mTranslationProgressWasCalculated = true;
                    mTranslationProgress = progress;
                    progressView.setText(progress + "%");
                }
            }
        };
        if(!mTranslationProgressWasCalculated) {
            progressView.setText("");
            task.start();
        } else {
            progressView.setText(mTranslationProgress + "%");
        }

        TextView translatorsView = (TextView)v.findViewById(R.id.translators);
        translatorsView.setText("");
        Profile profile = ProfileManager.getProfile();
        if(profile != null) {
            translatorsView.setText(profile.getName());
        }
        // TODO: 10/1/2015 support displaying multiple translators

        ImageButton deleteButton = (ImageButton)v.findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CustomAlertDialog.Create(getActivity())
                        .setTitle(R.string.label_delete)
                        .setIcon(R.drawable.ic_delete_black_24dp)
                        .setMessage(R.string.confirm_delete_target_translation)
                        .setPositiveButton(R.string.confirm, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                task.stop();
                                if(mTargetTranslation != null) {
                                    mTranslator.deleteTargetTranslation(mTargetTranslation.getId());
                                    AppContext.clearTargetTranslationSettings(mTargetTranslation.getId());
                                }
                                if(mListener != null) {
                                    mListener.onDeleteTargetTranslation(mTargetTranslation.getId());
                                }
                                dismiss();
                            }
                        })
                        .setNegativeButton(R.string.no, null)
                        .show("DeleteTrans");
            }
        });

        ImageButton backupButton = (ImageButton)v.findViewById(R.id.backup_button);
        backupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction backupFt = getFragmentManager().beginTransaction();
                Fragment backupPrev = getFragmentManager().findFragmentByTag(BackupDialog.TAG);
                if (backupPrev != null) {
                    backupFt.remove(backupPrev);
                }
                backupFt.addToBackStack(null);

                BackupDialog backupDialog = new BackupDialog();
                Bundle args = new Bundle();
                args.putString(BackupDialog.ARG_TARGET_TRANSLATION_ID, mTargetTranslation.getId());
                backupDialog.setArguments(args);
                backupDialog.show(backupFt, BackupDialog.TAG);
            }
        });

        ImageButton publishButton = (ImageButton)v.findViewById(R.id.publish_button);
        publishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent publishIntent = new Intent(getActivity(), PublishActivity.class);
                publishIntent.putExtra(PublishActivity.EXTRA_TARGET_TRANSLATION_ID, mTargetTranslation.getId());
                publishIntent.putExtra(PublishActivity.EXTRA_CALLING_ACTIVITY, PublishActivity.ACTIVITY_HOME);
                startActivity(publishIntent);
            }
        });

        ImageButton printButton = (ImageButton)v.findViewById(R.id.print_button);
        printButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction printFt = getFragmentManager().beginTransaction();
                Fragment printPrev = getFragmentManager().findFragmentByTag("printDialog");
                if (printPrev != null) {
                    printFt.remove(printPrev);
                }
                printFt.addToBackStack(null);

                PrintDialog printDialog = new PrintDialog();
                Bundle printArgs = new Bundle();
                printArgs.putString(PrintDialog.ARG_TARGET_TRANSLATION_ID, mTargetTranslation.getId());
                printDialog.setArguments(printArgs);
                printDialog.show(printFt, "printDialog");
            }
        });
        return v;
    }

    /**
     * Assigns a listener for this dialog
     * @param listener
     */
    public void setOnDeleteListener(OnDeleteListener listener) {
        mListener = listener;
    }

    public interface OnDeleteListener {
        void onDeleteTargetTranslation(String targetTranslationId);
    }
}
