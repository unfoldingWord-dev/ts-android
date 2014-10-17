package com.door43.translationstudio;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.SharingAdapter;
import com.door43.translationstudio.util.SharingToolItem;
import com.door43.translationstudio.util.TranslatorBaseActivity;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class SharingActivity extends TranslatorBaseActivity {
    private ArrayList<SharingToolItem> mSharingTools = new ArrayList<SharingToolItem>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sharing);

        // TODO: internal dest dir should be the cache directory and we should clean them up after the export is complete
        final String internalDestDir = getFilesDir() + "/sharing/export/";
        // TODO: external dest dir should point to sd
        final String externalDestDir = getFilesDir() + "/sharing/export/";
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("ddMMyyyyhhmmss");

        // define sharing tools
        mSharingTools.add(new SharingToolItem("Export to App", R.drawable.ic_icon_export_app, new SharingToolItem.SharingToolAction() {
            @Override
            public void run() {
                Thread thread = new Thread() {
                    public void run() {
                        app().showProgressDialog(R.string.exporting_project);
                        try {
                            Project p = app().getSharedProjectManager().getSelectedProject();
                            // TODO: compile the project as Doku Wiki before archiving

                            String date = simpleDateFormat.format(new Date());
                            String archivePath = internalDestDir+p.getGlobalProjectId()+"-"+p.getId()+"-"+p.getSelectedTargetLanguage().getId()+"_"+date+".tar";
                            File dest = new File(archivePath);
                            dest.getParentFile().mkdirs();
                            app().tar(p.getRepositoryPath(), archivePath);

                            File f = new File(archivePath);
                            if(f.isFile()) {
                                Uri u = FileProvider.getUriForFile(SharingActivity.this, "com.door43.translationstudio.fileprovider", f);
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
                        app().showProgressDialog(R.string.exporting_project);
                        try {
                            Project p = app().getSharedProjectManager().getSelectedProject();
                            // TODO: compile the project as Doku Wiki before archiving to sd

                            String date = simpleDateFormat.format(new Date());
                            String archivePath = externalDestDir+p.getGlobalProjectId()+"-"+p.getId()+"-"+p.getSelectedTargetLanguage().getId()+"_"+date+".tar";
                            File dest = new File(archivePath);
                            dest.getParentFile().mkdirs();
                            app().tar(p.getRepositoryPath(), archivePath);
                            File f = new File(archivePath);
                            if(f.isFile()) {
                                // TODO: display success message
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
        }));
        mSharingTools.add(new SharingToolItem("Export to nearby device", R.drawable.ic_icon_export_nearby, new SharingToolItem.SharingToolAction() {
            @Override
            public void run() {
                Thread thread = new Thread() {
                    public void run() {
                        app().showProgressDialog(R.string.exporting_project);
                        try {
                            Project p = app().getSharedProjectManager().getSelectedProject();
                            String date = simpleDateFormat.format(new Date());
                            String archivePath = externalDestDir+p.getGlobalProjectId()+"-"+p.getId()+"-"+p.getSelectedTargetLanguage().getId()+"_"+date+".tar";
                            File dest = new File(archivePath);
                            dest.getParentFile().mkdirs();
                            app().tar(p.getRepositoryPath(), archivePath);
                            File f = new File(archivePath);
                            if(f.isFile()) {
                                // TODO: serve the archive to listening devices
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
                    tool.getAction().run();
                }
            }
        });
    }
}
