package com.door43.translationstudio.git.tasks.repo;

import android.widget.Toast;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.git.Repo;
import com.door43.translationstudio.git.tasks.GitSyncAsyncTask;

import org.eclipse.jgit.lib.ProgressMonitor;

/**
 * Created by joel on 9/15/2014.
 */
public abstract class RepoOpTask extends GitSyncAsyncTask<Void, String, Boolean> {
    protected Repo mRepo;
    protected boolean mIsTaskAdded;
    private int mSuccessMsg = 0;

    public RepoOpTask(Repo repo) {
        mRepo = repo;
        mIsTaskAdded = repo.addTask(this);
    }

    protected void onPostExecute(Boolean isSuccess) {
        super.onPostExecute(isSuccess);
        mRepo.removeTask(this);
        if (!isSuccess && !isTaskCanceled()) {
            showError();
            return;
        }
        if (isSuccess && mSuccessMsg != 0) {
            // success
        }
    }

    protected void setSuccessMsg(int successMsg) {
        mSuccessMsg = successMsg;
    }

    public void executeTask() {
        if (mIsTaskAdded) {
            execute();
            return;
        }
        // task already running
    }

//    protected void handleAuthError(OnPasswordEntered onPassEntered) {
//        String msg = mException.getMessage();
//
//        if (msg == null)
//            return;
//
//        if ((!msg.contains("Auth fail"))
//                && (!msg.toLowerCase().contains("auth")))
//            return;
//
//        String errorInfo = null;
//        if (msg.contains("Auth fail")) {
//            errorInfo = BasicFunctions.getActiveActivity().getString(
//                    R.string.dialog_prompt_for_password_title_auth_fail);
//        }
//        BasicFunctions.getActiveActivity().promptForPassword(onPassEntered,
//                errorInfo);
//    }

    class BasicProgressMonitor implements ProgressMonitor {

        private int mTotalWork;
        private int mWorkDone;
        private String mTitle;

        @Override
        public void start(int i) {
        }

        @Override
        public void beginTask(String title, int totalWork) {
            mTotalWork = totalWork;
            mWorkDone = 0;
            if (title != null) {
                mTitle = title;
            }
            setProgress();
        }

        @Override
        public void update(int i) {
            mWorkDone += i;
            if (mTotalWork != ProgressMonitor.UNKNOWN && mTotalWork != 0) {
                setProgress();
            }
        }

        @Override
        public void endTask() {

        }

        @Override
        public boolean isCancelled() {
            return isTaskCanceled();
        }

        private void setProgress() {
            String msg = mTitle;
            int showedWorkDown = Math.min(mWorkDone, mTotalWork);
            int progress = 0;
            String rightHint = "0/0";
            String leftHint = "0%";
            if (mTotalWork != 0) {
                progress = 100 * showedWorkDown / mTotalWork;
                rightHint = showedWorkDown + "/" + mTotalWork;
                leftHint = progress + "%";
            }
            publishProgress(msg, leftHint, rightHint, Integer.toString(progress));
        }

    }
}

