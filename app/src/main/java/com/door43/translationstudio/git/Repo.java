package com.door43.translationstudio.git;

import android.util.SparseArray;

import com.door43.translationstudio.git.tasks.repo.RepoOpTask;
import com.door43.util.FileUtilities;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.errors.LockFailedException;
import org.eclipse.jgit.lib.StoredConfig;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by joel on 9/15/2014.
 */
public class Repo {
    private static int numRepos;
    private static SparseArray<RepoOpTask> mRepoTasks = new SparseArray<RepoOpTask>();

    private Git mGit;
    private int mId;
    private String mLocalPath;
    private StoredConfig mStoredConfig;
    private Set<String> mRemotes = new HashSet<String>();

    /**
     * Creates a new repository instance
     * @param repositoryPath the path to the repository directory (not including the .git directory)
     */
    public Repo(String repositoryPath) {
        // automatically generate repo ids
        mId = numRepos;
        numRepos ++;

        // create the directory if missing
        File repoPath = new File(repositoryPath);
        if(!repoPath.exists()) {
            repoPath.mkdir();
        }

        mLocalPath = repositoryPath;

        // initialize new repository
        File gitPath = new File(mLocalPath + "/.git");
        if(!gitPath.exists()) {
            initRepo();
        }
    }

    /**
     * Initialize the git repository
     */
    private void initRepo() {
        InitCommand init = Git.init();
        File initFile = new File(getLocalPath());
        init.setDirectory(initFile);
        try {
            init.call();
        } catch (GitAPIException e) {
            e.printStackTrace();
            // could not create repo
        }
    }

    /**
     * Returns this repository's unique id
     * @return
     */
    public int getID() {
        return mId;
    }

    /**
     * Returns the repository directory
     * @return
     */
    public File getDir() {
        return new File(getLocalPath());
    }

    public Git getGit() throws IOException {
        if (mGit != null) {
            return mGit;
        } else {
            File repoFile = getDir();
            mGit = Git.open(repoFile);
            return mGit;
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
//            App.context().showException(e);
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
        if (mRemotes.size() > 0)
            return mRemotes;
        try {
            StoredConfig config = getStoredConfig();
            Set<String> remotes = config.getSubsections("remote");
            mRemotes = new HashSet<String>(remotes);
            return mRemotes;
        } catch (IOException e) {
            e.printStackTrace();
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteRemote(String remote) throws IOException {
        StoredConfig config = getStoredConfig();
        config.unsetSection("remote", remote);
    }

    public StoredConfig getStoredConfig() throws IOException {
        if (mStoredConfig == null) {
            mStoredConfig = getGit().getRepository().getConfig();
        }
        return mStoredConfig;
    }

    /**
     * This will call a git command while attempting to handle lock exceptions.
     * If the repo is locked it will wait and try again several times before removing the lock and
     * calling the command once more. This last call may throw an exception.
     *
     * @param command the command to call
     */
    public static Object forceCall(GitCommand command) throws GitAPIException {
        try {
            return command.call();
        } catch (JGitInternalException | GitAPIException e) {
            // throw the error if not a lock exception
            Throwable cause = getCause(e, LockFailedException.class);
            if(cause == null) throw e;
        }

        // re-try several times
        int attempts = 0;
        do {
            attempts ++;
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                return command.call();
            } catch (JGitInternalException | GitAPIException e) {
                // throw the error if not a lock exception
                Throwable cause = getCause(e, LockFailedException.class);
                if(cause == null) {
                    throw e;
                }
            }
        } while(attempts < 30);

        // remove lock and call once more
        File gitDir = command.getRepository().getDirectory();
        File lockFile = new File(gitDir, "index.lock");
        if(lockFile.exists()) FileUtilities.deleteQuietly(lockFile);
        return command.call();
    }

    /**
     * Checks if the throwable has the given cause
     * @param thrown the thrown object
     * @param cause the cause class
     * @return the matched cause
     */
    private static Throwable getCause(Throwable thrown, Class cause) {
        if(cause.isInstance(thrown)) return thrown;
        Throwable child = thrown.getCause();
        do {
            if(cause.isInstance(child)) return child;
            child = child.getCause();
        } while(child != null);
        return null;
    }
}
