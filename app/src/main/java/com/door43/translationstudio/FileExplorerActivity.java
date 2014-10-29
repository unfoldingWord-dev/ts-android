package com.door43.translationstudio;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.door43.translationstudio.util.StorageUtils;
import com.door43.translationstudio.util.TranslatorBaseActivity;

/**
 * See https://github.com/mburman/Android-File-Explore
 */
public class FileExplorerActivity extends TranslatorBaseActivity {
    private FileExplorerActivity me = this;
	// Stores names of traversed directories
	ArrayList<String> mTraversedDirectories = new ArrayList<String>();

	// Check if the first level of the directory structure is the one showing
	private Boolean mFirstLevel = true;

	private static final String TAG = "F_PATH";

	private Item[] mFileList;
    private File mPath;
	private String mChosenFile;
	private static final int DIALOG_LOAD_FILE = 1000;

	ListAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // TODO: support fetching files from the internal downloads directory

        // external sd
        StorageUtils.StorageInfo removeableMediaInfo = StorageUtils.getRemoveableMediaDevice();
        if(removeableMediaInfo != null) {
            // write files to the removeable sd card
            mPath = new File("/storage/" + removeableMediaInfo.getMountName());
        } else {
            // the external storage could not be found
            finish();
        }

		loadFileList();

		showDialog(DIALOG_LOAD_FILE);
		Log.d(TAG, mPath.getAbsolutePath());

	}

	private void loadFileList() {
		try {
			mPath.mkdirs();
		} catch (SecurityException e) {
			Log.e(TAG, "unable to write on the sd card ");
		}

		// Checks whether path exists
		if (mPath.exists()) {
			FilenameFilter filter = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {
					File sel = new File(dir, filename);
					// Filters based on whether the file is hidden or not
					return (sel.isFile() || sel.isDirectory())
							&& !sel.isHidden();

				}
			};

			String[] fList = mPath.list(filter);
			mFileList = new Item[fList.length];
			for (int i = 0; i < fList.length; i++) {
				mFileList[i] = new Item(fList[i], R.drawable.file_icon);

				// Convert into file path
				File sel = new File(mPath, fList[i]);

				// Set drawables
				if (sel.isDirectory()) {
					mFileList[i].icon = R.drawable.ic_folder_open;
					Log.d("DIRECTORY", mFileList[i].file);
				} else {
					Log.d("FILE", mFileList[i].file);
				}
			}

			if (!mFirstLevel) {
				Item temp[] = new Item[mFileList.length + 1];
				for (int i = 0; i < mFileList.length; i++) {
					temp[i + 1] = mFileList[i];
				}
				temp[0] = new Item("Up", R.drawable.directory_up);
				mFileList = temp;
			}
		} else {
			Log.e(TAG, "path does not exist");
		}

		adapter = new ArrayAdapter<Item>(this,
				android.R.layout.select_dialog_item, android.R.id.text1,
                mFileList) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				// creates view
				View view = super.getView(position, convertView, parent);
				TextView textView = (TextView) view
						.findViewById(android.R.id.text1);

				// put the image on the text view
				textView.setCompoundDrawablesWithIntrinsicBounds(
						mFileList[position].icon, 0, 0, 0);

				// add margin between image and text (support various screen
				// densities)
				int dp5 = (int) (5 * getResources().getDisplayMetrics().density + 0.5f);
				textView.setCompoundDrawablePadding(dp5);

				return view;
			}
		};

	}

	private class Item {
		public String file;
		public int icon;

		public Item(String file, Integer icon) {
			this.file = file;
			this.icon = icon;
		}

		@Override
		public String toString() {
			return file;
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		AlertDialog.Builder builder = new Builder(this);

		if (mFileList == null) {
			Log.e(TAG, "No files loaded");
			dialog = builder.create();
			return dialog;
		}

		switch (id) {
		case DIALOG_LOAD_FILE:
			builder.setTitle("Choose your file");
			builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mChosenFile = mFileList[which].file;
					File sel = new File(mPath + "/" + mChosenFile);
					if (sel.isDirectory()) {
						mFirstLevel = false;

						// Adds chosen directory to list
						mTraversedDirectories.add(mChosenFile);
						mFileList = null;
						mPath = new File(sel + "");

						loadFileList();

						removeDialog(DIALOG_LOAD_FILE);
						showDialog(DIALOG_LOAD_FILE);
						Log.d(TAG, mPath.getAbsolutePath());

					} else if (mChosenFile.equalsIgnoreCase("up") && !sel.exists()) {

						// present directory removed from list
						String s = mTraversedDirectories.remove(mTraversedDirectories.size() - 1);

						// path modified to exclude present directory
						mPath = new File(mPath.toString().substring(0,
								mPath.toString().lastIndexOf(s)));
						mFileList = null;

						// if there are no more directories in the list, then
						// its the first level
						if (mTraversedDirectories.isEmpty()) {
							mFirstLevel = true;
						}
						loadFileList();

						removeDialog(DIALOG_LOAD_FILE);
						showDialog(DIALOG_LOAD_FILE);
						Log.d(TAG, mPath.getAbsolutePath());

					} else {
						// pass path to the calling activity
                        Intent i = getIntent();

                        i.putExtra("path", new File(mPath, mChosenFile).getAbsolutePath());
                        setResult(RESULT_OK, i);
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
                me.finish();
            }
        });
		return dialog;
	}

}