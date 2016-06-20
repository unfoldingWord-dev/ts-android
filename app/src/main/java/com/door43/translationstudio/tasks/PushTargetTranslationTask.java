package com.door43.translationstudio.tasks;

import android.os.Process;

import org.unfoldingword.tools.logger.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.Profile;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.git.Repo;
import com.door43.translationstudio.git.TransportCallback;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;

import java.io.IOException;
import java.util.Collection;

/**
 * Pushes a target translation to the server
 */
public class PushTargetTranslationTask extends ManagedTask {

    public static final String TASK_ID = "push_target_translation_task";
    private final TargetTranslation targetTranslation;
    private final boolean pushTags;
    private Status status = Status.UNKNOWN;
    private String message = "";

    public PushTargetTranslationTask(TargetTranslation targetTranslation, boolean pushTags) {
        setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
        this.targetTranslation = targetTranslation;
        this.pushTags = pushTags;
    }

    @Override
    public void start() {
        Profile profile = AppContext.getProfile();
        if(AppContext.context().isNetworkAvailable() && profile != null && profile.gogsUser != null) {
            publishProgress(-1, "Uploading translation");
            String server = AppContext.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_GIT_SERVER, AppContext.context().getResources().getString(R.string.pref_default_git_server));
            String remote = server + ":" + profile.gogsUser.getUsername() + "/" + this.targetTranslation.getId() + ".git";
            try {
                this.targetTranslation.commitSync();
                Repo repo = this.targetTranslation.getRepo();
                this.message = push(repo, remote);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String push(Repo repo, String remote) throws JGitInternalException {
        Git git;
        try {
            git = repo.getGit();
        } catch (IOException e1) {
            return null;
        }
        // TODO: we might want to get some progress feedback for the user
        PushCommand pushCommand = git.push()
                .setTransportConfigCallback(new TransportCallback())
                .setRemote(remote)
                .setProgressMonitor(new ProgressMonitor() {
                    @Override
                    public void start(int totalTasks) {

                    }

                    @Override
                    public void beginTask(String title, int totalWork) {

                    }

                    @Override
                    public void update(int completed) {

                    }

                    @Override
                    public void endTask() {

                    }

                    @Override
                    public boolean isCancelled() {
                        return false;
                    }
                })
                .setForce(false)
                .setPushAll();
        if(this.pushTags) {
            pushCommand.setPushTags();
        }

        try {
            Iterable<PushResult> result = pushCommand.call();
            StringBuffer response = new StringBuffer();
            this.status = Status.OK; // will be OK if no errors are found
            for (PushResult r : result) {
                Collection<RemoteRefUpdate> updates = r.getRemoteUpdates();
                for (RemoteRefUpdate update : updates) {
                    response.append(parseRemoteRefUpdate(update, remote));

                    RemoteRefUpdate.Status status = update.getStatus();
                    if( (RemoteRefUpdate.Status.OK == status) || (RemoteRefUpdate.Status.UP_TO_DATE == status)) {
                        //no error here
                    } else if( ( RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD == status)
                            || ( RemoteRefUpdate.Status.REJECTED_NODELETE == status)
                            || ( RemoteRefUpdate.Status.REJECTED_REMOTE_CHANGED == status)
                            || ( RemoteRefUpdate.Status.REJECTED_OTHER_REASON == status) ) {

                        this.status = Status.REJECTED;
                    } else {
                        this.status = Status.UNKNOWN;
                    }
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
                    if ("Auth fail".equals(detail)) {
                        this.status = Status.AUTH_FAILURE;
                    }
                } else if(cause instanceof NoRemoteRepositoryException) {
                    this.status = Status.NO_REMOTE_REPO;
                } else if(cause.getMessage().contains("not permitted")) {
                    this.status = Status.AUTH_FAILURE;
                }
            }
            return null;
        } catch (Exception e) {
            Logger.e(this.getClass().getName(), e.getMessage(), e);
            return null;
        } catch (OutOfMemoryError e) {
            Logger.e(this.getClass().getName(), e.getMessage(), e);
            this.status = Status.OUT_OF_MEMORY;
            return null;
        } catch (Throwable e) {
            Logger.e(this.getClass().getName(), e.getMessage(), e);
            return null;
        }
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public enum Status {
        OK,
        OUT_OF_MEMORY,
        AUTH_FAILURE,
        NO_REMOTE_REPO,
        REJECTED,
        UNKNOWN
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
}
