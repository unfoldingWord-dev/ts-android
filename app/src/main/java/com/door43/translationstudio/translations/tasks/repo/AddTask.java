package com.door43.translationstudio.translations.tasks.repo;

import com.door43.translationstudio.R;
import com.door43.translationstudio.translations.tasks.StopTaskException;
import com.door43.translationstudio.translations.Repo;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

public class AddTask extends RepoOpTask {

    public String mFilePattern;

    public AddTask(Repo repo, String filepattern) {
        super(repo);
        mFilePattern = filepattern;
        setSuccessMsg(R.string.success_auto_save);
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        return addToStage();
    }

    protected void onPostExecute(Boolean isSuccess) {
        super.onPostExecute(isSuccess);
    }

    public boolean addToStage() {
        Git git = null;
        try {
            git = mRepo.getGit();
        } catch (StopTaskException e) {
            setException(e);
            return false;
        }

        // stage changes
        AddCommand add = git.add();
        try {
            add.addFilepattern(".").call();
        } catch (Throwable e) {
            setException(e);
            return false;
        }

        // commit the change
        CommitCommand commit = git.commit();
        commit.setAll(true);
        commit.setMessage("auto save");
        try {
            commit.call();
        } catch (GitAPIException e) {
            setException(e);
            return false;
        }
        return true;
    }
}