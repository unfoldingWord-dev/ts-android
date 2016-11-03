package com.door43.translationstudio.ui.dialogs;

import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.resourcecontainer.Project;
import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationViewMode;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.network.Peer;
import com.door43.translationstudio.ui.home.HomeActivity;
import com.door43.translationstudio.ui.translate.TargetTranslationActivity;
import com.door43.translationstudio.rendering.MergeConflictHandler;
import com.door43.translationstudio.services.BroadcastListenerService;
import com.door43.translationstudio.services.BroadcastService;
import com.door43.translationstudio.services.ClientService;
import com.door43.translationstudio.services.Request;
import com.door43.translationstudio.services.ServerService;
import com.door43.util.RSAEncryption;

import org.json.JSONException;

import java.io.File;
import java.security.InvalidParameterException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class ShareWithPeerDialog extends DialogFragment implements ServerService.OnServerEventListener, BroadcastListenerService.Callbacks, ClientService.OnClientEventListener {
    public static final String STATE_DIALOG_SHOWN = "state_dialog_shown";
    public static final String STATE_DIALOG_TRANSLATION_ID = "state_dialog_translationID";
    // TODO: 11/30/2015 get port from settings
    private static final int PORT_CLIENT_UDP = 9939;
    private static final int REFRESH_FREQUENCY = 2000;
    private static final int SERVER_TTL = 2000;
    public static final int MODE_CLIENT = 0;
    public static final int MODE_SERVER = 1;
    public static final String ARG_DEVICE_ALIAS = "arg_device_alias";
    public static final String TAG = ShareWithPeerDialog.class.getSimpleName();
    private PeerAdapter adapter;
    public static final String ARG_OPERATION_MODE = "arg_operation_mode";
    public static final String ARG_TARGET_TRANSLATION = "arg_target_translation";
    private eDialogShown mDialogShown = eDialogShown.NONE;
    private String mTargetTranslationID;

    private ClientService clientService;
    private ServiceConnection clientConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ClientService.LocalBinder binder = (ClientService.LocalBinder) service;
            clientService = binder.getServiceInstance();
            clientService.setOnClientEventListener(ShareWithPeerDialog.this);
            Logger.i(ShareWithPeerDialog.class.getName(), "Connected to import service");
            Handler hand = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    updatePeerList(clientService.getPeers());
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            clientService.setOnClientEventListener(null);
            Logger.i(ShareWithPeerDialog.class.getName(), "Disconnected from import service");
            // TODO: notify fragment that service was dropped.
        }
    };

    private ServerService serverService;
    private ServiceConnection serverConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ServerService.LocalBinder binder = (ServerService.LocalBinder) service;
            serverService = binder.getServiceInstance();
            serverService.setOnServerEventListener(ShareWithPeerDialog.this);
            Logger.i(ShareWithPeerDialog.class.getName(), "Connected to export service");
            Handler hand = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    updatePeerList(serverService.getPeers());
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serverService.setOnServerEventListener(null);
            Logger.i(ShareWithPeerDialog.class.getName(), "Disconnected from export service");
            // TODO: notify fragment that service was dropped.
        }
    };

    // TODO: 11/20/2015 we don't actually need to bind to the broadcast service
    private BroadcastService broadcastService;
    private ServiceConnection broadcastConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BroadcastService.LocalBinder binder = (BroadcastService.LocalBinder) service;
            broadcastService = binder.getServiceInstance();
            Logger.i(ShareWithPeerDialog.class.getName(), "Connected to broadcast service");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Logger.i(ShareWithPeerDialog.class.getName(), "Disconnected from broadcast service");
            // TODO: notify fragment that service was dropped.
        }
    };

    private BroadcastListenerService listenerService;
    private ServiceConnection listenerConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BroadcastListenerService.LocalBinder binder = (BroadcastListenerService.LocalBinder) service;
            listenerService = binder.getServiceInstance();
            listenerService.registerCallback(ShareWithPeerDialog.this);
            Logger.i(ShareWithPeerDialog.class.getName(), "Connected to broadcast listener service");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            listenerService.registerCallback(null);
            Logger.i(ShareWithPeerDialog.class.getName(), "Disconnected from broadcast listener service");
            // TODO: notify fragment that service was dropped.
        }
    };
    private File publicKeyFile;
    private File privateKeyFile;
    private static Intent serverIntent;
    private static Intent clientIntent;
    private static Intent broadcastIntent;
    private static Intent listenerIntent;
    private int operationMode;
    private String targetTranslationSlug;
    private boolean shutDownServices = true;
    private String deviceAlias;
    private TargetTranslation targetTranslation = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_share_with_peer, container, false);

        Bundle args = getArguments();
        if(args != null && args.containsKey(ARG_OPERATION_MODE) && args.containsKey(ARG_DEVICE_ALIAS)) {
            operationMode = args.getInt(ARG_OPERATION_MODE, MODE_CLIENT);
            targetTranslationSlug = args.getString(ARG_TARGET_TRANSLATION, null);
            deviceAlias = args.getString(ARG_DEVICE_ALIAS, null);
            targetTranslation = App.getTranslator().getTargetTranslation(targetTranslationSlug);
            if (operationMode == MODE_SERVER && targetTranslation == null) {
                throw new InvalidParameterException("Server mode requires a valid target translation");
            }
            if(deviceAlias == null) {
                throw new InvalidParameterException("The device alias cannot be null");
            }
        } else {
            throw new InvalidParameterException("Missing intent arguments");
        }

        publicKeyFile = new File(getActivity().getFilesDir(), getResources().getString(R.string.p2p_keys_dir) + "/id_rsa.pub");
        privateKeyFile = new File(getActivity().getFilesDir(), getResources().getString(R.string.p2p_keys_dir) + "/id_rsa");
        publicKeyFile.getParentFile().mkdirs();

        TextView title = (TextView)v.findViewById(R.id.title);
        TextView subTitle = (TextView)v.findViewById(R.id.target_translation_title);

        if(operationMode == MODE_SERVER) {
            title.setText(getResources().getString(R.string.backup_to_friend));

            // get project title
            Project p = App.getLibrary().index.getProject(Locale.getDefault().getLanguage(), targetTranslation.getProjectId(), true);
            if(p != null) {
                subTitle.setText(p.name + " - " + targetTranslation.getTargetLanguageName());
            } else {
                subTitle.setText(targetTranslation.getProjectId() + " - " + targetTranslation.getTargetLanguageName());
            }
        } else {
            title.setText(getResources().getString(R.string.import_from_friend));
            subTitle.setText("");
        }

        ListView list = (ListView)v.findViewById(R.id.list);
        adapter = new PeerAdapter(getActivity());
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Peer peer = adapter.getItem(position);
                if(operationMode == MODE_SERVER) {
                    // offer target translation to the client
                    String[] sourceTranslationSlugs = App.getSelectedSourceTranslations(targetTranslationSlug);
                    String sourceLanguageSlug = "en";
                    // try using their selected source first
                    if(sourceTranslationSlugs.length > 0) {
                        Translation t = App.getLibrary().index.getTranslation(sourceTranslationSlugs[0]);
                        if(t != null) {
                            sourceLanguageSlug = t.language.slug;
                        } else {
                            // try using the next available source
                            Project p = App.getLibrary().index.getProject(Locale.getDefault().getLanguage(), targetTranslationSlug);
                            if(p != null) {
                                sourceLanguageSlug = p.languageSlug;
                            }
                        }
                    }
                    serverService.offerTargetTranslation(peer, sourceLanguageSlug, targetTranslationSlug);
                } else if(operationMode == MODE_CLIENT) {
                    // TODO: 12/1/2015 eventually provide a ui for viewing multiple different requests from this peer
                    // display request user
                    Request[] requests = peer.getRequests();
                    if(requests.length > 0) {
                        final Request request = requests[0];
                        if(request.type == Request.Type.AlertTargetTranslation) {
                            // TRICKY: for now we are just looking at one request at a time.
                            try {
                                final String targetTranslationSlug = request.context.getString("target_translation_id");
                                String projectName = request.context.getString("project_name");
                                String targetLanguageName = request.context.getString("target_language_name");
                                int packageVersion = request.context.getInt("package_version");
                                if(packageVersion <= TargetTranslation.PACKAGE_VERSION) {
                                    new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                                            .setTitle(peer.getName())
                                            .setMessage(String.format(getResources().getString(R.string.confirm_import_target_translation), projectName + " - " + targetLanguageName))
                                            .setPositiveButton(R.string.label_import, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    peer.dismissRequest(request);
                                                    if (adapter != null) {
                                                        adapter.notifyDataSetChanged();
                                                    }
                                                    clientService.requestTargetTranslation(peer, targetTranslationSlug);
                                                    dialog.dismiss();
                                                }
                                            })
                                            .setNegativeButton(R.string.dismiss, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    peer.dismissRequest(request);
                                                    if (adapter != null) {
                                                        adapter.notifyDataSetChanged();
                                                    }
                                                    dialog.dismiss();
                                                }
                                            })
                                            .show();
                                } else {
                                    // our app is to old to import this version of a target translation
                                    Logger.w(ShareWithPeerDialog.class.getName(), "Could not import target translation with package version " + TargetTranslation.PACKAGE_VERSION + ". Supported version is " + TargetTranslation.PACKAGE_VERSION);
                                    peer.dismissRequest(request);
                                    if (adapter != null) {
                                        adapter.notifyDataSetChanged();
                                    }
                                    new AlertDialog.Builder(getActivity(),R.style.AppTheme_Dialog)
                                            .setTitle(peer.getName())
                                            .setMessage(String.format(getResources().getString(R.string.error_importing_unsupported_target_translation), projectName, targetLanguageName, getResources().getString(R.string.app_name)))
                                            .setNeutralButton(R.string.dismiss, null)
                                            .show();
                                }
                            } catch (JSONException e) {
                                peer.dismissRequest(request);
                                if (adapter != null) {
                                    adapter.notifyDataSetChanged();
                                }
                                new AlertDialog.Builder(getActivity(),R.style.AppTheme_Dialog)
                                        .setTitle(peer.getName())
                                        .setMessage(R.string.error)
                                        .setNeutralButton(R.string.dismiss, null)
                                        .show();
                                Logger.e(ShareWithPeerDialog.class.getName(), "Invalid request context", e);
                            }
                        } else {
                            // we do not currently support other requests
                        }
                    }
                }
            }
        });

        Button dismissButton = (Button)v.findViewById(R.id.dismiss_button);
        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        if(savedInstanceState != null) {
            mDialogShown = eDialogShown.fromInt(savedInstanceState.getInt(STATE_DIALOG_SHOWN, eDialogShown.NONE.getValue()));
            mTargetTranslationID = savedInstanceState.getString(STATE_DIALOG_TRANSLATION_ID, null);
        }

        restoreDialogs();
        return v;
    }

    /**
     * restore the dialogs that were displayed before rotation
     */
    private void restoreDialogs() {
        switch(mDialogShown) {
            case MERGE_CONFLICT:
                showMergeConflict(mTargetTranslationID);
                break;

            case NONE:
                break;

            default:
                Logger.e(TAG,"Unsupported restore dialog: " + mDialogShown.toString());
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        shutDownServices = true;

        if(operationMode == MODE_SERVER) {
            serverIntent = new Intent(getActivity(), ServerService.class);
            broadcastIntent = new Intent(getActivity(), BroadcastService.class);
            if(!ServerService.isRunning()) {
                try {
                    initializeService(serverIntent);
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "Failed to initialize the server service", e);
                    dismiss();
                }
            }
            getActivity().bindService(serverIntent, serverConnection, Context.BIND_AUTO_CREATE);
        } else if(operationMode == MODE_CLIENT) {
            clientIntent = new Intent(getActivity(), ClientService.class);
            listenerIntent = new Intent(getActivity(), BroadcastListenerService.class);
            if(!ClientService.isRunning()) {
                try {
                    initializeService(clientIntent);
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "Failed to initialize the client service", e);
                    dismiss();
                }
            }
            getActivity().bindService(clientIntent, clientConnection, Context.BIND_AUTO_CREATE);
        }
    }

    /**
     * Initializes the service intent
     * @param intent
     * @throws Exception
     */
    private void initializeService(Intent intent) throws Exception {
        if(!privateKeyFile.exists() || !publicKeyFile.exists()) {
            RSAEncryption.generateKeys(privateKeyFile, publicKeyFile);
        }
        // TODO: 11/30/2015 we should use a shared interface for setting parameters so we don't have to manage two sets
        PrivateKey privateKey;
        PublicKey publicKey;
        try {
            privateKey = RSAEncryption.readPrivateKeyFromFile(privateKeyFile);
            publicKey = RSAEncryption.readPublicKeyFromFile(publicKeyFile);
        } catch (Exception e) {
            // try to regenerate the keys if loading fails
            Logger.w(this.getClass().getName(), "Failed to load the p2p keys. Attempting to regenerate...", e);
            RSAEncryption.generateKeys(privateKeyFile, publicKeyFile);
            privateKey = RSAEncryption.readPrivateKeyFromFile(privateKeyFile);
            publicKey = RSAEncryption.readPublicKeyFromFile(publicKeyFile);
        }

        intent.putExtra(ServerService.PARAM_PRIVATE_KEY, privateKey);
        intent.putExtra(ServerService.PARAM_PUBLIC_KEY, RSAEncryption.getPublicKeyAsString(publicKey));
        intent.putExtra(ServerService.PARAM_DEVICE_ALIAS, App.getDeviceNetworkAlias());
        Logger.i(this.getClass().getName(), "Starting service " + intent.getComponent().getClassName());
        getActivity().startService(intent);
    }

    /**
     * Updates the peer list on the screen
     * @param peers
     */
    public void updatePeerList(ArrayList<Peer> peers) {
        if(adapter != null) {
            adapter.setPeers(peers);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        shutDownServices = false;
        out.putInt(STATE_DIALOG_SHOWN, mDialogShown.getValue());
        out.putString(STATE_DIALOG_TRANSLATION_ID, mTargetTranslationID);
        super.onSaveInstanceState(out);
    }

    @Override
    public void onDestroy(){
        // unbind services
        try {
            getActivity().unbindService(broadcastConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            getActivity().unbindService(listenerConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            getActivity().unbindService(serverConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            getActivity().unbindService(clientConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // shut down services
        if(shutDownServices) {
            if (BroadcastService.isRunning() && broadcastIntent != null) {
                if (!getActivity().stopService(broadcastIntent)) {
                    Logger.w(this.getClass().getName(), "Failed to stop service " + BroadcastService.class.getName());
                }
            }
            if (BroadcastListenerService.isRunning() && listenerIntent != null) {
                if (!getActivity().stopService(listenerIntent)) {
                    Logger.w(this.getClass().getName(), "Failed to stop service " + BroadcastListenerService.class.getName());
                }
            }
            if (ServerService.isRunning() && serverIntent != null) {
                if (!getActivity().stopService(serverIntent)) {
                    Logger.w(this.getClass().getName(), "Failed to stop service " + ServerService.class.getName());
                }
            }
            if (ClientService.isRunning() && clientIntent != null) {
                if (!getActivity().stopService(clientIntent)) {
                    Logger.w(this.getClass().getName(), "Failed to stop service " + ClientService.class.getName());
                }
            }
        }
        super.onDestroy();
    }

    @Override
    public void onServerServiceReady(int port) {
        // begin broadcasting
        if(!BroadcastService.isRunning()) {
            broadcastIntent.putExtra(BroadcastService.PARAM_BROADCAST_PORT, PORT_CLIENT_UDP);
            broadcastIntent.putExtra(BroadcastService.PARAM_SERVICE_PORT, port);
            broadcastIntent.putExtra(BroadcastService.PARAM_FREQUENCY, 2000);
            getActivity().startService(broadcastIntent);
        }
        getActivity().bindService(broadcastIntent, broadcastConnection, Context.BIND_AUTO_CREATE);
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                updatePeerList(serverService.getPeers());
            }
        });
    }

    @Override
    public void onClientConnected(Peer peer) {
        serverService.acceptConnection(peer);
    }

    @Override
    public void onClientLost(Peer peer) {

        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                updatePeerList(serverService.getPeers());
            }
        });
    }

    @Override
    public void onClientChanged(Peer peer) {
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                updatePeerList(serverService.getPeers());
            }
        });
    }

    @Override
    public void onServerServiceError(Throwable e) {
        Logger.e(this.getClass().getName(), "Server service encountered an exception: " + e.getMessage(), e);
    }

    @Override
    public void onFoundServer(Peer server) {
        clientService.connectToServer(server);
    }

    @Override
    public void onLostServer(Peer server) {

    }

    @Override
    public void onClientServiceReady() {
        // begin listening for servers
        if(!BroadcastListenerService.isRunning()) {
            listenerIntent.putExtra(BroadcastListenerService.PARAM_BROADCAST_PORT, PORT_CLIENT_UDP);
            listenerIntent.putExtra(BroadcastListenerService.PARAM_REFRESH_FREQUENCY, REFRESH_FREQUENCY);
            listenerIntent.putExtra(BroadcastListenerService.PARAM_SERVER_TTL, SERVER_TTL);
            getActivity().startService(listenerIntent);
        }
        getActivity().bindService(listenerIntent, listenerConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onServerConnectionLost(Peer peer) {
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                updatePeerList(clientService.getPeers());
            }
        });
    }

    @Override
    public void onServerConnectionChanged(Peer peer) {
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                updatePeerList(clientService.getPeers());
            }
        });
    }

    @Override
    public void onClientServiceError(Throwable e) {
        Logger.e(this.getClass().getName(), "Client service encountered an exception: " + e.getMessage(), e);
    }

    @Override
    public void onReceivedTargetTranslations(Peer server, final Translator.ImportResults importResults) {
        // build name list
        Translator translator = App.getTranslator();
        TargetTranslation targetTranslation = translator.getTargetTranslation(importResults.importedSlug);
        Translation st = App.getLibrary().index().getTranslation(targetTranslation.getId());

        String tempName;
        if(st != null) {
            tempName = st.project.name + " - " + targetTranslation.getTargetLanguage().name;
        } else {
            tempName = targetTranslation.getProjectId() + " - " + targetTranslation.getTargetLanguage().name;
        }
        final String name = tempName;

        // notify user
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                if(importResults.isSuccess() && importResults.mergeConflict) {
                    showMergeConflict(importResults.importedSlug);
                } else {
                    new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                            .setTitle(R.string.success)
                            .setMessage(String.format(getResources().getString(R.string.success_import_target_translation), name))
                            .setPositiveButton(R.string.dismiss, null)
                            .show();
                    // TODO: 12/1/2015 this is a bad hack
                    ((HomeActivity) getActivity()).notifyDatasetChanged();
                }
            }
        });
    }

    public void showMergeConflict(String targetTranslationID) {
        mDialogShown = eDialogShown.MERGE_CONFLICT;
        mTargetTranslationID = targetTranslationID;
        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                .setTitle(R.string.merge_conflict_title).setMessage(R.string.import_merge_conflict)
                .setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = eDialogShown.NONE;
                        doManualMerge();
                    }
                }).show();
    }

    private void doManualMerge() {
        // ask parent activity to navigate to a new activity
        Intent intent = new Intent(getActivity(), TargetTranslationActivity.class);
        Bundle args = new Bundle();
        args.putString(App.EXTRA_TARGET_TRANSLATION_ID, mTargetTranslationID);

        MergeConflictHandler.CardLocation location = MergeConflictHandler.findFirstMergeConflict( mTargetTranslationID );
        if(location != null) {
            args.putString(App.EXTRA_CHAPTER_ID, location.chapterID);
            args.putString(App.EXTRA_FRAME_ID, location.frameID);
        }

        args.putInt(App.EXTRA_VIEW_MODE, TranslationViewMode.REVIEW.ordinal());
        intent.putExtras(args);
        startActivity(intent);
        dismiss();
    }

    @Override
    public void onReceivedRequest(final Peer peer, final Request request) {
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                if(adapter != null) {
                    adapter.newRequestAlert(peer, request);
                }
            }
        });
    }


    /**
     * for keeping track which dialog is being shown for orientation changes (not for DialogFragments)
     */
    public enum eDialogShown {
        NONE(0),
        MERGE_CONFLICT(1);

        private int value;

        eDialogShown(int Value) {
            this.value = Value;
        }

        public int getValue() {
            return value;
        }

        public static eDialogShown fromInt(int i) {
            for (eDialogShown b : eDialogShown.values()) {
                if (b.getValue() == i) {
                    return b;
                }
            }
            return null;
        }
    }

}
