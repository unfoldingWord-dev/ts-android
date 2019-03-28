package com.door43.translationstudio.ui.devtools;

import android.app.ActivityManager;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.ui.dialogs.ErrorLogDialog;
import com.door43.translationstudio.ui.BaseActivity;
import com.door43.util.SdUtils;
import com.door43.util.StringUtilities;
import com.door43.widget.ViewUtil;

import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.TaskManager;

import java.io.File;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
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
        udidText.setText(String.format(getResources().getString(R.string.app_udid), App.udid()));

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
                StringUtilities.copyToClipboard(DeveloperToolsActivity.this, App.udid());
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
                        App.generateSSHKeys();
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
//        mDeveloperTools.add(new ToolItem("Simulate crash", "", R.drawable.ic_warning_black_24dp, new ToolItem.ToolAction() {
//            @Override
//            public void run() {
//                int killme = 1/0;
//            }
//        }));

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mDeveloperTools.add(new ToolItem("Reset SD Card Access", "", R.drawable.ic_warning_black_24dp, new ToolItem.ToolAction() {
                @Override
                public void run() {
                    Logger.i(this.getClass().getSimpleName(), "User has reset SD Card access");
                    SdUtils.removeSdCardWriteAccess();
                }
            }));
        }

        mDeveloperTools.add(new ToolItem("Check system resources", "Check for minimum system resources.", R.drawable.ic_description_black_24dp, new ToolItem.ToolAction() {
            @Override
            public void run() {
                DeveloperToolsActivity context = DeveloperToolsActivity.this;
                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                String message = "System Resources:\n";
                if(am != null) {
                    int numProcessors = Runtime.getRuntime().availableProcessors();
                    message += "Number of processors: " + numProcessors + " (" + App.minimumNumberOfProcessors + " required)\n";
                    long maxMem = Runtime.getRuntime().maxMemory();
                    String maxMemStr = getFormattedSize(maxMem);
                    String minReqRamStr = getFormattedSize(App.minimumRequiredRAM);
                    message += "JVM max memory: " + maxMemStr + " (" + minReqRamStr + " required)\n";
                    ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
                    am.getMemoryInfo(info);
                    String availMemStr = getFormattedSize(info.availMem);
                    message += "Available memory on the system: " + availMemStr + "\n";
                    String totalMemStr = "NA";
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        totalMemStr = getFormattedSize(info.totalMem);
                    }
                    message += "Total memory on the system (getMemoryInfo): " + totalMemStr + "\n";
                    String getTotalRamStr = getFormattedSize(getTotalRAM());
                    message += "Total memory on the system (/proc/meminfo): " + getTotalRamStr + "\n";

                    message += "Low memory threshold on the system: " + getFormattedSize(info.threshold) + "\n";
                    message += "Low memory state on the system: " + info.lowMemory + "\n";

                    message += "\nManufacturer: " + Build.MANUFACTURER + "\n";
                    message += "Model: " + Build.MODEL + "\n";
                    message += "Version: " + Build.VERSION.SDK_INT + "\n";
                    message += "Version Release: " + Build.VERSION.RELEASE + "\n";

                    DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                    message += "\nScreen size " + displayMetrics.heightPixels + "H*" + displayMetrics.widthPixels + "W";
                    message += ", density: " + displayMetrics.density;
                    message += ", dpi: " + displayMetrics.xdpi + "X*" + displayMetrics.ydpi + "Y";

                    Logger.i(TAG, "system resources check:\n" + message);

                    new AlertDialog.Builder(context, R.style.AppTheme_Dialog)
                            .setTitle("System Resources Check")
                            .setMessage(message)
                            .setCancelable(false)
                            .setNegativeButton(R.string.label_close, null)
                            .show();
                }
            }
        }));

        mDeveloperTools.add(new ToolItem("Delete Library", "Deletes the entire library database so it can be rebuilt from scratch", R.drawable.ic_delete_black_24dp, new ToolItem.ToolAction() {
            @Override
            public void run() {
                App.deleteLibrary();
                Snackbar snack = Snackbar.make(findViewById(android.R.id.content), "The library content was deleted", Snackbar.LENGTH_LONG);
                ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                snack.show();
            }
        }));

        // connect to existing tasks
        ManagedTask task = TaskManager.getTask(TASK_INDEX_CHUNK_MARKERS);
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
        ManagedTask task = TaskManager.getTask(TASK_INDEX_CHUNK_MARKERS);
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

    final static long KB = 1024;
    final static long MB = KB*KB;
    final static long GB = MB*KB;
    final static long TB = GB*KB;

    /**
     * convert memory size in bytes to value with units (e.g. "128MB")
     * @param bytes
     * @return
     */
    private String getFormattedSize(long bytes) {
        if(bytes/GB > 0) {
            return formatWithUnits((double) bytes/GB,"GB");
        }

        if(bytes/MB > 0) {
            return formatWithUnits((double) bytes/MB,"MB");
        }

        if(bytes/KB > 0) {
            return formatWithUnits((double) bytes/KB, "KB");
        }

        return bytes + "B";
    }

    private String formatWithUnits(double size, String units) {
        if(size >= 100) {
            return (long) (size+0.5) + units;
        }
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        if(size >= 10) {
            decimalFormat = new DecimalFormat("#.#");
        }
        return decimalFormat.format(size).concat(units);
    }

    /**
     * get memory available to system
     * @return
     */
    private long getTotalRAM() {
        RandomAccessFile reader = null;
        String load = null;
        double totalRam = 0;
        long lastValue = 0;
        try {
            reader = new RandomAccessFile("/proc/meminfo", "r");
            load = reader.readLine();
            reader.close();

            String[] parts = load.trim().split("\\s+");
            String value = parts[1];
            String units = parts[2];
            String unitsFirst = units.substring(0,1);

            totalRam = Double.parseDouble(value);

            if("T".equalsIgnoreCase(unitsFirst)) {
                totalRam *= TB;
            } else
            if("G".equalsIgnoreCase(unitsFirst)) {
                totalRam *= GB;
            } else
            if("M".equalsIgnoreCase(unitsFirst)) {
                totalRam *= MB;
            } else
            if("K".equalsIgnoreCase(unitsFirst)) {
                totalRam *= KB;
            }
            lastValue = (long) totalRam;

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            // Streams.close(reader);
        }

        return lastValue;
    }

    @Override
    public void onTaskFinished(final ManagedTask task) {
        TaskManager.clearTask(task);
        if(progressDialog != null) {
            progressDialog.dismiss();
        }

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.AppTheme_Dialog);

        if(task.getTaskId().equals(TASK_REGENERATE_KEYS)) {
            dialogBuilder
                    .setTitle(R.string.success)
                    .setMessage("The SSH keys have been regenerated")
                    .setNeutralButton(R.string.dismiss, null);
        }
        if(task.getTaskId().equals(TASK_INDEX_TA)) {
            dialogBuilder
                    .setTitle(R.string.success)
                    .setMessage("tA has been indexed")
                    .setNeutralButton(R.string.dismiss, null);
        }
        if(task.getTaskId().equals(TASK_EXPORT_LIBRARY)) {
            final File archive = (File)task.getResult();
            if(archive != null && archive.exists()) {
                dialogBuilder
                        .setTitle(R.string.success)
                        .setIcon(R.drawable.ic_done_black_24dp)
                        .setMessage(R.string.source_export_complete)
                        .setPositiveButton(R.string.menu_share, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Uri u = FileProvider.getUriForFile(DeveloperToolsActivity.this, "com.door43.translationstudio.fileprovider", archive);
                                Intent intent = new Intent(Intent.ACTION_SEND);
                                intent.setType("application/zip");
                                intent.putExtra(Intent.EXTRA_STREAM, u);
                                startActivity(Intent.createChooser(intent, "Email:"));
                            }
                        });
            } else {
                dialogBuilder
                        .setTitle(R.string.error)
                        .setIcon(R.drawable.ic_done_black_24dp)
                        .setMessage("The library could not be exported")
                        .setPositiveButton(R.string.dismiss, null);
            }
        }
        if(task.getTaskId().equals(TASK_INDEX_CHUNK_MARKERS)) {
            if(!task.isCanceled()) {
                dialogBuilder
                        .setTitle(R.string.success)
                        .setIcon(R.drawable.ic_done_black_24dp)
                        .setMessage("Chunk Markers have been indexed")
                        .setCancelable(false)
                        .setPositiveButton(R.string.label_ok, null);
            }
        }

        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                dialogBuilder.show();
            }
        });
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
                progressDialog.setMessage(message);
            }
        });
    }
}
