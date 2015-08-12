package com.door43.translationstudio.tasks;

import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.git.Repo;
import com.door43.translationstudio.git.TransportCallback;
import com.door43.translationstudio.git.tasks.StopTaskException;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.ProjectManager;
import com.door43.translationstudio.projects.TranslationNote;
import com.door43.translationstudio.user.ProfileManager;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.FileUtilities;
import com.door43.tools.reporting.Logger;
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
 * This task will upload the device public ssh key to the server
 */
public class UploadProjectTask extends ManagedTask {

    public static final String TASK_ID = "push_project_task";
    private final Project mProject;
    private final Language mTargetLanguage;
    private final String mAuthServer;
    private final int mAuthServerPort;
    private String mErrorMessages = "";
    private String mResponse = "";

    public UploadProjectTask(Project p, Language target) {
        setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT);
        mProject = p;
        mTargetLanguage = target;
        mAuthServer = AppContext.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_AUTH_SERVER, AppContext.context().getResources().getString(R.string.pref_default_auth_server));
        mAuthServerPort = Integer.parseInt(AppContext.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_AUTH_SERVER_PORT, AppContext.context().getResources().getString(R.string.pref_default_auth_server_port)));
    }

    @Override
    public void start() {
        if(AppContext.context().isNetworkAvailable()) {
            publishProgress(-1, AppContext.context().getResources().getString(R.string.loading));
            if(!AppContext.context().hasRegisteredKeys()) {
                if(AppContext.context().hasKeys()) {
                    publishProgress(-1, AppContext.context().getResources().getString(R.string.submitting_security_keys));
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
                                        processError(e);
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
                                        processResponse(serverMessage);
                                        messageReceived = true;
                                    }
                                    serverMessage = null;
                                }
                            } catch (JSONException e) {
                                processError(e);
                            } catch (Exception e) {
                                processError(e);
                            }
                        } catch (Exception e) {
                            processError(e);
                        } finally {
                            socket.close();

                            // stop everything
                            if(in != null) {
                                try {
                                    in.close();
                                } catch (IOException e) {
                                    processError(e);
                                }
                            }
                            if(out != null) {
                                out.close();
                            }
                        }
                    } catch (Exception e) {
                        processError(e);
                    }
                } else {
                    processError(R.string.error_missing_ssh_keys);
                }
            } else {
                pushProject();
            }
        } else {
            processError(R.string.internet_not_available);
        }
    }

    /**
     * Returns the response message
     * @return
     */
    public String getResponse() {
        return mResponse;
    }

    /**
     * Checks if there were any errors
     * @return
     */
    public boolean hasErrors() {
        return !mErrorMessages.isEmpty();
    }

    /**
     * Returns the error messages
     * @return
     */
    public String getErrors() {
        return mErrorMessages;
    }

    /**
     * Processes the response from the server
     * @param response
     */
    private void processResponse(String response) {
        try {
            JSONObject json = new JSONObject(response);
            if (json.has("ok")) {
                AppContext.context().setHasRegisteredKeys(true);
                pushProject();
            } else {
                processError(new Exception(json.getString("error")));
            }
        } catch (JSONException e) {
            processError(e);
        }
    }

    /**
     * Uploads the project repository to the server
     */
    private void pushProject() {
        if(mProject.translationIsReady(mTargetLanguage)) {
            publishProgress(-1, AppContext.context().getResources().getString(R.string.publishing_translation));
        } else {
            publishProgress(-1, AppContext.context().getResources().getString(R.string.backing_up_translation));
        }
        // commit and push project
        Repo projectRepo = new Repo(ProjectManager.getRepositoryPath(mProject, mTargetLanguage));
        if(commitRepo(projectRepo)) {
            mResponse = pushRepo(projectRepo, ProjectManager.getRemotePath(mProject, mTargetLanguage));
        }

        // commit and push translation notes
        Repo notesRepo = new Repo(TranslationNote.getRepositoryPath(mProject.getId(), mTargetLanguage.getId()));
        if(commitRepo(notesRepo)) {
            String response = pushRepo(notesRepo, TranslationNote.getRemotePath(mProject.getId(), mTargetLanguage.getId()));
            if(response != null) {
                mResponse += "\n" + response;
            }
        }

        // commit and push profile
        if(mProject.translationIsReady(mTargetLanguage)) {
            Repo profileRepo = new Repo(ProfileManager.getRepositoryPath());
            if (commitRepo(profileRepo)) {
                String response = pushRepo(profileRepo, ProfileManager.getRemotePath());
                if(response != null) {
                    mResponse += "\n" + response;
                }
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
//                .setProgressMonitor(...)
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
            processError(e);
            return null;
        } catch (Exception e) {
            processError(e);
            return null;
        } catch (OutOfMemoryError e) {
            processError(e);
            return null;
        } catch (Throwable e) {
            processError(e);
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
            processError(e);
            return false;
        } catch (GitAPIException e) {
            processError(e);
            return false;
        }
        return true;
    }

    /**
     * Handles an error
     * @param e
     */
    private void processError(Throwable e) {
        Logger.e(this.getClass().getName(), "Project upload exception", e);
        mErrorMessages += e.getMessage() + "\n";
    }

    /**
     * Handles an error
     * @param resId
     */
    private void processError(int resId) {
        String message = AppContext.context().getResources().getString(resId);
        Logger.e(this.getClass().getName(), message);
        mErrorMessages += message + "\n";
    }
}
