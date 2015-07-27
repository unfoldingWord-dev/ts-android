package com.door43.translationstudio.service;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.device2device.PeerStatusKeys;
import com.door43.translationstudio.device2device.SocketMessages;
import com.door43.translationstudio.dialogs.ProjectTranslationImportApprovalDialog;
import com.door43.translationstudio.network.Connection;
import com.door43.translationstudio.network.Peer;
import com.door43.translationstudio.network.Service;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.PseudoProject;
import com.door43.translationstudio.projects.Sharing;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.projects.imports.ProjectImport;
import com.door43.util.ListMap;
import com.door43.util.RSAEncryption;
import com.door43.util.StringUtilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class provides an importing service (effectively a client) that can
 * communicate with an exporting service (server) to browse and retrieve translations
 */
public class ImportingService extends NetworkService {
    private static final String PARAM_PUBLIC_KEY = "param_public_key";
    private static final String PARAM_PRIVATE_KEY = "param_private_key";
    private final IBinder mBinder = new LocalBinder();
    private Callbacks mListener;
    private static boolean sRunning = false;
    private Map<String, Connection> mServerConnections = new HashMap<>();
    private PrivateKey mPrivateKey;
    private String mPublicKey;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void registerCallback(Callbacks callback) {
        mListener = callback;
        if(sRunning && mListener != null) {
            mListener.onImportServiceReady();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        Bundle args = intent.getExtras();
        if(args != null) {
            mPrivateKey = (PrivateKey)args.get(PARAM_PRIVATE_KEY);
            mPublicKey = args.getString(PARAM_PUBLIC_KEY);
            return START_STICKY;
        } else {
            Logger.e(this.getClass().getName(), "Import service requires arguments");
            stopService();
            return START_NOT_STICKY;
        }
    }

    /**
     * Establishes a TCP connection with the server.
     * Once this connection has been made the cleanup thread won't identify the server as lost unless the tcp connection is also disconnected.
     * @param server the server we will connect to
     */
    public void connectToServer(Peer server) {
        if(!mServerConnections.containsKey(server.getIpAddress())) {
            ServerThread serverThread = new ServerThread(server);
            new Thread(serverThread).start();
        }
    }

    /**
     * Stops the service
     */
    public void stopService() {
        Logger.i(this.getClass().getName(), "Stopping import service");
        sRunning = false;
    }

    /**
     * Checks if the service is running
     * @return
     */
    public static boolean isRunning() {
        return sRunning;
    }

    @Override
    public void onDestroy() {
        stopService();
    }

    /**
     * Sends a message to the peer
     * @param server the client to which the message will be sent
     * @param message the message being sent to the client
     */
    private void sendMessage(Peer server, String message) {
        if (mServerConnections.containsKey(server.getIpAddress())) {
            if(server.isConnected()) {
                // encrypt message
                PublicKey key = RSAEncryption.getPublicKeyFromString(server.keyStore.getString(PeerStatusKeys.PUBLIC_KEY));
                if(key != null) {
                    message = encryptMessage(key, message);
                } else {
                    Logger.w(this.getClass().getName(), "Missing the server's public key");
                    message = SocketMessages.MSG_EXCEPTION;
                }
            }
            mServerConnections.get(server.getIpAddress()).write(message);
        }
    }

    /**
     * Requests a list of projects from the server
     * @param server the server that will give the project list
     * @param preferredLanguages the languages preferred by the client
     */
    public void requestProjectList(Peer server, List<String> preferredLanguages) {
        JSONArray languagesJson = new JSONArray();
        for(String l:preferredLanguages) {
            languagesJson.put(l);
        }
        sendMessage(server, SocketMessages.MSG_PROJECT_LIST + ":" + languagesJson);
    }

    public void requestProjectArchive(Peer server, JSONObject languagesJson) {
        sendMessage(server, SocketMessages.MSG_PROJECT_ARCHIVE + ":" + languagesJson);
    }

    /**
     * Handles the initial handshake and authorization
     * @param server
     * @param message
     */
    private void onMessageReceived(Peer server, String message) {
        if(server.isConnected()) {
            message = decryptMessage(mPrivateKey, message);
            if(message != null) {
                String[] data = StringUtilities.chunk(message, ":");
                Logger.i(this.getClass().getName(), "message from " + server.getIpAddress() + ": " + message);
                onCommandReceived(server, data[0], Arrays.copyOfRange(data, 1, data.length-1));
            } else if(mListener != null) {
                mListener.onImportServiceError(new Exception("Message descryption failed"));
            }
        } else {
            String[] data = StringUtilities.chunk(message, ":");
            switch(data[0]) {
                case SocketMessages.MSG_PUBLIC_KEY:
                    // receive the server's public key
                    server.keyStore.add(PeerStatusKeys.PUBLIC_KEY, data[1]);
                    server.keyStore.add(PeerStatusKeys.WAITING, false);
                    server.keyStore.add(PeerStatusKeys.CONTROL_TEXT, getResources().getString(R.string.import_project));
                    server.setIsConnected(true);
                    if(mListener != null) {
                        mListener.onServerConnectionChanged(server);
                    }
                    break;
                case SocketMessages.MSG_OK:
                    // we are authorized to access the server
                    // send public key to server
                    sendMessage(server, SocketMessages.MSG_PUBLIC_KEY + ":" + mPublicKey);
                    break;
                default:
                    Logger.w(this.getClass().getName(), "Invalid request: " + message);
                    sendMessage(server, SocketMessages.MSG_INVALID_REQUEST);
            }
        }
    }

    /**
     * Handles commands sent from the server
     * @param server
     * @param command
     * @param data
     */
    private void onCommandReceived(final Peer server, String command, String[] data) {
        switch(command) {
            case SocketMessages.MSG_PROJECT_ARCHIVE:
                // receive project archive from server
                JSONObject infoJson;
                try {
                    infoJson = new JSONObject(data[1]);
                } catch (JSONException e) {
                    if(mListener != null) {
                        mListener.onImportServiceError(e);
                    }
                    break;
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
                        if(mListener != null) {
                            mListener.onImportServiceError(e);
                        }
                        break;
                    }
                    // the server is sending a project archive
                    generateReadSocket(server, port, new OnSocketEventListener() {
                        @Override
                        public void onOpen(Connection connection) {
                            connection.setOnCloseListener(new Connection.OnCloseListener() {
                                @Override
                                public void onClose() {
                                    if(mListener != null) {
                                        mListener.onImportServiceError(new Exception("Socket was closed before download completed"));
                                    }
                                }
                            });

//                            showProgress(getResources().getString(R.string.downloading));
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
                                    if(mListener != null) {
                                        mListener.onServerConnectionChanged(server);
                                    }
                                    out.write(buffer, 0, count);
                                }
                                server.keyStore.add(PeerStatusKeys.PROGRESS, 0);
                                if(mListener != null) {
                                    mListener.onServerConnectionChanged(server);
                                }
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
                                        // TODO: stash the changes and ask the user to review warnings
//                                        FragmentTransaction ft = getFragmentManager().beginTransaction();
//                                        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
//                                        if (prev != null) {
//                                            ft.remove(prev);
//                                        }
//                                        ft.addToBackStack(null);
//                                        app().closeToastMessage();
//                                        ProjectTranslationImportApprovalDialog newFragment = new ProjectTranslationImportApprovalDialog();
//                                        newFragment.setOnClickListener(new ProjectTranslationImportApprovalDialog.OnClickListener() {
//                                            @Override
//                                            public void onOk(ProjectImport[] requests) {
//                                                showProgress(getResources().getString(R.string.loading));
//                                                for (ProjectImport r : requests) {
//                                                    Sharing.importProject(r);
//                                                }
//                                                Sharing.cleanImport(requests);
//                                                file.delete();
//                                                hideProgress();
//                                                app().showToastMessage(R.string.success);
//                                            }
//
//                                            @Override
//                                            public void onCancel(ProjectImport[] requests) {
//                                                // import was aborted
//                                                Sharing.cleanImport(requests);
//                                                file.delete();
//                                            }
//                                        });
//                                        // NOTE: we don't place this dialog into the peer dialog map because this will work even if the server disconnects
//                                        newFragment.setImportRequests(importStatuses);
//                                        newFragment.show(ft, "dialog");
                                    } else {
                                        for (ProjectImport r : importStatuses) {
                                            Sharing.importProject(r);
                                        }
                                        Sharing.cleanImport(importStatuses);
                                        file.delete();
//                                        app().showToastMessage(R.string.success);
                                        // TODO: notify we are done
                                    }
//                                    hideProgress();
                                } else {
                                    file.delete();
                                    Logger.w(this.getClass().getName(), "failed to import the project archive");
                                    if(mListener != null) {
                                        mListener.onImportServiceError(new Exception("failed to import the project archive"));
                                    }
                                }
                            } catch (IOException e) {
                                Logger.e(this.getClass().getName(), "Failed to download the file", e);
                                file.delete();
                                if(mListener != null) {
                                    mListener.onImportServiceError(e);
                                }
                            }
                        }
                    });
                } else {
                    Logger.w(this.getClass().getName(), "Invalid response from server: " + data.toString());
                    if(mListener != null) {
                        mListener.onImportServiceError(new Exception("Invalid response from server"));
                    }
                }
                break;
            case SocketMessages.MSG_PROJECT_LIST:
                // the sever gave us the list of available projects for import
                String library = data[1];
                final ListMap<Model> listableProjects = new ListMap<>();

                JSONArray json;
                try {
                    json = new JSONArray(library);
                } catch (final JSONException e) {
                    if(mListener != null) {
                        mListener.onImportServiceError(e);
                    }
                    break;
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
                            Logger.w(this.getClass().getName(), "An invalid response was received from the server");
                        }
                    } catch(final JSONException e) {
                        if(mListener != null) {
                            mListener.onImportServiceError(e);
                        }
                    }
                }

//                handle.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        hideProgress();
//                        if(listableProjects.size() > 0) {
//                            showProjectSelectionDialog(server, listableProjects.getAll().toArray(new Model[listableProjects.size()]));
//                        } else {
//                            // there are no available projects on the server
//                            // TODO: eventually we'll want to display the user friendly name of the server.
//                            app().showMessageDialog(server.getIpAddress(), getResources().getString(R.string.no_projects_available_on_server));
//                        }
//                    }
//                });
                break;
            default:
                sendMessage(server, SocketMessages.MSG_INVALID_REQUEST);
        }
    }

    /**
     * Interface for communication with service clients.
     */
    public interface Callbacks {
        void onImportServiceReady();
        void onServerConnectionLost(Peer peer);
        void onServerConnectionChanged(Peer peer);
        void onImportServiceError(Throwable e);
    }
    /**
     * Class to retrieve instance of service
     */
    public class LocalBinder extends Binder {
        public ImportingService getServiceInstance() {
            return ImportingService.this;
        }
    }

    /**
     * Manages a single server connection on it's own thread
     */
    private class ServerThread implements Runnable {
        private Connection mConnection;
        private Peer mServer;

        public ServerThread(Peer server) {
            mServer = server;
        }

        @Override
        public void run() {
            // set up sockets
            try {
                InetAddress serverAddr = InetAddress.getByName(mServer.getIpAddress());
                mConnection = new Connection(new Socket(serverAddr, mServer.getPort()));
                mConnection.setOnCloseListener(new Connection.OnCloseListener() {
                    @Override
                    public void onClose() {
                        Thread.currentThread().interrupt();
                    }
                });
                // we store references to all connections so we can access them later
                mServerConnections.put(mConnection.getIpAddress(), mConnection);
            } catch (Exception e) {
                mListener.onImportServiceError(e);
                Thread.currentThread().interrupt();
                return;
            }

            // begin listening to server
            while (!Thread.currentThread().isInterrupted()) {
                String message = mConnection.readLine();
                if(message == null) {
                    Thread.currentThread().interrupt();
                } else {
                    onMessageReceived(mServer, message);
                }
            }
            // close the connection
            mConnection.close();
            // remove all instances of the peer
            if(mServerConnections.containsKey(mConnection.getIpAddress())) {
                mServerConnections.remove(mConnection.getIpAddress());
            }
            removePeer(mServer);
            mListener.onServerConnectionLost(mServer);
        }
    }
}
