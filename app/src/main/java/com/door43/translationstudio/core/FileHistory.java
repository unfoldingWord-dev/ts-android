package com.door43.translationstudio.core;

import com.door43.translationstudio.git.Repo;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Represents the commit history of a single file within a git repository
 */
public class FileHistory {
    private final Repo repo;
    private final File file;
    private final Git git;
    private RevCommit[] history = new RevCommit[0];
    private int index = 0;

    /**
     *
     * @param gitRepo
     * @param relativeFile relative path to file in repo
     */
    public FileHistory(Repo gitRepo, File relativeFile) throws IOException, GitAPIException {
        this.repo = gitRepo;
        this.git = gitRepo.getGit();

        // sanitize file to be relative to repo
        if(relativeFile != null) {
            int pos = relativeFile.toString().indexOf(gitRepo.getLocalPath());
            if (pos >= 0) {
                this.file = new File(relativeFile.toString().substring(pos + gitRepo.getLocalPath().length() + 1));
            } else {
                this.file = null;
            }
        } else {
            this.file = null;
        }
    }

    /**
     * Reloads the commit history
     */
    public void loadHistory() throws IOException, GitAPIException {
        if(this.file != null) {
            // preserve current position if not at HEAD
            RevCommit currentCommit = null;
            if(this.index > 0) {
                currentCommit = current();
            }

            // load history
            Repository repository = this.git.getRepository();
            ObjectId head = repository.resolve("HEAD");
            LogCommand log = this.git.log();
            log.add(head);
            log.addPath(this.file.toString());
            Iterable<RevCommit> commits = log.call();
            ArrayList<RevCommit> historyList = new ArrayList<>();
            for(RevCommit commit:commits) {
                historyList.add(commit);

                // restore current position
                if(currentCommit != null) {
                    String hash = commit.toString().split(" ")[1];
                    String currentHash = currentCommit.toString().split(" ")[1];
                    if(hash.equals(currentHash)) {
                        index = historyList.size() - 1;
                        currentCommit = null;
                    }
                }
            }
            this.history = historyList.toArray(new RevCommit[historyList.size()]);
        } else {
            this.history = new RevCommit[0];
        }
    }

    /**
     * Checks if there is a previous commit
     * @return
     */
    public boolean hasPrevious() {
        return this.index + 1 < this.history.length;
    }

    /**
     * Returns the previous commit in the file history
     * The position in the history will not be changed if the previous index would be out of bounds
     * @return null if we have reached the bottom of the commit history
     */
    public RevCommit previous() {
        int prevIndex = this.index + 1;
        RevCommit commit = getCommit(prevIndex);
        if(commit != null) {
            this.index = prevIndex;
        }
        return commit;
    }

    /**
     * Returns the currently viewed commit of the file history
     * @return null if the history was not loaded
     */
    public RevCommit current() {
        return getCommit(this.index);
    }

    /**
     * Checks if there is a next commit
     * @return
     */
    public boolean hasNext() {
        return this.index -1 >= 0 && this.history.length > 0;
    }

    /**
     * Returns the next commit in the file history
     * The position in the history will not be changed if the next index would be out of bounds
     * @return null if we have reached the top of the commit history
     */
    public RevCommit next() {
        int nextIndex = this.index - 1;
        RevCommit commit = getCommit(nextIndex);
        if(commit != null) {
            this.index = nextIndex;
        }
        return commit;
    }

    /**
     * Returns the HEAD of the commit tree
     * @return null if the history was not loaded
     */
    public RevCommit head() {
        return getCommit(0);
    }

    /**
     * Resets the history back to HEAD
     */
    public void reset() {
        this.index = 0;
    }

    /**
     * Returns the commit at the given position
     * @param pos the position of the commit in history
     * @return null if the position is out of bounds
     */
    private RevCommit getCommit(int pos) {
        if(pos >= 0 && pos < this.history.length) {
            return this.history[pos];
        } else {
            return null;
        }
    }

    /**
     * Returns the file contents at the given commit
     * @param commit
     * @return
     */
    public String read(RevCommit commit) throws IOException, IllegalStateException {
        if(commit != null) {
            TreeWalk walk = new TreeWalk(this.git.getRepository());
            walk.addTree(commit.getTree());
            walk.setRecursive(true);
            walk.setFilter(PathFilter.create(this.file.toString()));
            if (!walk.next()) {
                throw new IllegalStateException("Did not find expected file '" + this.file.toString() + "'");
            }
            ObjectId objectId = walk.getObjectId(0);
            ObjectLoader loader = this.git.getRepository().open(objectId);
            return new String(loader.getBytes(), "UTF-8");
        } else {
            return null;
        }
    }
}
