package com.door43.translationstudio.device2device;

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
import com.door43.translationstudio.network.Client;
import com.door43.translationstudio.network.Connection;
import com.door43.translationstudio.network.Peer;
import com.door43.translationstudio.network.Service;
import com.door43.translationstudio.network.Server;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.projects.SudoProject;
import com.door43.translationstudio.util.MainContext;
import com.door43.translationstudio.util.TranslatorBaseActivity;

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

import javax.xml.transform.Source;

public class DeviceToDeviceActivity extends TranslatorBaseActivity {
    private static final String MSG_PROJECT_ARCHIVE = "pa";
    private static final String MSG_OK = "ok";
    private static final String MSG_PROJECT_LIST = "pl";
    private static final String MSG_AUTHORIZATION_ERROR = "ae";
    private boolean mStartAsServer = false;
    private Service mService;
    private DevicePeerAdapter mAdapter;
    private ProgressBar mLoadingBar;
    private TextView mLoadingText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_to_device);

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
                        // TODO: later we may use a button instead of just clicking on the list item.
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                app().showProgressDialog(R.string.loading);
                            }
                        });

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
                            // for better readability we attempt to give the project list in the preferred language of the client
                            SourceLanguage shownLanguage = null;
                            if(preferredSourceLanguages.size() > 0) {
                                for(SourceLanguage prefferedLang:preferredSourceLanguages) {
                                    shownLanguage = p.getSourceLanguage(prefferedLang.getId());
                                    if(shownLanguage != null) {
                                        json.put("name", p.getTitle(shownLanguage));
                                        json.put("description", p.getDescription(shownLanguage));
                                        SudoProject[] sudoProjects = p.getSudoProjects();
                                        JSONArray sudoProjectsJson = new JSONArray();
                                        for(SudoProject sp:sudoProjects) {
                                            sudoProjectsJson.put(sp.getTitle(shownLanguage));
                                        }
                                        json.put("meta", sudoProjectsJson);
                                        break;
                                    }
                                }
                            }
                            // use the default language
                            if(shownLanguage == null) {
                                // use the default language
                                json.put("name", p.getTitle());
                                json.put("description", p.getDescription());
                                SudoProject[] sudoProjects = p.getSudoProjects();
                                JSONArray sudoProjectsJson = new JSONArray();
                                for(SudoProject sp:sudoProjects) {
                                    sudoProjectsJson.put(sp.getTitle());
                                }
                                json.put("meta", sudoProjectsJson);
                            }

                            Language[] targetLanguages = p.getActiveTargetLanguages();
                            JSONArray languagesJson = new JSONArray();
                            for(Language l:targetLanguages) {
                                JSONObject langJson = new JSONObject();
                                langJson.put("id", l.getId());
                                langJson.put("name", l.getName());
                                languagesJson.put(langJson);
                            }
                            json.put("languages", languagesJson);
                            projectsJson.put(json);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
                server.writeTo(client, MSG_PROJECT_LIST + ":" + projectsJson.toString());
            } else if(data[0].equals(MSG_PROJECT_ARCHIVE)) {
                // TODO: right now this is just sending the currently selected project. It needs to send the one requested
                // send the project archive to the client
                String projectId = data[1];
                ServerSocket fileSocket = server.createSenderSocket(new Service.OnSocketEventListener() {
                    @Override
                    public void onOpen(Connection connection) {
                        // send an archive of the current project to the connection
                        Project p = app().getSharedProjectManager().getSelectedProject();
                        if(p != null) {
                            try {
                                String path = p.exportProject();
                                File archive = new File(path);

                                // send the file to the connection
                                // TODO: first we should tell client about the available projects
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
                    }
                });
                // send the port number to the client
                server.writeTo(client, MSG_PROJECT_ARCHIVE +":" + fileSocket.getLocalPort());
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
    private void onClientReceivedMessage(final Handler handle, Peer server, String message) {
        Client client = (Client)mService;

        String[] data = chunk(message, ":");
        if(data[0].equals(MSG_PROJECT_ARCHIVE)) {
            int port = Integer.parseInt(data[1]);
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
                    // TODO: we should really break this try catch up so we can be more specific with reports
                    try {
                        long time = System.currentTimeMillis();
                        DataInputStream in = new DataInputStream(connection.getSocket().getInputStream());
                        File file = new File(getExternalCacheDir() + "transferred/" + time + ".zip");
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
                        if (Project.importProject(file)) {
                            handle.post(new Runnable() {
                                @Override
                                public void run() {
                                    app().showToastMessage(R.string.success);
                                }
                            });
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
            final String projectList = data[1];
            // TODO: display the list of projects to the user.
            handle.post(new Runnable() {
                @Override
                public void run() {
                    app().showToastMessage(projectList);
                }
            });
        }
    }
}
