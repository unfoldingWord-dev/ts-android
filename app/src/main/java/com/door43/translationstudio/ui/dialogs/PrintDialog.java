package com.door43.translationstudio.ui.dialogs;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.tasks.DownloadImagesTask;
import com.door43.translationstudio.tasks.PrintPDFTask;
import com.door43.translationstudio.ui.filechooser.FileChooserActivity;
import com.door43.util.FileUtilities;
import com.door43.util.SdUtils;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.resourcecontainer.Project;
import org.unfoldingword.resourcecontainer.Resource;
import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.logger.Logger;
import org.unfoldingword.tools.taskmanager.SimpleTaskWatcher;
import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.TaskManager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.util.List;

/**
 * Created by joel on 11/16/2015.
 */
public class PrintDialog extends DialogFragment implements SimpleTaskWatcher.OnFinishedListener, SimpleTaskWatcher.OnCanceledListener {

    public static final String TAG = "printDialog";
    private static final int SELECT_PDF_FOLDER_REQUEST = 314;
    public static final String ARG_TARGET_TRANSLATION_ID = "arg_target_translation_id";
    public static final String STATE_INCLUDE_IMAGES = "include_images";
    public static final String STATE_INCLUDE_INCOMPLETE = "include_incomplete";
    public static final String DOWNLOAD_IMAGES_TASK_KEY = "download_images_task";
    public static final String DOWNLOAD_IMAGES_TASK_GROUP = "download_images_task";
    private Translator translator;
    private TargetTranslation mTargetTranslation;
    private Door43Client library;
    private boolean includeImages = false;
    private boolean includeIncompleteFrames = true;
    private Button printButton;
    private CheckBox includeImagesCheckBox;
    private CheckBox includeIncompleteCheckBox;
    private SimpleTaskWatcher taskWatcher;
    private File mExportFile;
    private boolean isPdfOutputToDocumentFile;
    private Uri mPdfOutputUri;
    private File mPdfOutputFolder;


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

        translator = App.getTranslator();
        library = App.getLibrary();

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

        taskWatcher = new SimpleTaskWatcher(getActivity(), R.string.loading);
        taskWatcher.setOnFinishedListener(this);
        taskWatcher.setOnCanceledListener(this);

        if(savedInstanceState != null) {
            includeImages = savedInstanceState.getBoolean(STATE_INCLUDE_IMAGES, includeImages);
            includeIncompleteFrames = savedInstanceState.getBoolean(STATE_INCLUDE_INCOMPLETE, includeIncompleteFrames);
        }

        TextView projectTitle = (TextView)v.findViewById(R.id.project_title);
        Project p = library.index().getProject(App.getDeviceLanguageCode(), mTargetTranslation.getProjectId(), true);
        List<Resource> resources = library.index().getResources(p.languageSlug, p.slug);
        ResourceContainer resourceContainer = null;
        try {
            resourceContainer = library.open(p.languageSlug, p.slug, resources.get(0).slug);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        SourceLanguage sourceLanguage = library.getPreferredSourceLanguage(mTargetTranslation.getProjectId(), App.getDeviceLanguageCode());
        if(resourceContainer != null) {
            projectTitle.setText(resourceContainer.readChunk("front", "title") + " - " + mTargetTranslation.getTargetLanguageName());
        } else {
            projectTitle.setText(mTargetTranslation.getProjectId() + " - " + mTargetTranslation.getTargetLanguageName());
        }

        boolean isObsProject = mTargetTranslation.isObsProject();

        this.includeImagesCheckBox = (CheckBox)v.findViewById(R.id.print_images);
        this.includeIncompleteCheckBox = (CheckBox)v.findViewById(R.id.print_incomplete_frames);

        if(isObsProject) {
            includeImagesCheckBox.setEnabled(true);
            includeImagesCheckBox.setChecked(includeImages);
        } else { // no images in bible stories
            includeImagesCheckBox.setVisibility(View.GONE);
            includeImagesCheckBox.setChecked(false);
        }

        includeIncompleteCheckBox.setEnabled(true);
        includeIncompleteCheckBox.setChecked(includeIncompleteFrames);

        mExportFile = new File(App.getSharingDir(), mTargetTranslation.getId() + ".pdf");

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
                doSelectDestinationFolder();
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
     * let user select output folder
     */
    private void doSelectDestinationFolder() {
        String typeStr = null;
        Intent intent = new Intent(getActivity(), FileChooserActivity.class);
        isPdfOutputToDocumentFile = SdUtils.isSdCardPresentLollipop();
        if(isPdfOutputToDocumentFile) {
            typeStr = FileChooserActivity.SD_CARD_TYPE;
        } else {
            typeStr = FileChooserActivity.INTERNAL_TYPE;
        }

        intent.setType(typeStr);
        Bundle args = new Bundle();
        args.putString(FileChooserActivity.EXTRA_MODE, FileChooserActivity.SelectionMode.DIRECTORY.name());
        intent.putExtras(args);
        startActivityForResult(intent, SELECT_PDF_FOLDER_REQUEST);
    }

    /**
     * start PDF printing (prompt for internet usage if images selected)
     */
    private void startPdfPrinting() {
        if(includeImages && !App.hasImages()) {
            new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                    .setTitle(R.string.use_internet_confirmation)
                    .setMessage(R.string.image_large_download)
                    .setNegativeButton(R.string.title_cancel, null)
                    .setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            DownloadImagesTask task = new DownloadImagesTask();
                            taskWatcher.watch(task);
                            TaskManager.addTask(task, DownloadImagesTask.TASK_ID);
                        }
                    })
                    .show();
        } else {
            PrintPDFTask task = new PrintPDFTask(mTargetTranslation.getId(), mExportFile, includeImages, includeIncompleteFrames);
            taskWatcher.watch(task);
            TaskManager.addTask(task, PrintPDFTask.TASK_ID);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (requestCode == SELECT_PDF_FOLDER_REQUEST) {
            if (resultCode == Activity.RESULT_OK && data != null) {

                // TODO: 11/10/16 prompt for name

                LayoutInflater inflater = LayoutInflater.from(getActivity());
                final View filenameFragment = inflater.inflate(R.layout.fragment_pdf_output_filename, null);
                if(filenameFragment != null) {
                    final EditText filenameText = (EditText) filenameFragment.findViewById(R.id.filename_text);
                    if ((filenameText != null)) {
                        filenameText.setText(mExportFile.getName());

                        // pop up file name prompt
                        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                                .setTitle(R.string.pdf_output_filename_title_prompt)
                                .setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String filename = filenameText.getText().toString();

                                        if (isPdfOutputToDocumentFile) {
                                            Uri destinationFolder = data.getData();
                                            mPdfOutputUri = Uri.withAppendedPath(destinationFolder, filename);
                                        } else {
                                            File destinationFolder = new File(data.getData().getPath());
                                            mPdfOutputFolder = new File(destinationFolder, filename);
                                        }
                                        startPdfPrinting();
                                    }
                                })
                                .setNegativeButton(R.string.title_cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .setView(filenameFragment)
                                .show();
                    }
                }
            }
        }
    }

    /**
     * Begins printing the translation
     */
//    private void print() {
//        // TODO: 11/16/2015 place the actual print operation within a task
//        File exportFile = new File(App.getSharingDir(), mTargetTranslation.getId() + ".pdf");
//        try {
//            SourceTranslation sourceTranslation = App.getLibrary().getDefaultSourceTranslation(mTargetTranslation.getProjectId(), "en");
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
                final PrintPDFTask printTask = new PrintPDFTask(mTargetTranslation.getId(), mExportFile, includeImages, includeIncompleteFrames);
                Handler hand = new Handler(Looper.getMainLooper());
                hand.post(new Runnable() {
                    @Override
                    public void run() {
                        taskWatcher.watch(printTask);
                    }
                });
                TaskManager.addTask(printTask, PrintPDFTask.TASK_ID);
            } else {
                // download failed
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                                .setTitle(R.string.download_failed)
                                .setMessage(R.string.downloading_images_for_print_failed)
                                .setPositiveButton(R.string.label_ok, null)
                                .show();
                    }
                });
            }
        } else if(task instanceof PrintPDFTask) {
            if(((PrintPDFTask)task).isSuccess()) {

                if (isPdfOutputToDocumentFile) {
                    try {
                        OutputStream outputStream = App.context().getContentResolver().openOutputStream(mPdfOutputUri);
                        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);

                        FileInputStream fis = new FileInputStream(mExportFile);
                        int bytes = FileUtilities.copy(fis, bufferedOutputStream);
                        bufferedOutputStream.close();
                        fis.close();
                    } catch (Exception e) {
                        Logger.e(TAG, "Failed to copy the PDF file to: " + mPdfOutputUri, e);
                    }

                } else { // destination is regular file
                    try {
                        FileUtilities.copyFile(mExportFile, mPdfOutputFolder);
                    } catch (IOException e) {
                        Logger.e(TAG, "Failed to copy the PDF file to: " + mPdfOutputFolder, e);
                    }
                }

                // send to print provider
                Uri u = FileProvider.getUriForFile(App.context(), "com.door43.translationstudio.fileprovider", mExportFile);
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("application/pdf");
                i.putExtra(Intent.EXTRA_STREAM, u);
                startActivity(Intent.createChooser(i, "Print:"));
            } else {
                new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                        .setTitle(R.string.error)
                        .setMessage(R.string.print_failed)
                        .setPositiveButton(R.string.dismiss, null)
                        .show();
            }
        }
    }

    @Override
    public void onCanceled(ManagedTask task) {
        // try to stop downloading if the user cancels the download
        task.stop();
    }
}
