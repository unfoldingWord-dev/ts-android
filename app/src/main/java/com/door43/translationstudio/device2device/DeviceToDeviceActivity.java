package com.door43.translationstudio.device2device;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
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
import com.door43.translationstudio.network.Server;
import com.door43.translationstudio.network.Service;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Sharing;
import com.door43.translationstudio.projects.PseudoProject;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.projects.imports.ProjectImport;
import com.door43.util.ListMap;
import com.door43.util.Logger;
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

public class DeviceToDeviceActivity extends TranslatorBaseActivity {
    private boolean mStartAsServer = false;
    private static Service mService;
    private DevicePeerAdapter mAdapter;
    private ProgressBar mLoadingBar;
    private TextView mLoadingText;
    private static ProgressDialog mProgressDialog;
    private File mPublicKeyFile;
    private File mPrivateKeyFile;
    private static Map<String, DialogFragment> mPeerDialogs = new HashMap<>();
    private static final String TASK_SERVER = "p2p_server_service";
    private static final String TASK_CLIENT = "p2p_client_service";
    private boolean mShuttingDown = true;
    private static final int PORT_CLIENT_UDP = 9939;

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
        } else {
            setTitle(R.string.import_from_device);
        }

        // start up the service
//        ServerTask serverTask = (ServerTask)TaskManager.getTask(TASK_SERVER);
//        ClientTask clientTask = (ClientTask)TaskManager.getTask(TASK_CLIENT);
//        if(mStartAsServer) {
//            // run as server
//            TaskManager.clearTask(clientTask);
//            if(serverTask == null) {
//                serverTask = new ServerTask();
//                TaskManager.addTask(serverTask);
//            }
//            // TODO: set up listeners
//        } else {
//            // run as client
//            TaskManager.clearTask(serverTask);
//            if(clientTask == null) {
//                clientTask = new ClientTask();
//                TaskManager.addTask(clientTask);
//            }
//            // TODO: set up listeners
//        }


        if(mProgressDialog == null) mProgressDialog = new ProgressDialog(this);


        // reset things on first load
        if(savedInstanceState == null) {
            mPeerDialogs.clear();
            if(mService != null) {
                mService.stop();
                mService = null;
            }
        }

        if(mService == null || !mService.isRunning()) {
            // set up the threads
            if(mStartAsServer) {
                mService = new Server(DeviceToDeviceActivity.this, PORT_CLIENT_UDP, new Server.OnServerEventListener() {

                    @Override
                    public void onBeforeStart(Handler handle) {
                        // generate new session keys
                        try {
                            generateSessionKeys();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Logger.e(DeviceToDeviceActivity.class.getName(), "Failed to generate session keys", e);
                            mService.stop();
                        }
                    }

                    @Override
                    public void onError(Handler handle, final Exception e) {
                        handle.post(new Runnable() {
                            @Override
                            public void run() {
                                app().showException(e);
                                finish();
                            }
                        });
                    }

                    @Override
                    public void onFoundClient(Handler handle, final Peer client) {
                        handle.post(new Runnable() {
                            @Override
                            public void run() {
                                updatePeerList();
                            }
                        });
                    }

                    @Override
                    public void onLostClient(Handler handle, Peer client) {
                        handle.post(new Runnable() {
                            @Override
                            public void run() {
                                updatePeerList();
                            }
                        });
                    }

                    @Override
                    public void onMessageReceived(Handler handle, Peer client, String message) {
                        // decrypt messages when the server is connected
                        if(client.isConnected()) {
                            message = decryptMessage(message);
                            if(message == null) {
                                Logger.e(this.getClass().getName(), "The message could not be decrypted");
                                app().showToastMessage("Decryption exception");
                                return;
                            }
                        }
                        onServerReceivedMessage(handle, client, message);
                    }

                    @Override
                    public String onWriteMessage(Handler handle, Peer client, String message) {
                        if(client.isConnected()) {
                            // encrypt message once the client has connected
                            PublicKey key = getPublicKeyFromString(client.keyStore.getString(PeerStatusKeys.PUBLIC_KEY));
                            if(key != null) {
                                return encryptMessage(key, message);
                            } else {
                                Logger.w(this.getClass().getName(), "Missing the client's public key");
                                return SocketMessages.MSG_EXCEPTION;
                            }
                        } else {
                            return message;
                        }
                    }
                });
            } else {
                mService = new Client(DeviceToDeviceActivity.this, PORT_CLIENT_UDP, new Client.OnClientEventListener() {
                    @Override
                    public void onBeforeStart(Handler handle) {
                        // generate new session keys
                        try {
                            generateSessionKeys();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Logger.e(DeviceToDeviceActivity.class.getName(), "Failed to generate session keys", e);
                            mService.stop();
                        }
                    }

                    @Override
                    public void onError(Handler handle, final Exception e) {
                        handle.post(new Runnable() {
                            @Override
                            public void run() {
                                app().showException(e);
                                finish();
                            }
                        });
                    }

                    @Override
                    public void onFoundServer(Handler handle, final Peer server) {
                        handle.post(new Runnable() {
                            @Override
                            public void run() {
                                updatePeerList();
                            }
                        });
                    }

                    @Override
                    public void onLostServer(Handler handle, final Peer server) {
                        // close any dialogs for this server
                        if(mPeerDialogs.containsKey(server.getIpAddress())) {
                            DialogFragment dialog = mPeerDialogs.get(server.getIpAddress());
                            if(dialog.getActivity() != null) {
                                dialog.dismiss();
                            }
                            mPeerDialogs.remove(server.getIpAddress());
                        }
                        // reload the list
                        handle.post(new Runnable() {
                            @Override
                            public void run() {
                                updatePeerList();
                            }
                        });
                    }

                    @Override
                    public void onMessageReceived(Handler handle, Peer server, String message) {
                        // decrypt messages when the server is connected
                        if(server.isConnected()) {
                            message = decryptMessage(message);
                            if(message == null) {
                                Logger.e(this.getClass().getName(), "The message could not be decrypted");
                                app().showToastMessage("Decryption exception");
                                return;
                            }
                        }
                        onClientReceivedMessage(handle, server, message);
                    }

                    @Override
                    public String onWriteMessage(Handler handle, Peer server, String message) {
                        if(server.isConnected()) {
                            // encrypt message once the server has connected
                            PublicKey key = getPublicKeyFromString(server.keyStore.getString(PeerStatusKeys.PUBLIC_KEY));
                            if(key != null) {
                                return encryptMessage(key, message);
                            } else {
                                Logger.w(this.getClass().getName(), "Missing the server's public key");
                                return SocketMessages.MSG_EXCEPTION;
                            }
                        } else {
                            return message;
                        }
                    }
                });
            }
        }


        // set up the ui
        final Handler handler = new Handler(getMainLooper());
        mLoadingBar = (ProgressBar)findViewById(R.id.loadingBar);
        mLoadingText = (TextView)findViewById(R.id.loadingText);
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
                        client.setIsAuthorized(true);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                updatePeerList();
                            }
                        });
                        s.writeTo(client, "ok");
                    } else {
                        // TODO: maybe display a popup to disconnect the client.
                    }
                } else {
                    Client c = (Client)mService;
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
                            c.connectToServer(mAdapter.getItem(i));
                        }
                    } else {
                        // request a list of projects from the server.
                        // TODO: the response to this request should be cached until the server disconnects.
                        showProgress(getResources().getString(R.string.loading));
                        // Include the suggested language(s) in which the results should be returned (if possible)
                        // This just makes it easier for users to read the results
                        JSONArray preferredLanguagesJson = new JSONArray();
                        // device language
                        preferredLanguagesJson.put(Locale.getDefault().getLanguage());
                        // current project language
                        Project p = AppContext.projectManager().getSelectedProject();
                        if(p != null) {
                            preferredLanguagesJson.put(p.getSelectedSourceLanguage());
                        }
                        // english as default
                        preferredLanguagesJson.put("en");

                        c.writeTo(server, SocketMessages.MSG_PROJECT_LIST + ":" + preferredLanguagesJson.toString());
                    }
                }
            }
        });

        // start the service if the activity is starting the first time.
        if(savedInstanceState == null) {
            // This will set up a service on the local network named "tS".
            mService.start("tS", new Handler(getMainLooper()));
        }
    }

    @Override
    public void onPause() {
        super.onPause();


    }

    @Override
    public void onBackPressed() {
       // mService.stop();
        super.onBackPressed();
    }

    @Override
    public void onStop() {
        if(mShuttingDown) {
            mService.stop();
            mProgressDialog = null;
            mPeerDialogs.clear();
        }
        super.onStop();
    }

    @Override
    public void onPostResume() {
        super.onPostResume();
        // TODO: init things here
    }

    @Override
    public boolean isFinishing() {
        //mService.stop();
        return super.isFinishing();
    }

    @Override
    public void onResume() {
        super.onResume();
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
     * Returns the public key parsed from the key string
     * @param keyString the raw key string
     * @return the public key
     */
    public PublicKey getPublicKeyFromString(String keyString) {
        return RSAEncryption.getPublicKeyFromString(keyString);
    }

    /**
     * Returns the private key used for this session
     * @return a private key object or null
     * @throws IOException
     */
    public PrivateKey getPrivateKey() throws IOException {
        return RSAEncryption.readPrivateKeyFromFile(mPrivateKeyFile);
    }

    /**
     * Encrypts a message with a public key
     * @param publicKey the public key that will be used to encrypt the message
     * @param message the message to be encrypted
     * @return the encrypted message
     */
    public String encryptMessage(PublicKey publicKey, String message)  {
        // TRICKY: RSA is not good for encrypting large amounts of data.
        // So we first encrypt the data then encrypt the encryption key using the public key.
        // the encrypted key is then attached to the encrypted message.

        try {
            // encrypt message
            AesCbcWithIntegrity.SecretKeys key = generateKey();
            AesCbcWithIntegrity.CipherTextIvMac civ = encrypt(message, key);
            String encryptedMessage = civ.toString();

            // encrypt key
            byte[] encryptedKeyBytes = RSAEncryption.encryptData(keyString(key), publicKey);
            if(encryptedKeyBytes == null) {
                Logger.e(this.getClass().getName(), "Failed to encrypt the message");
                return null;
            }
            // encode key
            String encryptedKey = new String(Base64.encode(encryptedKeyBytes, Base64.NO_WRAP));
            return encryptedKey + "-key-" + encryptedMessage;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Decrypts a message using the private key
     * @param message the message to be decrypted
     * @return the decrypted message
     */
    public String decryptMessage(String message) {
        // extract encryption key
        try {
            String[] pieces = message.split("\\-key\\-");
            if (pieces.length == 2) {
                // decode key
                byte[] data = Base64.decode(pieces[0].getBytes(), Base64.NO_WRAP);
                // decrypt key
                AesCbcWithIntegrity.SecretKeys key = keys(RSAEncryption.decryptData(data, getPrivateKey()));

                // decrypt message
                AesCbcWithIntegrity.CipherTextIvMac civ = new AesCbcWithIntegrity.CipherTextIvMac(pieces[1]); // encrypt("", key);
                return decryptString(civ, key);
            } else {
                Logger.w(this.getClass().getName(), "Invalid message to decrypt");
                return null;
            }
        } catch(Exception e) {
            Logger.e(this.getClass().getName(), "Invalid message to decrypt", e);
            return null;
        }
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
     * Handles messages received by the server
     * @param client the client peer that sent the message
     * @param message the message received from the client
     */
    private void onServerReceivedMessage(final Handler handle, Peer client, String message) {
        String[] data = StringUtilities.chunk(message, ":");
        // TODO: we should probably place these into different methods for better organization
        // validate client
        if(client.isAuthorized()) {
            if(client.isConnected()) {
                // *********************************
                // authorized and connected
                // *********************************
                if(data[0].equals(SocketMessages.MSG_PROJECT_LIST)) {
                    // send the project list to the client

                    // read preferred source languages (for better readability on the client)
                    List<Language> preferredLanguages = new ArrayList<>();
                    try {
                        JSONArray preferredLanguagesJson = new JSONArray(data[1]);
                        for(int i = 0; i < preferredLanguagesJson.length(); i ++) {
                            Language lang = AppContext.projectManager().getLanguage(preferredLanguagesJson.getString(i));
                            if(lang != null) {
                                preferredLanguages.add(lang);
                            }
                        }
                    } catch (JSONException e) {
                        Logger.e(this.getClass().getName(), "failed to parse preferred language list", e);
                    }

                    // generate project library
                    // TODO: identifying the projects that have changes could be expensive if there are lots of clients and lots of projects. We might want to cache this
                    String library = Sharing.generateLibrary(AppContext.projectManager().getProjects(), preferredLanguages);

                    mService.writeTo(client, SocketMessages.MSG_PROJECT_LIST + ":" + library);
                } else if(data[0].equals(SocketMessages.MSG_PROJECT_ARCHIVE)) {
                    // send the project archive to the client
                    JSONObject json;
                    try {
                        json = new JSONObject(data[1]);
                    } catch (final JSONException e) {
                        Logger.e(this.getClass().getName(), "failed to parse project archive response", e);
                        mService.writeTo(client, SocketMessages.MSG_INVALID_REQUEST);
                        return;
                    }

                    // load data
                    if(json.has("id") && json.has("target_languages")) {
                        try {
                            String projectId = json.getString("id");

                            final Project p = AppContext.projectManager().getProject(projectId);
                            // validate project
                            if(p != null) {
                                // validate requested source languages
                                List<SourceLanguage> requestedSourceLanguages = new ArrayList<>();
                                if(json.has("source_languages")) {
                                    JSONArray sourceLanguagesJson = json.getJSONArray("source_languages");
                                    for(int i = 0; i < sourceLanguagesJson.length(); i ++) {
                                        String languageId = sourceLanguagesJson.getString(i);
                                        SourceLanguage s = p.getSourceLanguage(languageId);
                                        if(s != null) {
                                            requestedSourceLanguages.add(s);
                                        }
                                    }
                                }

                                // validate requested target languages
                                Language[] activeLanguages = p.getActiveTargetLanguages();
                                JSONArray targetLanguagesJson = json.getJSONArray("target_languages");
                                List<Language> requestedTranslations = new ArrayList<>();
                                for (int i = 0; i < targetLanguagesJson.length(); i++) {
                                    String languageId = (String) targetLanguagesJson.get(i);
                                    for(Language l:activeLanguages) {
                                        if(l.getId().equals(languageId)) {
                                            requestedTranslations.add(l);
                                            break;
                                        }
                                    }
                                }
                                if(requestedTranslations.size() > 0) {
                                    String path = Sharing.export(p, requestedSourceLanguages.toArray(new SourceLanguage[requestedSourceLanguages.size()]), requestedTranslations.toArray(new Language[requestedTranslations.size()]));
                                    final File archive = new File(path);
                                    if(archive.exists()) {
                                        // open a socket to send the project
                                        ServerSocket fileSocket = mService.generateWriteSocket(new Service.OnSocketEventListener() {
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
                                                    while ((count = in.read(buffer)) > 0) {
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
                                                }
                                            }
                                        });
                                        // send details to the client so they can download
                                        JSONObject infoJson = new JSONObject();
                                        infoJson.put("port", fileSocket.getLocalPort());
                                        infoJson.put("name", archive.getName());
                                        infoJson.put("size", archive.length());
                                        mService.writeTo(client, SocketMessages.MSG_PROJECT_ARCHIVE +":" + infoJson.toString());
                                    } else {
                                        // the archive could not be created
                                        mService.writeTo(client, SocketMessages.MSG_SERVER_ERROR);
                                    }
                                } else {
                                    // the client should have known better
                                    mService.writeTo(client, SocketMessages.MSG_INVALID_REQUEST);
                                }
                            } else {
                                // the client should have known better
                                mService.writeTo(client, SocketMessages.MSG_INVALID_REQUEST);
                            }
                        } catch (JSONException e) {
                            Logger.e(this.getClass().getName(), "malformed or corrupt project archive response", e);
                            mService.writeTo(client, SocketMessages.MSG_INVALID_REQUEST);
                        } catch (IOException e) {
                            Logger.e(this.getClass().getName(), "unable to read project archive response", e);
                            mService.writeTo(client, SocketMessages.MSG_SERVER_ERROR);
                        }
                    } else {
                        mService.writeTo(client, SocketMessages.MSG_INVALID_REQUEST);
                    }
                }
            } else {
                // *********************************
                // authorized but not connected yet
                // *********************************

                if(data[0].equals(SocketMessages.MSG_PUBLIC_KEY)) {
                    // receive the client's public key
                    client.keyStore.add(PeerStatusKeys.PUBLIC_KEY, data[1]);

                    // send the client our public key
                    String key;
                    try {
                        key = getPublicKeyString();
                    } catch (Exception e) {
                        Logger.e(this.getClass().getName(), "Missing the client public key", e);
                        return;
                    }
                    mService.writeTo(client, SocketMessages.MSG_PUBLIC_KEY+":"+key);
                    client.setIsConnected(true);

                    // reload the list
                    handle.post(new Runnable() {
                        @Override
                        public void run() {
                            updatePeerList();
                        }
                    });
                }
            }
        } else {
            // *********************************
            // not authorized
            // *********************************
            // the client is not authorized
            mService.writeTo(client, SocketMessages.MSG_AUTHORIZATION_ERROR);
        }
    }

    /**
     * Handles messages received by the client
     * @param server the server peer that sent the message
     * @param message the message sent by the server
     */
    private void onClientReceivedMessage(final Handler handle, final Peer server, String message) {
        String[] data = StringUtilities.chunk(message, ":");
        if(data[0].equals(SocketMessages.MSG_PROJECT_ARCHIVE)) {

            // load data
            JSONObject infoJson;
            try {
                infoJson = new JSONObject(data[1]);
            } catch (JSONException e) {
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
                    app().showException(e);
                    return;
                }
                // the server is sending a project archive
                mService.generateReadSocket(server, port, new Service.OnSocketEventListener() {
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

                        showProgress(getResources().getString(R.string.downloading));
                        final File file = new File(getExternalCacheDir() + "/transferred/" + name);
                        try {
                            // download archive
                            DataInputStream in = new DataInputStream(connection.getSocket().getInputStream());
                            file.getParentFile().mkdirs();
                            file.createNewFile();
                            OutputStream out = new FileOutputStream(file.getAbsolutePath());
                            byte[] buffer = new byte[8 * 1024];
                            int totalCount = 0;
                            int count;
                            while ((count = in.read(buffer)) > 0) {
                                totalCount += count;
                                server.keyStore.add(PeerStatusKeys.PROGRESS, totalCount / ((int) size) * 100);
                                handle.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        updatePeerList();
                                    }
                                });
                                out.write(buffer, 0, count);
                            }
                            server.keyStore.add(PeerStatusKeys.PROGRESS, 0);
                            handle.post(new Runnable() {
                                @Override
                                public void run() {
                                    updatePeerList();
                                }
                            });
                            out.close();
                            in.close();

                            // import the project
                            ProjectImport[] importStatuses = Sharing.prepareArchiveImport(file);
                            if (importStatuses.length > 0) {
                                boolean importWarnings = false;
                                for (ProjectImport s : importStatuses) {
                                    if (!s.isApproved()) {
                                        importWarnings = true;
                                    }
                                }
                                if (importWarnings) {
                                    // review the import status in a dialog
                                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                                    Fragment prev = getFragmentManager().findFragmentByTag("dialog");
                                    if (prev != null) {
                                        ft.remove(prev);
                                    }
                                    ft.addToBackStack(null);
                                    app().closeToastMessage();
                                    ProjectTranslationImportApprovalDialog newFragment = new ProjectTranslationImportApprovalDialog();
                                    newFragment.setOnClickListener(new ProjectTranslationImportApprovalDialog.OnClickListener() {
                                        @Override
                                        public void onOk(ProjectImport[] requests) {
                                            showProgress(getResources().getString(R.string.loading));
                                            for (ProjectImport r : requests) {
                                                Sharing.importProject(r);
                                            }
                                            Sharing.cleanImport(requests);
                                            file.delete();
                                            hideProgress();
                                            app().showToastMessage(R.string.success);
                                        }

                                        @Override
                                        public void onCancel(ProjectImport[] requests) {
                                            // import was aborted
                                            Sharing.cleanImport(requests);
                                            file.delete();
                                        }
                                    });
                                    // NOTE: we don't place this dialog into the peer dialog map because this will work even if the server disconnects
                                    newFragment.setImportRequests(importStatuses);
                                    newFragment.show(ft, "dialog");
                                } else {
                                    for (ProjectImport r : importStatuses) {
                                        Sharing.importProject(r);
                                    }
                                    Sharing.cleanImport(importStatuses);
                                    file.delete();
                                    app().showToastMessage(R.string.success);
                                }
                                hideProgress();
                            } else {
                                file.delete();
                                Logger.w(this.getClass().getName(), "failed to import the project archive");
                                // failed to import translation
                                handle.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        app().showToastMessage(R.string.translation_import_failed);
                                    }
                                });
                            }
                        } catch (final IOException e) {
                            Logger.e(this.getClass().getName(), "Failed to download the file", e);
                            file.delete();
                            handle.post(new Runnable() {
                                @Override
                                public void run() {
                                    app().showException(e);
                                    hideProgress();
                                }
                            });
                        }
                    }
                });
            } else {
                // the server did not give us the expected response.
                app().showToastMessage("invalid response");
            }
        } else if(data[0].equals(SocketMessages.MSG_OK)) {
            // we are authorized to access the server
            // send public key to server
            String key;
            try {
                key = getPublicKeyString();
            } catch (Exception e) {
                Logger.e(this.getClass().getName(), "Missing the public key", e);
                // TODO: missing public key
                return;
            }
            mService.writeTo(server, SocketMessages.MSG_PUBLIC_KEY+":"+key);
        } else if(data[0].equals(SocketMessages.MSG_PROJECT_LIST)) {
            // the sever gave us the list of available projects for import
            String library = data[1];
            final ListMap<Model> listableProjects = new ListMap<>();

            JSONArray json;
            try {
                json = new JSONArray(library);
            } catch (final JSONException e) {
                handle.post(new Runnable() {
                    @Override
                    public void run() {
                        app().showException(e);
                    }
                });
                return;
            }

            ListMap<PseudoProject> pseudoProjects = new ListMap<>();

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

                        // load meta
                        // TRICKY: we are actually getting the meta names instead of the id's since we only receive one translation of the project info
                        PseudoProject rootPseudoProject = null;
                        if (projectInfoJson.has("meta")) {
                            JSONArray jsonMeta = projectInfoJson.getJSONArray("meta");
                            if(jsonMeta.length() > 0) {
                                // get the root meta
                                String metaSlug = jsonMeta.getString(0); // this is actually the meta name in this case
                                rootPseudoProject = pseudoProjects.get(metaSlug);
                                if(rootPseudoProject == null) {
                                    rootPseudoProject = new PseudoProject(metaSlug);
                                    pseudoProjects.add(rootPseudoProject.getId(), rootPseudoProject);
                                }
                                // load children meta
                                PseudoProject currentPseudoProject = rootPseudoProject;
                                for (int j = 1; j < jsonMeta.length(); j++) {
                                    PseudoProject sp = new PseudoProject(jsonMeta.getString(j));
                                    if(currentPseudoProject.getMetaChild(sp.getId()) != null) {
                                        // load already created meta
                                        currentPseudoProject = currentPseudoProject.getMetaChild(sp.getId());
                                    } else {
                                        // create new meta
                                        currentPseudoProject.addChild(sp);
                                        currentPseudoProject = sp;
                                    }
                                    // add to project
                                    p.addSudoProject(sp);
                                }
                                currentPseudoProject.addChild(p);
                            }
                        }

                        // available translation languages
                        JSONArray languagesJson = projectJson.getJSONArray("target_languages");
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
                        // add project or meta to the project list
                        if(rootPseudoProject == null) {
                            listableProjects.add(p.getId(), p);
                        } else {
                            listableProjects.add(rootPseudoProject.getId(), rootPseudoProject);
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
                    if(listableProjects.size() > 0) {
                        showProjectSelectionDialog(server, listableProjects.getAll().toArray(new Model[listableProjects.size()]));
                    } else {
                        // there are no available projects on the server
                        // TODO: eventually we'll want to display the user friendly name of the server.
                        app().showMessageDialog(server.getIpAddress(), getResources().getString(R.string.no_projects_available_on_server));
                    }
                }
            });
        } else if(data[0].equals(SocketMessages.MSG_PUBLIC_KEY)) {
            // receive the server's public key
            server.keyStore.add(PeerStatusKeys.PUBLIC_KEY, data[1]);
            server.keyStore.add(PeerStatusKeys.WAITING, false);
            server.keyStore.add(PeerStatusKeys.CONTROL_TEXT, getResources().getString(R.string.import_project));
            server.setIsConnected(true);
            handle.post(new Runnable() {
                @Override
                public void run() {
                    updatePeerList();
                }
            });
        }
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
        Client c = (Client)mService;
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
            c.writeTo(server, SocketMessages.MSG_PROJECT_ARCHIVE+":"+json.toString());
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
        // update the handler so we are connected to the correct activity
        // TODO: this is not nessesary
//        if(mService != null) {
//            mService.setHandler(new Handler(getMainLooper()));
//        }
    }
}
