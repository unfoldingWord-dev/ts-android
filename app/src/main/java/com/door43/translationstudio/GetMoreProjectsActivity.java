package com.door43.translationstudio;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.door43.translationstudio.device2device.DeviceToDeviceActivity;
import com.door43.translationstudio.newui.library.ServerLibraryActivity;
import com.door43.translationstudio.util.AppContext;
import com.door43.translationstudio.util.ToolAdapter;
import com.door43.translationstudio.util.ToolItem;
import com.door43.translationstudio.util.TranslatorBaseActivity;

import java.util.ArrayList;

public class GetMoreProjectsActivity extends TranslatorBaseActivity {

    private ArrayList<ToolItem> mGetProjectTools = new ArrayList<>();
    private ToolAdapter mAdapter;
    private AlertDialog mBrowseDialog;

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
//        mGetProjectTools.add(new ToolItem(getResources().getString(R.string.browse_projects), getResources().getString(R.string.browse_projects_description), R.drawable.ic_download, new ToolItem.ToolAction() {
//            @Override
//            public void run() {
//                mBrowseDialog = new AlertDialog.Builder(GetMoreProjectsActivity.this)
//                        .setTitle(R.string.browse_projects)
//                        .setMessage(R.string.use_internet_confirmation)
//                        .setIcon(R.drawable.ic_download_small)
//                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialogInterface, int i) {
//                                Intent intent = new Intent(GetMoreProjectsActivity.this, ServerLibraryActivity.class);
//                                intent.putExtra(ServerLibraryActivity.ARG_SHOW_NEW, true);
//                                startActivity(intent);
//                            }
//                        })
//                        .setNegativeButton(R.string.no, null)
//                        .show();
//            }
//        }, hasNetwork, getResources().getString(R.string.internet_not_available)));
        mGetProjectTools.add(new ToolItem(getResources().getString(R.string.update_projects), getResources().getString(R.string.update_projects_description), R.drawable.icon_update_cloud_dark, new ToolItem.ToolAction() {
            @Override
            public void run() {
                mBrowseDialog = new AlertDialog.Builder(GetMoreProjectsActivity.this)
                        .setTitle(R.string.update_projects)
                        .setMessage(R.string.use_internet_confirmation)
                        .setIcon(R.drawable.icon_update_cloud_blue)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(GetMoreProjectsActivity.this, ServerLibraryActivity.class);
//                                intent.putExtra(ServerLibraryActivity.ARG_SHOW_UPDATES, true);
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
            }
        }, hasNetwork, getResources().getString(R.string.internet_not_available)));
        mGetProjectTools.add(new ToolItem(getResources().getString(R.string.transfer_from_device), getResources().getString(R.string.transfer_from_device_description), R.drawable.icon_update_nearby_dark, new ToolItem.ToolAction() {
            @Override
            public void run() {
                // TODO: we need to update the p2p sharing to support requesting just the source.
                Intent intent = new Intent(GetMoreProjectsActivity.this, DeviceToDeviceActivity.class);
                Bundle extras = new Bundle();
                extras.putBoolean("startAsServer", false);
                extras.putBoolean("browseSourceProjects", true); // TODO: this is not implemented
                intent.putExtras(extras);
                startActivity(intent);
            }
        }, hasNetwork, getResources().getString(R.string.internet_not_available)));
        mGetProjectTools.add(new ToolItem(getResources().getString(R.string.import_from_storage), getResources().getString(R.string.import_from_storage_description), R.drawable.icon_update_storage_dark, new ToolItem.ToolAction() {
            @Override
            public void run() {
                // TODO: This is the same as for the Sharing activity though we should package it all up into a single method call.
            }
        }, false, "Not implimented"));
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        if(mBrowseDialog != null) {
            mBrowseDialog.dismiss();
        }
        super.onDestroy();
    }
}
