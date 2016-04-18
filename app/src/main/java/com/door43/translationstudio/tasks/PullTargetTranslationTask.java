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

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;

import java.io.IOException;
import java.util.Iterator;

/**
 * Created by joel on 4/18/16.
 */
public class PullTargetTranslationTask extends ManagedTask {

    private final TargetTranslation targetTranslation;
    private String message = "";
    private Status status = Status.UNKNOWN;

    /**
     * todo: eventually we need to support pulling from a particular user.
     * We may need to create a different task or just pass in the target translation id
     * @param targetTranslation
     */
    public PullTargetTranslationTask(TargetTranslation targetTranslation) {
        setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
        this.targetTranslation = targetTranslation;
    }

    @Override
    public void start() {
        Profile profile = AppContext.getProfile();
        if(AppContext.context().isNetworkAvailable() && profile != null && profile.gogsUser != null) {
            String server = AppContext.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_GIT_SERVER, AppContext.context().getResources().getString(R.string.pref_default_git_server));
            String remote = server + ":" + profile.gogsUser.getUsername() + "/" + this.targetTranslation.getId() + ".git";

            Repo repo = this.targetTranslation.getRepo();
            this.message = pull(repo, remote);
        }
    }

    private String pull(Repo repo, String remote) {
        Git git;
        try {
            git = repo.getGit();
        } catch (IOException e1) {
            return null;
        }

        // TODO: we might want to get some progress feedback for the user
        PullCommand pullCommand = git.pull()
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
                });
        try {
            PullResult result = pullCommand.call();
            this.status = Status.OK;
            // TODO: 4/18/16 parse result
            return "message";
        } catch (GitAPIException e) {
            Logger.e(this.getClass().getName(), e.getMessage(), e);
        }
        return null;
    }

    public String getMessage() {
        return message;
    }

    public Status getStatus() {
        return status;
    }

    public enum Status {
        OK,
        MERGE_CONFLICTS,
        UNKNOWN
    }
}
