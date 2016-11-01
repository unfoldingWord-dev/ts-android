package com.door43.translationstudio.ui.home;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.NativeSpeaker;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.ui.dialogs.PrintDialog;
import com.door43.translationstudio.ui.newtranslation.NewTargetTranslationActivity;
import com.door43.translationstudio.ui.publish.PublishActivity;
import com.door43.translationstudio.ui.dialogs.BackupDialog;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.models.SourceLanguage;
import org.unfoldingword.resourcecontainer.Project;
import org.unfoldingword.resourcecontainer.Resource;
import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.logger.Logger;
import org.unfoldingword.tools.taskmanager.ThreadableUI;

import java.util.ArrayList;
import java.util.List;


/**
 * Displays detailed information about a target translation
 */
public class TargetTranslationInfoDialog extends DialogFragment {

    public static final String ARG_TARGET_TRANSLATION_ID = "arg_target_translation_id";
    public static final int CHANGE_TARGET_TRANSLATION_LANGUAGE = 2;
    private TargetTranslation mTargetTranslation;
    private Translator mTranslator;
    private OnDeleteListener mListener;
    private int mTranslationProgress = 0;
    private boolean mTranslationProgressWasCalculated = false;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_target_translation_info, container, false);

        mTranslator = App.getTranslator();
        Bundle args = getArguments();
        if(args == null || !args.containsKey(ARG_TARGET_TRANSLATION_ID)) {
            dismiss();
        } else {
            String targetTranslationId = args.getString(ARG_TARGET_TRANSLATION_ID, null);
            mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
            if(mTargetTranslation == null) {
                Logger.w("TargetTranslationInfoDialog", "Unknown target translation " + targetTranslationId);
                dismiss();
                return null;
            }
        }

        final Door43Client library = App.getLibrary();

        TextView title = (TextView)v.findViewById(R.id.title);
        TextView projectTitle = (TextView)v.findViewById(R.id.project_title);
        Project p = library.index().getProject(App.getDeviceLanguageCode(), mTargetTranslation.getProjectId(), true);
        SourceLanguage sourceLanguage = library.index().getSourceLanguage(p.languageSlug);
        List<Resource> resources = library.index().getResources(sourceLanguage.slug, p.slug);
        ResourceContainer resourceContainer;
        try {
            resourceContainer = library.open(sourceLanguage.slug, p.slug, resources.get(0).slug);
        } catch (Exception e) {
            Logger.e("TargetTranslationInfoDialog", "Failed to open the container", e);
            dismiss();
            return v;
        }
        String projectTitleString = null;
        projectTitleString = resourceContainer.readChunk("front", "title");
        if(projectTitleString != null) {
            title.setText(projectTitleString + " - " + mTargetTranslation.getTargetLanguageName());
            projectTitle.setText(projectTitleString + " (" + mTargetTranslation.getProjectId() + ")");
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
                // TODO: 9/28/16 this should use the task instead
//                progress = Math.round(library.getTranslationProgress(mTargetTranslation) * 100);
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
        String translators = getTranslaterNames("\n");
        if(translators != null) {
            translatorsView.setText(translators);
        }
        // TODO: 10/1/2015 support displaying multiple translators

        TextView publishView = (TextView)v.findViewById(R.id.publish_state);
        publishView.setText("");
        int statusID = 0;
        switch (mTargetTranslation.getPublishedStatus()) {
            case NOT_PUBLISHED:
                statusID = R.string.publish_status_not;
                break;

            case IS_CURRENT:
                statusID = R.string.publish_status_current;
                break;

            case NOT_CURRENT:
                statusID = R.string.publish_status_not_current;
                break;

            default:
            case ERROR:
                statusID = R.string.error;
                break;
        }
        publishView.setText(statusID);

        TextView changeButton = (TextView)v.findViewById(R.id.change_language);
        changeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), NewTargetTranslationActivity.class);
                intent.putExtra(NewTargetTranslationActivity.EXTRA_TARGET_TRANSLATION_ID, mTargetTranslation.getId());
                intent.putExtra(NewTargetTranslationActivity.EXTRA_CHANGE_TARGET_LANGUAGE_ONLY, true);
                startActivityForResult(intent, CHANGE_TARGET_TRANSLATION_LANGUAGE);
            }
        });

        ImageButton deleteButton = (ImageButton)v.findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            new AlertDialog.Builder(getActivity(),R.style.AppTheme_Dialog)
                    .setTitle(R.string.label_delete)
                    .setIcon(R.drawable.ic_delete_black_24dp)
                    .setMessage(R.string.confirm_delete_target_translation)
                    .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            task.stop();
                            if(mTargetTranslation != null) {
                                mTranslator.deleteTargetTranslation(mTargetTranslation.getId());
                                App.clearTargetTranslationSettings(mTargetTranslation.getId());
                            }
                            if(mListener != null) {
                                mListener.onDeleteTargetTranslation(mTargetTranslation.getId());
                            }
                            dismiss();
                        }
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
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
                dismiss(); // close dialog so notifications will pass back to HomeActivity
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

        View contributorsGroup = v.findViewById(R.id.contributors_group);
        contributorsGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction();
                Fragment prev = getActivity().getFragmentManager().findFragmentByTag("manage-contributors");
                if (prev != null) {
                    ft.remove(prev);
                }
                ft.addToBackStack(null);

                ManageContributorsDialog dialog = new ManageContributorsDialog();
                Bundle args = new Bundle();
                args.putString(ManageContributorsDialog.EXTRA_TARGET_TRANSLATION_ID, mTargetTranslation.getId());
                dialog.setArguments(args);
                dialog.show(ft, "manage-contributors");
            }
        });

        // TODO: re-connect to dialogs

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

    /**
     * returns a concatenated list of names or null if error
     */
    public String getTranslaterNames(String between) {

        ArrayList<NativeSpeaker> nameList = mTargetTranslation.getContributors();

        if(null != nameList) {
            String listString = "";

            for (int i = 0; i < nameList.size(); i++) {
                if(!listString.isEmpty()) {
                    listString += between;
                }
                listString += nameList.get(i).name;
            }

            return listString;
        }
        return null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (CHANGE_TARGET_TRANSLATION_LANGUAGE == requestCode) {
            if(NewTargetTranslationActivity.RESULT_MERGE_CONFLICT == resultCode ) {
                String targetTranslationID = data.getStringExtra(NewTargetTranslationActivity.EXTRA_TARGET_TRANSLATION_ID);
                if(targetTranslationID != null) {
                    Activity activity = getActivity();
                    if(activity instanceof HomeActivity) {
                        ((HomeActivity) activity).doManualMerge(targetTranslationID);
                    }
                }
            }

            dismiss();
        }
    }
}
