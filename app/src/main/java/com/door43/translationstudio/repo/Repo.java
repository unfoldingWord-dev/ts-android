package com.door43.translationstudio.repo;

import android.util.SparseArray;

import com.door43.translationstudio.R;
import com.door43.translationstudio.util.MainContextLink;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.StoredConfig;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by joel on 9/15/2014.
 */
public class Repo {
    private Git mGit;
    private String mLocalPath;
    private static SparseArray<RepoOpTask> mRepoTasks = new SparseArray<RepoOpTask>();
    private StoredConfig mStoredConfig;
    private Set<String> mRemotes;

    /**
     * Creates a new repository instance
     * @param repositoryPath the path to the repository directory (not including the .git directory)
     */
    public Repo(String repositoryPath) {
        mLocalPath = repositoryPath + "/.git";
    }

    public int getID() {
        // TODO: impliment
        return 0;
    }

    /**
     * Returns the repository directory
     * @return
     */
    public File getDir() {
        return new File(getLocalPath());
    }

    public Git getGit() throws StopTaskException {
        if (mGit != null)
            return mGit;
        try {
            File repoFile = getDir();
            mGit = Git.open(repoFile);
            return mGit;
        } catch (RepositoryNotFoundException e) {
            MainContextLink.getContext().showException(e, R.string.error_repository_not_found);
            throw new StopTaskException();
        } catch (IOException e) {
            MainContextLink.getContext().showException(e);
            throw new StopTaskException();
        }
    }

    /**
     * Returns the local path to the repository
     * @return
     */
    public String getLocalPath() {
        return mLocalPath;
    }

    public void cancelTask() {
        RepoOpTask task = mRepoTasks.get(getID());
        if(task == null) {
            return;
        } else {
            task.cancelTask();
            removeTask(task);
        }
    }

    public String getBranchName() {
        try {
            return getGit().getRepository().getFullBranch();
        } catch (IOException e) {
            MainContextLink.getContext().showException(e);
        } catch (StopTaskException e) {
        }
        return "";
    }

    public void removeTask(RepoOpTask task) {
        RepoOpTask runningTask = mRepoTasks.get(getID());
        if (runningTask == null || runningTask != task)
            return;
        mRepoTasks.remove(getID());
    }

    public boolean addTask(RepoOpTask task) {
        if (mRepoTasks.get(getID()) != null)
            return false;
        mRepoTasks.put(getID(), task);
        return true;
    }

    public Set<String> getRemotes() {
        if (mRemotes != null)
            return mRemotes;
        try {
            StoredConfig config = getStoredConfig();
            Set<String> remotes = config.getSubsections("remote");
            mRemotes = new HashSet<String>(remotes);
            return mRemotes;
        } catch (StopTaskException e) {
        }
        return new HashSet<String>();
    }

    public void setRemote(String remote, String url) throws IOException {
        try {
            StoredConfig config = getStoredConfig();
            Set<String> remoteNames = config.getSubsections("remote");
            if (remoteNames.contains(remote)) {
                throw new IOException(String.format(
                        "Remote %s already exists.", remote));
            }
            config.setString("remote", remote, "url", url);
            String fetch = String.format("+refs/heads/*:refs/remotes/%s/*",
                    remote);
            config.setString("remote", remote, "fetch", fetch);
            config.save();
            mRemotes.add(remote);
        } catch (StopTaskException e) {
        }
    }

    public StoredConfig getStoredConfig() throws StopTaskException {
        if (mStoredConfig == null) {
            mStoredConfig = getGit().getRepository().getConfig();
        }
        return mStoredConfig;
    }
}
