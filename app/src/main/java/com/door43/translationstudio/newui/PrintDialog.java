package com.door43.translationstudio.newui;

import android.app.DialogFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.SourceLanguage;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.tasks.DownloadImagesTask;
import com.door43.translationstudio.tasks.PrintPDFTask;
import com.door43.util.tasks.GenericTaskWatcher;
import com.door43.util.tasks.ManagedTask;
import com.door43.util.tasks.TaskManager;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.Locale;

/**
 * Created by joel on 11/16/2015.
 */
public class PrintDialog extends DialogFragment implements GenericTaskWatcher.OnFinishedListener, GenericTaskWatcher.OnCanceledListener {

    public static final String ARG_TARGET_TRANSLATION_ID = "arg_target_translation_id";
    public static final String STATE_INCLUDE_IMAGES = "include_images";
    public static final String STATE_INCLUDE_INCOMPLETE = "include_incomplete";
    public static final String DOWNLOAD_IMAGES_TASK_KEY = "download_images_task";
    public static final String DOWNLOAD_IMAGES_TASK_GROUP = "download_images_task";
    private Translator translator;
    private TargetTranslation mTargetTranslation;
    private Library library;
    private boolean includeImages = false;
    private boolean includeIncompleteFrames = true;
    private Button printButton;
    private CheckBox includeImagesCheckBox;
    private CheckBox includeIncompleteCheckBox;
    private GenericTaskWatcher taskWatcher;
    private File mExportFile;

    @Override
    public void onDestroyView() {
        taskWatcher.stop();
        taskWatcher.setOnCanceledListener(null);
        taskWatcher.setOnFinishedListener(null);
        super.onDestroyView();
    }

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

        taskWatcher = new GenericTaskWatcher(getActivity(), R.string.loading);
        taskWatcher.setOnFinishedListener(this);
        taskWatcher.setOnCanceledListener(this);

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

        this.includeImagesCheckBox = (CheckBox)v.findViewById(R.id.print_images);
        this.includeIncompleteCheckBox = (CheckBox)v.findViewById(R.id.print_incomplete_frames);
        includeImagesCheckBox.setEnabled(true);
        includeIncompleteCheckBox.setEnabled(true);
        includeImagesCheckBox.setChecked(includeImages);
        includeIncompleteCheckBox.setChecked(includeIncompleteFrames);

        mExportFile = new File(AppContext.getSharingDir(), mTargetTranslation.getId() + ".pdf");

        Button cancelButton  = (Button)v.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        printButton  = (Button)v.findViewById(R.id.print_button);
        printButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                includeImages = includeImagesCheckBox.isChecked();
                includeIncompleteFrames = includeIncompleteCheckBox.isChecked();
                if(includeImages && !AppContext.getLibrary().hasImages()) {
                    CustomAlertDialog
                            .Create(getActivity())
                            .setTitle(R.string.use_internet_confirmation)
                            .setMessage(R.string.image_large_download)
                            .setNegativeButton(R.string.title_cancel, null)
                            .setPositiveButton(R.string.label_ok, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    DownloadImagesTask task = new DownloadImagesTask();
                                    taskWatcher.watch(task);
                                    TaskManager.addTask(task, DownloadImagesTask.TASK_ID);
                                }
                            })
                            .show("print-download-images-confirmation");
                } else {
                    PrintPDFTask task = new PrintPDFTask(mTargetTranslation.getId(), mExportFile, includeImages, includeIncompleteFrames);
                    taskWatcher.watch(task);
                    TaskManager.addTask(task, PrintPDFTask.TASK_ID);
                }
            }
        });

        // re-attach to tasks
        ManagedTask downloadTask = TaskManager.getTask(DownloadImagesTask.TASK_ID);
        ManagedTask printTask = TaskManager.getTask(PrintPDFTask.TASK_ID);
        if(downloadTask != null) {
            taskWatcher.watch(downloadTask);
        } else if(printTask != null) {
            taskWatcher.watch(printTask);
        }

        return v;
    }

    /**
     * Begins printing the translation
     */
//    private void print() {
//        // TODO: 11/16/2015 place the actual print operation within a task
//        File exportFile = new File(AppContext.getSharingDir(), mTargetTranslation.getId() + ".pdf");
//        try {
//            SourceTranslation sourceTranslation = AppContext.getLibrary().getDefaultSourceTranslation(mTargetTranslation.getProjectId(), "en");
//            File imagesDir = library.getImagesDir();
//            this.translator.exportPdf(library, mTargetTranslation, sourceTranslation.getFormat(), Typography.getAssetPath(getActivity()), imagesDir, includeImages, includeIncompleteFrames, exportFile);
//            if (exportFile.exists()) {
//                Uri u = FileProvider.getUriForFile(getActivity(), "com.door43.translationstudio.fileprovider", exportFile);
//                Intent i = new Intent(Intent.ACTION_SEND);
//                i.setType("application/pdf");
//                i.putExtra(Intent.EXTRA_STREAM, u);
//                startActivity(Intent.createChooser(i, "Print:"));
//            } else {
//                Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.translation_export_failed, Snackbar.LENGTH_LONG);
//                ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
//                snack.show();
//            }
//        } catch (Exception e) {
//            Logger.e(PrintDialog.class.getName(), "Failed to export as pdf " + mTargetTranslation.getId(), e);
//            Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.translation_export_failed, Snackbar.LENGTH_LONG);
//            ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
//            snack.show();
//        }
//    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        out.putBoolean(STATE_INCLUDE_IMAGES, includeImages);
        out.putBoolean(STATE_INCLUDE_INCOMPLETE, includeIncompleteFrames);
        super.onSaveInstanceState(out);
    }

    @Override
    public void onFinished(ManagedTask task) {
        taskWatcher.stop();
        TaskManager.clearTask(task);

        if(task instanceof DownloadImagesTask) {
            if (((DownloadImagesTask) task).getSuccess()) {
                PrintPDFTask printTask = new PrintPDFTask(mTargetTranslation.getId(), mExportFile, includeImages, includeIncompleteFrames);
                taskWatcher.watch(printTask);
                TaskManager.addTask(printTask, PrintPDFTask.TASK_ID);
            } else {
                // download failed
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        CustomAlertDialog
                                .Create(getActivity())
                                .setTitle(R.string.download_failed)
                                .setMessage(R.string.downloading_images_for_print_failed)
                                .setPositiveButton(R.string.label_ok, null)
                                .show("print-download-images-failed");
                    }
                });
            }
        } else if(task instanceof PrintPDFTask) {
            if(((PrintPDFTask)task).isSuccess()) {
                // send to print provider
                Uri u = FileProvider.getUriForFile(AppContext.context(), "com.door43.translationstudio.fileprovider", mExportFile);
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("application/pdf");
                i.putExtra(Intent.EXTRA_STREAM, u);
                startActivity(Intent.createChooser(i, "Print:"));
            } else {
                CustomAlertDialog
                        .Create(getActivity())
                        .setTitle(R.string.error)
                        .setMessage(R.string.print_failed)
                        .setPositiveButton(R.string.dismiss, null)
                        .show("print-pdf-failed");
            }
        }
    }

    @Override
    public void onCanceled(ManagedTask task) {
        // try to stop downloading if the user cancels the download
        task.stop();
    }
}
