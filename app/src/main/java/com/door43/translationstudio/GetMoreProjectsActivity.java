package com.door43.translationstudio;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.door43.translationstudio.device2device.DeviceToDeviceActivity;
import com.door43.translationstudio.library.ProjectLibraryListActivity;
import com.door43.translationstudio.util.AppContext;
import com.door43.translationstudio.util.ToolAdapter;
import com.door43.translationstudio.util.ToolItem;
import com.door43.translationstudio.util.TranslatorBaseActivity;

import java.util.ArrayList;

public class GetMoreProjectsActivity extends TranslatorBaseActivity {

    private ArrayList<ToolItem> mGetProjectTools = new ArrayList<>();
    private ToolAdapter mAdapter;
    private AlertDialog mBrowseDialog;

    /**
     * State options for browsing projects
     */
    private enum BrowseState {
        NOTHING,
        DOWNLOADING_PROJECTS_CATALOG,
        BROWSING_PROJECTS_CATALOG,
        DOWNLOADING_LANGUAGES_CATALOG,
        BROWSING_LANGUAGES_CATALOG,
        DOWNLOADING_PROJECT_SOURCE,
        RELOADING_SELECTED_PROJECT
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_more_projects);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ListView list = (ListView)findViewById(R.id.getProjectsListView);
        mAdapter = new ToolAdapter(mGetProjectTools, this);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ToolItem tool = mAdapter.getItem(i);
                // execute the get/update project action
                if (tool.isEnabled()) {
                    tool.getAction().run();
                } else {
                    app().showToastMessage(tool.getDisabledNotice());
                }
            }
        });
        init();
    }

    private void init() {
        Boolean hasNetwork = AppContext.context().isNetworkAvailable();
        mGetProjectTools.add(new ToolItem(getResources().getString(R.string.browse_projects), getResources().getString(R.string.browse_projects_description), R.drawable.ic_download, new ToolItem.ToolAction() {
            @Override
            public void run() {
                mBrowseDialog = new AlertDialog.Builder(GetMoreProjectsActivity.this)
                        .setTitle(R.string.browse_projects)
                        .setMessage(R.string.use_internet_confirmation)
                        .setIcon(R.drawable.ic_download_small)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(GetMoreProjectsActivity.this, ProjectLibraryListActivity.class);
                                intent.putExtra(ProjectLibraryListActivity.ARG_ONLY_SHOW_NEW, true);
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
            }
        }, hasNetwork, getResources().getString(R.string.internet_not_available)));
        mGetProjectTools.add(new ToolItem(getResources().getString(R.string.update_projects), getResources().getString(R.string.update_projects_description), R.drawable.ic_update, new ToolItem.ToolAction() {
            @Override
            public void run() {
                mBrowseDialog = new AlertDialog.Builder(GetMoreProjectsActivity.this)
                        .setTitle(R.string.update_projects)
                        .setMessage(R.string.use_internet_confirmation)
                        .setIcon(R.drawable.ic_update_small)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(GetMoreProjectsActivity.this, ProjectLibraryListActivity.class);
                                intent.putExtra(ProjectLibraryListActivity.ARG_ONLY_SHOW_UPDATES, true);
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
            }
        }, hasNetwork, getResources().getString(R.string.internet_not_available)));
        mGetProjectTools.add(new ToolItem("Transfer from device", "Projects will be transferred over the network from a nearby device", R.drawable.ic_phone, new ToolItem.ToolAction() {
            @Override
            public void run() {
                Intent intent = new Intent(GetMoreProjectsActivity.this, DeviceToDeviceActivity.class);
                Bundle extras = new Bundle();
                extras.putBoolean("startAsServer", false);
                extras.putBoolean("browseSourceProjects", true);
                intent.putExtras(extras);
                startActivity(intent);
            }
        }, false, "Not implimented"));// hasNetwork, getResources().getString(R.string.internet_not_available)));
        mGetProjectTools.add(new ToolItem("Import from storage", "Projects will be imported from the external storage on this device", R.drawable.ic_folder, new ToolItem.ToolAction() {
            @Override
            public void run() {
                // TODO: This is the same as for the Sharing activity though we should package it all up into a single method call.
            }
        }, false, "Not implimented"));
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_get_more_projects, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        if(mBrowseDialog != null) {
            mBrowseDialog.dismiss();
        }
        super.onDestroy();
    }
}
