package com.door43.translationstudio.newui;

import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.device2device.PeerAdapter;
import com.door43.translationstudio.service.PeerStatusKeys;
import com.door43.translationstudio.network.Peer;
import com.door43.translationstudio.service.BroadcastListenerService;
import com.door43.translationstudio.service.BroadcastService;
import com.door43.translationstudio.service.ClientService;
import com.door43.translationstudio.service.ServerService;
import com.door43.util.RSAEncryption;
import com.door43.widget.ViewUtil;

import org.json.JSONObject;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by joel on 11/19/2015.
 */
public class ShareWithPeerDialog extends DialogFragment implements ServerService.Callbacks, BroadcastListenerService.Callbacks, ClientService.OnClientEventListener {

    private static final int PORT_CLIENT_UDP = 9939;
    private static final String SERVICE_NAME = "tS";
    private static final int REFRESH_FREQUENCY = 2000;
    private static final int SERVER_TTL = 2000;
    public static final int MODE_CLIENT = 0;
    public static final int MODE_SERVER = 1;
    private PeerAdapter adapter;
    public static final String ARG_OPERATION_MODE = "arg_operation_mode";
    public static final String ARG_TARGET_TRANSLATION = "arg_target_translation";

    private ClientService clientService;
    private ServiceConnection clientConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ClientService.LocalBinder binder = (ClientService.LocalBinder) service;
            clientService = binder.getServiceInstance();
            clientService.setOnClientEventListener(ShareWithPeerDialog.this);
            Logger.i(ShareWithPeerDialog.class.getName(), "Connected to import service");
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
            serverService.registerCallback(ShareWithPeerDialog.this);
            Logger.i(ShareWithPeerDialog.class.getName(), "Connected to export service");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serverService.registerCallback(null);
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
            Handler hand = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    // TODO: 11/19/2015 set initial peer list
                    updatePeerList(listenerService.getPeers());
                }
            });
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
    private Intent serverIntent;
    private Intent clientIntent;
    private Intent broadcastIntent;
    private Intent listenerIntent;
    private int operationMode;
    private String targetTranslationSlug;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_share_with_peer, container, false);

        Bundle args = getArguments();
        if(args != null && args.containsKey(ARG_OPERATION_MODE)) {
            operationMode = args.getInt(ARG_OPERATION_MODE, MODE_CLIENT);
            targetTranslationSlug = args.getString(ARG_TARGET_TRANSLATION, null);
            if(operationMode == MODE_SERVER && targetTranslationSlug == null) {
                throw new InvalidParameterException("Server mode requires a target translation slug");
            }
        } else {
            throw new InvalidParameterException("Missing intent arguments");
        }

        publicKeyFile = new File(getActivity().getFilesDir(), getResources().getString(R.string.p2p_keys_dir) + "/id_rsa.pub");
        privateKeyFile = new File(getActivity().getFilesDir(), getResources().getString(R.string.p2p_keys_dir) + "/id_rsa");
        publicKeyFile.getParentFile().mkdirs();

        // TODO: 11/19/2015 determine what title should be
        // TODO: 11/19/2015 set target translation title
        // TODO: 11/19/2015 set up ui

        final Handler hand = new Handler(Looper.getMainLooper());
        ListView list = (ListView)v.findViewById(R.id.list);
        adapter = new PeerAdapter(operationMode == MODE_SERVER, getActivity());
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Peer peer = adapter.getItem(position);
                if(operationMode == MODE_SERVER) {
                    // TODO: 11/20/2015 we need to display options for this peer.
                    // if they are also a server we need to have options for interacting with them as well
                    if(!peer.isSecure()) {
                        serverService.acceptConnection(peer);
                        hand.post(new Runnable() {
                            @Override
                            public void run() {
                                updatePeerList(serverService.getPeers());
                            }
                        });
                    } else {
                        serverService.sendTargetTranslation(peer, targetTranslationSlug);
                    }
                } else if(operationMode == MODE_CLIENT) {
                    if(!peer.isSecure()) {
                        // TRICKY: we don't let the client connect again otherwise it may get an encryption exception due to miss-matched keys
                        if(!peer.keyStore.getBool(PeerStatusKeys.WAITING)) {
                            // connect to the server, implicitly requesting permission to access it
                            peer.keyStore.add(PeerStatusKeys.WAITING, true);
                            updatePeerList(listenerService.getPeers());
                            clientService.connectToServer(peer);
                        }
                    } else {
                        // TODO: 11/20/2015 display options for interacting with this user

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

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

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
        intent.putExtra(ServerService.PARAM_PRIVATE_KEY, RSAEncryption.readPrivateKeyFromFile(privateKeyFile));
        intent.putExtra(ServerService.PARAM_PUBLIC_KEY, RSAEncryption.getPublicKeyAsString(RSAEncryption.readPublicKeyFromFile(publicKeyFile)));
        Logger.i(this.getClass().getName(), "Starting service " + intent.getComponent().getClassName());
        getActivity().startService(intent);
    }

    /**
     * Updates the peer list on the screen
     * @param peers
     */
    public void updatePeerList(ArrayList<Peer> peers) {
        if(adapter != null) {
            adapter.setPeerList(peers);
        }
    }

    @Override
    public void onDestroy(){
        // TODO: 11/19/2015 stop services if view is being completely destroyed
        if (BroadcastService.isRunning() && broadcastIntent != null) {
            if(!getActivity().stopService(broadcastIntent)) {
                Logger.w(this.getClass().getName(), "Failed to stop service " + BroadcastService.class.getName());
            }
        }
        if (BroadcastListenerService.isRunning() && listenerIntent != null) {
            if(!getActivity().stopService(listenerIntent)) {
                Logger.w(this.getClass().getName(), "Failed to stop service " + BroadcastListenerService.class.getName());
            }
        }
        if (ServerService.isRunning() && serverIntent != null) {
            if(!getActivity().stopService(serverIntent)) {
                Logger.w(this.getClass().getName(), "Failed to stop service " + ServerService.class.getName());
            }
        }
        if (ClientService.isRunning() && clientIntent != null) {
            if(!getActivity().stopService(clientIntent)) {
                Logger.w(this.getClass().getName(), "Failed to stop service " + ClientService.class.getName());
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
//            broadcastIntent.putExtra(BroadcastService.PARAM_SERVICE_NAME, SERVICE_NAME);
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
// close dialogs
//        if(mPeerDialogs.containsKey(server.getIpAddress())) {
//            DialogFragment dialog = mPeerDialogs.get(server.getIpAddress());
//            if(dialog.getActivity() != null) {
//                dialog.dismiss();
//            }
//            mPeerDialogs.remove(server.getIpAddress());
//        }

        // reload the list
//        Handler hand = new Handler(Looper.getMainLooper());
//        hand.post(new Runnable() {
//            @Override
//            public void run() {
//                updatePeerList(listenerService.getPeers());
//            }
//        });
    }

    @Override
    public void onClientServiceReady() {
        // begin listening for servers
        if(!BroadcastListenerService.isRunning()) {
            listenerIntent.putExtra(BroadcastListenerService.PARAM_BROADCAST_PORT, PORT_CLIENT_UDP);
//            listenerIntent.putExtra(BroadcastListenerService.PARAM_SERVICE_NAME, SERVICE_NAME);
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
                updatePeerList(listenerService.getPeers());
            }
        });
    }

    @Override
    public void onServerConnectionChanged(Peer peer) {
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                updatePeerList(listenerService.getPeers());
            }
        });
    }

    @Override
    public void onClientServiceError(Throwable e) {
        Logger.e(this.getClass().getName(), "Client service encountered an exception: " + e.getMessage(), e);
    }

    @Override
    public void onReceivedTargetTranslations(Peer server, String[] targetTranslations) {
        // TODO: 11/23/2015 notify user that download is complete.
        Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), "Target translation successfully imported", Snackbar.LENGTH_LONG);
        ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
        snack.show();
    }
}
