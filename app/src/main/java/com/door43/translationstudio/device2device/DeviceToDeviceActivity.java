package com.door43.translationstudio.device2device;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.dialogs.ChooseProjectToImportDialog;
import com.door43.translationstudio.dialogs.ChooseProjectLanguagesToImportDialog;
import com.door43.translationstudio.dialogs.ProjectImportApprovalDialog;
import com.door43.translationstudio.events.ChoseProjectLanguagesToImportEvent;
import com.door43.translationstudio.events.ChoseProjectToImportEvent;
import com.door43.translationstudio.events.ProjectImportApprovalEvent;
import com.door43.translationstudio.network.Client;
import com.door43.translationstudio.network.Connection;
import com.door43.translationstudio.network.Peer;
import com.door43.translationstudio.network.Service;
import com.door43.translationstudio.network.Server;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.projects.SudoProject;
import com.door43.translationstudio.util.MainContext;
import com.door43.translationstudio.util.TranslatorBaseActivity;
import com.squareup.otto.Subscribe;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DeviceToDeviceActivity extends TranslatorBaseActivity {
    private static final String MSG_PROJECT_ARCHIVE = "pa";
    private static final String MSG_OK = "ok";
    private static final String MSG_PROJECT_LIST = "pl";
    private static final String MSG_AUTHORIZATION_ERROR = "ae";
    private static final String MSG_INVALID_REQUEST = "ir";
    private static final String MSG_SERVER_ERROR = "se";
    private boolean mStartAsServer = false;
    private Service mService;
    private DevicePeerAdapter mAdapter;
    private ProgressBar mLoadingBar;
    private TextView mLoadingText;
    private ProgressDialog mProgressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_to_device);
        mProgressDialog = new ProgressDialog(this);

        mStartAsServer = getIntent().getBooleanExtra("startAsServer", false);
        final int clientUDPPort = 9939;
        final Handler handler = new Handler(getMainLooper());

        // set up the threads
        if(mStartAsServer) {
            mService = new Server(DeviceToDeviceActivity.this, clientUDPPort, new Server.OnServerEventListener() {

                @Override
                public void onError(final Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            // TODO: it would be nice to place this in a log that could be submitted to github later on.
                            app().showException(e);
                            finish();
                        }
                    });
                }

                @Override
                public void onFoundClient(Peer client) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            updatePeerList();
                        }
                    });
                }

                @Override
                public void onLostClient(Peer client) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            updatePeerList();
                        }
                    });
                }

                @Override
                public void onMessageReceived(final Peer client, final String message) {
                    onServerReceivedMessage(handler, client, message);
                }
            });
        } else {
            mService = new Client(DeviceToDeviceActivity.this, clientUDPPort, new Client.OnClientEventListener() {
                @Override
                public void onError(final Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            // TODO: it would be nice to place this in a log that could be submitted to github later on.
                            app().showException(e);
                            finish();
                        }
                    });
                }

                @Override
                public void onFoundServer(final Peer server) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            updatePeerList();
                        }
                    });
                }

                @Override
                public void onLostServer(final Peer server) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            updatePeerList();
                        }
                    });
                    // TODO: close any dialogs related to this server.
                }

                @Override
                public void onMessageReceived(final Peer server, final String message) {
                    onClientReceivedMessage(handler, server, message);
                }
            });
        }

        // set up the ui
        mLoadingBar = (ProgressBar)findViewById(R.id.loadingBar);
        mLoadingText = (TextView)findViewById(R.id.loadingText);
        TextView titleText = (TextView)findViewById(R.id.titleText);
        if(mStartAsServer) {
            titleText.setText(R.string.export_to_device);
        } else {
            titleText.setText(R.string.import_from_device);
        }
        ListView peerListView = (ListView)findViewById(R.id.peerListView);
        mAdapter = new DevicePeerAdapter(mService.getPeers(), mStartAsServer, this);
        peerListView.setAdapter(mAdapter);
        peerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(mStartAsServer) {
                    Server s = (Server)mService;
                    Peer client = mAdapter.getItem(i);
                    if(!client.isConnected()) {
                        // let the client know it's connection has been authorized.
                        client.setIsConnected(true);
                        updatePeerList();
                        s.writeTo(client, "ok");
                    } else {
                        // TODO: maybe display a popup to disconnect the client.
                    }
                } else {
                    Client c = (Client)mService;
                    Peer server = mAdapter.getItem(i);
                    if(!server.isConnected()) {
                        // connect to the server, implicitly requesting permission to access it
                        c.connectToServer(mAdapter.getItem(i));
                    } else {
                        // request a list of projects from the server.
                        // TODO: the response to this request should be cached until the server disconnects.
                        // TODO: later we may use a button instead of just clicking on the list item.
                        showProgress(getResources().getString(R.string.loading));

                        // Include the suggested language(s) in which the results should be returned (if possible)
                        // This just makes it easier for users to read the results
                        JSONArray preferredLanguagesJson = new JSONArray();
                        // device language
                        preferredLanguagesJson.put(Locale.getDefault().getLanguage());
                        // current project language
                        Project p = MainContext.getContext().getSharedProjectManager().getSelectedProject();
                        if(p != null) {
                            preferredLanguagesJson.put(p.getSelectedSourceLanguage());
                        }
                        // english as default
                        preferredLanguagesJson.put("en");
                        c.writeTo(server, MSG_PROJECT_LIST + ":" + preferredLanguagesJson.toString());
                    }
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        mService.stop();

    }

    @Override
    public void onResume() {
        super.onResume();
        // This will set up a service on the local network named "tS".
        mService.start("tS");
    }

    /**
     * Splits a string by delimiter into two pieces
     * @param string the string to split
     * @param delimiter
     * @return
     */
    private String[] chunk(String string, String delimiter) {
        if(string == null || string.isEmpty()) {
            return new String[]{"", ""};
        }
        String[] pieces = string.split(delimiter, 2);
        if(pieces.length == 1) {
            pieces = new String[] {string, ""};
        }
        return pieces;
    }

    /**
     * Updates the peer list on the screen.
     * This should always be ran on the main thread or a handler
     */
    public void updatePeerList() {
        // TRICKY: when using this in threads we need to make sure everything has been initialized and not null
        // update the progress bar dispaly
        if(mLoadingBar != null) {
            if(mService.getPeers().size() == 0) {
                mLoadingBar.setVisibility(View.VISIBLE);
            } else {
                mLoadingBar.setVisibility(View.GONE);
            }
        }
        if(mLoadingText != null) {
            if(mService.getPeers().size() == 0) {
                mLoadingText.setVisibility(View.VISIBLE);
            } else {
                mLoadingText.setVisibility(View.GONE);
            }
        }
        // update the adapter
        if(mAdapter != null) {
            mAdapter.setPeerList(mService.getPeers());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_device_to_device, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // TODO: we could have additional menu items to adjust the sharing settings.
        if (id == R.id.action_share_to_all) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Handles messages received by the server
     * @param handle
     * @param client
     * @param message
     */
    private void onServerReceivedMessage(final Handler handle, Peer client, String message) {
        Server server = (Server)mService;

        String[] data = chunk(message, ":");

        // validate client
        if(client.isConnected()) {
            if(data[0].equals(MSG_PROJECT_LIST)) {
                // send the project list to the client

                // read preferred source language (for better readability on the client)
                List<SourceLanguage> preferredSourceLanguages = new ArrayList<SourceLanguage>();
                try {
                    JSONArray preferredLanguagesJson = new JSONArray(data[1]);
                    for(int i = 0; i < preferredLanguagesJson.length(); i ++) {
                        SourceLanguage lang = MainContext.getContext().getSharedProjectManager().getSourceLanguage(preferredLanguagesJson.getString(i));
                        if(lang != null) {
                            preferredSourceLanguages.add(lang);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // locate available projects
                JSONArray projectsJson = new JSONArray();
                Project[] projects = app().getSharedProjectManager().getProjects();
                // TODO: identifying the projects that have changes could be expensive if there are lots of clients and lots of projects. We might want to cache this
                for(Project p:projects) {
                    if(p.isTranslatingGlobal()) {
                        JSONObject json = new JSONObject();
                        try {
                            json.put("id", p.getId());

                            // for better readability we attempt to give the project details in the preferred language of the client
                            SourceLanguage shownLanguage = null;
                            if(preferredSourceLanguages.size() > 0) {
                                for(SourceLanguage prefferedLang:preferredSourceLanguages) {
                                    shownLanguage = p.getSourceLanguage(prefferedLang.getId());
                                    if(shownLanguage != null) {

                                        break;
                                    }
                                }
                            }
                            // use the default language
                            if(shownLanguage == null) {
                                shownLanguage = p.getSelectedSourceLanguage();
                            }

                            // project details
                            JSONObject projectInfoJson = new JSONObject();
                            projectInfoJson.put("name", p.getTitle(shownLanguage));
                            projectInfoJson.put("description", p.getDescription(shownLanguage));
                            // TRICKY: since we are only providing the project details in a single source language we don't need to include the meta id's
                            SudoProject[] sudoProjects = p.getSudoProjects();
                            JSONArray sudoProjectsJson = new JSONArray();
                            for(SudoProject sp:sudoProjects) {
                                sudoProjectsJson.put(sp.getTitle(shownLanguage));
                            }
                            projectInfoJson.put("meta", sudoProjectsJson);
                            json.put("project", projectInfoJson);

                            // project details language
                            JSONObject projectLanguageJson = new JSONObject();
                            projectLanguageJson.put("slug", shownLanguage.getId());
                            projectLanguageJson.put("name", shownLanguage.getName());
                            if(shownLanguage.getDirection() == Language.Direction.RightToLeft) {
                                projectLanguageJson.put("direction", "rtl");
                            } else {
                                projectLanguageJson.put("direction", "ltr");
                            }
                            json.put("language", projectLanguageJson);

                            // available target languages
                            Language[] targetLanguages = p.getActiveTargetLanguages();
                            JSONArray languagesJson = new JSONArray();
                            for(Language l:targetLanguages) {
                                JSONObject langJson = new JSONObject();
                                langJson.put("slug", l.getId());
                                langJson.put("name", l.getName());
                                if(l.getDirection() == Language.Direction.RightToLeft) {
                                    langJson.put("direction", "rtl");
                                } else {
                                    langJson.put("direction", "ltr");
                                }
                                languagesJson.put(langJson);
                            }
                            json.put("target_languages", languagesJson);
                            projectsJson.put(json);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
                server.writeTo(client, MSG_PROJECT_LIST + ":" + projectsJson.toString());
            } else if(data[0].equals(MSG_PROJECT_ARCHIVE)) {
                // send the project archive to the client
                JSONObject json;
                try {
                    json = new JSONObject(data[1]);
                } catch (final JSONException e) {
                    e.printStackTrace();
                    server.writeTo(client, MSG_INVALID_REQUEST);
                    return;
                }

                // load data
                if(json.has("id") && json.has("target_languages")) {
                    try {
                        String projectId = json.getString("id");
                        final Project p = app().getSharedProjectManager().getProject(projectId);
                        // validate project
                        if(p != null) {
                            // validate requested target languages
                            Language[] activeLanguages = p.getActiveTargetLanguages();
                            JSONArray languagesJson = json.getJSONArray("target_languages");
                            final List<Language> requestedTranslations = new ArrayList<Language>();
                            for (int i = 0; i < languagesJson.length(); i++) {
                                String languageId = (String) languagesJson.get(i);
                                for(Language l:activeLanguages) {
                                    if(l.getId().equals(languageId)) {
                                        requestedTranslations.add(l);
                                        break;
                                    }
                                }
                            }
                            if(requestedTranslations.size() > 0) {
                                String path = p.exportProject(requestedTranslations.toArray(new Language[requestedTranslations.size()]));
                                final File archive = new File(path);
                                if(archive.exists()) {
                                    // open a socket to send the project
                                    ServerSocket fileSocket = server.createSenderSocket(new Service.OnSocketEventListener() {
                                        @Override
                                        public void onOpen(Connection connection) {
                                            // send an archive of the current project to the connection
                                            try {
                                                // send the file to the connection
                                                // TODO: display a progress bar when the files are being transferred (on each client list item)
                                                DataOutputStream out = new DataOutputStream(connection.getSocket().getOutputStream());
                                                DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(archive)));
                                                byte[] buffer = new byte[8 * 1024];
                                                int count;
                                                while ((count = in.read(buffer)) > 0)
                                                {
                                                    out.write(buffer, 0, count);
                                                }
                                                out.close();
                                                in.close();
                                            } catch (final IOException e) {
                                                handle.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        app().showException(e);
                                                    }
                                                });
                                                return;
                                            }
                                        }
                                    });
                                    // send details to the client so they can download
                                    JSONObject infoJson = new JSONObject();
                                    infoJson.put("port", fileSocket.getLocalPort());
                                    infoJson.put("name", archive.getName());
                                    infoJson.put("size", archive.getTotalSpace());
                                    server.writeTo(client, MSG_PROJECT_ARCHIVE +":" + infoJson.toString());
                                } else {
                                    // the archive could not be created
                                    server.writeTo(client, MSG_SERVER_ERROR);
                                }
                            } else {
                                // the client should have known better
                                server.writeTo(client, MSG_INVALID_REQUEST);
                            }
                        } else {
                            // the client should have known better
                            server.writeTo(client, MSG_INVALID_REQUEST);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        server.writeTo(client, MSG_INVALID_REQUEST);
                    } catch (IOException e) {
                        e.printStackTrace();
                        server.writeTo(client, MSG_SERVER_ERROR);
                    }
                } else {
                    server.writeTo(client, MSG_INVALID_REQUEST);
                }
            }
        } else {
            // the client is not authorized
            server.writeTo(client, MSG_AUTHORIZATION_ERROR);
        }
    }

    /**
     * Handles messages received by the client
     * @param handle
     * @param server
     * @param message
     */
    private void onClientReceivedMessage(final Handler handle, final Peer server, String message) {
        Client client = (Client)mService;

        String[] data = chunk(message, ":");
        if(data[0].equals(MSG_PROJECT_ARCHIVE)) {

            // load data
            JSONObject infoJson = null;
            try {
                infoJson = new JSONObject(data[1]);
            } catch (JSONException e) {
                e.printStackTrace();
                app().showException(e);
                return;
            }

            if(infoJson.has("port") && infoJson.has("size") && infoJson.has("name")) {
                int port;
                final long size;
                final String name;
                try {
                    port = infoJson.getInt("port");
                    size = infoJson.getLong("size");
                    name = infoJson.getString("name");
                } catch (JSONException e) {
                    e.printStackTrace();
                    app().showException(e);
                    return;
                }
                // the server is sending a project archive
                client.createReceiverSocket(server, port, new Service.OnSocketEventListener() {
                    @Override
                    public void onOpen(Connection connection) {
                        connection.setOnCloseListener(new Connection.OnCloseListener() {
                            @Override
                            public void onClose() {
                                // the socket has closed
                                handle.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        app().showToastMessage("file socket closed");
                                    }
                                });
                            }
                        });

                        // download archive
                        showProgress(getResources().getString(R.string.downloading));
                        try {
                            DataInputStream in = new DataInputStream(connection.getSocket().getInputStream());
                            File file = new File(getExternalCacheDir() + "transferred/" + name);
                            file.getParentFile().mkdirs();
                            file.createNewFile();
                            OutputStream out = new FileOutputStream(file.getAbsolutePath());
                            byte[] buffer = new byte[8 * 1024];
                            int count;
                            // TODO: display a progress bar. We will probably need to send the size of the file with the port #
                            while ((count = in.read(buffer)) > 0) {
                                out.write(buffer, 0, count);
                            }
                            out.close();
                            in.close();

                            // import the project
                            List<Project.ImportRequest> importStatuses = Project.prepareProjectArchiveImport(file);
                            if (importStatuses.size() > 0) {
                                boolean importWarnings = false;
                                for(Project.ImportRequest s:importStatuses) {
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
                                    newFragment.setImportRequests(importStatuses);
                                    newFragment.show(ft, "dialog");
                                } else {
                                    // TODO: we should update the status with the results of the import and let the user see an overview of the import process.
                                    for(Project.ImportRequest r:importStatuses) {
                                        Project.importProject(r);
                                        Project.cleanImport(r);
                                    }
                                    app().showToastMessage(R.string.success);
                                }
                                hideProgress();
                            } else {
                                // failed to import translation
                                handle.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        app().showToastMessage(R.string.translation_import_failed);
                                    }
                                });
                            }
                        } catch (final IOException e) {
                            handle.post(new Runnable() {
                                @Override
                                public void run() {
                                    app().showException(e);
                                }
                            });
                        }
                    }
                });
            } else {
                // the server did not give us the expected response.
                app().showToastMessage("invalid response");
            }
        } else if(data[0].equals(MSG_OK)) {
            // we are authorized to access the server
            server.setIsConnected(true);
            handle.post(new Runnable() {
                @Override
                public void run() {
                    updatePeerList();
                }
            });
        } else if(data[0].equals(MSG_PROJECT_LIST)) {
            // the sever gave us the list of available projects for import
            String rawProjectList = data[1];
            final ArrayList<Model> availableProjects = new ArrayList<Model>();

            JSONArray json = null;
            try {
                json = new JSONArray(rawProjectList);
            } catch (final JSONException e) {
                handle.post(new Runnable() {
                    @Override
                    public void run() {
                        app().showException(e);
                    }
                });
                return;
            }

            // load the data
            for(int i=0; i<json.length(); i++) {
                try {
                    JSONObject projectJson = json.getJSONObject(i);
                    if (projectJson.has("id") && projectJson.has("project") && projectJson.has("language") && projectJson.has("target_languages")) {
                        Project p = new Project(projectJson.getString("id"));

                        // source language (just for project info)
                        JSONObject sourceLangJson = projectJson.getJSONObject("language");
                        String sourceLangDirection = sourceLangJson.getString("direction");
                        Language.Direction langDirection;
                        if(sourceLangDirection.toLowerCase().equals("ltr")) {
                            langDirection = Language.Direction.LeftToRight;
                        } else {
                            langDirection = Language.Direction.RightToLeft;
                        }
                        SourceLanguage sourceLanguage = new SourceLanguage(sourceLangJson.getString("slug"), sourceLangJson.getString("name"), langDirection, 0);
                        p.addSourceLanguage(sourceLanguage);
                        p.setSelectedSourceLanguage(sourceLanguage.getId());

                        // project info
                        JSONObject projectInfoJson = projectJson.getJSONObject("project");
                        p.setDefaultTitle(projectInfoJson.getString("name"));
                        if(projectInfoJson.has("description")) {
                            p.setDefaultDescription(projectInfoJson.getString("description"));
                        }

                        // meta (sudo projects)
                        // TRICKY: we are actually getting the meta names instead of the id's since we only receive one translation of the project info
                        if (projectInfoJson.has("meta")) {
                            JSONArray metaJson = projectInfoJson.getJSONArray("meta");
                            SudoProject currentSudoProject = null;
                            for(int j=0; j<metaJson.length(); j++) {
                                // create sudo project out of the meta name
                                SudoProject sp  = new SudoProject(metaJson.getString(j));
                                // link to parent sudo project
                                if(currentSudoProject != null) {
                                    currentSudoProject.addChild(sp);
                                }
                                // add to project
                                p.addSudoProject(sp);
                                currentSudoProject = sp;
                            }
                        }

                        // available translation languages
                        JSONArray languagesJson = projectJson.getJSONArray("target_languages");
                        ArrayList<Language> languages = new ArrayList<Language>();
                        for(int j=0; j<languagesJson.length(); j++) {
                            JSONObject langJson = languagesJson.getJSONObject(j);
                            String languageId = langJson.getString("slug");
                            String languageName = langJson.getString("name");
                            String direction  = langJson.getString("direction");
                            Language.Direction langDir;
                            if(direction.toLowerCase().equals("ltr")) {
                                langDir = Language.Direction.LeftToRight;
                            } else {
                                langDir = Language.Direction.RightToLeft;
                            }
                            Language l = new Language(languageId, languageName, langDir);
                            p.addTargetLanguage(l);
                        }
                        // finish linking the sudo projects together with the project so the menu can be rendered correctly
                        if(p.numSudoProjects() > 0) {
                            p.getSudoProject(p.numSudoProjects() - 1).addChild(p);
                            availableProjects.add(p.getSudoProject(0));
                        } else {
                            availableProjects.add(p);
                        }
                    } else {
                        app().showToastMessage("An invalid response was received from the server");
                    }
                } catch(final JSONException e) {
                    handle.post(new Runnable() {
                        @Override
                        public void run() {
                            app().showException(e);
                        }
                    });
                }
            }

            handle.post(new Runnable() {
                @Override
                public void run() {
                    hideProgress();
                    // TODO: we don't have the meta projects configured quite right. We need to finish that to show meta information in the list.
                    if(availableProjects.size() > 0) {
                        showProjectSelectionDialog(server, availableProjects.toArray(new Model[availableProjects.size()]));
                    } else {
                        // there are no available projects on the server
                        // TODO: eventually we'll want to display the user friendly name of the server.
                        app().showMessageDialog(server.getIpAddress().toString(), getResources().getString(R.string.no_projects_available_on_server));
                    }
                }
            });
        }
    }

    /**
     * Displays a dialog to choose the project to import
     * @param models an array of projects and sudo projects to choose from
     */
    private void showProjectSelectionDialog(Peer server, Model[] models) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        app().closeToastMessage();
        // Create and show the dialog.
        ChooseProjectToImportDialog newFragment = new ChooseProjectToImportDialog();
        newFragment.setImportDetails(server, models);
        newFragment.show(ft, "dialog");
    }

    /**
     * Triggered when the client chooses a project from the server's project list
     * @param event
     */
    @Subscribe
    public void onChoseProjectToImport(ChoseProjectToImportEvent event) {
        // TODO: if we do not have this project yet we need to fetch the project image if it exists.
        event.getDialog().dismiss();
        showProjectLanguageSelectionDialog(event.getPeer(), event.getProject());
    }

    /**
     * Triggered when the client chooses the translations they wish to import with the project.
     * @param event
     */
    @Subscribe
    public void onChoseProjectTranslationsToImport(ChoseProjectLanguagesToImportEvent event) {
        Handler handle = new Handler(getMainLooper());

        showProgress(getResources().getString(R.string.loading));

        // send the request to the server
        Client c = (Client)mService;
        Peer server = event.getPeer();

        JSONObject json = new JSONObject();
        try {
            json.put("id", event.getProject().getId());
            JSONArray languagesJson = new JSONArray();
            for(Language l:event.getLanguages()) {
                languagesJson.put(l.getId());
            }
            json.put("target_languages", languagesJson);
            c.writeTo(server, MSG_PROJECT_ARCHIVE+":"+json.toString());
        } catch (final JSONException e) {
            handle.post(new Runnable() {
                @Override
                public void run() {
                    app().showException(e);
                }
            });
        }
    }

    @Subscribe
    public void onProjectImportApproval(ProjectImportApprovalEvent event) {
        showProgress(getResources().getString(R.string.loading));
        for(Project.ImportRequest r:event.getImportRequests()) {
            if(r.isApproved()) {
                // TODO: update the status with the result of the import and show the user a report when the imports are finished.
                Project.importProject(r);
            }
            Project.cleanImport(r);
        }
        hideProgress();
        app().showToastMessage(R.string.success);
    }

    /**
     * Displays a dialog to choose the languages that will be imported with the project.
     * @param p
     */
    private void showProjectLanguageSelectionDialog(Peer peer, Project p) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        app().closeToastMessage();
        // Create and show the dialog.
        ChooseProjectLanguagesToImportDialog newFragment = new ChooseProjectLanguagesToImportDialog();
        newFragment.setImportDetails(peer, p);
        newFragment.show(ft, "dialog");
    }

    /**
     * shows or updates the progress dialog
     * @param message the message to display in the progress dialog.
     */
    private void showProgress(final String message) {
        Handler handle = new Handler(getMainLooper());
        handle.post(new Runnable() {
            @Override
            public void run() {
                mProgressDialog.setMessage(message);
                if(!mProgressDialog.isShowing()) {
                    mProgressDialog.show();
                }
            }
        });
    }

    /**
     * closes the progress dialog
     */
    private void hideProgress() {
        Handler handle = new Handler(getMainLooper());
        handle.post(new Runnable() {
            @Override
            public void run() {
                mProgressDialog.dismiss();
            }
        });
    }
}
