package com.door43.translationstudio.filebrowser;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.provider.DocumentFile;
import android.view.Gravity;
import android.widget.BaseAdapter;
import android.widget.Toast;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.newui.BaseActivity;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This activity displays a simple file browsing ui.
 * In your intent pass the directory to open.
 * The file chosen by the user will be returned in the activity result.
 */
public class FileBrowserActivity extends BaseActivity {
    public static final String DOC_FILE_TYPE = "docfile/*";
    public static final String FILE_TYPE = "file/*";
    private File mCurrentDir;
    private DocumentFile mCurrentDocFileDir;
    private String mType;
    private boolean mDocFileType = false;
	private static final int DIALOG_LOAD_FILE = 1000;
	BaseAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mType = intent.getType();
        mDocFileType = DOC_FILE_TYPE.equals(mType);

        if(intent.getData() != null) {

            if(!mDocFileType) {
                File path = new File(intent.getData().getPath());
                if (!path.exists()) {
                    finish();
                    setResult(RESULT_CANCELED, null);
                } else {
                    mAdapter = new FileBrowserAdapter();
                    loadFileList(path);
                }
            } else {
                DocumentFile path = null;
                try {
                    Uri uri = intent.getData();
                    path = DocumentFile.fromTreeUri(AppContext.context(),uri);
                    Bundle bundle = intent.getExtras();
                    String folder = (String) bundle.get("Folder");
                    if(folder != null) {
                        DocumentFile subDoc = AppContext.documentFileMkdirs(path, folder);
                        if(subDoc != null) {
                            path = subDoc;
                        }
                    }

                } catch (Exception e) {
                    Logger.w(FileBrowserActivity.class.toString(), "onCreate: Exception occurred opening file", e);
                    path = null;
                }

                if ((null == path) || (!path.exists())) {
                    finish();
                    setResult(RESULT_CANCELED, null);
                } else {
                    mAdapter = new DocumentFileBrowserAdapter();
                    loadDocFileList(path);
                }
            }
        } else {
            finish();
            setResult(RESULT_CANCELED, null);
        }

		showDialog(DIALOG_LOAD_FILE);
	}

    /**
     * Generates a list of files to display from a directory
     * @param dir
     * @return
     */
    private void loadDocFileList(DocumentFile dir) {
        DocumentFileBrowserAdapter adapter = (DocumentFileBrowserAdapter) mAdapter;
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
            adapter.loadFiles(this, fileList);
        } else {
            Toast toast = Toast.makeText(this, R.string.empty_directory, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP, 0, 0);
            toast.show();
        }
    }

    /**
     * Generates a list of files to display from a directory
     * @param dir
     * @return
     */
    private void loadFileList(File dir) {
        FileBrowserAdapter adapter = (FileBrowserAdapter) mAdapter;
        List<FileItem> fileList = new ArrayList<>();

        if (dir.exists() && dir.isDirectory()) {
            // remember directory
            mCurrentDir = dir;

            // list files
            File[] files = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    File f = new File(dir, filename);
                    return (f.isFile() || f.isDirectory()) && !f.isHidden() && f.canRead();
                }
            });
            if(files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        fileList.add(FileItem.getInstance(f));
                    } else {
                        fileList.add(FileItem.getInstance(f));
                    }
                }

                // add up button
                if (dir.getParentFile() != null && dir.getParentFile().exists() && dir.getParentFile().canRead()) {
                    fileList.add(0, FileItem.getUpInstance());
                }
            }
        }

        if(fileList.size() > 0) {
            adapter.loadFiles(this, fileList);
        } else {
            Toast toast = Toast.makeText(this, R.string.empty_directory, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP, 0, 0);
            toast.show();
        }
    }

    @Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		AlertDialog.Builder builder = new Builder(this);

		switch (id) {
		    case DIALOG_LOAD_FILE:
                dialogLoadFile(builder);
                break;
		}
		dialog = builder.show();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                finish();
            }
        });
		return dialog;
	}

    private void dialogLoadFile(final Builder builder) {
        if (!mDocFileType) {
            dialogLoadFile(builder, (FileBrowserAdapter) mAdapter);
        } else {
            dialogLoadFile(builder, (DocumentFileBrowserAdapter) mAdapter);
        }
    }

    private void dialogLoadFile(final Builder builder, final DocumentFileBrowserAdapter adapter) {
        builder.setTitle(getResources().getString(R.string.choose_file));
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                DocumentFile file = adapter.getItem(i).file;
                if (adapter.getItem(i).isUpButton) {
                    // open parent directory
                    if (!mDocFileType) {
                        loadFileList(mCurrentDir.getParentFile());
                    } else {
                        loadDocFileList(mCurrentDocFileDir.getParentFile());
                    }
                    removeDialog(DIALOG_LOAD_FILE);
                    showDialog(DIALOG_LOAD_FILE);
                } else if (file.isDirectory()) {
                    // open directory
                    loadDocFileList(file);
                    removeDialog(DIALOG_LOAD_FILE);
                    showDialog(DIALOG_LOAD_FILE);
                } else {
                    // return selected file
                    Intent intent = getIntent();
                    intent.setData( file.getUri() );
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        });
    }

    private void dialogLoadFile(final Builder builder, final FileBrowserAdapter adapter) {
        builder.setTitle(getResources().getString(R.string.choose_file));
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                File file = adapter.getItem(i).file;
                if (adapter.getItem(i).isUpButton) {
                    // open parent directory
                    loadFileList(mCurrentDir.getParentFile());
                    removeDialog(DIALOG_LOAD_FILE);
                    showDialog(DIALOG_LOAD_FILE);
                } else if (file.isDirectory()) {
                    // open directory
                    loadFileList(file);
                    removeDialog(DIALOG_LOAD_FILE);
                    showDialog(DIALOG_LOAD_FILE);
                } else {
                    // return selected file
                    Intent intent = getIntent();
                    intent.setData(Uri.fromFile(file));
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        });
    }
}