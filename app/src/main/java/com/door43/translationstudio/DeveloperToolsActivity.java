package com.door43.translationstudio;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.FileProvider;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.door43.translationstudio.dialogs.ErrorLogDialog;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Sharing;
import com.door43.translationstudio.tasks.DownloadAvailableProjectsTask;
import com.door43.translationstudio.tasks.DownloadProjectsTask;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.StringUtilities;
import com.door43.util.tasks.ManagedTask;
import com.door43.util.tasks.TaskManager;
import com.door43.util.tasks.ThreadableUI;
import com.door43.util.Logger;
import com.door43.translationstudio.util.ToolAdapter;
import com.door43.translationstudio.util.ToolItem;
import com.door43.translationstudio.util.TranslatorBaseActivity;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class DeveloperToolsActivity extends TranslatorBaseActivity implements ManagedTask.OnProgressListener, ManagedTask.OnFinishedListener, DialogInterface.OnCancelListener {

    private static final String TASK_PREP_FORCE_DOWNLOAD_ALL_PROJECTS = "prep_force_download_all_projects";
    private ArrayList<ToolItem> mDeveloperTools = new ArrayList<>();
    private ToolAdapter mAdapter;
    private String mVersionName;
    private String mVersionCode;
    private String TASK_FORCE_DOWNLOAD_ALL_PROJECTS = "force_download_all_projects";
    private ProgressDialog mDownloadProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_developer_tools);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView versionText = (TextView)findViewById(R.id.appVersionText);
        TextView buildText = (TextView)findViewById(R.id.appBuildNumberText);
        TextView udidText = (TextView)findViewById(R.id.deviceUDIDText);

        // display app version
        PackageInfo pInfo;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            mVersionName = pInfo.versionName;
            versionText.setText(String.format(getResources().getString(R.string.app_version_name), pInfo.versionName));
            mVersionCode = pInfo.versionCode+"";
            buildText.setText(String.format(getResources().getString(R.string.app_version_code) ,pInfo.versionCode));
        } catch (PackageManager.NameNotFoundException e) {
            Logger.e(this.getClass().getName(), "failed to get package name", e);
        }

        // display device id
        udidText.setText(String.format(getResources().getString(R.string.app_udid), AppContext.udid()));

        // set up copy handlers
        versionText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StringUtilities.copyToClipboard(DeveloperToolsActivity.this, mVersionName);

                AppContext.context().showToastMessage(R.string.copied_to_clipboard);
            }
        });
        buildText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StringUtilities.copyToClipboard(DeveloperToolsActivity.this, mVersionCode);

                AppContext.context().showToastMessage(R.string.copied_to_clipboard);
            }
        });
        udidText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StringUtilities.copyToClipboard(DeveloperToolsActivity.this, AppContext.udid());

                AppContext.context().showToastMessage(R.string.copied_to_clipboard);
            }
        });



        // set up developer tools
        ListView list = (ListView)findViewById(R.id.developerToolsListView);
        mAdapter = new ToolAdapter(mDeveloperTools, this);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ToolItem tool = mAdapter.getItem(i);
                if(tool.isEnabled()) {
                    tool.getAction().run();
                }
            }
        });

        // load tools
        mDeveloperTools.add(new ToolItem(getResources().getString(R.string.regenerate_keys), getResources().getString(R.string.regenerate_keys_description), 0, new ToolItem.ToolAction() {
            @Override
            public void run() {
                final ProgressDialog dialog = new ProgressDialog(DeveloperToolsActivity.this);
                dialog.setMessage(getResources().getString(R.string.loading));
                dialog.setCancelable(false);
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();
                final Handler handle = new Handler(Looper.getMainLooper());
                new ThreadableUI(DeveloperToolsActivity.this) {

                    @Override
                    public void onStop() {
                        handle.post(new Runnable() {
                            @Override
                            public void run() {
                                dialog.dismiss();
                            }
                        });
                    }

                    @Override
                    public void run() {
                        // disable screen rotation so we don't break things
//                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
                        AppContext.context().generateKeys();
                    }

                    @Override
                    public void onPostExecute() {
                        dialog.dismiss();
                        // re-enable screen rotation
//                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                        AppContext.context().showToastMessage(R.string.success);
                    }
                }.start();
            }
        }));
        mDeveloperTools.add(new ToolItem(getResources().getString(R.string.read_debugging_log), getResources().getString(R.string.read_debugging_log_description), 0, new ToolItem.ToolAction() {
            @Override
            public void run() {
                ErrorLogDialog dialog = new ErrorLogDialog();
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                Fragment prev = getFragmentManager().findFragmentByTag("dialog");
                if (prev != null) {
                    ft.remove(prev);
                }
                ft.addToBackStack(null);
                dialog.show(ft, "dialog");
            }
        }));
        mDeveloperTools.add(new ToolItem(getResources().getString(R.string.force_update_projects), getResources().getString(R.string.force_update_projects_description), 0, new ToolItem.ToolAction() {
            @Override
            public void run() {
                new AlertDialog.Builder(DeveloperToolsActivity.this)
                        .setTitle(R.string.action_download_all)
                        .setMessage(R.string.download_all_confirmation)
                        .setIcon(R.drawable.ic_download_small)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                DownloadAvailableProjectsTask prepTask = (DownloadAvailableProjectsTask) TaskManager.getTask(TASK_PREP_FORCE_DOWNLOAD_ALL_PROJECTS);
                                DownloadProjectsTask task = (DownloadProjectsTask) TaskManager.getTask(TASK_FORCE_DOWNLOAD_ALL_PROJECTS);
                                if(task == null && prepTask == null) {
                                    // create new prep task
                                    prepTask = new DownloadAvailableProjectsTask(true);
                                    prepTask.addOnProgressListener(DeveloperToolsActivity.this);
                                    prepTask.addOnFinishedListener(DeveloperToolsActivity.this);
                                    TaskManager.addTask(prepTask, TASK_PREP_FORCE_DOWNLOAD_ALL_PROJECTS);
                                } else {
                                    connectDownloadAllTask();
                                }
                            }
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
            }
        }));
        mDeveloperTools.add(new ToolItem(getResources().getString(R.string.export_source), getResources().getString(R.string.export_source_description), 0, new ToolItem.ToolAction() {
            @Override
            public void run() {
                final Handler handle = new Handler(Looper.getMainLooper());
                final Project[] projects = AppContext.projectManager().getProjects();
                final ProgressDialog dialog = new ProgressDialog(DeveloperToolsActivity.this);
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(false);
                dialog.setIndeterminate(true);
                dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                dialog.setMax(projects.length);
                dialog.setMessage(getResources().getString(R.string.exporting));

                // TODO: this should be placed inside of a task instead.
                final ThreadableUI thread = new ThreadableUI(DeveloperToolsActivity.this) {
                    private File output;
                    @Override
                    public void onStop() {

                    }

                    @Override
                    public void run() {
                        try {
                            File archiveFile = new File(Sharing.exportSource(projects, new Sharing.OnProgressCallback() {
                                @Override
                                public void onProgress(final double progress, final String message) {
                                    handle.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            dialog.setIndeterminate(false);
                                            dialog.setMessage(message);
                                            dialog.setProgress((int)Math.round(projects.length*progress));
                                        }
                                    });
                                }

                                @Override
                                public void onSuccess() {

                                }
                            }));
                            if(isInterrupted()) {
                                archiveFile.delete();
                                return;
                            }
                            if(archiveFile.exists()) {
                                File internalDestDir = new File(getCacheDir(), "sharing/");
                                output = new File(internalDestDir, archiveFile.getName());
                                FileUtils.copyFile(archiveFile, output);
                            }
                        } catch (IOException e) {
                            AppContext.context().showException(e);
                            stop();
                        }
                    }

                    @Override
                    public void onPostExecute() {
                        if(!isInterrupted()) {
                            new AlertDialog.Builder(DeveloperToolsActivity.this)
                                    .setTitle(R.string.success)
                                    .setIcon(R.drawable.ic_check_small)
                                    .setMessage(R.string.source_export_complete)
                                    .setPositiveButton(R.string.menu_share, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            if (output != null) {
                                                Uri u = FileProvider.getUriForFile(DeveloperToolsActivity.this, "com.door43.translationstudio.fileprovider", output);
                                                Intent intent = new Intent(Intent.ACTION_SEND);
                                                intent.setType("application/zip");
                                                intent.putExtra(Intent.EXTRA_STREAM, u);
                                                startActivity(Intent.createChooser(intent, "Email:"));
                                            }
                                        }
                                    })
                                    .show();
                        }
                        dialog.dismiss();
                    }
                };

                // allow the user to cancel the dialog
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        thread.stop();
                    }
                });
                dialog.show();

                thread.start();
            }
        }));
        mDeveloperTools.add(new ToolItem(getResources().getString(R.string.simulate_crash), getResources().getString(R.string.simulate_crash_description), 0, new ToolItem.ToolAction() {
            @Override
            public void run() {
                int killme = 1/0;
            }
        }));

        connectDownloadAllTask();
    }

    /**
     * Connects to an existing task
     */
    public void connectDownloadAllTask() {
        DownloadAvailableProjectsTask prepTask = (DownloadAvailableProjectsTask)TaskManager.getTask(TASK_PREP_FORCE_DOWNLOAD_ALL_PROJECTS);
        DownloadProjectsTask task = (DownloadProjectsTask)TaskManager.getTask(TASK_FORCE_DOWNLOAD_ALL_PROJECTS);
        if(prepTask != null) {
            // connect to existing task
            prepTask.addOnProgressListener(this);
            prepTask.addOnFinishedListener(this);
        } else if(task != null) {
            // connect to existing task
            task.addOnProgressListener(this);
            task.addOnFinishedListener(this);
        } else {
            onFinished(null);
        }
    }

    @Override
    public void onFinished(final ManagedTask task) {
        Handler hand = new Handler(Looper.getMainLooper());
        if(task != null) {
            TaskManager.clearTask(task.getTaskId());

            // display success
            hand.post(new Runnable() {
                @Override
                public void run() {
                    if (mDownloadProgressDialog != null && mDownloadProgressDialog.isShowing()) {
                        mDownloadProgressDialog.dismiss();
                        mDownloadProgressDialog = null;
                    }

                    if(!task.isCanceled()) {
                        if (task instanceof DownloadAvailableProjectsTask) {
                            // start task to download projects
                            DownloadProjectsTask downloadTask = new DownloadProjectsTask(((DownloadAvailableProjectsTask) task).getProjects(), true);
                            downloadTask.addOnProgressListener(DeveloperToolsActivity.this);
                            downloadTask.addOnFinishedListener(DeveloperToolsActivity.this);
                            TaskManager.addTask(downloadTask, TASK_FORCE_DOWNLOAD_ALL_PROJECTS);
                        } else {
                            // the download is complete
                            new AlertDialog.Builder(DeveloperToolsActivity.this)
                                    .setTitle(R.string.success)
                                    .setIcon(R.drawable.ic_check_small)
                                    .setMessage(R.string.download_complete)
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.label_ok, null)
                                    .show();
                        }
                    }
                }
            });
        } else {
            // dismiss everything
            TaskManager.clearTask(TASK_PREP_FORCE_DOWNLOAD_ALL_PROJECTS);
            TaskManager.clearTask(TASK_FORCE_DOWNLOAD_ALL_PROJECTS);
            hand.post(new Runnable() {
                @Override
                public void run() {
                    if (mDownloadProgressDialog != null && mDownloadProgressDialog.isShowing()) {
                        mDownloadProgressDialog.dismiss();
                        mDownloadProgressDialog = null;
                    }
                }
            });
        }
    }

    @Override
    public void onProgress(final ManagedTask task, final double progress, final String message) {
        if(!task.isFinished()) {
            Handler hand = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    if (task.isFinished()) {
                        // because we start on a new thread we need to make sure the task didn't finish while this thread was starting.
                        if (mDownloadProgressDialog != null) {
                            mDownloadProgressDialog.dismiss();
                        }
                        return;
                    }
                    if (mDownloadProgressDialog == null) {
                        mDownloadProgressDialog = new ProgressDialog(DeveloperToolsActivity.this);
                        mDownloadProgressDialog.setCancelable(true);
                        mDownloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        mDownloadProgressDialog.setCanceledOnTouchOutside(false);
                        mDownloadProgressDialog.setOnCancelListener(DeveloperToolsActivity.this);
                        mDownloadProgressDialog.setMax(100);
                        mDownloadProgressDialog.setIcon(R.drawable.ic_download_small);
                        if(task instanceof DownloadAvailableProjectsTask) {
                            mDownloadProgressDialog.setTitle(getResources().getString(R.string.loading));
                        } else {
                            mDownloadProgressDialog.setTitle(getResources().getString(R.string.downloading));
                        }
                        mDownloadProgressDialog.setMessage("");
                    }
                    if (!mDownloadProgressDialog.isShowing()) {
                        mDownloadProgressDialog.show();
                    }
                    if (progress == -1) {
                        mDownloadProgressDialog.setIndeterminate(true);
                        mDownloadProgressDialog.setProgress(mDownloadProgressDialog.getMax());
                    } else {
                        mDownloadProgressDialog.setIndeterminate(false);
                        mDownloadProgressDialog.setProgress((int) Math.ceil(progress * 100));
                    }
                    if (!message.isEmpty()) {
                        if(task instanceof DownloadAvailableProjectsTask) {
                            mDownloadProgressDialog.setMessage(message);
                        } else {
                            mDownloadProgressDialog.setMessage(String.format(getResources().getString(R.string.downloading_project), message));
                        }
                    } else {
                        mDownloadProgressDialog.setMessage("");
                    }
                }
            });
        }
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        // the download dialog was canceled
        mDownloadProgressDialog = null;
        DownloadProjectsTask task = (DownloadProjectsTask) TaskManager.getTask(TASK_FORCE_DOWNLOAD_ALL_PROJECTS);
        if(task != null) {
            TaskManager.cancelTask(task);
        }
    }
}
