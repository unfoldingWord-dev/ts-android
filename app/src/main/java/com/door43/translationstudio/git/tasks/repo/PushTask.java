package com.door43.translationstudio.git.tasks.repo;

import com.door43.translationstudio.R;
import com.door43.translationstudio.git.Repo;
import com.door43.translationstudio.git.TransportCallback;
import com.door43.translationstudio.AppContext;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;

import java.io.IOException;
import java.util.Collection;

/**
 * An asyncronous task to push commits to a remote branch.
 */
public class PushTask extends RepoOpTask {
    private AsyncTaskCallback mCallback;
    private boolean mPushAll;
    private boolean mForcePush;
    private String mRemote;
    private StringBuffer resultMsg = new StringBuffer();

    public PushTask(Repo repo, String remote, boolean pushAll, boolean forcePush, AsyncTaskCallback callback) {
        super(repo);
        mCallback = callback;
        mPushAll = pushAll;
        mRemote = remote;
        mForcePush = forcePush;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        boolean result = pushRepo();
        if (mCallback != null) {
            result = mCallback.doInBackground(params) & result;
        }
        return result;
    }

    @Override
    protected void onProgressUpdate(String... progress) {
        super.onProgressUpdate(progress);
        if (mCallback != null) {
            mCallback.onProgressUpdate(progress);
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (mCallback != null) {
            mCallback.onPreExecute();
        }
    }

    protected void onPostExecute(Boolean isSuccess) {
        super.onPostExecute(isSuccess);
        if (mCallback != null) {
            mCallback.onPostExecute(isSuccess);
        }
        if (isSuccess) {
            AppContext.context().showMessageDialogDetails(R.string.success, R.string.project_uploaded, resultMsg.toString());
        }
    }

    /**
     * Pushes the repository to the remote server.
     * NOTE: the remote must already have been set.
     * @return
     */
    public boolean pushRepo() {
        Git git;
        try {
            git = mRepo.getGit();
        } catch (IOException e1) {
            return false;
        }
        PushCommand pushCommand = git.push().setPushTags()
                .setProgressMonitor(new BasicProgressMonitor())
                .setTransportConfigCallback(new TransportCallback())
                .setRemote(mRemote)
                .setForce(mForcePush);
        if (mPushAll) {
            pushCommand.setPushAll();
        } else {
            RefSpec spec = new RefSpec(mRepo.getBranchName());
            pushCommand.setRefSpecs(spec);
        }

        try {
            Iterable<PushResult> result = pushCommand.call();
            for (PushResult r : result) {
                Collection<RemoteRefUpdate> updates = r.getRemoteUpdates();
                for (RemoteRefUpdate update : updates) {
                    parseRemoteRefUpdate(update);
                }
            }
        } catch (TransportException e) {
            setException(e);
            return false;
        } catch (Exception e) {
            setException(e);
            return false;
        } catch (OutOfMemoryError e) {
            setException(e, R.string.error_out_of_memory);
            return false;
        } catch (Throwable e) {
            setException(e);
            return false;
        }
        return true;
    }

    private void parseRemoteRefUpdate(RemoteRefUpdate update) {
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
        msg += "\n" + String.format(AppContext.context().getResources().getString(R.string.git_server_details), mRemote);
        resultMsg.append(msg);
    }
}
