package com.door43.translationstudio;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.provider.DocumentFile;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.filebrowser.DocumentFileBrowserAdapter;
import com.door43.translationstudio.filebrowser.DocumentFileItem;
import com.door43.translationstudio.newui.BaseActivity;
import com.door43.translationstudio.util.SdUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by blm on 12/31/15.
 */
public class ImportFileChooserActivity extends BaseActivity {
    public static final String FOLDER_KEY = "Folder";
    public static final String SD_CARD_TYPE = "sd_card";
    public static final String INTERNAL_TYPE = "internal";

    private Button mUpButton;
    private Button mInternalButton;
    private Button mSdCardButton;
    private TextView mCurrentFolder;
    private ListView mFileList;
    private DocumentFileBrowserAdapter mAdapter;

    private DocumentFile mCurrentDocFileDir;
    private String mType;
    private String mSubFolder = "";
    private boolean mIsSdCard = false;
    private static final int DIALOG_LOAD_FILE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_import_file);

        mUpButton = (Button) findViewById(R.id.up_folder_button);
        mInternalButton = (Button) findViewById(R.id.internal_button);
        mSdCardButton = (Button) findViewById(R.id.sd_card_button);
        mCurrentFolder = (TextView) findViewById(R.id.current_folder);
        mFileList = (ListView) findViewById(R.id.file_list);

        mCurrentFolder.setText(".");

        mUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        mInternalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        mSdCardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        Intent intent = getIntent();
        mType = intent.getType();
        mIsSdCard = SD_CARD_TYPE.equals(mType);
        Uri uri = intent.getData();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            mSubFolder = (String) bundle.get(FOLDER_KEY);
        }

        if (uri != null) {

            DocumentFile path = null;
            try {
                path = DocumentFile.fromTreeUri(AppContext.context(),uri);
                if(mSubFolder != null) {
                    DocumentFile subDoc = SdUtils.documentFileMkdirs(path, mSubFolder);
                    if(subDoc != null) {
                        path = subDoc;
                    }
                }

                loadDocFileList(path);

            } catch (Exception e) {
                Logger.w(ImportFileChooserActivity.class.toString(), "onCreate: Exception occurred opening file", e);
            }
        }

        mAdapter = new DocumentFileBrowserAdapter();
    }

    /**
     * Generates a list of files to display from a directory
     * @param dir
     * @return
     */
    private void loadDocFileList(DocumentFile dir) {
        Context context = AppContext.context();
        List<DocumentFileItem> fileList = new ArrayList<>();

        if (dir.exists() && dir.isDirectory()) {
            // remember directory
            mCurrentDocFileDir = dir;

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
                if (dir.getParentFile() != null && dir.getParentFile().exists() && dir.getParentFile().canRead()) {
                    fileList.add(0, DocumentFileItem.getUpInstance());
                }
            }
        }

        if(fileList.size() > 0) {
            mAdapter.loadFiles(mFileList, fileList);
        } else {
            Toast toast = Toast.makeText(this, R.string.empty_directory, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP, 0, 0);
            toast.show();
        }
    }


    @Override
    public void onResume() {
        super.onResume();

        mCurrentFolder.setText("...");

    }
}

