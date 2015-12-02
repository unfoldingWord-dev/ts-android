package com.door43.translationstudio.device2device;

import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.network.Peer;
import com.door43.translationstudio.newui.BaseActivity;
import com.door43.translationstudio.service.BroadcastListenerService;
import com.door43.translationstudio.service.BroadcastService;
import com.door43.translationstudio.service.PeerStatusKeys;
import com.door43.translationstudio.service.Request;
import com.door43.translationstudio.service.ServerService;
import com.door43.translationstudio.service.ClientService;
import com.door43.util.RSAEncryption;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DeviceToDeviceActivity extends BaseActivity implements ServerService.OnServerEventListener, ClientService.OnClientEventListener, BroadcastListenerService.Callbacks {
    private static final int REFRESH_FREQUENCY = 5000;
    private static final int SERVER_TTL = 5000; // time before a slient server is considered lost
    private boolean mStartAsServer = false;
    private PeerAdapter mAdapter;
    private File mPublicKeyFile;
    private File mPrivateKeyFile;
    private static Map<String, DialogFragment> mPeerDialogs = new HashMap<>();
    private static final String SERVICE_NAME = "tS";
    private static final int PORT_CLIENT_UDP = 9939;
    private ServerService mExportService;
    private ClientService mImportService;
    private BroadcastService mBroadcastService;
    private BroadcastListenerService mBroadcastListenerService;

    private ServiceConnection mExportConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ServerService.LocalBinder binder = (ServerService.LocalBinder) service;
            mExportService = binder.getServiceInstance();
            mExportService.setOnServerEventListener(DeviceToDeviceActivity.this);
            Logger.i(DeviceToDeviceActivity.class.getName(), "Connected to export service");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mExportService.setOnServerEventListener(null);
            Logger.i(DeviceToDeviceActivity.class.getName(), "Disconnected from export service");
            // TODO: notify activity that service was dropped.
        }
    };
    private ServiceConnection mBroadcastConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BroadcastService.LocalBinder binder = (BroadcastService.LocalBinder) service;
            mBroadcastService = binder.getServiceInstance();
            Logger.i(DeviceToDeviceActivity.class.getName(), "Connected to broadcast service");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Logger.i(DeviceToDeviceActivity.class.getName(), "Disconnected from broadcast service");
            // TODO: notify activity that service was dropped.
        }
    };
    private ServiceConnection mImportConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ClientService.LocalBinder binder = (ClientService.LocalBinder) service;
            mImportService = binder.getServiceInstance();
            mImportService.setOnClientEventListener(DeviceToDeviceActivity.this);
            Logger.i(DeviceToDeviceActivity.class.getName(), "Connected to import service");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mImportService.setOnClientEventListener(null);
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
            Handler hand = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    updatePeerList(mBroadcastListenerService.getPeers());
                }
            });
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
//    private ProjectTranslationImportApprovalDialog mImportDialog = null;
    private LinearLayout mLoadingLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_to_device);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mPublicKeyFile = new File(getFilesDir(), getResources().getString(R.string.p2p_keys_dir) + "/id_rsa.pub");
        mPrivateKeyFile = new File(getFilesDir(), getResources().getString(R.string.p2p_keys_dir) + "/id_rsa");
        mPublicKeyFile.getParentFile().mkdirs();

        mStartAsServer = getIntent().getBooleanExtra("startAsServer", false);

//        if(mProgressDialog == null) mProgressDialog = new ProgressDialog(this);

        // set up the ui
        final Handler handler = new Handler(getMainLooper());
        mLoadingLayout = (LinearLayout)findViewById(R.id.loadingLayout);
        ListView peerListView = (ListView)findViewById(R.id.peerListView);
        mAdapter = new PeerAdapter(this);
        peerListView.setAdapter(mAdapter);
        peerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(mStartAsServer) {
                    Peer client = mAdapter.getItem(i);
                    if(!client.isSecure()) {
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
                    if(!server.isSecure()) {
                        // TRICKY: we don't let the client connect again otherwise it may get an encryption exception due to miss-matched keys
                        if(!server.keyStore.getBool(PeerStatusKeys.WAITING)) {
                            // connect to the server, implicitly requesting permission to access it
                            server.keyStore.add(PeerStatusKeys.WAITING, true);
                            updatePeerList(mBroadcastListenerService.getPeers());
                            mImportService.connectToServer(server);
                        }
                    } else {
                        // request a list of projects from the server.
                        showProgress(getResources().getString(R.string.loading));
                        // Include the suggested language(s) in which the results should be returned (if possible)
                        // This just makes it easier for users to read the results
                        ArrayList<String> preferredLanguages = new ArrayList<>();
                        // device
                        preferredLanguages.add(Locale.getDefault().getLanguage());
                        // current project
//                        Project p = null;//AppContext.projectManager().getSelectedProject();
//                        if(p != null) {
//                            preferredLanguages.add(p.getSelectedSourceLanguage().getId());
//                        }
                        // default
                        preferredLanguages.add("en");
                        mImportService.requestProjectList(server, preferredLanguages);
                    }
                }
            }
        });
    }

    public void onStart() {
        super.onStart();
        // start and/or connect to services
        if(mStartAsServer) {
            setTitle(R.string.export_to_device);
            exportServiceIntent = new Intent(this, ServerService.class);
            broadcastServiceIntent = new Intent(this, BroadcastService.class);

            // begin export service
            if(!ServerService.isRunning()) {
                try {
                    generateSessionKeys();
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "Failed to generate the session keys", e);
                    finish();
                }
                try {
                    exportServiceIntent.putExtra(ServerService.PARAM_PRIVATE_KEY, RSAEncryption.readPrivateKeyFromFile(mPrivateKeyFile));
                    exportServiceIntent.putExtra(ServerService.PARAM_PUBLIC_KEY, RSAEncryption.getPublicKeyAsString(RSAEncryption.readPublicKeyFromFile(mPublicKeyFile)));
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "Failed to retreive the encryption keys", e);
                    finish();
                }
                Logger.i(this.getClass().getName(), "Starting export service");
                startService(exportServiceIntent);
            }
            bindService(exportServiceIntent, mExportConnection, Context.BIND_AUTO_CREATE);
        } else {
            setTitle(R.string.import_from_friend);
            importServiceIntent = new Intent(this, ClientService.class);
            broadcastListenerServiceIntent = new Intent(this, BroadcastListenerService.class);

            // begin import service
            if(!ClientService.isRunning()) {
                try {
                    generateSessionKeys();
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "Failed to generate the session keys", e);
                    finish();
                }
                try {
                    importServiceIntent.putExtra(ServerService.PARAM_PRIVATE_KEY, RSAEncryption.readPrivateKeyFromFile(mPrivateKeyFile));
                    importServiceIntent.putExtra(ServerService.PARAM_PUBLIC_KEY, RSAEncryption.getPublicKeyAsString(RSAEncryption.readPublicKeyFromFile(mPublicKeyFile)));
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "Failed to retreive the encryption keys", e);
                    finish();
                }
                Logger.i(this.getClass().getName(), "Starting import service");
                startService(importServiceIntent);
            }
            bindService(importServiceIntent, mImportConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onStop() {
        // disconnect from services
        if(mExportService != null) {
            mExportService.setOnServerEventListener(null);
            try {
                unbindService(mExportConnection);
                Logger.i(this.getClass().getName(), "Disconnected from export service");
            } catch (Exception e) {
                Logger.w(this.getClass().getName(), "Failed to unbind connection to export service", e);
            }
        }
        if(mImportService != null) {
            mImportService.setOnClientEventListener(null);
            try {
                unbindService(mImportConnection);
                Logger.i(this.getClass().getName(), "Disconnected from import service");
            } catch (Exception e) {
                Logger.w(this.getClass().getName(), "Failed to unbind connection to import service", e);
            }
        }
        if(mBroadcastListenerService != null) {
            mBroadcastListenerService.registerCallback(null);
            try {
                unbindService(mBroadcastListenerConnection);
                Logger.i(this.getClass().getName(), "Disconnected from broadcast listener service");
            } catch (Exception e) {
                Logger.w(this.getClass().getName(), "Failed to unbind connection to listener service", e);
            }
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if(isFinishing()) {
            // stop services
            if (BroadcastService.isRunning() && broadcastServiceIntent != null) {
                if(!stopService(broadcastServiceIntent)) {
                    Logger.w(this.getClass().getName(), "Failed to stop service " + BroadcastService.class.getName());
                }
            }
            if (ServerService.isRunning() && exportServiceIntent != null) {
                if(!stopService(exportServiceIntent)) {
                    Logger.w(this.getClass().getName(), "Failed to stop service " + ServerService.class.getName());
                }
            }
            if (BroadcastListenerService.isRunning() && broadcastListenerServiceIntent != null) {
                if(!stopService(broadcastListenerServiceIntent)) {
                    Logger.w(this.getClass().getName(), "Failed to stop service " + BroadcastListenerService.class.getName());
                }
            }
            if (ClientService.isRunning() && importServiceIntent != null) {
                if(!stopService(importServiceIntent)) {
                    Logger.w(this.getClass().getName(), "Failed to stop service " + ClientService.class.getName());
                }
            }
//            mProgressDialog = null;
            mPeerDialogs.clear();
        }
        super.onDestroy();
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
     * Updates the peer list on the screen.
     * This should always be ran on the main thread or a handler
     */
    public void updatePeerList(ArrayList<Peer> peers) {
        if(mLoadingLayout != null) {
            if(peers.size() == 0) {
                mLoadingLayout.setVisibility(View.VISIBLE);
            } else {
                mLoadingLayout.setVisibility(View.GONE);
            }
        }

        // update the adapter
        if(mAdapter != null) {
            mAdapter.setPeers(peers);
        }
    }

    /**
     * Displays a dialog to choose the project to import
     * @param models an array of projects and sudo projects to choose from
     */
//    private void showProjectSelectionDialog(Peer server, Model[] models) {
//        AppContext.context().closeToastMessage();
//        if(!isFinishing()) {
//            FragmentTransaction ft = getFragmentManager().beginTransaction();
//            Fragment prev = getFragmentManager().findFragmentByTag("dialog");
//            if (prev != null) {
//                ft.remove(prev);
//            }
//            ft.addToBackStack(null);
//            ChooseProjectToImportDialog newFragment = new ChooseProjectToImportDialog();
//            mPeerDialogs.put(server.getIpAddress(), newFragment);
//            newFragment.setImportDetails(server, models);
//            newFragment.show(ft, "dialog");
//        }
//    }

//    /**
//     * Triggered when the client chooses a project from the server's project list
//     * @param event the event fired
//     */
//    @Subscribe
//    public void onChoseProjectToImport(ChoseProjectToImportEvent event) {
//        // TODO: if we do not have this project yet we need to fetch the project image if it exists.
//        event.getDialog().dismiss();
//        showProjectLanguageSelectionDialog(event.getPeer(), event.getProject());
//    }

//    /**
//     * Triggered when the client chooses the translations they wish to import with the project.
//     * @param event the event fired
//     */
//    @Subscribe
//    public void onChoseProjectTranslationsToImport(ChoseProjectLanguagesToImportEvent event) {
//        Handler handle = new Handler(getMainLooper());
//
//        showProgress(getResources().getString(R.string.loading));
//
//        // send the request to the server
//        Peer server = event.getPeer();
//
//        JSONObject json = new JSONObject();
//        try {
//            json.put("id", event.getProject().getId());
//            // check if we have the source for this project
//            Project existingProject = null;//AppContext.projectManager().getProject(event.getProject().getId());
//            if(existingProject == null || existingProject.getSelectedSourceLanguage() == null) {
//                JSONArray sourceLanguagesJson = new JSONArray();
//                sourceLanguagesJson.put(event.getProject().getSelectedSourceLanguage().getId());
//                json.put("source_languages", sourceLanguagesJson);
//            }
//            JSONArray languagesJson = new JSONArray();
//            for(Language l:event.getLanguages()) {
//                languagesJson.put(l.getId());
//            }
//            json.put("target_languages", languagesJson);
//            mImportService.requestProjectArchive(server, json);
//        } catch (final JSONException e) {
//            handle.post(new Runnable() {
//                @Override
//                public void run() {
//                    AppContext.context().showException(e);
//                }
//            });
//        }
//    }

    /**
     * Displays a dialog to choose the languages that will be imported with the project.
     * @param p
     */
//    private void showProjectLanguageSelectionDialog(Peer peer, Project p) {
//        FragmentTransaction ft = getFragmentManager().beginTransaction();
//        AppContext.context().closeToastMessage();
//        // Create and show the dialog.
//        ChooseProjectLanguagesToImportDialog newFragment = new ChooseProjectLanguagesToImportDialog();
//        mPeerDialogs.put(peer.getIpAddress(), newFragment);
//        newFragment.setImportDetails(peer, p);
//        newFragment.show(ft, "dialog");
//    }

    /**
     * shows or updates the progress dialog
     * @param message the message to display in the progress dialog.
     */
    private void showProgress(final String message) {
//        Handler handle = new Handler(getMainLooper());
//        handle.post(new Runnable() {
//            @Override
//            public void run() {
//                mProgressDialog.setMessage(message);
//                if (!mProgressDialog.isShowing()) {
//                    mProgressDialog.show();
//                }
//            }
//        });
    }

    /**
     * closes the progress dialog
     */
    private void hideProgress() {
//        Handler handle = new Handler(getMainLooper());
//        handle.post(new Runnable() {
//            @Override
//            public void run() {
//                mProgressDialog.dismiss();
//            }
//        });
    }

    @Override
    public void onServerServiceReady(int port) {
        // begin broadcasting services
        if(!BroadcastService.isRunning()) {
            broadcastServiceIntent.putExtra(BroadcastService.PARAM_BROADCAST_PORT, PORT_CLIENT_UDP);
            broadcastServiceIntent.putExtra(BroadcastService.PARAM_SERVICE_PORT, port);
            broadcastServiceIntent.putExtra(BroadcastService.PARAM_FREQUENCY, 2000);
//            broadcastServiceIntent.putExtra(BroadcastService.PARAM_SERVICE_NAME, SERVICE_NAME);
            startService(broadcastServiceIntent);
        }
        bindService(broadcastServiceIntent, mBroadcastConnection, Context.BIND_AUTO_CREATE);
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                updatePeerList(mExportService.getPeers());
            }
        });
    }

    @Override
    public void onClientConnected(Peer peer) {
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                updatePeerList(mExportService.getPeers());
            }
        });
    }

    @Override
    public void onClientLost(Peer peer) {
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                updatePeerList(mExportService.getPeers());
            }
        });
    }

    @Override
    public void onClientChanged(Peer peer) {
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                updatePeerList(mExportService.getPeers());
            }
        });
    }

    @Override
    public void onServerServiceError(Throwable e) {
        Logger.e(this.getClass().getName(), "Export service encountered an exception: " + e.getMessage(), e);
    }

    @Override
    public void onClientServiceReady() {
        // begin listening for service broadcasts
        if(!BroadcastListenerService.isRunning()) {
            broadcastListenerServiceIntent.putExtra(BroadcastListenerService.PARAM_BROADCAST_PORT, PORT_CLIENT_UDP);
//            broadcastListenerServiceIntent.putExtra(BroadcastListenerService.PARAM_SERVICE_NAME, SERVICE_NAME);
            broadcastListenerServiceIntent.putExtra(BroadcastListenerService.PARAM_REFRESH_FREQUENCY, REFRESH_FREQUENCY);
            broadcastListenerServiceIntent.putExtra(BroadcastListenerService.PARAM_SERVER_TTL, SERVER_TTL);
            startService(broadcastListenerServiceIntent);
        }
        bindService(broadcastListenerServiceIntent, mBroadcastListenerConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onServerConnectionLost(Peer peer) {
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                updatePeerList(mBroadcastListenerService.getPeers());
            }
        });
    }

    @Override
    public void onServerConnectionChanged(Peer peer) {
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                updatePeerList(mBroadcastListenerService.getPeers());
            }
        });
    }

    @Override
    public void onClientServiceError(Throwable e) {
        Logger.e(this.getClass().getName(), "Import service encountered an exception: " + e.getMessage(), e);
    }

    @Override
    public void onReceivedTargetTranslations(Peer server, String[] targetTranslations) {

    }

    @Override
    public void onReceivedRequest(Peer peer, Request request) {

    }

//    @Override
//    public void onReceivedProjectList(final Peer server, Model[] models) {
//        if(models.length > 0) {
////            showProjectSelectionDialog(server, models);
//        } else {
////            app().showToastMessage(getResourceSlugs().getString(R.string.no_projects_available_on_server));
//            Handler hand = new Handler(Looper.getMainLooper());
//            hand.post(new Runnable() {
//                @Override
//                public void run() {
//                    AlertDialog.Builder builder = new AlertDialog.Builder(DeviceToDeviceActivity.this);
//                    builder.setTitle(server.getIpAddress()).setMessage(getResources().getString(R.string.no_projects_available_on_server)).show();
//                }
//            });
//        }
//    }

//    @Override
//    public void onReceivedProject(Peer server, ProjectImport[] importStatuses) {
//        FragmentTransaction ft = getFragmentManager().beginTransaction();
//        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
//        if (prev != null) {
//            ft.remove(prev);
//        }
//        ft.addToBackStack(null);
//        mImportDialog = new ProjectTranslationImportApprovalDialog();
//        mImportDialog.setOnClickListener(new ProjectTranslationImportApprovalDialog.OnClickListener() {
//            @Override
//            public void onOk(ProjectImport[] requests) {
//                showProgress(getResources().getString(R.string.loading));
//                // TODO: we need to tell the import service what we want to import. It needs to be able to keep track of multiple imports.
//                for (ProjectImport r : requests) {
//                    Sharing.importProject(r);
//                }
//                Sharing.cleanImport(requests);
////                file.delete();
//                hideProgress();
//                AppContext.context().showToastMessage(R.string.success);
//                // TODO: success dialog
//            }
//
//            @Override
//            public void onCancel(ProjectImport[] requests) {
//                // TODO: tell the import service to cancel.
//                // import was aborted
//                Sharing.cleanImport(requests);
//                hideProgress();
////                file.delete();
//            }
//        });
//        // NOTE: we don't place this dialog into the peer dialog map because this will work even if the server disconnects
//        mImportDialog.setImportRequests(importStatuses);
//        mImportDialog.show(ft, "dialog");
//        hideProgress();
//    }

    @Override
    public void onFoundServer(Peer server) {
        switch(server.getVersion()) {
            default:
                // TODO: initialize the api that will be used to handle this version of the server
        }
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
