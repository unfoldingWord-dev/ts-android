package com.door43.translationstudio;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.content.FileProvider;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.door43.translationstudio.device2device.DeviceToDeviceActivity;
import com.door43.translationstudio.dialogs.ProjectImportApprovalDialog;
import com.door43.translationstudio.events.ProjectImportApprovalEvent;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.ProjectManager;
import com.door43.translationstudio.projects.imports.ProjectImport;
import com.door43.translationstudio.util.MainContext;
import com.door43.translationstudio.util.SharingAdapter;
import com.door43.translationstudio.util.SharingToolItem;
import com.door43.translationstudio.util.StorageUtils;
import com.door43.translationstudio.util.TranslatorBaseActivity;
import com.squareup.otto.Subscribe;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class SharingActivity extends TranslatorBaseActivity {
    private SharingActivity me = this;
    private ArrayList<SharingToolItem> mSharingTools = new ArrayList<SharingToolItem>();
    private SharingAdapter mAdapter;
    private static int IMPORT_PROJECT_FROM_SD_REQUEST = 0;
    private static int IMPORT_DOKUWIKI_FROM_SD_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sharing);

        // hook up list view
        ListView list = (ListView)findViewById(R.id.sharingListView);
        mAdapter = new SharingAdapter(mSharingTools, this);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (mSharingTools.size() > i && i >= 0) {
                    SharingToolItem tool = mSharingTools.get(i);
                    // execute the sharing action
                    if (tool.isEnabled()) {
                        tool.getAction().run();
                    } else {
                        app().showToastMessage(tool.getDisabledNotice());
                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        final File internalDestDir = new File(getCacheDir(), "sharing/");
        final Project p = app().getSharedProjectManager().getSelectedProject();
        if(p == null) {
            finish();
            return;
        }

        MainContext.getContext().showProgressDialog(R.string.loading);

        // stage and commit changes to the project
        p.commit(new Project.OnCommitComplete() {
            @Override
            public void success() {
                // NOTE: we check again in the threads just in case they removed the card while this activity was open
                StorageUtils.StorageInfo removeableMedia = StorageUtils.getRemoveableMediaDevice();
                internalDestDir.mkdirs();

                // load export format
                String exportFormt = MainContext.getContext().getUserPreferences().getString(SettingsActivity.KEY_PREF_EXPORT_FORMAT, MainContext.getContext().getResources().getString(R.string.pref_default_export_format));
                final boolean exportAsProject = exportFormt.equals("project");
                final boolean exportAsDokuwiki = exportFormt.equals("dokuwiki");

                int descriptionResource = 0;
                if(exportAsProject) {
                    descriptionResource = R.string.export_as_project;
                } else if(exportAsDokuwiki) {
                    descriptionResource = R.string.export_as_dokuwiki;
                }

                mSharingTools.clear();

                // define sharing tools
                mSharingTools.add(new SharingToolItem(getResources().getString(R.string.export_to_app), getResources().getString(descriptionResource), R.drawable.ic_icon_export_app, new SharingToolItem.SharingToolAction() {
                    @Override
                    public void run() {
                        Thread thread = new Thread() {
                            public void run() {
                                app().showProgressDialog(R.string.exporting_project);
                                try {
                                    String  archivePath;

                                    if(exportAsDokuwiki) {
                                        archivePath = p.exportDW();
                                    } else {
                                        archivePath = p.exportProject();
                                    }
                                    File archiveFile = new File(archivePath);
                                    File output = new File(internalDestDir, archiveFile.getName());

                                    // copy exported archive to the sharing directory
                                    FileUtils.copyFile(archiveFile, output);

                                    // share
                                    if(output.exists() && output.isFile()) {
                                        Uri u = FileProvider.getUriForFile(SharingActivity.this, "com.door43.translationstudio.fileprovider", output);
                                        Intent i = new Intent(Intent.ACTION_SEND);
                                        i.setType("application/zip");
                                        i.putExtra(Intent.EXTRA_STREAM, u);
                                        startActivity(Intent.createChooser(i, "Email:"));
                                    } else {
                                        app().showToastMessage(R.string.project_archive_missing);
                                    }
                                } catch (IOException e) {
                                    app().showException(e);
                                }
                                app().closeProgressDialog();
                            }
                        };
                        thread.start();
                    }
                }));

                mSharingTools.add(new SharingToolItem(getResources().getString(R.string.export_to_sd), getResources().getString(descriptionResource), R.drawable.ic_icon_export_sd, new SharingToolItem.SharingToolAction() {
                    @Override
                    public void run() {
                        Thread thread = new Thread() {
                            public void run() {
                                Looper.prepare();
                                app().showProgressDialog(R.string.exporting_project);
                                try {
                                    File externalDestDir;
                                    String archivePath;

                                    if(exportAsDokuwiki) {
                                        archivePath = p.exportDW();
                                    } else {
                                        archivePath = p.exportProject();
                                    }
                                    File archiveFile = new File(archivePath);

                                    // try to locate the removable sd card
                                    StorageUtils.StorageInfo removeableMediaInfo = StorageUtils.getRemoveableMediaDevice();
                                    if(removeableMediaInfo != null) {
                                        // write files to the removeable sd card
                                        externalDestDir = new File("/storage/" + removeableMediaInfo.getMountName() + "/TranslationStudio/");
                                    } else {
                                        app().showToastMessage(R.string.missing_external_storage);
                                        return;
                                    }
                                    externalDestDir.mkdirs();
                                    File output = new File(externalDestDir, archiveFile.getName());

                                    // copy the exported archive to the sd card
                                    FileUtils.copyFile(archiveFile, output);

                                    if(output.exists() && output.isFile()) {
                                        app().showToastMessage(String.format(getResources().getString(R.string.project_exported_to), output.getParentFile().getAbsolutePath()), Toast.LENGTH_SHORT);
                                    } else {
                                        app().showToastMessage(R.string.project_archive_missing);
                                    }
                                } catch(IOException e) {
                                    app().showException(e);
                                }
                                app().closeProgressDialog();
                            }
                        };
                        thread.start();
                    }
                }, removeableMedia != null , R.string.missing_external_storage));

                mSharingTools.add(new SharingToolItem(getResources().getString(R.string.import_from_sd), "", R.drawable.ic_icon_import_sd, new SharingToolItem.SharingToolAction() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(me, FileExplorerActivity.class);
                        startActivityForResult(intent, IMPORT_PROJECT_FROM_SD_REQUEST);
                    }
                }, removeableMedia != null, R.string.missing_external_storage));

                // p2p sharing requires an active network connection.
                // TODO: Later we may need to adjust this since bluetooth and other services do not require an actual network.
                boolean isNetworkAvailable = app().isNetworkAvailable();

                mSharingTools.add(new SharingToolItem(getResources().getString(R.string.export_to_device), "", R.drawable.ic_icon_export_nearby, new SharingToolItem.SharingToolAction() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(me, DeviceToDeviceActivity.class);
                        Bundle extras = new Bundle();
                        extras.putBoolean("startAsServer", true);
                        intent.putExtras(extras);
                        startActivity(intent);
                    }
                }, isNetworkAvailable, R.string.internet_not_available));

                mSharingTools.add(new SharingToolItem(getResources().getString(R.string.import_from_device), "", R.drawable.ic_icon_import_nearby, new SharingToolItem.SharingToolAction() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(me, DeviceToDeviceActivity.class);
                        Bundle extras = new Bundle();
                        extras.putBoolean("startAsServer", false);
                        intent.putExtras(extras);
                        startActivity(intent);
                    }
                }, isNetworkAvailable, R.string.internet_not_available));

                mAdapter.notifyDataSetChanged();
                MainContext.getContext().closeProgressDialog();
            }

            @Override
            public void error() {
                MainContext.getContext().closeProgressDialog();
                MainContext.getContext().showToastMessage(R.string.project_share_exception);
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sharing_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == IMPORT_PROJECT_FROM_SD_REQUEST) {
            if(data != null) {
                final File file = new File(data.getExtras().getString("path"));
                if(file.exists() && file.isFile()) {
                    String[] name = file.getName().split("\\.");
                    Boolean success = false;
                    if (name[name.length - 1].equals(Project.PROJECT_EXTENSION)) {
                        // import translationStudio project
                        Runnable prepareImport = new Runnable() {
                            public void run() {
                                app().showProgressDialog(R.string.importing_project);
                                ProjectImport[] importRequests = Project.prepareProjectArchiveImport(file);
                                if (importRequests.length > 0) {
                                    boolean importWarnings = false;
                                    for(ProjectImport s:importRequests) {
                                        if(!s.isApproved()) {
                                            importWarnings = true;
                                        }
                                    }
                                    if(importWarnings) {
                                        // review the import status in a dialog
                                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                                        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
                                        if (prev != null) {
                                            ft.remove(prev);
                                        }
                                        ft.addToBackStack(null);
                                        app().closeToastMessage();
                                        ProjectImportApprovalDialog newFragment = new ProjectImportApprovalDialog();
                                        newFragment.setImportRequests(importRequests);
                                        newFragment.show(ft, "dialog");
                                    } else {
                                        // TODO: we should update the status with the results of the import and let the user see an overview of the import process.
                                        for(ProjectImport r:importRequests) {
                                            Project.importProject(r);
                                        }
                                        Project.cleanImport(importRequests);
                                        app().showToastMessage(R.string.success);
                                    }
                                } else {
                                    Project.cleanImport(importRequests);
                                    app().showToastMessage(R.string.translation_import_failed);
                                }

                                app().closeProgressDialog();
                            }
                        };
                        new Thread(prepareImport).start();
                    } else if(name[name.length - 1].equals("zip")) {
                        // import DokuWiki files
                        final ProjectManager pm = app().getSharedProjectManager();
                        Runnable prepareImport = new Runnable() {
                            public void run() {
                                app().showProgressDialog(R.string.importing_project);
                                if(pm.importTranslationArchive(file)) {
                                    app().showToastMessage(R.string.success);
                                } else {
                                    app().showToastMessage(R.string.translation_import_failed);
                                }
                                app().closeProgressDialog();
                            }
                        };
                        new Thread(prepareImport).start();
                    } else if(name[name.length - 1].equals("txt")) {
                        // import legacy 1.x DokuWiki files
                        final ProjectManager pm = app().getSharedProjectManager();
                        Runnable prepareImport = new Runnable() {
                            public void run() {
                                app().showProgressDialog(R.string.importing_project);
                                if(pm.importLegacyTranslation(file)) {
                                    app().showToastMessage(R.string.success);
                                } else {
                                    app().showToastMessage(R.string.translation_import_failed);
                                }
                                app().closeProgressDialog();
                            }
                        };
                        new Thread(prepareImport).start();
                    }
                } else {
                    app().showToastMessage(R.string.missing_file);
                }
            }
        }
    }

    @Subscribe
    public void onProjectImportApproval(ProjectImportApprovalEvent event) {
        app().showProgressDialog(R.string.loading);
        for(ProjectImport r:event.getImportRequests()) {
            if(r.isApproved()) {
                // TODO: update the status with the result of the import and show the user a report when the imports are finished.
                Project.importProject(r);
            }
        }
        Project.cleanImport(event.getImportRequests());
        app().closeProgressDialog();
    }
}
