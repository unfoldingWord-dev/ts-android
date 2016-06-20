package com.door43.translationstudio;

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
import android.support.v7.widget.ViewUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import org.unfoldingword.tools.logger.Logger;
import com.door43.translationstudio.core.Project;
import com.door43.translationstudio.core.Resource;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.Util;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.dialogs.ErrorLogDialog;
import com.door43.translationstudio.newui.BaseActivity;
import com.door43.translationstudio.tasks.CheckForLibraryUpdatesTask;
import com.door43.translationstudio.tasks.DownloadAllProjectsTask;
import com.door43.translationstudio.util.ToolAdapter;
import com.door43.translationstudio.util.ToolItem;
import com.door43.util.StringUtilities;
import com.door43.widget.ViewUtil;

import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.TaskManager;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;

public class DeveloperToolsActivity extends BaseActivity implements ManagedTask.OnProgressListener, ManagedTask.OnFinishedListener {

    public static final String TASK_INDEX_CHUNK_MARKERS = "index_chunk_markers";
    public static final String TAG = DeveloperToolsActivity.class.getSimpleName();
    private static final String TASK_INDEX_TA = "index-ta-task";
    private static final String TASK_REGENERATE_KEYS = "regenerate-keys-task";
    private ArrayList<ToolItem> mDeveloperTools = new ArrayList<>();
    private ToolAdapter mAdapter;
    private String mVersionName;
    private String mVersionCode;
    private String TASK_EXPORT_LIBRARY = "export-library-task";
    private ProgressDialog progressDialog;

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
                Snackbar snack = Snackbar.make(findViewById(android.R.id.content), R.string.copied_to_clipboard, Snackbar.LENGTH_LONG);
                ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                snack.show();
            }
        });
        buildText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StringUtilities.copyToClipboard(DeveloperToolsActivity.this, mVersionCode);
                Snackbar snack = Snackbar.make(findViewById(android.R.id.content), R.string.copied_to_clipboard, Snackbar.LENGTH_LONG);
                ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                snack.show();
            }
        });
        udidText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StringUtilities.copyToClipboard(DeveloperToolsActivity.this, AppContext.udid());
                Snackbar snack = Snackbar.make(findViewById(android.R.id.content), R.string.copied_to_clipboard, Snackbar.LENGTH_LONG);
                ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                snack.show();
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
        mDeveloperTools.add(new ToolItem("Regenerate SSH keys", "Discards and regenerates the ssh keys used for git", R.drawable.ic_security_black_24dp, new ToolItem.ToolAction() {
            @Override
            public void run() {
                ManagedTask task = new ManagedTask() {
                    @Override
                    public void start() {
                        publishProgress(-1, "Regenerating keys");
                        AppContext.context().generateSSHKeys();
                    }
                };
                task.addOnProgressListener(DeveloperToolsActivity.this);
                task.addOnFinishedListener(DeveloperToolsActivity.this);
                TaskManager.addTask(task, TASK_REGENERATE_KEYS);
            }
        }));

        mDeveloperTools.add(new ToolItem("Read debugging log", "View the error logs that have been generated on this device.", R.drawable.ic_description_black_24dp, new ToolItem.ToolAction() {
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
        mDeveloperTools.add(new ToolItem("Expire library data", "Resets the modified date of indexed library data", R.drawable.ic_history_black_24dp, new ToolItem.ToolAction() {
            @Override
            public void run() {
                AppContext.getLibrary().setExpired();
                Snackbar snack = Snackbar.make(findViewById(android.R.id.content), "The resources have been expired", Snackbar.LENGTH_LONG);
                ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                snack.show();
            }
        }));
        mDeveloperTools.add(new ToolItem("Download library data", "Re-downloads the library data from the api and indexes it", R.drawable.ic_local_library_black_24dp, new ToolItem.ToolAction() {
            @Override
            public void run() {
                CustomAlertDialog.Builder(DeveloperToolsActivity.this)
                        .setTitle(R.string.action_download_all)
                        .setMessage(R.string.download_confirmation)
                        .setIcon(R.drawable.icon_update_cloud_dark)
                        .setPositiveButton(R.string.yes, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // create new prep task
                                CheckForLibraryUpdatesTask task = new CheckForLibraryUpdatesTask();
                                task.addOnProgressListener(DeveloperToolsActivity.this);
                                task.addOnFinishedListener(DeveloperToolsActivity.this);
                                TaskManager.addTask(task, CheckForLibraryUpdatesTask.TASK_ID);
                            }
                        })
                        .setNegativeButton(R.string.no, null)
                        .show("download-all-dialog");
            }
        }));
        mDeveloperTools.add(new ToolItem("Index tA", "(Hack) Indexes the bundled tA json", R.drawable.ic_local_library_black_24dp, new ToolItem.ToolAction() {
            @Override
            public void run() {
                ManagedTask task = new ManagedTask() {
                    @Override
                    public void start() {
                        publishProgress(-1, "Indexing tA...");
                        Project[] projects = AppContext.getLibrary().getProjects("en");
                        String catalog = null;
                        try {
                            catalog = Util.readStream(getAssets().open("ta.json"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if(catalog != null) {
                            for (Project p : projects) {
                                Resource[] resources = AppContext.getLibrary().getResources(p.getId(), "en");
                                for (Resource r : resources) {
                                    SourceTranslation sourceTranslation = SourceTranslation.simple(p.getId(), "en", r.getId());
                                    AppContext.getLibrary().manuallyIndexTranslationAcademy(sourceTranslation, catalog);
                                }
                            }
                        }
                    }
                };
                task.addOnProgressListener(DeveloperToolsActivity.this);
                task.addOnFinishedListener(DeveloperToolsActivity.this);
                TaskManager.addTask(task, TASK_INDEX_TA);
            }
        }));
        mDeveloperTools.add(new ToolItem("Index Chunk Markers", "(Hack) Injects the chunk marker catalog url into the database and runs the update check", R.drawable.ic_local_library_black_24dp, new ToolItem.ToolAction() {
            @Override
            public void run() {
                // manually inject chunk marker details into db
                AppContext.getLibrary().manuallyInjectChunkMarkerCatalogUrl();

                // run update check to index the chunk markers
                CheckForLibraryUpdatesTask task = new CheckForLibraryUpdatesTask();
                task.addOnProgressListener(DeveloperToolsActivity.this);
                task.addOnFinishedListener(DeveloperToolsActivity.this);
                TaskManager.addTask(task, TASK_INDEX_CHUNK_MARKERS);
            }
        }));
        mDeveloperTools.add(new ToolItem("Export Library", "Zips up the library data so you can share it with another device", R.drawable.ic_share_black_24dp, new ToolItem.ToolAction() {
            @Override
            public void run() {
                ManagedTask task = new ManagedTask() {
                    @Override
                    public void start() {
                        publishProgress(-1, "Exporting library...");
                        File archive = AppContext.getLibrary().export(new File(getCacheDir(), "sharing/"));
                        this.setResult(archive);
                    }
                };
                task.addOnProgressListener(DeveloperToolsActivity.this);
                task.addOnProgressListener(DeveloperToolsActivity.this);
                TaskManager.addTask(task, TASK_EXPORT_LIBRARY);
            }
        }));
        mDeveloperTools.add(new ToolItem("Simulate crash", "", R.drawable.ic_warning_black_24dp, new ToolItem.ToolAction() {
            @Override
            public void run() {
                int killme = 1/0;
            }
        }));
        mDeveloperTools.add(new ToolItem("Delete Library", "Deletes the entire library database so it can be rebuilt from scratch", R.drawable.ic_delete_black_24dp, new ToolItem.ToolAction() {
            @Override
            public void run() {
                AppContext.getLibrary().delete();
                Snackbar snack = Snackbar.make(findViewById(android.R.id.content), "The library content was deleted", Snackbar.LENGTH_LONG);
                ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                snack.show();
            }
        }));

        // connect to existing tasks
        ManagedTask task = TaskManager.getTask(CheckForLibraryUpdatesTask.TASK_ID);
        if(task != null) {
            task.addOnFinishedListener(this);
            task.addOnProgressListener(this);
        }
        task = TaskManager.getTask(DownloadAllProjectsTask.TASK_ID);
        if(task != null) {
            task.addOnFinishedListener(this);
            task.addOnProgressListener(this);
        }
        task = TaskManager.getTask(TASK_INDEX_CHUNK_MARKERS);
        if(task != null) {
            task.addOnFinishedListener(this);
            task.addOnProgressListener(this);
        }
        task = TaskManager.getTask(TASK_EXPORT_LIBRARY);
        if(task != null) {
            task.addOnFinishedListener(this);
            task.addOnProgressListener(this);
        }
        task = TaskManager.getTask(TASK_INDEX_TA);
        if(task != null) {
            task.addOnFinishedListener(this);
            task.addOnProgressListener(this);
        }
        task = TaskManager.getTask(TASK_REGENERATE_KEYS);
        if(task != null) {
            task.addOnFinishedListener(this);
            task.addOnProgressListener(this);
        }
    }

    @Override
    public void onDestroy() {
        ManagedTask task = TaskManager.getTask(CheckForLibraryUpdatesTask.TASK_ID);
        if(task != null) {
            task.removeOnFinishedListener(this);
            task.removeOnProgressListener(this);
        }
        task = TaskManager.getTask(DownloadAllProjectsTask.TASK_ID);
        if(task != null) {
            task.removeOnFinishedListener(this);
            task.removeOnProgressListener(this);
        }
        task = TaskManager.getTask(TASK_INDEX_CHUNK_MARKERS);
        if(task != null) {
            task.removeOnFinishedListener(this);
            task.removeOnProgressListener(this);
        }
        task = TaskManager.getTask(TASK_EXPORT_LIBRARY);
        if(task != null) {
            task.removeOnFinishedListener(this);
            task.removeOnProgressListener(this);
        }
        task = TaskManager.getTask(TASK_INDEX_TA);
        if(task != null) {
            task.removeOnFinishedListener(this);
            task.removeOnProgressListener(this);
        }
        task = TaskManager.getTask(TASK_REGENERATE_KEYS);
        if(task != null) {
            task.removeOnFinishedListener(this);
            task.removeOnProgressListener(this);
        }
        super.onDestroy();
    }

    @Override
    public void onTaskFinished(final ManagedTask task) {
        TaskManager.clearTask(task);
        if(progressDialog != null) {
            progressDialog.dismiss();
        }

        if(task.getTaskId().equals(TASK_REGENERATE_KEYS)) {
            CustomAlertDialog.Builder(DeveloperToolsActivity.this)
                    .setTitle(R.string.success)
                    .setMessage("The SSH keys have been regenerated")
                    .setNeutralButton(R.string.dismiss, null)
                    .show("key-gen-success");
        }
        if(task.getTaskId().equals(TASK_INDEX_TA)) {
            CustomAlertDialog.Builder(DeveloperToolsActivity.this)
                    .setTitle(R.string.success)
                    .setMessage("tA has been indexed")
                    .setNeutralButton(R.string.dismiss, null)
                    .show("ta-index-success");
        }
        if(task.getTaskId().equals(TASK_EXPORT_LIBRARY)) {
            final File archive = (File)task.getResult();
            if(archive != null && archive.exists()) {
                CustomAlertDialog.Builder(DeveloperToolsActivity.this)
                        .setTitle(R.string.success)
                        .setIcon(R.drawable.ic_done_black_24dp)
                        .setMessage(R.string.source_export_complete)
                        .setPositiveButton(R.string.menu_share, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Uri u = FileProvider.getUriForFile(DeveloperToolsActivity.this, "com.door43.translationstudio.fileprovider", archive);
                                Intent intent = new Intent(Intent.ACTION_SEND);
                                intent.setType("application/zip");
                                intent.putExtra(Intent.EXTRA_STREAM, u);
                                startActivity(Intent.createChooser(intent, "Email:"));
                            }
                        })
                        .show("export-success");
            } else {
                CustomAlertDialog.Builder(DeveloperToolsActivity.this)
                        .setTitle(R.string.error)
                        .setIcon(R.drawable.ic_done_black_24dp)
                        .setMessage("The library could not be exported")
                        .setPositiveButton(R.string.dismiss, null)
                        .show("export-failed");
            }
        }

        if(task.getTaskId().equals(TASK_INDEX_CHUNK_MARKERS)) {
            if(!task.isCanceled()) {
                CustomAlertDialog.Builder(DeveloperToolsActivity.this)
                        .setTitle(R.string.success)
                        .setIcon(R.drawable.ic_done_black_24dp)
                        .setMessage("Chunk Markers have been indexed")
                        .setCancelableChainable(false)
                        .setPositiveButton(R.string.label_ok, null)
                        .show("Success");
            }
        }

        if(task.getTaskId().equals(CheckForLibraryUpdatesTask.TASK_ID)) {
            DownloadAllProjectsTask newTask = new DownloadAllProjectsTask();
            newTask.addOnProgressListener(this);
            newTask.addOnFinishedListener(this);
            TaskManager.addTask(newTask, DownloadAllProjectsTask.TASK_ID);
        }

        if(task.getTaskId().equals(DownloadAllProjectsTask.TASK_ID)) {
            CustomAlertDialog.Builder(DeveloperToolsActivity.this)
                    .setTitle(R.string.success)
                    .setIcon(R.drawable.ic_done_black_24dp)
                    .setMessage(R.string.download_complete)
                    .setCancelableChainable(false)
                    .setPositiveButton(R.string.label_ok, null)
                    .show("download-success");
        }
    }

    @Override
    public void onTaskProgress(final ManagedTask task, final double progress, final String message, final boolean secondary) {
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                if (task.isFinished()) {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                    return;
                }

                if (progressDialog == null) {
                    progressDialog = new ProgressDialog(DeveloperToolsActivity.this);
                    progressDialog.setCancelable(false);
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.setCanceledOnTouchOutside(false);
                    progressDialog.setIcon(R.drawable.ic_cloud_download_black_24dp);
                    if(task instanceof CheckForLibraryUpdatesTask) {
                        progressDialog.setTitle(getResources().getString(R.string.checking_for_updates));
                    } else if(task instanceof DownloadAllProjectsTask) {
                        progressDialog.setTitle(getResources().getString(R.string.downloading));
                    }
                    progressDialog.setMessage("");
                }
                progressDialog.setMax(task.maxProgress());
                if (!progressDialog.isShowing()) {
                    progressDialog.show();
                }
                if (progress == -1) {
                    progressDialog.setIndeterminate(true);
                    progressDialog.setProgress(progressDialog.getMax());
                    progressDialog.setProgressNumberFormat(null);
                    progressDialog.setProgressPercentFormat(null);
                } else {
                    progressDialog.setIndeterminate(false);
                    if(secondary) {
                        progressDialog.setSecondaryProgress((int) progress);
                    } else {
                        progressDialog.setProgress((int) progress);
                    }
                    progressDialog.setProgressNumberFormat("%1d/%2d");
                    progressDialog.setProgressPercentFormat(NumberFormat.getPercentInstance());
                }
                if (task instanceof DownloadAllProjectsTask && !message.isEmpty()) {
                    progressDialog.setMessage(String.format(getResources().getString(R.string.downloading_project), message));
                } else {
                    progressDialog.setMessage(message);
                }
            }
        });
    }
}
