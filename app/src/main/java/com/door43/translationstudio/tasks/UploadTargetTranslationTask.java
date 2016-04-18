package com.door43.translationstudio.tasks;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.git.Repo;
import com.door43.translationstudio.git.TransportCallback;
import com.door43.translationstudio.AppContext;
import com.door43.util.tasks.ManagedTask;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;

import java.io.IOException;
import java.util.Collection;

/**
 * Uploads a target translation to the server.
 *
 */
@Deprecated
public class UploadTargetTranslationTask extends ManagedTask {
    public static final String TASK_ID = "upload_target_translation";
    public static final String AUTH_FAIL = "Auth fail";
    private final TargetTranslation mTargetTranslation;
    private String mResponse = "";
    private boolean mUploadSucceeded = true;
    private boolean mUploadAuthFailure = false;

    public UploadTargetTranslationTask(TargetTranslation targetTranslation) {
        setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT);
        mTargetTranslation = targetTranslation;
    }

    @Override
    public void start() {
        mUploadSucceeded = true;
        mUploadAuthFailure = false;
        if(AppContext.context().isNetworkAvailable()) {
            publishProgress(-1, AppContext.context().getResources().getString(R.string.loading));
            if(!AppContext.context().hasRegisteredKeys()) {
                // register the keys
                if(AppContext.context().hasKeys()) {
                    // open tcp connection with server
                    publishProgress(-1, AppContext.context().getResources().getString(R.string.submitting_security_keys));
                    KeyRegistration keyReg = new KeyRegistration();
                    keyReg.registerKeys(new KeyRegistration.OnRegistrationFinishedListener() {
                        @Override
                        public void onRestoreFinish(boolean registrationSuccess) {
                            upload();
                        }
                    });
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

    public boolean uploadAuthFailure() {
        return mUploadAuthFailure;
    }

    /**
     * Uploads the target translation to the server
     */
    private void upload() {
        publishProgress(-1, AppContext.context().getResources().getString(R.string.uploading));

        String server = AppContext.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_GIT_SERVER, AppContext.context().getResources().getString(R.string.pref_default_git_server));
        String targetTranslationRemoteRepository = server + ":tS/" + AppContext.udid() + "/" + mTargetTranslation.getId();

        // push the translation
        Repo translationRepo = mTargetTranslation.getRepo();
        try {
            if (commitRepo(translationRepo)) {
                Logger.i(this.getClass().getName(), "Pushing target translation " + mTargetTranslation.getId() + " to " + targetTranslationRemoteRepository);
                mResponse = pushRepo(translationRepo, targetTranslationRemoteRepository);
            }
        } catch (JGitInternalException e) {
            Logger.e(this.getClass().getName(), "Failed to push the target translation " + mTargetTranslation.getId() + " to " + targetTranslationRemoteRepository, e);
            mUploadSucceeded = false;
        }
    }

    /**
     * Pushes a repository to the server
     * @param repo
     * @return
     */
    private String pushRepo(Repo repo, String remote) throws JGitInternalException {
        Git git;
        try {
            git = repo.getGit();
        } catch (IOException e1) {
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
            Throwable cause = e.getCause();
            if(cause != null) {
                Throwable subException = cause.getCause();
                if(subException != null) {
                    String detail = subException.getMessage();
                    if (AUTH_FAIL.equals(detail)) {
                        mUploadAuthFailure = true; // we do special handling for auth failure
                    }
                }
            }
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
    private boolean commitRepo(Repo repo) throws JGitInternalException {
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
        } catch (IOException e) {
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

   public TargetTranslation getTargetTranslation() {
        return mTargetTranslation;
    }
}
