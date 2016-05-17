package com.door43.translationstudio.tasks;

import android.os.Process;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.Profile;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.git.Repo;
import com.door43.translationstudio.git.TransportCallback;
import com.door43.util.tasks.ManagedTask;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.DeleteBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.merge.MergeStrategy;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Pulls down changes from a remote target translation repository
 */
public class PullTargetTranslationTask extends ManagedTask {

    public static final String TASK_ID = "pull_target_translation_task";
    private final TargetTranslation targetTranslation;
    private final MergeStrategy mergeStrategy;
    private String message = "";
    private Status status = Status.UNKNOWN;
    private Map<String, int[][]> conflicts = new HashMap<>();

    /**
     * todo: eventually we need to support pulling from a particular user.
     * We may need to create a different task or just pass in the target translation id
     * @param targetTranslation
     */
    public PullTargetTranslationTask(TargetTranslation targetTranslation) {
        setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
        this.targetTranslation = targetTranslation;
        this.mergeStrategy = MergeStrategy.RECURSIVE;
    }

    /**
     * todo: eventually we need to support pulling from a particular user.
     * We may need to create a different task or just pass in the target translation id
     * @param targetTranslation
     */
    public PullTargetTranslationTask(TargetTranslation targetTranslation, MergeStrategy mergeStrategy) {
        setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
        this.targetTranslation = targetTranslation;
        this.mergeStrategy = mergeStrategy;
    }

    @Override
    public void start() {
        Profile profile = AppContext.getProfile();
        if(AppContext.context().isNetworkAvailable() && profile != null && profile.gogsUser != null) {
            publishProgress(-1, "Downloading updates");
            String server = AppContext.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_GIT_SERVER, AppContext.context().getResources().getString(R.string.pref_default_git_server));
            String remote = server + ":" + profile.gogsUser.getUsername() + "/" + this.targetTranslation.getId() + ".git";
            try {
                this.targetTranslation.commitSync();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Repo repo = this.targetTranslation.getRepo();
            createBackupBranch(repo);
            this.message = pull(repo, remote);
        }
    }

    private void createBackupBranch(Repo repo) {
        try {
            Git git  = repo.getGit();
            DeleteBranchCommand deleteBranchCommand = git.branchDelete();
            deleteBranchCommand.setBranchNames("backup-master")
                    .setForce(true)
                    .call();
            CreateBranchCommand createBranchCommand = git.branchCreate();
            createBranchCommand.setName("backup-master")
                    .setForce(true)
                    .call();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String pull(Repo repo, String remote) {
        Git git;
        try {
            repo.deleteRemote("origin");
            repo.setRemote("origin", remote);
            git = repo.getGit();
        } catch (IOException e) {
            return null;
        }

        // TODO: we might want to get some progress feedback for the user
        PullCommand pullCommand = git.pull()
                .setTransportConfigCallback(new TransportCallback())
                .setRemote("origin")
                .setStrategy(mergeStrategy)
                .setRemoteBranchName("master")
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
                });
        try {
            PullResult result = pullCommand.call();
            MergeResult mergeResult = result.getMergeResult();
            if(mergeResult != null && mergeResult.getConflicts() != null && mergeResult.getConflicts().size() > 0) {
                this.status = Status.MERGE_CONFLICTS;
                this.conflicts = mergeResult.getConflicts();

                // revert manifest merge conflict to avoid corruption
                if(this.conflicts.containsKey("manifest.json")) {
                    try {
                        git.checkout()
                                .setStage(CheckoutCommand.Stage.OURS)
                                .addPath("manifest.json")
                                .call();
                    } catch (CheckoutConflictException e) {
                        // failed to reset manifest.json
                        Logger.e(this.getClass().getName(), e.getMessage(), e);
                    }
                }
            } else {
                this.status = Status.UP_TO_DATE;
            }
            return "message";
        } catch (TransportException e) {
            Logger.e(this.getClass().getName(), e.getMessage(), e);
            Throwable cause = e.getCause();
            if(cause != null) {
                Throwable subException = cause.getCause();
                if(subException != null) {
                    String detail = subException.getMessage();
                    if ("Auth fail".equals(detail)) {
                        this.status = Status.AUTH_FAILURE; // we do special handling for auth failure
                    }
                } else if(cause instanceof NoRemoteRepositoryException) {
                    this.status = Status.NO_REMOTE_REPO;
                }
            }
            return null;
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if(cause instanceof NoRemoteRepositoryException) {
                this.status = Status.NO_REMOTE_REPO;
            }
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

    public String getMessage() {
        return message;
    }

    public Status getStatus() {
        return status;
    }

    public Map<String, int[][]> getConflicts() {
        return conflicts;
    }

    public enum Status {
        UP_TO_DATE,
        MERGE_CONFLICTS,
        OUT_OF_MEMORY,
        AUTH_FAILURE,
        NO_REMOTE_REPO,
        UNKNOWN
    }
}
