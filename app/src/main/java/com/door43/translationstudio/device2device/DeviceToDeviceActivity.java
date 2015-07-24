package com.door43.translationstudio.device2device;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.dialogs.ChooseProjectLanguagesToImportDialog;
import com.door43.translationstudio.dialogs.ChooseProjectToImportDialog;
import com.door43.translationstudio.dialogs.ProjectTranslationImportApprovalDialog;
import com.door43.translationstudio.events.ChoseProjectLanguagesToImportEvent;
import com.door43.translationstudio.events.ChoseProjectToImportEvent;
import com.door43.translationstudio.network.Client;
import com.door43.translationstudio.network.Connection;
import com.door43.translationstudio.network.Peer;
import com.door43.translationstudio.network.Service;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Sharing;
import com.door43.translationstudio.projects.PseudoProject;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.projects.imports.ProjectImport;
import com.door43.translationstudio.service.BroadcastListenerService;
import com.door43.translationstudio.service.BroadcastService;
import com.door43.translationstudio.service.ExportingService;
import com.door43.translationstudio.service.ImportingService;
import com.door43.util.ListMap;
import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.RSAEncryption;
import com.door43.util.StringUtilities;
import com.door43.translationstudio.util.TranslatorBaseActivity;
import com.squareup.otto.Subscribe;
import com.tozny.crypto.android.AesCbcWithIntegrity;

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
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.tozny.crypto.android.AesCbcWithIntegrity.decryptString;
import static com.tozny.crypto.android.AesCbcWithIntegrity.encrypt;
import static com.tozny.crypto.android.AesCbcWithIntegrity.generateKey;
import static com.tozny.crypto.android.AesCbcWithIntegrity.keyString;
import static com.tozny.crypto.android.AesCbcWithIntegrity.keys;

public class DeviceToDeviceActivity extends TranslatorBaseActivity implements ExportingService.Callbacks, ImportingService.Callbacks, BroadcastListenerService.Callbacks {
    private boolean mStartAsServer = false;
    private DevicePeerAdapter mAdapter;
    private ProgressBar mLoadingBar;
    private TextView mLoadingText;
    private static ProgressDialog mProgressDialog;
    private File mPublicKeyFile;
    private File mPrivateKeyFile;
    private static Map<String, DialogFragment> mPeerDialogs = new HashMap<>();
    private static final String SERVICE_NAME = "tS";
    private boolean mShuttingDown = true;
    private static final int PORT_CLIENT_UDP = 9939;
    private ExportingService mExportService;
    private ImportingService mImportService;
    private BroadcastListenerService mBroadcastListenerService;

    private ServiceConnection mExportConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ExportingService.LocalBinder binder = (ExportingService.LocalBinder) service;
            mExportService = binder.getServiceInstance();
            mExportService.registerCallback(DeviceToDeviceActivity.this);
            Logger.i(DeviceToDeviceActivity.class.getName(), "Connected to export service");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mExportService.registerCallback(null);
            Logger.i(DeviceToDeviceActivity.class.getName(), "Disconnected from export service");
            // TODO: notify activity that service was dropped.
        }
    };
    private ServiceConnection mImportConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ImportingService.LocalBinder binder = (ImportingService.LocalBinder) service;
            mImportService = binder.getServiceInstance();
            mImportService.registerCallback(DeviceToDeviceActivity.this);
            Logger.i(DeviceToDeviceActivity.class.getName(), "Connected to import service");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mImportService.registerCallback(null);
            Logger.i(DeviceToDeviceActivity.class.getName(), "Disconnected from import service");
            // TODO: notify activity that service was dropped.
        }
    };
    private ServiceConnection mBroadcastListenerConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BroadcastListenerService.LocalBinder binder = (BroadcastListenerService.LocalBinder) service;
            mBroadcastListenerService = binder.getServiceInstance();
            mBroadcastListenerService.registerCallback(DeviceToDeviceActivity.this);
            Logger.i(DeviceToDeviceActivity.class.getName(), "Connected to broadcast listener service");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBroadcastListenerService.registerCallback(null);
            Logger.i(DeviceToDeviceActivity.class.getName(), "Disconnected from broadcast listener service");
            // TODO: notify activity that service was dropped.
        }
    };
    private Intent exportServiceIntent;
    private Intent broadcastServiceIntent;
    private Intent importServiceIntent;
    private Intent broadcastListenerServiceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_to_device);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mPublicKeyFile = new File(getFilesDir(), getResources().getString(R.string.p2p_keys_dir) + "/id_rsa.pub");
        mPrivateKeyFile = new File(getFilesDir(), getResources().getString(R.string.p2p_keys_dir) + "/id_rsa");
        mPublicKeyFile.getParentFile().mkdirs();

        mStartAsServer = getIntent().getBooleanExtra("startAsServer", false);

        if(mStartAsServer) {
            setTitle(R.string.export_to_device);
            exportServiceIntent = new Intent(this, ExportingService.class);
            broadcastServiceIntent = new Intent(this, BroadcastService.class);

            // begin export service
            if(!ExportingService.isRunning()) {
                try {
                    generateSessionKeys();
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "Failed to generate the session keys", e);
                    finish();
                }
                try {
                    exportServiceIntent.putExtra(ExportingService.PARAM_PRIVATE_KEY, getPrivateKey());
                    exportServiceIntent.putExtra(ExportingService.PARAM_PUBLIC_KEY, getPublicKeyString());
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "Failed to retreive the encryption keys", e);
                    finish();
                }
                startService(exportServiceIntent);
            }
            bindService(exportServiceIntent, mExportConnection, Context.BIND_AUTO_CREATE);
        } else {
            setTitle(R.string.import_from_device);
            importServiceIntent = new Intent(this, ImportingService.class);
            broadcastListenerServiceIntent = new Intent(this, BroadcastListenerService.class);

            // begin import service
            if(!ImportingService.isRunning()) {
                try {
                    generateSessionKeys();
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "Failed to generate the session keys", e);
                    finish();
                }
                try {
                    importServiceIntent.putExtra(ExportingService.PARAM_PRIVATE_KEY, getPrivateKey());
                    importServiceIntent.putExtra(ExportingService.PARAM_PUBLIC_KEY, getPublicKeyString());
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "Failed to retreive the encryption keys", e);
                    finish();
                }
                startService(importServiceIntent);
            }
        }

        if(mProgressDialog == null) mProgressDialog = new ProgressDialog(this);

        // set up the ui
        final Handler handler = new Handler(getMainLooper());
        mLoadingBar = (ProgressBar)findViewById(R.id.loadingBar);
        mLoadingText = (TextView)findViewById(R.id.loadingText);
        ListView peerListView = (ListView)findViewById(R.id.peerListView);
        mAdapter = new DevicePeerAdapter(mStartAsServer, this);
        peerListView.setAdapter(mAdapter);
        peerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(mStartAsServer) {
                    Peer client = mAdapter.getItem(i);
                    if(!client.isConnected()) {
                        mExportService.acceptConnection(client);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                updatePeerList(mExportService.getPeers());
                            }
                        });
                    } else {
                        // TODO: maybe display a popup to disconnect the client.
                    }
                } else {
                    Peer server = mAdapter.getItem(i);
                    if(!server.isConnected()) {
                        // TRICKY: we don't let the client connect again otherwise it may get an encryption exception due to miss-matched keys
                        if(!server.keyStore.getBool(PeerStatusKeys.WAITING)) {
                            // connect to the server, implicitly requesting permission to access it
                            server.keyStore.add(PeerStatusKeys.WAITING, true);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    updatePeerList();
                                }
                            });
                            mImportService.connectToServer(server);
                        }
                    } else {
                        // request a list of projects from the server.
                        // TODO: the response to this request should be cached until the server disconnects.
                        showProgress(getResources().getString(R.string.loading));
                        // Include the suggested language(s) in which the results should be returned (if possible)
                        // This just makes it easier for users to read the results
                        ArrayList<String> preferredLanguages = new ArrayList<>();
                        // device
                        preferredLanguages.add(Locale.getDefault().getLanguage());
                        // current project
                        Project p = AppContext.projectManager().getSelectedProject();
                        if(p != null) {
                            preferredLanguages.add(p.getSelectedSourceLanguage().getId());
                        }
                        // default
                        preferredLanguages.add("en");
                        mImportService.requestProjectList(server, preferredLanguages);
                    }
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        if(mExportService != null) {
            mExportService.registerCallback(null);
        }
        if(mImportService != null) {
            mImportService.registerCallback(null);
        }
        if(mBroadcastListenerService != null) {
            mBroadcastListenerService.registerCallback(null);
        }
        try {
            unbindService(mExportConnection);
        } catch (Exception e) {
            Logger.e(this.getClass().getName(), "Failed to unbind connection to export service", e);
        }
        try {
            unbindService(mImportConnection);
        } catch (Exception e) {
            Logger.e(this.getClass().getName(), "Failed to unbind connection to import service", e);
        }
        if(mShuttingDown) {
            // TODO: eventually we'll allow these services to run in the background.
            if (BroadcastService.isRunning() && broadcastServiceIntent != null) {
                stopService(broadcastServiceIntent);
            }
            if (ExportingService.isRunning() && exportServiceIntent != null) {
                stopService(exportServiceIntent);
            }
            if (BroadcastListenerService.isRunning() && broadcastListenerServiceIntent != null) {
                stopService(broadcastListenerServiceIntent);
            }
            if (ImportingService.isRunning() && importServiceIntent != null) {
                stopService(importServiceIntent);
            }
            mProgressDialog = null;
            mPeerDialogs.clear();
        }
        super.onDestroy();
    }

    @Override
    public boolean isFinishing() {
        // TODO: this is where we should perform shutdown operations
        return super.isFinishing();
    }

    /**
     * Generates new encryption keys to be used durring this session
     */
    public void generateSessionKeys() throws Exception {
        // TODO: this is not throwing exceptiosn like it should. When the directory of the keys does not exist it doesn't throw an exception
        if(!mPrivateKeyFile.exists() || !mPublicKeyFile.exists()) {
            RSAEncryption.generateKeys(mPrivateKeyFile, mPublicKeyFile);
        }
    }

    /**
     * Returns the public key used for this session
     * @return the raw public key string
     * @throws IOException
     */
    public String getPublicKeyString() throws Exception {
        return RSAEncryption.getPublicKeyAsString(RSAEncryption.readPublicKeyFromFile(mPublicKeyFile));
    }

    /**
     * Returns the private key used for this session
     * @return a private key object or null
     * @throws IOException
     */
    public PrivateKey getPrivateKey() throws IOException {
        return RSAEncryption.readPrivateKeyFromFile(mPrivateKeyFile);
    }

    @Deprecated
    public void updatePeerList() {
    }

    /**
     * Updates the peer list on the screen.
     * This should always be ran on the main thread or a handler
     */
    public void updatePeerList(ArrayList<Peer> peers) {
        if(mLoadingBar != null) {
            if(peers.size() == 0) {
                mLoadingBar.setVisibility(View.VISIBLE);
            } else {
                mLoadingBar.setVisibility(View.GONE);
            }
        }
        if(mLoadingText != null) {
            if(peers.size() == 0) {
                mLoadingText.setVisibility(View.VISIBLE);
            } else {
                mLoadingText.setVisibility(View.GONE);
            }
        }
        // update the adapter
        if(mAdapter != null) {
            mAdapter.setPeerList(peers);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_device_to_device, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_share_to_all) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Displays a dialog to choose the project to import
     * @param models an array of projects and sudo projects to choose from
     */
    private void showProjectSelectionDialog(Peer server, Model[] models) {
        app().closeToastMessage();
        if(!isFinishing()) {
            // Create and show the dialog.
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ChooseProjectToImportDialog newFragment = new ChooseProjectToImportDialog();
            mPeerDialogs.put(server.getIpAddress(), newFragment);
            newFragment.setImportDetails(server, models);
            newFragment.show(ft, "dialog");
        }
    }

    /**
     * Triggered when the client chooses a project from the server's project list
     * @param event the event fired
     */
    @Subscribe
    public void onChoseProjectToImport(ChoseProjectToImportEvent event) {
        // TODO: if we do not have this project yet we need to fetch the project image if it exists.
        event.getDialog().dismiss();
        showProjectLanguageSelectionDialog(event.getPeer(), event.getProject());
    }

    /**
     * Triggered when the client chooses the translations they wish to import with the project.
     * @param event the event fired
     */
    @Subscribe
    public void onChoseProjectTranslationsToImport(ChoseProjectLanguagesToImportEvent event) {
        Handler handle = new Handler(getMainLooper());

        showProgress(getResources().getString(R.string.loading));

        // send the request to the server
        Peer server = event.getPeer();

        JSONObject json = new JSONObject();
        try {
            json.put("id", event.getProject().getId());
            // check if we have the source for this project
            Project existingProject = AppContext.projectManager().getProject(event.getProject().getId());
            if(existingProject == null || existingProject.getSelectedSourceLanguage() == null) {
                JSONArray sourceLanguagesJson = new JSONArray();
                sourceLanguagesJson.put(event.getProject().getSelectedSourceLanguage().getId());
                json.put("source_languages", sourceLanguagesJson);
            }
            JSONArray languagesJson = new JSONArray();
            for(Language l:event.getLanguages()) {
                languagesJson.put(l.getId());
            }
            json.put("target_languages", languagesJson);
            mImportService.requestProjectArchive(server, json);
        } catch (final JSONException e) {
            handle.post(new Runnable() {
                @Override
                public void run() {
                    app().showException(e);
                }
            });
        }
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
        mPeerDialogs.put(peer.getIpAddress(), newFragment);
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mShuttingDown = false;
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mShuttingDown = true;
    }

    @Override
    public void onExportServiceReady(int port) {
        // begin broadcasting services
        broadcastServiceIntent.putExtra(BroadcastService.PARAM_BROADCAST_PORT, PORT_CLIENT_UDP);
        broadcastServiceIntent.putExtra(BroadcastService.PARAM_SERVICE_PORT, port);
        broadcastServiceIntent.putExtra(BroadcastService.PARAM_FREQUENCY, 2000);
        broadcastServiceIntent.putExtra(BroadcastService.PARAM_SERVICE_NAME, SERVICE_NAME);
        startService(broadcastServiceIntent);

        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                updatePeerList(mExportService.getPeers());
            }
        });
    }

    @Override
    public void onClientConnectionRequest(Peer peer) {
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                updatePeerList(mExportService.getPeers());
            }
        });
    }

    @Override
    public void onClientConnectionLost(Peer peer) {
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                updatePeerList(mExportService.getPeers());
            }
        });
    }

    @Override
    public void onClientConnectionChanged(Peer peer) {
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                updatePeerList(mExportService.getPeers());
            }
        });
    }

    @Override
    public void onExportServiceError(Throwable e) {
        Logger.e(this.getClass().getName(), "Export service encountered an exception", e);
    }

    @Override
    public void onImportServiceReady() {
        // begin listening for service broadcasts
        broadcastListenerServiceIntent.putExtra(BroadcastListenerService.PARAM_BROADCAST_PORT, PORT_CLIENT_UDP);
        broadcastListenerServiceIntent.putExtra(BroadcastListenerService.PARAM_SERVICE_NAME, SERVICE_NAME);
        startService(broadcastListenerServiceIntent);

        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                updatePeerList(mImportService.getPeers());
            }
        });
    }

    @Override
    public void onServerConnectionLost(Peer peer) {
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                updatePeerList(mImportService.getPeers());
            }
        });
    }

    @Override
    public void onServerConnectionChanged(Peer peer) {
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                updatePeerList(mImportService.getPeers());
            }
        });
    }

    @Override
    public void onImportServiceError(Throwable e) {
        Logger.e(this.getClass().getName(), "Import service encountered an exception", e);
    }

    @Override
    public void onFoundServer(Peer server) {
        // TODO: identify which api to use based on the server version: server.getVersion()
        // This will allow clients to identify what features a server provides.
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                updatePeerList(mBroadcastListenerService.getPeers());
            }
        });
    }

    @Override
    public void onLostServer(Peer server) {
        // close dialogs
        if(mPeerDialogs.containsKey(server.getIpAddress())) {
            DialogFragment dialog = mPeerDialogs.get(server.getIpAddress());
            if(dialog.getActivity() != null) {
                dialog.dismiss();
            }
            mPeerDialogs.remove(server.getIpAddress());
        }

        // reload the list
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                updatePeerList(mBroadcastListenerService.getPeers());
            }
        });
    }
}
