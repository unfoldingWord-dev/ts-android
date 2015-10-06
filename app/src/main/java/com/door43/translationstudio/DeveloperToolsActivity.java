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
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.dialogs.ErrorLogDialog;
import com.door43.translationstudio.newui.BaseActivity;
import com.door43.translationstudio.newui.library.ServerLibraryCache;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.tasks.GetLibraryUpdatesTask;
import com.door43.translationstudio.tasks.DownloadAllProjectsTask;
import com.door43.translationstudio.util.ToolAdapter;
import com.door43.translationstudio.util.ToolItem;
import com.door43.util.StringUtilities;
import com.door43.util.tasks.ManagedTask;
import com.door43.util.tasks.TaskManager;
import com.door43.util.tasks.ThreadableUI;
import com.door43.widget.ViewUtil;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;

public class DeveloperToolsActivity extends BaseActivity implements ManagedTask.OnProgressListener, ManagedTask.OnFinishedListener, DialogInterface.OnCancelListener {

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
                if (tool.isEnabled()) {
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
                        .setIcon(R.drawable.icon_update_cloud_dark)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // create new prep task
                                GetLibraryUpdatesTask task = new GetLibraryUpdatesTask();
                                task.addOnProgressListener(DeveloperToolsActivity.this);
                                task.addOnFinishedListener(DeveloperToolsActivity.this);
                                TaskManager.addTask(task, GetLibraryUpdatesTask.TASK_ID);
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
//                final Project[] projects = AppContext.projectManager().getProjects();
                final ProgressDialog dialog = new ProgressDialog(DeveloperToolsActivity.this);
                dialog.setCancelable(false);
                dialog.setCanceledOnTouchOutside(false);
                dialog.setIndeterminate(true);
                dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//                dialog.setMax(projects.length);
                dialog.setMessage(getResources().getString(R.string.exporting));

                // TODO: this should be placed inside of a task instead.
                final ThreadableUI thread = new ThreadableUI(DeveloperToolsActivity.this) {
                    private File archive;
                    @Override
                    public void onStop() {

                    }

                    @Override
                    public void run() {
                        // TODO: add progress listener
                        archive = AppContext.getLibrary().export(new File(getCacheDir(), "sharing/"));
                    }

                    @Override
                    public void onPostExecute() {
                        if(archive != null && archive.exists()) {
                            new AlertDialog.Builder(DeveloperToolsActivity.this)
                                    .setTitle(R.string.success)
                                    .setIcon(R.drawable.ic_done_black_24dp)
                                    .setMessage(R.string.source_export_complete)
                                    .setPositiveButton(R.string.menu_share, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            Uri u = FileProvider.getUriForFile(DeveloperToolsActivity.this, "com.door43.translationstudio.fileprovider", archive);
                                            Intent intent = new Intent(Intent.ACTION_SEND);
                                            intent.setType("application/zip");
                                            intent.putExtra(Intent.EXTRA_STREAM, u);
                                            startActivity(Intent.createChooser(intent, "Email:"));
                                        }
                                    })
                                    .show();
                        }
                        dialog.dismiss();
                    }
                };
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
        mDeveloperTools.add(new ToolItem("Delete Library", "Completely deletes the library and all of it's indexes", R.drawable.ic_delete_black_24dp, new ToolItem.ToolAction() {
            @Override
            public void run() {
                AppContext.getLibrary().destroyIndexes();
                ServerLibraryCache.clear();
                Snackbar snack = Snackbar.make(findViewById(android.R.id.content), "The library content was deleted", Snackbar.LENGTH_LONG);
                ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                snack.show();
            }
        }));

        if(savedInstanceState != null) {
            connectDownloadAllTask();
        }
    }

    /**
     * Connects to an existing task
     */
    public void connectDownloadAllTask() {
        DownloadAllProjectsTask downloadAllTask = (DownloadAllProjectsTask)TaskManager.getTask(DownloadAllProjectsTask.TASK_ID);
        GetLibraryUpdatesTask getUpdatesTask = (GetLibraryUpdatesTask)TaskManager.getTask(GetLibraryUpdatesTask.TASK_ID);
        if(downloadAllTask != null) {
            downloadAllTask.addOnProgressListener(this);
            downloadAllTask.addOnFinishedListener(this);
        }
        if(getUpdatesTask != null) {
            getUpdatesTask.addOnProgressListener(this);
            getUpdatesTask.addOnFinishedListener(this);
        }
    }

    @Override
    public void onFinished(final ManagedTask task) {
        TaskManager.clearTask(task);

        if(task instanceof GetLibraryUpdatesTask) {
            DownloadAllProjectsTask newTask = new DownloadAllProjectsTask();
            newTask.addOnProgressListener(this);
            newTask.addOnFinishedListener(this);
            TaskManager.addTask(newTask, DownloadAllProjectsTask.TASK_ID);
        }

        if(task instanceof DownloadAllProjectsTask) {
            Handler hand = new Handler(Looper.getMainLooper());
            // display success
            hand.post(new Runnable() {
                @Override
                public void run() {
                    if (mDownloadProgressDialog != null && mDownloadProgressDialog.isShowing()) {
                        mDownloadProgressDialog.dismiss();

                    }
                    mDownloadProgressDialog = null;

                    if (!task.isCanceled()) {
                        // the download is complete
                        new AlertDialog.Builder(DeveloperToolsActivity.this)
                                .setTitle(R.string.success)
                                .setIcon(R.drawable.ic_done_black_24dp)
                                .setMessage(R.string.download_complete)
                                .setCancelable(false)
                                .setPositiveButton(R.string.label_ok, null)
                                .show();
                    }
                }
            });
        }
    }

    @Override
    public void onProgress(final ManagedTask task, final double progress, final String message, final boolean secondary) {
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
                        mDownloadProgressDialog.setCancelable(false); // TODO: need to update the download method to support cancelling
                        mDownloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        mDownloadProgressDialog.setCanceledOnTouchOutside(false);
//                        mProgressDialog.setOnCancelListener(ServerLibraryActivity.this);
                        mDownloadProgressDialog.setIcon(R.drawable.ic_cloud_download_black_24dp);
                        if(task instanceof GetLibraryUpdatesTask) {
                            mDownloadProgressDialog.setTitle(getResources().getString(R.string.checking_for_updates));
                        } else if(task instanceof DownloadAllProjectsTask) {
                            mDownloadProgressDialog.setTitle(getResources().getString(R.string.downloading));
                        }
                        mDownloadProgressDialog.setMessage("");
                    }
                    mDownloadProgressDialog.setMax(task.maxProgress());
                    if (!mDownloadProgressDialog.isShowing()) {
                        mDownloadProgressDialog.show();
                    }
                    if (progress == -1) {
                        mDownloadProgressDialog.setIndeterminate(true);
                        mDownloadProgressDialog.setProgress(mDownloadProgressDialog.getMax());
                        mDownloadProgressDialog.setProgressNumberFormat(null);
                        mDownloadProgressDialog.setProgressPercentFormat(null);
                    } else {
                        mDownloadProgressDialog.setIndeterminate(false);
                        if(secondary) {
                            mDownloadProgressDialog.setSecondaryProgress((int) progress);
                        } else {
                            mDownloadProgressDialog.setProgress((int) progress);
                        }
                        mDownloadProgressDialog.setProgressNumberFormat("%1d/%2d");
                        mDownloadProgressDialog.setProgressPercentFormat(NumberFormat.getPercentInstance());
                    }
                    if (task instanceof DownloadAllProjectsTask && !message.isEmpty()) {
                        mDownloadProgressDialog.setMessage(String.format(getResources().getString(R.string.downloading_project), message));
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
        DownloadAllProjectsTask task = (DownloadAllProjectsTask) TaskManager.getTask(TASK_FORCE_DOWNLOAD_ALL_PROJECTS);
        if(task != null) {
            TaskManager.cancelTask(task);
        }
    }
}
