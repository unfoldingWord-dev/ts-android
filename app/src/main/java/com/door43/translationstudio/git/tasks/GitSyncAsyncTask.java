package com.door43.translationstudio.git.tasks;

import android.os.AsyncTask;

import com.door43.translationstudio.util.MainContext;

/**
 * Created by joel on 9/15/2014.
 */
public abstract class GitSyncAsyncTask<A, B, C> extends AsyncTask<A, B, C> {
    protected Throwable mException;
    protected int mErrorRes = 0;

    protected void setException(Throwable e) {
        mException = e;
    }

    protected void setException(Throwable e, int errorRes) {
        mException = e;
        mErrorRes = errorRes;
    }

    protected void showError() {
        if (mErrorRes != 0) {
            MainContext.getContext().showException(mException, mErrorRes);
        } else if (mException != null) {
            MainContext.getContext().showException(mException);
        }
    }

    private boolean mIsCanceled = false;

    public void cancelTask() {
        mIsCanceled = true;
    }

    public boolean isTaskCanceled() {
        return mIsCanceled;
    }

    public static interface AsyncTaskPostCallback {
        public void onPostExecute(Boolean isSuccess);
    }

    public static interface AsyncTaskCallback {
        public boolean doInBackground(Void... params);

        public void onPreExecute();

        public void onProgressUpdate(String... progress);

        public void onPostExecute(Boolean isSuccess);
    }
}
