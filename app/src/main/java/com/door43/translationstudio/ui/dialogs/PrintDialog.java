package com.door43.translationstudio.ui.dialogs;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.provider.DocumentFile;
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
import com.door43.translationstudio.core.ContainerCache;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.tasks.DownloadImagesTask;
import com.door43.translationstudio.tasks.PrintPDFTask;
import com.door43.translationstudio.ui.filechooser.FileChooserActivity;
import com.door43.util.FileUtilities;
import com.door43.util.SdUtils;

import org.unfoldingword.door43client.Door43Client;
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

/**
 * Created by joel on 11/16/2015.
 */
public class PrintDialog extends DialogFragment implements SimpleTaskWatcher.OnFinishedListener, SimpleTaskWatcher.OnCanceledListener {

    public static final String TAG = "printDialog";
    private static final int SELECT_PDF_FOLDER_REQUEST = 314;
    public static final String ARG_TARGET_TRANSLATION_ID = "arg_target_translation_id";
    public static final String STATE_INCLUDE_IMAGES = "include_images";
    public static final String STATE_INCLUDE_INCOMPLETE = "include_incomplete";
    public static final String STATE_DIALOG_SHOWN = "state_dialog_shown";
    public static final String DOWNLOAD_IMAGES_TASK_KEY = "download_images_task";
    public static final String DOWNLOAD_IMAGES_TASK_GROUP = "download_images_task";
    public static final int INVALID = -1;
    public static final String STATE_OUTPUT_TO_DOCUMENT_FILE = "state_output_to_document_file";
    public static final String STATE_OUTPUT_FOLDER_URI = "state_output_folder_uri";
    public static final String STATE_OUTPUT_FILENAME = "state_output_filename";
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
    private Uri mDestinationFolderUri;
    private String mDestinationFilename;
    private DialogShown mAlertShown = DialogShown.NONE;
    private AlertDialog mPrompt;
    private File mImagesDir;

    @Override
    public void onDestroyView() {
        taskWatcher.stop();
        taskWatcher.setOnCanceledListener(null);
        taskWatcher.setOnFinishedListener(null);
        if(mPrompt != null) {
            mPrompt.dismiss();
        }
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
            mAlertShown = DialogShown.fromInt(savedInstanceState.getInt(STATE_DIALOG_SHOWN, INVALID), DialogShown.NONE);
            isPdfOutputToDocumentFile = savedInstanceState.getBoolean(STATE_OUTPUT_TO_DOCUMENT_FILE, false);
            mDestinationFolderUri = Uri.parse(savedInstanceState.getString(STATE_OUTPUT_FOLDER_URI, ""));
            mDestinationFilename = savedInstanceState.getString(STATE_OUTPUT_FILENAME, null);
        }

        // load the project title
        TextView projectTitleView = (TextView)v.findViewById(R.id.project_title);
        String title = mTargetTranslation.getProjectTranslation().getTitle().replaceAll("\n+$", "");
        if(title.isEmpty()) {
            ResourceContainer sourceContainer = ContainerCache.cacheClosest(library, null, mTargetTranslation.getProjectId(), mTargetTranslation.getResourceSlug());
            if(sourceContainer != null) {
                title = sourceContainer.readChunk("front", "title").replaceAll("\n+$", "");
            }
        }
        if(title.isEmpty()) {
            title = mTargetTranslation.getProjectId();
        }
        projectTitleView.setText(title + " - " + mTargetTranslation.getTargetLanguageName());

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

        restoreDialogs();
        return v;
    }

    /**
     * Restores dialogs
     */
    private void restoreDialogs() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {

                // restore alert dialogs
                switch(mAlertShown) {
                    case INTERNET_PROMPT:
                        showInternetUsePrompt();
                        break;

                    case FILENAME_PROMPT:
                        showPdfFilenamePrompt();
                        break;

                    case OVERWRITE_PROMPT:
                        showPdfOverwrite();
                        break;

                    case NONE:
                        break;

                    default:
                        Logger.e(TAG,"Unsupported restore dialog: " + mAlertShown.toString());
                        break;
                }
            }
        });
    }

    /**
     * starts activity to let user select output folder
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
        args.putString(FileChooserActivity.EXTRA_TITLE, getActivity().getResources().getString(R.string.choose_destination_folder));
        intent.putExtras(args);
        startActivityForResult(intent, SELECT_PDF_FOLDER_REQUEST);
    }

    /**
     * start PDF printing.  If image printing is selected, will prompt to warn for internet usage
     * @param force - if true then we will overwrite existing file
     * @return false if not forced and file already is present
     */
    private boolean startPdfPrinting(boolean force) {
        if(!force && SdUtils.exists(mDestinationFolderUri, mDestinationFilename)) {
            return false;
        }
        if(includeImages && !App.hasImages()) {
            showInternetUsePrompt();
        } else {
            PrintPDFTask task = new PrintPDFTask(mTargetTranslation.getId(), mExportFile, includeImages, includeIncompleteFrames, mImagesDir);
            taskWatcher.watch(task);
            TaskManager.addTask(task, PrintPDFTask.TASK_ID);
        }
        return true;
    }

    /**
     * prompt user to show internet
     */
    private void showInternetUsePrompt() {
        mAlertShown = DialogShown.INTERNET_PROMPT;

        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                .setTitle(R.string.use_internet_confirmation)
                .setMessage(R.string.image_large_download)
                .setNegativeButton(R.string.title_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mAlertShown = DialogShown.NONE;
                    }
                })
                .setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mAlertShown = DialogShown.NONE;
                        DownloadImagesTask task = new DownloadImagesTask();
                        taskWatcher.watch(task);
                        TaskManager.addTask(task, DownloadImagesTask.TASK_ID);
                    }
                })
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_PDF_FOLDER_REQUEST) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                mDestinationFolderUri = data.getData();
                showPdfFilenamePrompt();
            }
        }
    }

    /**
     * prompt for the filename for PDF output
     */
    private void showPdfFilenamePrompt() {
        mAlertShown = DialogShown.FILENAME_PROMPT;
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final View filenameFragment = inflater.inflate(R.layout.fragment_output_filename, null);
        if(filenameFragment != null) {
            final EditText filenameText = (EditText) filenameFragment.findViewById(R.id.filename_text);
            if ((filenameText != null)) {
                filenameText.setText( mDestinationFilename != null ? mDestinationFilename : mExportFile.getName()); // restore previous data, or set to default value

                // pop up file name prompt
                mPrompt = new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                        .setTitle(R.string.pdf_output_filename_title_prompt)
                        .setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mAlertShown = DialogShown.NONE;
                                mDestinationFilename = filenameText.getText().toString();
                                boolean conflict = !startPdfPrinting(false);
                                if(conflict) {
                                    showPdfOverwrite();
                                }
                            }
                        })
                        .setNegativeButton(R.string.title_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mAlertShown = DialogShown.NONE;
                                mDestinationFilename = null;
                                dialog.dismiss();
                            }
                        })
                        .setView(filenameFragment)
                        .show();
            }
        }
    }

    /**
     * display confirmation prompt before USFM export (also allow entry of filename)
     */
    private void showPdfOverwrite() {
        mAlertShown = DialogShown.OVERWRITE_PROMPT;
        String message = getOverwriteMessage(mDestinationFolderUri, mDestinationFilename);
        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                .setTitle(R.string.overwrite_file_title)
                .setMessage(message)
                .setPositiveButton(R.string.overwrite_label, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mAlertShown = DialogShown.NONE;
                        startPdfPrinting(true);
                    }
                })
                .setNeutralButton(R.string.title_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mAlertShown = DialogShown.NONE;
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.rename_label, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mAlertShown = DialogShown.NONE;
                        showPdfFilenamePrompt();
                    }
                })
                .show();
    }

    /**
     * format a message for overwrite warning
     * @param fileName
     * @return
     */
    private String getOverwriteMessage(Uri uri, String fileName) {
        String path = SdUtils.getPathString(uri, fileName);
        String sizeStr = SdUtils.getFormattedFileSize(getActivity(), uri, fileName);
        String dateStr = SdUtils.getDate(getActivity(), uri, fileName);
        return getString(R.string.overwrite_file_warning, path, sizeStr, dateStr);
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        out.putBoolean(STATE_INCLUDE_IMAGES, includeImages);
        out.putBoolean(STATE_INCLUDE_INCOMPLETE, includeIncompleteFrames);
        out.putInt(STATE_DIALOG_SHOWN, mAlertShown.getValue());
        out.putBoolean(STATE_OUTPUT_TO_DOCUMENT_FILE, isPdfOutputToDocumentFile);

        if((mAlertShown == DialogShown.FILENAME_PROMPT)
                && (mPrompt != null)) {

            final EditText filenameText = (EditText) mPrompt.findViewById(R.id.filename_text);
            mDestinationFilename = filenameText.getText().toString();
        }
        out.putString(STATE_OUTPUT_FILENAME, mDestinationFilename);

        if(mDestinationFolderUri != null) {
            out.putString(STATE_OUTPUT_FOLDER_URI, mDestinationFolderUri.toString());
        }

        super.onSaveInstanceState(out);
    }

    @Override
    public void onFinished(ManagedTask task) {
        taskWatcher.stop();
        TaskManager.clearTask(task);

        if(task instanceof DownloadImagesTask) {
            DownloadImagesTask downloadImagesTask = (DownloadImagesTask) task;
            if (downloadImagesTask.getSuccess()) {
                mImagesDir = downloadImagesTask.getImagesDir();
                final PrintPDFTask printTask = new PrintPDFTask(mTargetTranslation.getId(), mExportFile, includeImages, includeIncompleteFrames, mImagesDir);
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
                File pdfOutputFolder = null;
                Uri pdfOutputUri = null;
                boolean success = false;
                String pdfDestination = "";

                String scheme = mDestinationFolderUri.getScheme();
                isPdfOutputToDocumentFile = !"file".equalsIgnoreCase(scheme);

                // copy PDF to location the user selected
                if (isPdfOutputToDocumentFile) {
                    try {
                        SdUtils.documentFileDelete(mDestinationFolderUri, mDestinationFilename); // make sure file does not exist, otherwise api will create a duplicate file in next line
                        DocumentFile sdCardFile = SdUtils.documentFileCreate(mDestinationFolderUri, mDestinationFilename);
                        OutputStream outputStream = SdUtils.createOutputStream(sdCardFile);
                        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);

                        FileInputStream fis = new FileInputStream(mExportFile);
                        int bytes = FileUtilities.copy(fis, bufferedOutputStream);
                        bufferedOutputStream.close();
                        fis.close();
                        pdfDestination = SdUtils.getPathString(sdCardFile);
                        success = true;
                    } catch (Exception e) {
                        Logger.e(TAG, "Failed to copy the PDF file to: " + pdfOutputUri, e);
                    }

                } else { // destination is regular file
                    try {
                        pdfOutputFolder = new File(mDestinationFolderUri.getPath(), mDestinationFilename);
                        FileUtilities.copyFile(mExportFile, pdfOutputFolder);
                        pdfDestination = pdfOutputFolder.toString();
                        success = true;
                    } catch (IOException e) {
                        Logger.e(TAG, "Failed to copy the PDF file to: " + pdfOutputFolder, e);
                    }
                }

                if(success) {
                    String message = getActivity().getResources().getString(R.string.print_success, pdfDestination);
                    new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                            .setTitle(R.string.success)
                            .setMessage(message)
                            .setPositiveButton(R.string.dismiss, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    PrintDialog.this.dismiss();
                                }
                            })
                            .show();
                    return;
                }
            }

            new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                    .setTitle(R.string.error)
                    .setMessage(R.string.print_failed)
                    .setPositiveButton(R.string.dismiss, null)
                    .show();
        }
    }

    @Override
    public void onCanceled(ManagedTask task) {
        // try to stop downloading if the user cancels the download
        task.stop();
    }

    /**
     * for keeping track if dialog is being shown for orientation changes
     */
    public enum DialogShown {
        NONE,
        INTERNET_PROMPT,
        FILENAME_PROMPT,
        OVERWRITE_PROMPT;

        public int getValue() {
            return this.ordinal();
        }

        public static DialogShown fromInt(int ordinal, DialogShown defaultValue) {
            if (ordinal > 0 && ordinal < DialogShown.values().length) {
                return DialogShown.values()[ordinal];
            }
            return defaultValue;
        }
    }

}
