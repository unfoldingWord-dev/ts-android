package com.door43.translationstudio;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.content.FileProvider;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.SharingAdapter;
import com.door43.translationstudio.util.SharingToolItem;
import com.door43.translationstudio.util.StorageUtils;
import com.door43.translationstudio.util.TranslatorBaseActivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


public class SharingActivity extends TranslatorBaseActivity {
    private ArrayList<SharingToolItem> mSharingTools = new ArrayList<SharingToolItem>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sharing);

        final File internalDestDir = new File(getCacheDir(), "sharing/");
        final Project p = app().getSharedProjectManager().getSelectedProject();
        // NOTE: we check again in the threads just in case they removed the card while this activity was open
        StorageUtils.StorageInfo removeableMedia = StorageUtils.getRemoveableMediaDevice();
        internalDestDir.mkdirs();

        // define sharing tools
        mSharingTools.add(new SharingToolItem("Export to App", R.drawable.ic_icon_export_app, new SharingToolItem.SharingToolAction() {
            @Override
            public void run() {
                Thread thread = new Thread() {
                    public void run() {
                        app().showProgressDialog(R.string.exporting_project);
                        try {
                            // export to Doku Wiki
                            String dokuwikiPath = p.export();

                            // tar
                            File dest = new File(internalDestDir, getArchiveName(p));
                            dest.getParentFile().mkdirs();
                            app().tar(dokuwikiPath, dest.getAbsolutePath());

                            // share
                            if(dest.exists() && dest.isFile()) {
                                Uri u = FileProvider.getUriForFile(SharingActivity.this, "com.door43.translationstudio.fileprovider", dest);
                                Intent i = new Intent(Intent.ACTION_SEND);
                                i.setType("application/x-tar");
                                i.putExtra(Intent.EXTRA_STREAM, u);
                                startActivity(Intent.createChooser(i, "Email:"));
                            } else {
                                app().showToastMessage("Project archive not found");
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

        mSharingTools.add(new SharingToolItem("Export to SD", R.drawable.ic_icon_export_sd, new SharingToolItem.SharingToolAction() {
            @Override
            public void run() {
                Thread thread = new Thread() {
                    public void run() {
                        Looper.prepare();
                        app().showProgressDialog(R.string.exporting_project);
                        try {
                            File externalDestDir = null;

                            // export to Doku Wiki
                            String dokuwikiPath = p.export();

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
                            app().tar(dokuwikiPath, dest.getAbsolutePath());
                            if(dest.exists() && dest.isFile()) {
                                // TODO: define a global list of notification id's that we can use.
                                app().showToastMessage(getResources().getString(R.string.project_exported_to) + " " + dest.getParentFile().getAbsolutePath(), Toast.LENGTH_SHORT);
                            } else {
                                app().showToastMessage("Project archive not found");
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

        mSharingTools.add(new SharingToolItem("Import from SD", R.drawable.ic_icon_import_sd, new SharingToolItem.SharingToolAction() {
            @Override
            public void run() {
                Thread thread = new Thread() {
                    public void run() {
                        app().showProgressDialog(R.string.importing_project);
                        // TODO: display file chooser to select file and import the project
                        app().closeProgressDialog();
                    }
                };
                thread.start();
            }
        }, removeableMedia != null, R.string.missing_external_storage));

        mSharingTools.add(new SharingToolItem("Export to nearby device", R.drawable.ic_icon_export_nearby, new SharingToolItem.SharingToolAction() {
            @Override
            public void run() {
                Thread thread = new Thread() {
                    public void run() {
                        app().showProgressDialog(R.string.exporting_project);
                        try {
                            // tar
                            File dest = new File(internalDestDir, getArchiveName(p));
                            app().tar(p.getRepositoryPath(), dest.getAbsolutePath());
                            if(dest.exists() && dest.isFile()) {
                                // TODO: serve the archive to listening devices
                                app().showToastMessage("archive is ready");
                            } else {
                                app().showToastMessage("Project archive not found");
                            }
                        } catch(IOException e) {
                            app().showException(e);
                        }
                        app().closeProgressDialog();
                    }
                };
                thread.start();
            }
        }));

        mSharingTools.add(new SharingToolItem("Import from nearby device", R.drawable.ic_icon_import_nearby, new SharingToolItem.SharingToolAction() {
            @Override
            public void run() {
                Thread thread = new Thread() {
                    public void run() {
                        app().showProgressDialog(R.string.importing_project);
                        // TODO: watch for listening devices and download the shared project
                        app().closeProgressDialog();
                    }
                };
                thread.start();
            }
        }));

        // hook up list view
        ListView list = (ListView)findViewById(R.id.sharingListView);
        SharingAdapter adapter = new SharingAdapter(mSharingTools, this);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(mSharingTools.size() > i && i >= 0) {
                    SharingToolItem tool = mSharingTools.get(i);
                    // execute the sharing action
                    if(tool.isEnabled()) {
                        tool.getAction().run();
                    } else {
                        app().showToastMessage(tool.getDisabledNotice());
                    }
                }
            }
        });
    }

    /**
     * Generates the archive name for the project
     * @param p
     * @return
     */
    private String getArchiveName(Project p) {
        return p.getGlobalProjectId()+"-"+p.getId()+"-"+p.getSelectedTargetLanguage().getId()+"_"+p.getLocalTranslationVersion()+".tar";
    }
}
