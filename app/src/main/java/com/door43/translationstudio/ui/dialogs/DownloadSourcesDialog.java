package com.door43.translationstudio.ui.dialogs;

import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.tasks.GetAvailableSourcesTask;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.tools.logger.Logger;
import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.TaskManager;

import java.text.NumberFormat;

/**
 * Created by blm on 12/1/16.
 */

public class DownloadSourcesDialog extends DialogFragment implements ManagedTask.OnFinishedListener, ManagedTask.OnProgressListener {
    public static final String TAG = DownloadSourcesDialog.class.getSimpleName();
    private Door43Client mLibrary;
    private ProgressDialog progressDialog = null;
    private DownloadSourcesAdapter mAdapter;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_choose_source_translation, container, false);

        mLibrary = App.getLibrary();

        ManagedTask task = new GetAvailableSourcesTask();
        ((GetAvailableSourcesTask)task).setPrefix(this.getResources().getString(R.string.loading_sources));
        task.addOnProgressListener(this);
        task.addOnFinishedListener(this);
        TaskManager.addTask(task, GetAvailableSourcesTask.TASK_ID);


        EditText searchView = (EditText) v.findViewById(R.id.search_text);
        searchView.setHint(R.string.choose_source_translations);
        searchView.setEnabled(false);
        ImageButton searchBackButton = (ImageButton) v.findViewById(R.id.search_back_button);
//        searchBackButton.setVisibility(View.GONE);
        ImageView searchIcon = (ImageView) v.findViewById(R.id.search_mag_icon);
        searchIcon.setVisibility(View.GONE);
        // TODO: set up search

        mAdapter = new DownloadSourcesAdapter(getActivity());

        ListView listView = (ListView) v.findViewById(R.id.list);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                mAdapter.toggleSelection(position);
            }
        });
        return v;
    }

    @Override
    public void onTaskFinished(final ManagedTask task) {
        TaskManager.clearTask(task);

        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                if(task instanceof GetAvailableSourcesTask) {
                    GetAvailableSourcesTask availableSourcesTask = (GetAvailableSourcesTask) task;
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                        progressDialog = null;
                    }

                    mAdapter.setData(availableSourcesTask);
                }
            }
        });
    }

    @Override
    public void onTaskProgress(final ManagedTask task, final double progress, final String message, boolean secondary) {
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                // init dialog
                if(progressDialog == null) {
                    progressDialog = new ProgressDialog(getActivity());
                    progressDialog.setCancelable(true);
                    progressDialog.setCanceledOnTouchOutside(false);
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.setOnCancelListener(DownloadSourcesDialog.this);
                    progressDialog.setIcon(R.drawable.ic_cloud_download_black_24dp);
                    progressDialog.setTitle(R.string.updating);
                    progressDialog.setMessage("");

                    progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            TaskManager.cancelTask(task);
                        }
                    });
                }

                // dismiss if finished or cancelled
                if(task.isFinished() || task.isCanceled()) {
                    progressDialog.dismiss();
                    return;
                }

                // progress
                progressDialog.setMax(task.maxProgress());
                progressDialog.setMessage(message);
                if(progress > 0) {
                    progressDialog.setIndeterminate(false);
                    progressDialog.setProgress((int)(progress * progressDialog.getMax()));
                    progressDialog.setProgressNumberFormat("%1d/%2d");
                    progressDialog.setProgressPercentFormat(NumberFormat.getPercentInstance());
                } else {
                    progressDialog.setIndeterminate(true);
                    progressDialog.setProgress(progressDialog.getMax());
                    progressDialog.setProgressNumberFormat(null);
                    progressDialog.setProgressPercentFormat(null);
                }

                // show
                if(task.isFinished()) {
                    progressDialog.dismiss();
                } else if(!progressDialog.isShowing()) {
                    progressDialog.show();
                }
            }
        });
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        ManagedTask task = TaskManager.getTask(GetAvailableSourcesTask.TASK_ID);
        if(task != null) TaskManager.cancelTask(task);
    }

    @Override
    public void onDestroy() {
        ManagedTask task = TaskManager.getTask(GetAvailableSourcesTask.TASK_ID);
        if(task != null) {
            task.removeOnProgressListener(this);
            task.removeOnFinishedListener(this);
        }

        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        super.onDestroy();
    }

}
