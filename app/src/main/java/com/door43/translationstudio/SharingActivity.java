package com.door43.translationstudio;

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
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.ProjectManager;
import com.door43.translationstudio.util.FileUtilities;
import com.door43.translationstudio.util.MainContext;
import com.door43.translationstudio.util.SharingAdapter;
import com.door43.translationstudio.util.SharingToolItem;
import com.door43.translationstudio.util.StorageUtils;
import com.door43.translationstudio.util.TranslatorBaseActivity;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


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
        if(p == null) finish();

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
                mSharingTools.add(new SharingToolItem(R.string.export_to_app, descriptionResource, R.drawable.ic_icon_export_app, new SharingToolItem.SharingToolAction() {
                    @Override
                    public void run() {
                        Thread thread = new Thread() {
                            public void run() {
                                app().showProgressDialog(R.string.exporting_project);
                                try {
                                    String  sourcePath = p.getRepositoryPath();
                                    if(exportAsDokuwiki) {
                                        // export to Doku Wiki
                                        sourcePath = p.export();
                                    }

                                    // zip
                                    File dest = new File(internalDestDir, getArchiveName(p));
                                    dest.getParentFile().mkdirs();
                                    app().zip(sourcePath, dest.getAbsolutePath());

                                    // share
                                    if(dest.exists() && dest.isFile()) {
                                        Uri u = FileProvider.getUriForFile(SharingActivity.this, "com.door43.translationstudio.fileprovider", dest);
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

                mSharingTools.add(new SharingToolItem(R.string.export_to_sd, descriptionResource, R.drawable.ic_icon_export_sd, new SharingToolItem.SharingToolAction() {
                    @Override
                    public void run() {
                        Thread thread = new Thread() {
                            public void run() {
                                Looper.prepare();
                                app().showProgressDialog(R.string.exporting_project);
                                try {
                                    File externalDestDir = null;
                                    String sourcePath = p.getRepositoryPath();

                                    if(exportAsDokuwiki) {
                                        // export to Doku Wiki
                                        sourcePath = p.export();
                                    }

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

                                    // tar
                                    File dest = new File(externalDestDir, getArchiveName(p));
                                    dest.getParentFile().mkdirs();
                                    app().zip(sourcePath, dest.getAbsolutePath());
                                    if(dest.exists() && dest.isFile()) {
                                        // TODO: define a global list of notification id's that we can use.
                                        app().showToastMessage(String.format(getResources().getString(R.string.project_exported_to), dest.getParentFile().getAbsolutePath()), Toast.LENGTH_SHORT);
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

                // TODO: for now this is disabled until we get around to finishing the import. This is important to have so that 1.x exports can be loaded into 2.x
//                if(exportAsProject) {
                mSharingTools.add(new SharingToolItem(R.string.import_from_sd, descriptionResource, R.drawable.ic_icon_import_sd, new SharingToolItem.SharingToolAction() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(me, FileExplorerActivity.class);
                        startActivityForResult(intent, exportAsProject ? IMPORT_PROJECT_FROM_SD_REQUEST : IMPORT_DOKUWIKI_FROM_SD_REQUEST);
                    }
                }, removeableMedia != null, R.string.missing_external_storage));
//                }


                mSharingTools.add(new SharingToolItem(R.string.export_to_device, descriptionResource, R.drawable.ic_icon_export_nearby, new SharingToolItem.SharingToolAction() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(me, DeviceToDeviceActivity.class);
                        startActivity(intent);
//                        Thread thread = new Thread() {
//                            public void run() {
//                                app().showProgressDialog(R.string.exporting_project);
//                                try {
//                                    // tar
//                                    File dest = new File(internalDestDir, getArchiveName(p));
//                                    app().tar(p.getRepositoryPath(), dest.getAbsolutePath());
//                                    if (dest.exists() && dest.isFile()) {
//                                        // TODO: serve the archive to listening devices
//                                        app().showToastMessage("archive is ready");
//                                    } else {
//                                        app().showToastMessage("Project archive not found");
//                                    }
//                                } catch (IOException e) {
//                                    app().showException(e);
//                                }
//                                app().closeProgressDialog();
//                            }
//                        };
//                        thread.start();
                    }
                }));



//            mSharingTools.add(new SharingToolItem("Import from nearby device", R.drawable.ic_icon_import_nearby, new SharingToolItem.SharingToolAction() {
//                @Override
//                public void run() {
//                    Thread thread = new Thread() {
//                        public void run() {
//                            app().showProgressDialog(R.string.importing_project);
//                            // TODO: watch for listening devices and download the shared project
//                            app().closeProgressDialog();
//                        }
//                    };
//                    thread.start();
//                }
//            }));

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

    /**
     * Generates the archive name for the project
     * @param p
     * @return
     */
    private String getArchiveName(Project p) {
        String exportFormat = MainContext.getContext().getUserPreferences().getString(SettingsActivity.KEY_PREF_EXPORT_FORMAT, MainContext.getContext().getResources().getString(R.string.pref_default_export_format));
        String name = p.getGlobalProjectId()+"-"+p.getId()+"-"+p.getSelectedTargetLanguage().getId();
        if(exportFormat.equals("dokuwiki")) {
            name = name + "_" + exportFormat;
        }
        SimpleDateFormat s = new SimpleDateFormat("ddMMyyyyhhmmss");
        String timestamp = s.format(new Date());
        return name + "_" + timestamp + ".zip";
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
            // import translation studio project from the sd card
            if(data != null) {
                final File archiveFile = new File(data.getExtras().getString("path"));
                if(archiveFile.exists() && archiveFile.isFile()) {

                    // thread to prepare import
                    Runnable prepareImport = new Runnable() {
                        public void run() {
                            app().showProgressDialog(R.string.importing_project);

                            // place extracted archive into timestamped directory to prevent archives with no folder structure from throwing files everywhere
                            SimpleDateFormat s = new SimpleDateFormat("ddMMyyyyhhmmss");
                            String timestamp = s.format(new Date());
                            File exportDir = new File(getCacheDir() + "/" + getResources().getString(R.string.imported_projects_dir) + "/" + timestamp);

                            // TODO: validate that file is a zip

                            // extract
                            try {
                                app().unzip(archiveFile.getAbsolutePath(), exportDir.getAbsolutePath());
                                File[] files = exportDir.listFiles(new FilenameFilter() {
                                    @Override
                                    public boolean accept(File file, String s) {
                                        return Project.validateProjectArchiveName(s);
                                    }
                                });
                                if(files.length == 1) {
                                    // TODO: it would be nice if we could double check with the user before running the import.
                                    if(Project.importProject(files[0])) {
                                        app().showToastMessage(R.string.success);
                                    } else {
                                        // failed to import translation
                                        app().showToastMessage(R.string.translation_import_failed);
                                    }
                                } else {
                                    app().showToastMessage(R.string.malformed_translation_archive);
                                }
                            } catch (IOException e) {
                                app().showException(e);
                            }

                            // clean up
                            if(exportDir.exists()) {
                                FileUtilities.deleteRecursive(exportDir);
                            }

                            app().closeProgressDialog();
                        }
                    };

                    // begin the import
                    new Thread(prepareImport).start();
                } else {
                    app().showToastMessage(R.string.missing_file);
                }
            }
        } else if(requestCode == IMPORT_DOKUWIKI_FROM_SD_REQUEST) {
            // Import DokuWiki files
            if(data != null) {
                final File file = new File(data.getExtras().getString("path"));
                if(file.exists() && file.isFile()) {
                    final ProjectManager pm = app().getSharedProjectManager();
//                    Project p;
//                    if(pm.numProjects() > 1) {
//                        // TODO: display a dialog where the user can choose which project to import to.
//                        app().showToastMessage("Doku Wiki import is not configured for import with multiple projects yet.");
//                        return;
//                    } else {
//                        p = pm.getProject(0);
//                    }

//                    final Project project = p;

                    // thread to prepare import
                    Runnable prepareImport = new Runnable() {
                        public void run() {
                            app().showProgressDialog(R.string.importing_project);
                            if(pm.importTranslation(file)) {
                                app().showToastMessage(R.string.success);
                            } else {
                                app().showToastMessage(R.string.translation_import_failed);
                            }
                            app().closeProgressDialog();
                        }
                    };

                    // begin the import
                    new Thread(prepareImport).start();
                } else {
                    app().showToastMessage(R.string.missing_file);
                }
            }
        }
    }
}
