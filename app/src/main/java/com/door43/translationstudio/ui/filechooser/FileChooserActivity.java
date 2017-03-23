package com.door43.translationstudio.ui.filechooser;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.ui.BaseActivity;
import com.door43.util.SdUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays files on the device that the user can select.
 */
public class FileChooserActivity extends BaseActivity {
    public static final String EXTRA_MODE = "extras_selection-mode";
    public static final String EXTRA_FILTERS = "extras_file-filters";
    public static final String EXTRA_TITLE = "extras_title";
    public static final String EXTRA_READ_ACCESS = "extras_read_access";
    public static final String EXTRAS_ACCEPTED_EXTENSIONS = "extras_accepted_file_extensions";
    public static final String FOLDER_KEY = "folder";
    public static final String FILE_PATH_KEY = "file_path";
    public static final String SD_CARD_TYPE = "sd_card";
    public static final String INTERNAL_TYPE = "internal";

    private ImageButton mUpButton;
    private Button mInternalButton;
    private Button mSdCardButton;
    private Button mCancelButton;
    private Button mConfirmButton;
    private TextView mCurrentFolder;
    private ListView mFileList;
    private FileChooserAdapter mAdapter;
    private boolean mWriteAccess = false;

    private DocumentFile mCurrentDir;

    @Deprecated
    private String[] mAcceptedExtensions = { Translator.ARCHIVE_EXTENSION };

    private SelectionMode mode;
    private String filters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_import_file);

        String title = null;

        // load configuration
        Intent intent = getIntent();
        Bundle args = intent.getExtras();
        if(args != null) {
            // mode
            try {
                mode = SelectionMode.valueOf(args.getString(EXTRA_MODE, SelectionMode.FILE.name()));
            } catch (IllegalArgumentException e) {
                mode = SelectionMode.FILE;
            }

            // filters
            filters = args.getString(EXTRA_FILTERS, "*/*");

            // deprecated
            String extensions = args.getString(EXTRAS_ACCEPTED_EXTENSIONS, null);
            if (extensions != null) {
                mAcceptedExtensions = extensions.split(",");
            }
            title = args.getString(EXTRA_TITLE, null);
            mWriteAccess = !args.getBoolean(EXTRA_READ_ACCESS, false);
        }

        mUpButton = (ImageButton) findViewById(R.id.up_folder_button);
        mInternalButton = (Button) findViewById(R.id.internal_button);
        mSdCardButton = (Button) findViewById(R.id.sd_card_button);
        mCancelButton = (Button) findViewById(R.id.cancel_button);
        mConfirmButton = (Button) findViewById(R.id.confirm_button);
        mCurrentFolder = (TextView) findViewById(R.id.current_folder);
        mFileList = (ListView) findViewById(R.id.file_list);

        if(title != null) {
            setTitle(title);  // use title if given
        } else {
            setTitle(R.string.title_activity_file_explorer);
        }

        boolean haveSDCard = SdUtils.isSdCardAccessableInMode(mWriteAccess);
        showSdCardOption(haveSDCard);

        mUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DocumentFile parent = mCurrentDir.getParentFile();
                loadDocFileList(parent);
            }
        });

        mConfirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean itemSelected = false;
                DocumentFileItem selectedItem = null;
                int position = mFileList.getCheckedItemPosition();
                // validate the file
                if (mode != SelectionMode.DIRECTORY && position >= 0) {
                    selectedItem = mAdapter.getItem(position);
                    if (isUsableFileName(selectedItem)) {
                        itemSelected = true;
                    }
                }

                // return the current directory
                if(mode == SelectionMode.DIRECTORY) {
                    itemSelected = true;
                    selectedItem = DocumentFileItem.getInstance(FileChooserActivity.this, mCurrentDir);
                }

                if (itemSelected) {
                    returnSelectedFile(selectedItem);
                } else {
                    new AlertDialog.Builder(FileChooserActivity.this, R.style.AppTheme_Dialog)
                        .setTitle(R.string.title_activity_file_explorer)
                        .setMessage(R.string.no_item_selected)
                        .setPositiveButton(R.string.confirm, null)
                        .show();
                }
            }
        });

        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancel();
            }
        });

        mInternalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFileFolderFromInternalMemory();
            }
        });

        mSdCardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (SdUtils.doWeNeedToRequestSdCardAccess()) {
                    new AlertDialog.Builder(FileChooserActivity.this, R.style.AppTheme_Dialog)
                        .setTitle(R.string.enable_sd_card_access_title)
                        .setMessage(R.string.enable_sd_card_access)
                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SdUtils.triggerStorageAccessFramework(FileChooserActivity.this);
                            }
                        })
                        .setNegativeButton(R.string.label_skip, null)
                        .show();
                } else {
                    showFolderFromSdCard();
                }
            }
        });

        mFileList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DocumentFileItem selectedItem = mAdapter.getItem(position);
                if (selectedItem.file.isDirectory()) {
                    mAdapter.setSelectedPosition(-1);
                    mFileList.clearChoices();
                    loadDocFileList(selectedItem.file);
                    return;
                } else if(mode != SelectionMode.DIRECTORY) {   // file item selected
                    if (isUsableFileName(selectedItem)) {
                        mAdapter.setSelectedPosition(position);
                        mFileList.setItemChecked(position, true);
                        return;
                    }
                }

                //clear selections
                mAdapter.setSelectedPosition(-1);
                mFileList.clearChoices();
            }
        });

        showFolderFromSdCard();
    }

    /**
     * returns true if the filename extension for item matches list that we are accepting
     * @param item
     * @return
     */
    private boolean isUsableFileName(DocumentFileItem item) {

        for (String acceptedExtension : mAcceptedExtensions) {
            if (item.isFileMatchesExtension(acceptedExtension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * if sd card is available this will display buttons for "internal" and "sd card".
     * Otherwise neither will be displayed since we cannot switch.
     * @param haveSDCard
     */
    private void showSdCardOption(boolean haveSDCard) {
        mSdCardButton.setVisibility( haveSDCard ? View.VISIBLE : View.GONE);
        mInternalButton.setVisibility( haveSDCard ? View.VISIBLE : View.GONE);
    }

    /**
     * will display file list for SD card folder if accessible.
     * Otherwise it will display file list from internal memory.
     */
    private void showFolderFromSdCard() {
        DocumentFile path = null;
        boolean sdCardFound = false;
        boolean sdCardHaveAccess = false;
        boolean isSdCardPresentLollipop = SdUtils.isSdCardPresentLollipop();
        if (isSdCardPresentLollipop) {
            sdCardFound = true;
            DocumentFile baseFolder = SdUtils.sdCardMkdirs(null);
            String subFolder = SdUtils.searchFolderAndParentsForDocFile(baseFolder, Translator.ARCHIVE_EXTENSION);
            if (null != subFolder) {

                path = SdUtils.documentFileMkdirs(baseFolder, subFolder);
            } else {
                path = SdUtils.documentFileMkdirs(baseFolder, SdUtils.DOWNLOAD_FOLDER); // use downloads folder, or make if not present.
                if (null == path) {
                    path = baseFolder; // if folder creation fails, fall back to base folder
                }
            }

            if(null != path) {
                sdCardHaveAccess = true;
                loadDocFileList(path);
            }

        } else { // SD card not present or Android version is lower than Lollipop

            File sdCardFolder = SdUtils.getSdCardDirectory();
            if( (sdCardFolder != null) && SdUtils.isSdCardAccessableInMode(mWriteAccess) ) {
                if (sdCardFolder.isDirectory() && sdCardFolder.exists() && sdCardFolder.canRead()) {
                    File storagePath = Environment.getExternalStorageDirectory();
                    if(!sdCardFolder.equals(storagePath)) { // make sure it doesn't reflect back to internal memory
                        sdCardFound = true;
                        sdCardHaveAccess = true;
                        showFileFolder(sdCardFolder);
                    }
                }
            }
        }

        showSdCardOption(sdCardFound);
        if (!sdCardHaveAccess) {
            showFileFolderFromInternalMemory();
        }
    }

    /**
     * will display file list for external storage directory
     */
    private void showFileFolderFromInternalMemory() {
        File storagePath = Environment.getExternalStorageDirectory();
        showFileFolder(storagePath);
    }

    /**
     * will display file list for specified Folder
     * @param storagePath
     */
    private void showFileFolder(File storagePath) {
        DocumentFile path = null;
        DocumentFile baseFolder = DocumentFile.fromFile(storagePath);
        String subFolder = SdUtils.searchFolderAndParentsForDocFile(baseFolder, Translator.ARCHIVE_EXTENSION);
        if (null != subFolder) {
            path = SdUtils.documentFileMkdirs(baseFolder, subFolder);
        }else {
            path = SdUtils.documentFileMkdirs(baseFolder, SdUtils.DOWNLOAD_FOLDER); // use downloads folder, or make if not present.
        }

        if(null == path) {
            path = baseFolder; // if nothing is found, then default to base
        }

        loadDocFileList(path);
    }

    /**
     * returns the selected file to calling activity
     * @param selectedItem
     */
    private void returnSelectedFile(DocumentFileItem selectedItem) {
        // return selected file
        Intent intent = getIntent();
        intent.setData(selectedItem.file.getUri());
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * notify calling activity that user has cancelled
     */
    private void cancel() {
        Intent intent = getIntent();
        intent.setData(null);
        setResult(RESULT_CANCELED, intent);
        finish();
    }

    /**
     * Generates a list of files to display from a directory
     * @param dir
     * @return
     */
    private void loadDocFileList(DocumentFile dir) {

        mAdapter = new FileChooserAdapter(this.mode, this.filters);
        mFileList.setAdapter(mAdapter);

        Context context = App.context();
        List<DocumentFileItem> fileList = new ArrayList<>();

        mFileList.clearFocus();
        mFileList.clearChoices();
        mUpButton.setVisibility(View.GONE);

        if ((dir != null) && dir.exists() && dir.isDirectory()) {

             // remember directory
            mCurrentDir = dir;
            mCurrentFolder.setText(SdUtils.getPathString(dir));

            // list files
            DocumentFile[] files = dir.listFiles();
            if(files != null) {
                for (DocumentFile f : files) {
                    if( !f.canRead() ) {
                        continue; // skip if not readable
                    }

                    fileList.add(DocumentFileItem.getInstance(context, f));
                }

                // add up button
                if (dir.getParentFile() != null && dir.getParentFile().exists() && dir.getParentFile().canRead()) {
                    mUpButton.setVisibility(View.VISIBLE);
                }
            }
        }

        mAdapter.loadFiles(this, fileList);

        if(fileList.size() <= 0) {
            Toast toast = Toast.makeText(this, R.string.empty_directory, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP, 0, 0);
            toast.show();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SdUtils.REQUEST_CODE_STORAGE_ACCESS) {
            if(showSdCardAccessResults(this, resultCode, data)) {
                showFolderFromSdCard();
            }
        }
    }

    /**
     * show access results and return true if granted
     * @param resultCode
     * @param data
     * @return
     */
    public static boolean showSdCardAccessResults(Context context, int resultCode, Intent data) {
        Uri treeUri = null;
        int titleId = R.string.access_title;
        String msg = "";
        boolean granted = false;
        if (resultCode == Activity.RESULT_OK) {

            // Get Uri from Storage Access Framework.
            treeUri = data.getData();
            final int takeFlags = data.getFlags();
            SdUtils.WriteAccessMode status = SdUtils.validateSdCardWriteAccess(treeUri, takeFlags);
            if (status != SdUtils.WriteAccessMode.ENABLED_CARD_BASE) {
                accessErrorPrompt(context, treeUri, status);
                return false;
            } else {
                titleId = R.string.access_success_title;
                msg = context.getResources().getString(R.string.access_granted_sd_card);
                granted = true;
            }
        } else {
            msg = context.getResources().getString(R.string.access_skipped);
        }
        new AlertDialog.Builder(context, R.style.AppTheme_Dialog)
                .setTitle(titleId)
                .setMessage(msg)
                .setPositiveButton(R.string.label_ok, null)
                .show();
        return granted;
    }

    /**
     * show user how access attempt failed
     * @param context
     * @param treeUri
     * @param status
     */
    private static void accessErrorPrompt(Context context, Uri treeUri, SdUtils.WriteAccessMode status) {
        String msg;
        if (status == SdUtils.WriteAccessMode.NONE) {
            msg = context.getResources().getString(R.string.access_failed, treeUri.toString());
        } else {
            msg = context.getResources().getString(R.string.access_not_root);
            SdUtils.removeSdCardWriteAccess(); // invalidate keys since we dont want them
        }
        new AlertDialog.Builder(context, R.style.AppTheme_Dialog)
                .setTitle(R.string.access_title)
                .setMessage(msg)
                .setPositiveButton(R.string.label_ok, null)
                .show();
    }

    public enum SelectionMode {
        DIRECTORY,
        FILE,
        MULTI_SELECT_FILE
    }
}

