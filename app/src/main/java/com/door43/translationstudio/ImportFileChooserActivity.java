package com.door43.translationstudio;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.provider.DocumentFile;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.filebrowser.DocumentFileBrowserAdapter;
import com.door43.translationstudio.filebrowser.DocumentFileItem;
import com.door43.translationstudio.newui.BaseActivity;
import com.door43.translationstudio.newui.home.ImportDialog;
import com.door43.translationstudio.util.SdUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by blm on 12/31/15.
 */
public class ImportFileChooserActivity extends BaseActivity {

    public static final String EXTRAS_RAW_FILE = "extras_raw_file";
    public static final String EXTRAS_NO_ZIPZ = "extras_no_zipz";

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
    private DocumentFileBrowserAdapter mAdapter;

    private DocumentFile mCurrentDir;
    private String mType;

    private boolean mRawFileSupport = false;
    private boolean mNoZipFileSupport = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_import_file);

        mUpButton = (ImageButton) findViewById(R.id.up_folder_button);
        mInternalButton = (Button) findViewById(R.id.internal_button);
        mSdCardButton = (Button) findViewById(R.id.sd_card_button);
        mCancelButton = (Button) findViewById(R.id.cancel_button);
        mConfirmButton = (Button) findViewById(R.id.confirm_button);
        mCurrentFolder = (TextView) findViewById(R.id.current_folder);
        mFileList = (ListView) findViewById(R.id.file_list);

        setTitle(R.string.title_activity_file_explorer);

        File sdCardFolder = SdUtils.getSdCardDirectory();
        boolean haveSDCard = sdCardFolder != null;
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
                if (position >= 0) {
                    selectedItem = mAdapter.getItem(position);
                    if (!selectedItem.file.isDirectory()) {
                        if (selectedItem.isTranslationArchive()) {
                            itemSelected = true;
                        } else if ( mRawFileSupport ) {
                            if(mNoZipFileSupport && selectedItem.isThisZipFile()) {
                                itemSelected = false;
                            } else {
                                itemSelected = true;
                            }
                        }
                    }
                }

                if (itemSelected) {
                    returnSelectedFile(selectedItem);
                } else {
                    final CustomAlertDialog dialog = CustomAlertDialog.Create(ImportFileChooserActivity.this);
                    dialog.setTitle(R.string.title_activity_file_explorer)
                            .setMessageHtml(R.string.no_item_selected)
                            .setPositiveButton(R.string.confirm, null)
                            .show("no_selection");
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
                    final CustomAlertDialog dialog = CustomAlertDialog.Create(ImportFileChooserActivity.this);
                    dialog.setTitle(R.string.enable_sd_card_access_title)
                            .setMessageHtml(R.string.enable_sd_card_access)
                            .setPositiveButton(R.string.confirm, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    SdUtils.triggerStorageAccessFramework(ImportFileChooserActivity.this);
                                }
                            })
                            .setNegativeButton(R.string.label_skip, null)
                            .show("approve-SD-access");
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
                } else {   // file item selected
                    if (selectedItem.isTranslationArchive()) {
                        mAdapter.setSelectedPosition(position);
                        mFileList.setItemChecked(position, true);
                        return;
                    } else if(mRawFileSupport) {
                        if(mNoZipFileSupport && selectedItem.isThisZipFile()) {
                            // ignore zips if support disabled
                        } else {
                            mAdapter.setSelectedPosition(position);
                            mFileList.setItemChecked(position, true);
                            return;
                        }
                    }
                }

                //clear selections
                mAdapter.setSelectedPosition(-1);
                mFileList.clearChoices();
             }
        });

        Intent intent = getIntent();
        mType = intent.getType();
        Bundle args = intent.getExtras();
        mRawFileSupport = args.getBoolean(EXTRAS_RAW_FILE, false);
        mNoZipFileSupport = args.getBoolean(EXTRAS_NO_ZIPZ, false);

        showFolderFromSdCard();
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

        } else { // SD card not present or not lollipop

            File sdCardFolder = SdUtils.getSdCardDirectory();
            if (sdCardFolder != null) {
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

        mAdapter = new DocumentFileBrowserAdapter();
        mFileList.setAdapter(mAdapter);

        Context context = AppContext.context();
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

                    if (f.isDirectory()) {
                        fileList.add(DocumentFileItem.getInstance(context, f));
                    } else {
                        fileList.add(DocumentFileItem.getInstance(context, f));
                    }
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
            Uri treeUri = null;
            String msg = "";
            if (resultCode == Activity.RESULT_OK) {

                // Get Uri from Storage Access Framework.
                treeUri = data.getData();
                final int takeFlags = data.getFlags();
                boolean success = SdUtils.validateSdCardWriteAccess(treeUri, takeFlags);
                if (!success) {
                    String template = getResources().getString(R.string.access_failed);
                    msg = String.format(template, treeUri.toString());
                } else {
                    msg = getResources().getString(R.string.access_granted_import);
                    showFolderFromSdCard();
                }
            } else {
                msg = getResources().getString(R.string.access_skipped);
            }
            CustomAlertDialog.Create(this)
                    .setTitle(R.string.access_title)
                    .setMessage(msg)
                    .setPositiveButton(R.string.label_ok, null)
                    .show("AccessResults");
        }
    }
}

