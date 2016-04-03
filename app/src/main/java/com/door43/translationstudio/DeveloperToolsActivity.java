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
import com.door43.translationstudio.core.ImportUsfm;
import com.door43.translationstudio.core.Project;
import com.door43.translationstudio.core.Resource;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.Util;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.dialogs.ErrorLogDialog;
import com.door43.translationstudio.newui.BaseActivity;
import com.door43.translationstudio.tasks.GetLibraryUpdatesTask;
import com.door43.translationstudio.tasks.DownloadAllProjectsTask;
import com.door43.translationstudio.util.ToolAdapter;
import com.door43.translationstudio.util.ToolItem;
import com.door43.util.StringUtilities;
import com.door43.util.tasks.ManagedTask;
import com.door43.util.tasks.TaskManager;
import com.door43.util.tasks.ThreadableUI;
import com.door43.widget.ViewUtil;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.ArrayList;

public class DeveloperToolsActivity extends BaseActivity implements ManagedTask.OnProgressListener, ManagedTask.OnFinishedListener, DialogInterface.OnCancelListener {

    private static final String TASK_PREP_FORCE_DOWNLOAD_ALL_PROJECTS = "prep_force_download_all_projects";
    public static final String TAG = DeveloperToolsActivity.class.getSimpleName();
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

        mDeveloperTools.add(new ToolItem("Test USFM Import", "Test USFM Import", 0, new ToolItem.ToolAction() {
            @Override
            public void run() {
                String file = "mrk.usfm.txt";
                String chunkJsonStr = "[{\"chp\": \"01\", \"firstvs\": \"01\"}, {\"chp\": \"01\", \"firstvs\": \"04\"}, {\"chp\": \"01\", \"firstvs\": \"07\"}, {\"chp\": \"01\", \"firstvs\": \"09\"}, {\"chp\": \"01\", \"firstvs\": \"12\"}, {\"chp\": \"01\", \"firstvs\": \"14\"}, {\"chp\": \"01\", \"firstvs\": \"16\"}, {\"chp\": \"01\", \"firstvs\": \"19\"}, {\"chp\": \"01\", \"firstvs\": \"21\"}, {\"chp\": \"01\", \"firstvs\": \"23\"}, {\"chp\": \"01\", \"firstvs\": \"27\"}, {\"chp\": \"01\", \"firstvs\": \"29\"}, {\"chp\": \"01\", \"firstvs\": \"32\"}, {\"chp\": \"01\", \"firstvs\": \"35\"}, {\"chp\": \"01\", \"firstvs\": \"38\"}, {\"chp\": \"01\", \"firstvs\": \"40\"}, {\"chp\": \"01\", \"firstvs\": \"43\"}, {\"chp\": \"01\", \"firstvs\": \"45\"}, {\"chp\": \"02\", \"firstvs\": \"01\"}, {\"chp\": \"02\", \"firstvs\": \"03\"}, {\"chp\": \"02\", \"firstvs\": \"05\"}, {\"chp\": \"02\", \"firstvs\": \"08\"}, {\"chp\": \"02\", \"firstvs\": \"10\"}, {\"chp\": \"02\", \"firstvs\": \"13\"}, {\"chp\": \"02\", \"firstvs\": \"15\"}, {\"chp\": \"02\", \"firstvs\": \"17\"}, {\"chp\": \"02\", \"firstvs\": \"18\"}, {\"chp\": \"02\", \"firstvs\": \"20\"}, {\"chp\": \"02\", \"firstvs\": \"22\"}, {\"chp\": \"02\", \"firstvs\": \"23\"}, {\"chp\": \"02\", \"firstvs\": \"25\"}, {\"chp\": \"02\", \"firstvs\": \"27\"}, {\"chp\": \"03\", \"firstvs\": \"01\"}, {\"chp\": \"03\", \"firstvs\": \"03\"}, {\"chp\": \"03\", \"firstvs\": \"05\"}, {\"chp\": \"03\", \"firstvs\": \"07\"}, {\"chp\": \"03\", \"firstvs\": \"09\"}, {\"chp\": \"03\", \"firstvs\": \"11\"}, {\"chp\": \"03\", \"firstvs\": \"13\"}, {\"chp\": \"03\", \"firstvs\": \"17\"}, {\"chp\": \"03\", \"firstvs\": \"20\"}, {\"chp\": \"03\", \"firstvs\": \"23\"}, {\"chp\": \"03\", \"firstvs\": \"26\"}, {\"chp\": \"03\", \"firstvs\": \"28\"}, {\"chp\": \"03\", \"firstvs\": \"31\"}, {\"chp\": \"03\", \"firstvs\": \"33\"}, {\"chp\": \"04\", \"firstvs\": \"01\"}, {\"chp\": \"04\", \"firstvs\": \"03\"}, {\"chp\": \"04\", \"firstvs\": \"06\"}, {\"chp\": \"04\", \"firstvs\": \"08\"}, {\"chp\": \"04\", \"firstvs\": \"10\"}, {\"chp\": \"04\", \"firstvs\": \"13\"}, {\"chp\": \"04\", \"firstvs\": \"16\"}, {\"chp\": \"04\", \"firstvs\": \"18\"}, {\"chp\": \"04\", \"firstvs\": \"21\"}, {\"chp\": \"04\", \"firstvs\": \"24\"}, {\"chp\": \"04\", \"firstvs\": \"26\"}, {\"chp\": \"04\", \"firstvs\": \"30\"}, {\"chp\": \"04\", \"firstvs\": \"33\"}, {\"chp\": \"04\", \"firstvs\": \"35\"}, {\"chp\": \"04\", \"firstvs\": \"38\"}, {\"chp\": \"04\", \"firstvs\": \"40\"}, {\"chp\": \"05\", \"firstvs\": \"01\"}, {\"chp\": \"05\", \"firstvs\": \"03\"}, {\"chp\": \"05\", \"firstvs\": \"05\"}, {\"chp\": \"05\", \"firstvs\": \"07\"}, {\"chp\": \"05\", \"firstvs\": \"09\"}, {\"chp\": \"05\", \"firstvs\": \"11\"}, {\"chp\": \"05\", \"firstvs\": \"14\"}, {\"chp\": \"05\", \"firstvs\": \"16\"}, {\"chp\": \"05\", \"firstvs\": \"18\"}, {\"chp\": \"05\", \"firstvs\": \"21\"}, {\"chp\": \"05\", \"firstvs\": \"25\"}, {\"chp\": \"05\", \"firstvs\": \"28\"}, {\"chp\": \"05\", \"firstvs\": \"30\"}, {\"chp\": \"05\", \"firstvs\": \"33\"}, {\"chp\": \"05\", \"firstvs\": \"35\"}, {\"chp\": \"05\", \"firstvs\": \"36\"}, {\"chp\": \"05\", \"firstvs\": \"39\"}, {\"chp\": \"05\", \"firstvs\": \"41\"}, {\"chp\": \"06\", \"firstvs\": \"01\"}, {\"chp\": \"06\", \"firstvs\": \"04\"}, {\"chp\": \"06\", \"firstvs\": \"07\"}, {\"chp\": \"06\", \"firstvs\": \"10\"}, {\"chp\": \"06\", \"firstvs\": \"12\"}, {\"chp\": \"06\", \"firstvs\": \"14\"}, {\"chp\": \"06\", \"firstvs\": \"16\"}, {\"chp\": \"06\", \"firstvs\": \"18\"}, {\"chp\": \"06\", \"firstvs\": \"21\"}, {\"chp\": \"06\", \"firstvs\": \"23\"}, {\"chp\": \"06\", \"firstvs\": \"26\"}, {\"chp\": \"06\", \"firstvs\": \"30\"}, {\"chp\": \"06\", \"firstvs\": \"33\"}, {\"chp\": \"06\", \"firstvs\": \"35\"}, {\"chp\": \"06\", \"firstvs\": \"37\"}, {\"chp\": \"06\", \"firstvs\": \"39\"}, {\"chp\": \"06\", \"firstvs\": \"42\"}, {\"chp\": \"06\", \"firstvs\": \"45\"}, {\"chp\": \"06\", \"firstvs\": \"48\"}, {\"chp\": \"06\", \"firstvs\": \"51\"}, {\"chp\": \"06\", \"firstvs\": \"53\"}, {\"chp\": \"06\", \"firstvs\": \"56\"}, {\"chp\": \"07\", \"firstvs\": \"01\"}, {\"chp\": \"07\", \"firstvs\": \"02\"}, {\"chp\": \"07\", \"firstvs\": \"05\"}, {\"chp\": \"07\", \"firstvs\": \"06\"}, {\"chp\": \"07\", \"firstvs\": \"08\"}, {\"chp\": \"07\", \"firstvs\": \"11\"}, {\"chp\": \"07\", \"firstvs\": \"14\"}, {\"chp\": \"07\", \"firstvs\": \"17\"}, {\"chp\": \"07\", \"firstvs\": \"20\"}, {\"chp\": \"07\", \"firstvs\": \"24\"}, {\"chp\": \"07\", \"firstvs\": \"27\"}, {\"chp\": \"07\", \"firstvs\": \"29\"}, {\"chp\": \"07\", \"firstvs\": \"31\"}, {\"chp\": \"07\", \"firstvs\": \"33\"}, {\"chp\": \"07\", \"firstvs\": \"36\"}, {\"chp\": \"08\", \"firstvs\": \"01\"}, {\"chp\": \"08\", \"firstvs\": \"05\"}, {\"chp\": \"08\", \"firstvs\": \"07\"}, {\"chp\": \"08\", \"firstvs\": \"11\"}, {\"chp\": \"08\", \"firstvs\": \"14\"}, {\"chp\": \"08\", \"firstvs\": \"16\"}, {\"chp\": \"08\", \"firstvs\": \"18\"}, {\"chp\": \"08\", \"firstvs\": \"20\"}, {\"chp\": \"08\", \"firstvs\": \"22\"}, {\"chp\": \"08\", \"firstvs\": \"24\"}, {\"chp\": \"08\", \"firstvs\": \"27\"}, {\"chp\": \"08\", \"firstvs\": \"29\"}, {\"chp\": \"08\", \"firstvs\": \"31\"}, {\"chp\": \"08\", \"firstvs\": \"33\"}, {\"chp\": \"08\", \"firstvs\": \"35\"}, {\"chp\": \"08\", \"firstvs\": \"38\"}, {\"chp\": \"09\", \"firstvs\": \"01\"}, {\"chp\": \"09\", \"firstvs\": \"04\"}, {\"chp\": \"09\", \"firstvs\": \"07\"}, {\"chp\": \"09\", \"firstvs\": \"09\"}, {\"chp\": \"09\", \"firstvs\": \"11\"}, {\"chp\": \"09\", \"firstvs\": \"14\"}, {\"chp\": \"09\", \"firstvs\": \"17\"}, {\"chp\": \"09\", \"firstvs\": \"20\"}, {\"chp\": \"09\", \"firstvs\": \"23\"}, {\"chp\": \"09\", \"firstvs\": \"26\"}, {\"chp\": \"09\", \"firstvs\": \"28\"}, {\"chp\": \"09\", \"firstvs\": \"30\"}, {\"chp\": \"09\", \"firstvs\": \"33\"}, {\"chp\": \"09\", \"firstvs\": \"36\"}, {\"chp\": \"09\", \"firstvs\": \"38\"}, {\"chp\": \"09\", \"firstvs\": \"40\"}, {\"chp\": \"09\", \"firstvs\": \"42\"}, {\"chp\": \"09\", \"firstvs\": \"45\"}, {\"chp\": \"09\", \"firstvs\": \"47\"}, {\"chp\": \"09\", \"firstvs\": \"49\"}, {\"chp\": \"10\", \"firstvs\": \"01\"}, {\"chp\": \"10\", \"firstvs\": \"05\"}, {\"chp\": \"10\", \"firstvs\": \"07\"}, {\"chp\": \"10\", \"firstvs\": \"10\"}, {\"chp\": \"10\", \"firstvs\": \"13\"}, {\"chp\": \"10\", \"firstvs\": \"15\"}, {\"chp\": \"10\", \"firstvs\": \"17\"}, {\"chp\": \"10\", \"firstvs\": \"20\"}, {\"chp\": \"10\", \"firstvs\": \"23\"}, {\"chp\": \"10\", \"firstvs\": \"26\"}, {\"chp\": \"10\", \"firstvs\": \"29\"}, {\"chp\": \"10\", \"firstvs\": \"32\"}, {\"chp\": \"10\", \"firstvs\": \"35\"}, {\"chp\": \"10\", \"firstvs\": \"38\"}, {\"chp\": \"10\", \"firstvs\": \"41\"}, {\"chp\": \"10\", \"firstvs\": \"43\"}, {\"chp\": \"10\", \"firstvs\": \"46\"}, {\"chp\": \"10\", \"firstvs\": \"49\"}, {\"chp\": \"10\", \"firstvs\": \"51\"}, {\"chp\": \"11\", \"firstvs\": \"01\"}, {\"chp\": \"11\", \"firstvs\": \"04\"}, {\"chp\": \"11\", \"firstvs\": \"07\"}, {\"chp\": \"11\", \"firstvs\": \"11\"}, {\"chp\": \"11\", \"firstvs\": \"13\"}, {\"chp\": \"11\", \"firstvs\": \"15\"}, {\"chp\": \"11\", \"firstvs\": \"17\"}, {\"chp\": \"11\", \"firstvs\": \"20\"}, {\"chp\": \"11\", \"firstvs\": \"22\"}, {\"chp\": \"11\", \"firstvs\": \"24\"}, {\"chp\": \"11\", \"firstvs\": \"27\"}, {\"chp\": \"11\", \"firstvs\": \"29\"}, {\"chp\": \"11\", \"firstvs\": \"31\"}, {\"chp\": \"12\", \"firstvs\": \"01\"}, {\"chp\": \"12\", \"firstvs\": \"04\"}, {\"chp\": \"12\", \"firstvs\": \"06\"}, {\"chp\": \"12\", \"firstvs\": \"08\"}, {\"chp\": \"12\", \"firstvs\": \"10\"}, {\"chp\": \"12\", \"firstvs\": \"13\"}, {\"chp\": \"12\", \"firstvs\": \"16\"}, {\"chp\": \"12\", \"firstvs\": \"18\"}, {\"chp\": \"12\", \"firstvs\": \"20\"}, {\"chp\": \"12\", \"firstvs\": \"24\"}, {\"chp\": \"12\", \"firstvs\": \"26\"}, {\"chp\": \"12\", \"firstvs\": \"28\"}, {\"chp\": \"12\", \"firstvs\": \"32\"}, {\"chp\": \"12\", \"firstvs\": \"35\"}, {\"chp\": \"12\", \"firstvs\": \"38\"}, {\"chp\": \"12\", \"firstvs\": \"41\"}, {\"chp\": \"12\", \"firstvs\": \"43\"}, {\"chp\": \"13\", \"firstvs\": \"01\"}, {\"chp\": \"13\", \"firstvs\": \"03\"}, {\"chp\": \"13\", \"firstvs\": \"05\"}, {\"chp\": \"13\", \"firstvs\": \"07\"}, {\"chp\": \"13\", \"firstvs\": \"09\"}, {\"chp\": \"13\", \"firstvs\": \"11\"}, {\"chp\": \"13\", \"firstvs\": \"14\"}, {\"chp\": \"13\", \"firstvs\": \"17\"}, {\"chp\": \"13\", \"firstvs\": \"21\"}, {\"chp\": \"13\", \"firstvs\": \"24\"}, {\"chp\": \"13\", \"firstvs\": \"28\"}, {\"chp\": \"13\", \"firstvs\": \"30\"}, {\"chp\": \"13\", \"firstvs\": \"33\"}, {\"chp\": \"13\", \"firstvs\": \"35\"}, {\"chp\": \"14\", \"firstvs\": \"01\"}, {\"chp\": \"14\", \"firstvs\": \"03\"}, {\"chp\": \"14\", \"firstvs\": \"06\"}, {\"chp\": \"14\", \"firstvs\": \"10\"}, {\"chp\": \"14\", \"firstvs\": \"12\"}, {\"chp\": \"14\", \"firstvs\": \"15\"}, {\"chp\": \"14\", \"firstvs\": \"17\"}, {\"chp\": \"14\", \"firstvs\": \"20\"}, {\"chp\": \"14\", \"firstvs\": \"22\"}, {\"chp\": \"14\", \"firstvs\": \"26\"}, {\"chp\": \"14\", \"firstvs\": \"28\"}, {\"chp\": \"14\", \"firstvs\": \"30\"}, {\"chp\": \"14\", \"firstvs\": \"32\"}, {\"chp\": \"14\", \"firstvs\": \"35\"}, {\"chp\": \"14\", \"firstvs\": \"37\"}, {\"chp\": \"14\", \"firstvs\": \"40\"}, {\"chp\": \"14\", \"firstvs\": \"43\"}, {\"chp\": \"14\", \"firstvs\": \"47\"}, {\"chp\": \"14\", \"firstvs\": \"51\"}, {\"chp\": \"14\", \"firstvs\": \"53\"}, {\"chp\": \"14\", \"firstvs\": \"55\"}, {\"chp\": \"14\", \"firstvs\": \"57\"}, {\"chp\": \"14\", \"firstvs\": \"60\"}, {\"chp\": \"14\", \"firstvs\": \"63\"}, {\"chp\": \"14\", \"firstvs\": \"66\"}, {\"chp\": \"14\", \"firstvs\": \"69\"}, {\"chp\": \"14\", \"firstvs\": \"71\"}, {\"chp\": \"15\", \"firstvs\": \"01\"}, {\"chp\": \"15\", \"firstvs\": \"04\"}, {\"chp\": \"15\", \"firstvs\": \"06\"}, {\"chp\": \"15\", \"firstvs\": \"09\"}, {\"chp\": \"15\", \"firstvs\": \"12\"}, {\"chp\": \"15\", \"firstvs\": \"14\"}, {\"chp\": \"15\", \"firstvs\": \"16\"}, {\"chp\": \"15\", \"firstvs\": \"19\"}, {\"chp\": \"15\", \"firstvs\": \"22\"}, {\"chp\": \"15\", \"firstvs\": \"25\"}, {\"chp\": \"15\", \"firstvs\": \"29\"}, {\"chp\": \"15\", \"firstvs\": \"31\"}, {\"chp\": \"15\", \"firstvs\": \"33\"}, {\"chp\": \"15\", \"firstvs\": \"36\"}, {\"chp\": \"15\", \"firstvs\": \"39\"}, {\"chp\": \"15\", \"firstvs\": \"42\"}, {\"chp\": \"15\", \"firstvs\": \"45\"}, {\"chp\": \"16\", \"firstvs\": \"01\"}, {\"chp\": \"16\", \"firstvs\": \"03\"}, {\"chp\": \"16\", \"firstvs\": \"05\"}, {\"chp\": \"16\", \"firstvs\": \"08\"}, {\"chp\": \"16\", \"firstvs\": \"09\"}, {\"chp\": \"16\", \"firstvs\": \"12\"}, {\"chp\": \"16\", \"firstvs\": \"14\"}, {\"chp\": \"16\", \"firstvs\": \"17\"}, {\"chp\": \"16\", \"firstvs\": \"19\"}]";
                try {
                    InputStream usfmStream = getAssets().open(file);
                    String text = IOUtils.toString(usfmStream, "UTF-8");
                    ImportUsfm usfm = (new ImportUsfm());
                    usfm.addChunk("mrk", chunkJsonStr);
                    usfm.processBook(text);
                } catch (Exception e) {
                    Logger.e(TAG,"error reading " + file, e);
                }
            }
        }));


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

        mDeveloperTools.add(new ToolItem(getResources().getString(R.string.invalidate_keys), getResources().getString(R.string.invalidate_keys_description), 0, new ToolItem.ToolAction() {
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
                        AppContext.context().setHasRegisteredKeys(true);
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
        mDeveloperTools.add(new ToolItem("Expire local resources", "Resets the local date modified on content to allow manually updating", 0, new ToolItem.ToolAction() {
            @Override
            public void run() {
                AppContext.getLibrary().setExpired();
                Snackbar snack = Snackbar.make(findViewById(android.R.id.content), "The resources have been expired", Snackbar.LENGTH_LONG);
                ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                snack.show();
            }
        }));
        mDeveloperTools.add(new ToolItem(getResources().getString(R.string.force_update_projects), getResources().getString(R.string.force_update_projects_description), 0, new ToolItem.ToolAction() {
            @Override
            public void run() {
                CustomAlertDialog.Create(DeveloperToolsActivity.this)
                        .setTitle(R.string.action_download_all)
                        .setMessage(R.string.download_confirmation)
                        .setIcon(R.drawable.icon_update_cloud_dark)
                        .setPositiveButton(R.string.yes, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // create new prep task
                                GetLibraryUpdatesTask task = new GetLibraryUpdatesTask();
                                task.addOnProgressListener(DeveloperToolsActivity.this);
                                task.addOnFinishedListener(DeveloperToolsActivity.this);
                                TaskManager.addTask(task, GetLibraryUpdatesTask.TASK_ID);
                            }
                        })
                        .setNegativeButton(R.string.no, null)
                        .show("DownldAll");
            }
        }));
        mDeveloperTools.add(new ToolItem("Index tA", "Indexes the bundled tA json", 0, new ToolItem.ToolAction() {
            @Override
            public void run() {
                ThreadableUI thread = new ThreadableUI(DeveloperToolsActivity.this) {
                    @Override
                    public void onStop() {

                    }

                    @Override
                    public void run() {
                        // TRICKY: for now tA is only in english
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

                    @Override
                    public void onPostExecute() {
                        CustomAlertDialog.Create(DeveloperToolsActivity.this)
                                .setTitle(R.string.success)
                                .setMessage("tA has been indexed")
                                .setNeutralButton(R.string.dismiss, null)
                                .show("ta-index-success");
                    }
                };
                thread.start();
                Snackbar snack = Snackbar.make(findViewById(android.R.id.content), "indexing tA...", Snackbar.LENGTH_LONG);
                ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                snack.show();
            }
        }));
        mDeveloperTools.add(new ToolItem(getResources().getString(R.string.export_source), getResources().getString(R.string.export_source_description), 0, new ToolItem.ToolAction() {
            @Override
            public void run() {
                final Handler handle = new Handler(Looper.getMainLooper());
//                final Project[] projects = AppContext.projectManager().getProjectSlugs();
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
                AppContext.getLibrary().delete();
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
                        CustomAlertDialog.Create(DeveloperToolsActivity.this)
                                .setTitle(R.string.success)
                                .setIcon(R.drawable.ic_done_black_24dp)
                                .setMessage(R.string.download_complete)
                                .setCancelableChainable(false)
                                .setPositiveButton(R.string.label_ok, null)
                                .show("Success");
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
