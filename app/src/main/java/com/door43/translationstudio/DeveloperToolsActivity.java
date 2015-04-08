package com.door43.translationstudio;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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
import com.door43.translationstudio.projects.ProjectManager;
import com.door43.translationstudio.projects.Sharing;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.StringUtilities;
import com.door43.util.threads.ThreadableUI;
import com.door43.util.Logger;
import com.door43.translationstudio.util.ToolAdapter;
import com.door43.translationstudio.util.ToolItem;
import com.door43.translationstudio.util.TranslatorBaseActivity;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class DeveloperToolsActivity extends TranslatorBaseActivity {

    private ArrayList<ToolItem> mDeveloperTools = new ArrayList<>();
    private ToolAdapter mAdapter;
    private String mVersionName;
    private String mVersionCode;

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
            }
        });
        buildText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StringUtilities.copyToClipboard(DeveloperToolsActivity.this, mVersionCode);
            }
        });
        udidText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StringUtilities.copyToClipboard(DeveloperToolsActivity.this, AppContext.udid());
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
//        mDeveloperTools.add(new ToolItem(getResources().getString(R.string.force_update_projects), getResources().getString(R.string.force_update_projects_description), 0, new ToolItem.ToolAction() {
//            @Override
//            public void run() {
//                final Project[] projects = AppContext.projectManager().getProjects();
//
//                final ProgressDialog dialog = new ProgressDialog(DeveloperToolsActivity.this);
//                dialog.setCancelable(true);
//                dialog.setCanceledOnTouchOutside(false);
//                dialog.setIndeterminate(true);
//                dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//                dialog.setMax(projects.length);
//                dialog.setMessage(getResources().getString(R.string.downloading_updates));
//
//                final Handler handle = new Handler(Looper.getMainLooper());
//
//                final ThreadableUI thread = new ThreadableUI(DeveloperToolsActivity.this) {
//
//                    @Override
//                    public void onStop() {
//                        handle.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                dialog.setMessage("Cancelling...");
//                                dialog.setIndeterminate(true);
//                                dialog.show();
//                            }
//                        });
//
//                        AppContext.context().showToastMessage(getResources().getString(R.string.download_canceled));
//                    }
//
//                    @Override
//                    public void run() {
//                        // disable screen rotation so we don't break things
////                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
//                        for(int i=0; i<projects.length; i ++) {
//                            if(isInterrupted()) break;
//                            // update progress
//                            final int progress = i;
//                            final String title = String.format(getResources().getString(R.string.downloading_project_updates), projects[i].getId());
//                            handle.post(new Runnable() {
//                                @Override
//                                public void run() {
//                                    dialog.setIndeterminate(false);
//                                    dialog.setProgress(progress);
//                                    dialog.setMessage(title);
//                                }
//                            });
//
//                            // TODO: use the update all task with flag to ignore the cache
//                            AppContext.context().showToastMessage("Not implimented yet");
//                        }
//
//                        // reload the selected project source
//                        if(AppContext.projectManager().getSelectedProject() != null) {
//                            handle.post(new Runnable() {
//                                @Override
//                                public void run() {
//                                    dialog.setMessage(getResources().getString(R.string.loading_project_chapters));
//                                    dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
//                                    dialog.setIndeterminate(true);
//                                }
//                            });
//                            AppContext.projectManager().fetchProjectSource(AppContext.projectManager().getSelectedProject());
//                        }
//                    }
//
//                    @Override
//                    public void onPostExecute() {
//                        dialog.dismiss();
//                        // re-enable screen rotation
////                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
//                        if(!isInterrupted()) {
//                            AppContext.context().showToastMessage(R.string.success);
//                        }
//                    }
//                };
//
//                // allow the user to cancel the dialog
//                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
//                    @Override
//                    public void onCancel(DialogInterface dialogInterface) {
//                        thread.stop();
//                    }
//                });
//                dialog.show();
//
//                thread.start();
//            }
//        }));
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
                dialog.setMessage(getResources().getString(R.string.exporting_source));

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
    }
}
