package com.door43.translationstudio.tasks;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.git.Repo;
import com.door43.translationstudio.git.TransportCallback;
import com.door43.translationstudio.git.tasks.StopTaskException;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.ProjectManager;
import com.door43.translationstudio.user.ProfileManager;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.FileUtilities;
import com.door43.util.tasks.ManagedTask;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Collection;

/**
 * Performs the uploading of a translation to the server.
 * This will upload a target translation as the user profile
 */
public class UploadTargetTranslationTask extends ManagedTask {
    public static final String TASK_ID = "upload_target_translation";
    private final TargetTranslation mTargetTranslation;
    private final String mAuthServer;
    private final int mAuthServerPort;
    private String mResponse = "";
    private boolean mUploadSucceeded = true;

    public UploadTargetTranslationTask(TargetTranslation targetTranslation) {
        setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT);
        mTargetTranslation = targetTranslation;
        mAuthServer = AppContext.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_AUTH_SERVER, AppContext.context().getResources().getString(R.string.pref_default_auth_server));
        mAuthServerPort = Integer.parseInt(AppContext.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_AUTH_SERVER_PORT, AppContext.context().getResources().getString(R.string.pref_default_auth_server_port)));
    }

    @Override
    public void start() {
        mUploadSucceeded = true;
        if(AppContext.context().isNetworkAvailable()) {
            publishProgress(-1, AppContext.context().getResources().getString(R.string.loading));
            if(!AppContext.context().hasRegisteredKeys()) {
                // register the keys
                if(AppContext.context().hasKeys()) {
                    // open tcp connection with server
                    publishProgress(-1, AppContext.context().getResources().getString(R.string.submitting_security_keys));
                    registerKeys();
                } else {
                    Logger.w(this.getClass().getName(), "The ssh keys have not been generated");
                    mUploadSucceeded = false;
                }
            } else {
                upload();
            }
        } else {
            Logger.w(this.getClass().getName(), "Cannot upload target translation:" + mTargetTranslation.getId() + ". Network is not available");
            mUploadSucceeded = false;
        }
    }

    public String getResponse() {
        return mResponse;
    }

    public boolean uploadSucceeded() {
        return mUploadSucceeded;
    }

    /**
     * Uploads the target translation to the server
     */
    private void upload() {
        publishProgress(-1, AppContext.context().getResources().getString(R.string.publishing_translation));

        String server = AppContext.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_GIT_SERVER, AppContext.context().getResources().getString(R.string.pref_default_git_server));
        String targetTranslationRemoteRepository = server + ":tS/" + AppContext.udid() + "/uw-" + mTargetTranslation.getProjectId() + "-" + mTargetTranslation.getTargetLanguageId();

        // push the translation
        Repo translationRepo = mTargetTranslation.getRepo();
        if(commitRepo(translationRepo)) {
            Logger.i(this.getClass().getName(), "Pushing target translation " + mTargetTranslation.getId() + " to " + targetTranslationRemoteRepository);
            mResponse = pushRepo(translationRepo, targetTranslationRemoteRepository);
        }

        // push the profile
        // TODO: we need to update how profiles are managed
        Repo profileRepo = new Repo(ProfileManager.getRepositoryPath());
        if (commitRepo(profileRepo)) {
            Logger.i(this.getClass().getName(), "Pushing profile to " + ProfileManager.getRemotePath());
            String profileResponse = pushRepo(profileRepo, ProfileManager.getRemotePath());
            if(profileResponse != null) {
                mResponse += "\n" + profileResponse;
            }
        }
    }

    /**
     * Pushes a repository to the server
     * @param repo
     * @return
     */
    private String pushRepo(Repo repo, String remote) {
        Git git;
        try {
            git = repo.getGit();
        } catch (StopTaskException e1) {
            return null;
        }
        // TODO: we might want to get some progress feedback for the user
        PushCommand pushCommand = git.push().setPushTags()
                .setTransportConfigCallback(new TransportCallback())
                .setRemote(remote)
                .setForce(true)
                .setPushAll();

        try {
            Iterable<PushResult> result = pushCommand.call();
            StringBuffer response = new StringBuffer();
            for (PushResult r : result) {
                Collection<RemoteRefUpdate> updates = r.getRemoteUpdates();
                for (RemoteRefUpdate update : updates) {
                    response.append(parseRemoteRefUpdate(update, remote));
                }
            }
            // give back the response message
            return response.toString();
        } catch (TransportException e) {
            Logger.e(this.getClass().getName(), e.getMessage(), e);
            mUploadSucceeded = false;
            return null;
        } catch (Exception e) {
            Logger.e(this.getClass().getName(), e.getMessage(), e);
            mUploadSucceeded = false;
            return null;
        } catch (OutOfMemoryError e) {
            Logger.e(this.getClass().getName(), e.getMessage(), e);
            mUploadSucceeded = false;
            return null;
        } catch (Throwable e) {
            Logger.e(this.getClass().getName(), e.getMessage(), e);
            mUploadSucceeded = false;
            return null;
        }
    }

    /**
     * Parses the response from the remote
     * @param update
     * @return
     */
    private String parseRemoteRefUpdate(RemoteRefUpdate update, String remote) {
        String msg = null;
        switch (update.getStatus()) {
            case AWAITING_REPORT:
                msg = String.format(AppContext.context().getResources().getString(R.string.git_awaiting_report), update.getRemoteName());
                break;
            case NON_EXISTING:
                msg = String.format(AppContext.context().getResources().getString(R.string.git_non_existing), update.getRemoteName());
                break;
            case NOT_ATTEMPTED:
                msg = String.format(AppContext.context().getResources().getString(R.string.git_not_attempted), update.getRemoteName());
                break;
            case OK:
                msg = String.format(AppContext.context().getResources().getString(R.string.git_ok), update.getRemoteName());
                break;
            case REJECTED_NODELETE:
                msg = String.format(AppContext.context().getResources().getString(R.string.git_rejected_nondelete), update.getRemoteName());
                break;
            case REJECTED_NONFASTFORWARD:
                msg = String.format(AppContext.context().getResources().getString(R.string.git_rejected_nonfastforward), update.getRemoteName());
                break;
            case REJECTED_OTHER_REASON:
                String reason = update.getMessage();
                if (reason == null || reason.isEmpty()) {
                    msg = String.format(AppContext.context().getResources().getString(R.string.git_rejected_other_reason), update.getRemoteName());
                } else {
                    msg = String.format(AppContext.context().getResources().getString(R.string.git_rejected_other_reason_detailed), update.getRemoteName(), reason);
                }
                break;
            case REJECTED_REMOTE_CHANGED:
                msg = String.format(AppContext.context().getResources().getString(R.string.git_rejected_remote_changed),update.getRemoteName());
                break;
            case UP_TO_DATE:
                msg = String.format(AppContext.context().getResources().getString(R.string.git_uptodate), update.getRemoteName());
                break;
        }
        msg += "\n" + String.format(AppContext.context().getResources().getString(R.string.git_server_details), remote);
        return msg;
    }

    /**
     * Commits any unstaged changes
     * @param repo
     */
    private boolean commitRepo(Repo repo) {
        try {
            Git git = repo.getGit();
            if(!git.status().call().isClean()) {
                // stage changes
                AddCommand add = git.add();
                add.addFilepattern(".").call();

                // commit changes
                CommitCommand commit = git.commit();
                commit.setAll(true);
                commit.setMessage("auto save");
                commit.call();
            }
        } catch (StopTaskException e) {
            Logger.e(this.getClass().getName(), e.getMessage(), e);
            mUploadSucceeded = false;
            return false;
        } catch (GitAPIException e) {
            Logger.e(this.getClass().getName(), e.getMessage(), e);
            mUploadSucceeded = false;
            return false;
        }
        return true;
    }

    /**
     * Submits the device public ssh key to the server
     */
    private void registerKeys() {
        Logger.i(this.getClass().getName(), "Registering ssh key with " + mAuthServer);
        // open tcp connection with server
        try {
            InetAddress serverAddr = InetAddress.getByName(mAuthServer);
            Socket socket = new Socket(serverAddr, mAuthServerPort);
            PrintWriter out = null;
            InputStream in = null;
            try {
                //send the message to the server
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                in = socket.getInputStream();

                // submit key
                JSONObject json = new JSONObject();
                try {
                    String key = FileUtilities.getStringFromFile(AppContext.context().getPublicKey().getAbsolutePath()).trim();
                    json.put("key", key);
                    json.put("udid", AppContext.udid());
                    // TODO: provide support for using user names
//                              json.put("username", "");
                    out.println(json.toString());

                    // read response
                    boolean messageReceived = false;
                    String serverMessage = null;
                    while(!interrupted() && !messageReceived) {
                        byte[] buffer = new byte[4096];
                        int read = 0;
                        try {
                            read = in.read(buffer, 0, 4096);
                        } catch (IOException e) {
                            Logger.e(this.getClass().getName(), "Could not read response from server while registering keys", e);
                            mUploadSucceeded = false;
                            break;
                        }

                        // receive data
                        // TODO: right now we just read the first buffer. We should probably create a way to determine how much data has actually been sent. Probably need to have the server specify how many bytes have been sent.
                        if (read != -1) {
                            byte[] tempdata = new byte[read];
                            System.arraycopy(buffer, 0, tempdata, 0, read);
                            serverMessage = new String(tempdata);
                        }

                        // handle response from the server
                        if (serverMessage != null) {
                            handleRegistrationResponse(serverMessage);
                            messageReceived = true;
                        }
                        serverMessage = null;
                    }
                } catch (JSONException e) {
                    Logger.e(this.getClass().getName(), "Failed to build key registration request", e);
                    mUploadSucceeded = false;
                }
            } catch (Exception e) {
                Logger.e(this.getClass().getName(), "Failed to submit the key registration request to the server", e);
                mUploadSucceeded = false;
            } finally {
                socket.close();

                // stop everything
                if(in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        Logger.e(this.getClass().getName(), "Failed to close tcp connection with key server", e);
                        mUploadSucceeded = false;
                    }
                }
                if(out != null) {
                    out.close();
                }
            }
        } catch (Exception e) {
            Logger.e(this.getClass().getName(), "Failed to open a tcp connection with the key server", e);
            mUploadSucceeded = false;
        }
    }

    /**
     * Handles the server's response
     * @param serverMessage
     */
    private void handleRegistrationResponse(String serverMessage) {
        try {
            JSONObject json = new JSONObject(serverMessage);
            if (json.has("ok")) {
                AppContext.context().setHasRegisteredKeys(true);
                upload();
            } else {
                Logger.e(this.getClass().getName(), "Key registration was refused", new Exception(json.getString("error")));
                mUploadSucceeded = false;
            }
        } catch (JSONException e) {
            Logger.e(this.getClass().getName(), "Failed to parse response from keys server", e);
            mUploadSucceeded = false;
        }
    }

    public TargetTranslation getTargetTranslation() {
        return mTargetTranslation;
    }
}
