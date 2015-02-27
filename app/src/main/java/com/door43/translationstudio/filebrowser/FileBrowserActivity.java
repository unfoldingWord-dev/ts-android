package com.door43.translationstudio.filebrowser;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Gravity;
import android.widget.Toast;

import com.door43.translationstudio.R;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * This activity displays a simple file browsing ui.
 * In your intent pass the directory to open.
 * The file chosen by the user will be returned in the activity result.
 */
public class FileBrowserActivity extends ActionBarActivity {
    private File mCurrentDir;
	private static final int DIALOG_LOAD_FILE = 1000;
	FileBrowserAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        File path = null;
        if(intent.getData() != null) {
            path = new File(intent.getData().getPath());
            if(!path.exists()) {
                finish();
                setResult(RESULT_CANCELED, null);
            }
        } else {
            finish();
            setResult(RESULT_CANCELED, null);
        }

        mAdapter = new FileBrowserAdapter(this);
		loadFileList(path);
		showDialog(DIALOG_LOAD_FILE);
	}

    /**
     * Generates a list of files to display from a directory
     * @param dir
     * @return
     */
	private void loadFileList(File dir) {
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
                        fileList.add(new FileItem(f.getName(), R.drawable.ic_folder_open));
                    } else {
                        fileList.add(new FileItem(f.getName(), R.drawable.file_icon));
                    }
                }

                // add up button
                if (dir.getParentFile() != null && dir.getParentFile().exists() && dir.getParentFile().canRead()) {
                    fileList.add(0, new FileItem(getResources().getString(R.string.up)));
                }
            }
		}

        if(fileList.size() > 0) {
            mAdapter.loadFiles(fileList);
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
                builder.setTitle(getResources().getString(R.string.choose_file));
                builder.setAdapter(mAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        File file = new File(mCurrentDir + "/" + mAdapter.getItem(i).getFile());
                        if (file.isDirectory()) {
                            // open directory
                            loadFileList(file);
                            removeDialog(DIALOG_LOAD_FILE);
                            showDialog(DIALOG_LOAD_FILE);
                        } else if (mAdapter.getItem(i).isUp()) {
                            // open parent directory
                            loadFileList(mCurrentDir.getParentFile());
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
}