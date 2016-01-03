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
import com.door43.translationstudio.util.SdUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by blm on 12/31/15.
 */
public class ImportFileChooserActivity extends BaseActivity {
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
        mSdCardButton.setVisibility((sdCardFolder != null) ? View.VISIBLE : View.GONE);

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
                doImportFromInternal(null);
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
                            .setNegativeButton(R.string.label_skip, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    doImportFromSdCard();
                                }
                            })
                            .show("approve-SD-access");
                } else {
                    doImportFromSdCard();
                }
            }
        });

        mFileList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DocumentFileItem selectedItem = mAdapter.getItem(position);
                if (selectedItem.file.isDirectory()) {
                    mFileList.clearChoices();
                    loadDocFileList(selectedItem.file);
                    return;
                } else {   // file item selected
                    if (selectedItem.isTranslationArchive()) {
                        mFileList.setItemChecked(position, true);
                        return;
                    }
                }

                //clear selections
                mFileList.clearChoices();
            }
        });

        Intent intent = getIntent();
        mType = intent.getType();

        doImportFromSdCard();
    }

    private void doImportFromSdCard() {

        boolean isSdCardPresentLollipop = SdUtils.isSdCardPresentLollipop();
        if (isSdCardPresentLollipop) {
            DocumentFile baseFolder = SdUtils.sdCardMkdirs(null);
            String subFolder = SdUtils.searchFolderAndParentsForDocFile(baseFolder, Translator.ARCHIVE_EXTENSION);
            if (null != subFolder) {
                DocumentFile path = SdUtils.documentFileMkdirs(baseFolder, subFolder);
                loadDocFileList(path);
            } else {
                doImportFromInternal(null);
            }
        } else { // SD card not present or not lollipop
            boolean sdCardFound = false;
            File sdCardFolder = SdUtils.getSdCardDirectory();
            if (sdCardFolder != null) {
                if (sdCardFolder.isDirectory() && sdCardFolder.exists() && sdCardFolder.canRead()) {
                    doImportFromInternal(sdCardFolder);
                }
            }

            if (!sdCardFound) {
                doImportFromInternal(null);
            }
        }
    }

    private void doImportFromInternal(File dir) {

        File storagePath = dir;
        if (null == storagePath) {
            storagePath = Environment.getExternalStorageDirectory(); // AppContext.getPublicDownloadsDirectory();
        }
        DocumentFile baseFolder = DocumentFile.fromFile(storagePath);
        String subFolder = SdUtils.searchFolderAndParentsForDocFile(baseFolder, Translator.ARCHIVE_EXTENSION);
        if (null != subFolder) {
            DocumentFile path = SdUtils.documentFileMkdirs(baseFolder, subFolder);
            loadDocFileList(path);
        }
    }

    private void returnSelectedFile(DocumentFileItem selectedItem) {
        // return selected file
        Intent intent = getIntent();
        intent.setData(selectedItem.file.getUri());
        setResult(RESULT_OK, intent);
        finish();
    }

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

        if (dir.exists() && dir.isDirectory()) {

             // remember directory
            mCurrentDir = dir;
            mCurrentFolder.setText(SdUtils.getPathString(dir));

            // list files
            DocumentFile[] files = dir.listFiles();
            if(files != null) {
                for (DocumentFile f : files) {

                    if( !f.canRead() ) {
                        continue;
                    }

                    if (f.isDirectory()) {
                        fileList.add(DocumentFileItem.getInstance(context, f));
                    } else {
                        fileList.add(DocumentFileItem.getInstance(context, f));
                    }
                }

                // add up button
                boolean upButton = false;
                if (dir.getParentFile() != null && dir.getParentFile().exists() && dir.getParentFile().canRead()) {
                    upButton = true;
                }

                if(upButton) {
                    mUpButton.setVisibility(View.VISIBLE);
                } else {
                    mUpButton.setVisibility(View.GONE);
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

