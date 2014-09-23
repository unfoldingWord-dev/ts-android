package com.door43.tcp.tasks;

import android.os.AsyncTask;

/**
 * Created by joel on 9/19/2014.
 */
public abstract class TCPAsyncTask<A, B, C> extends AsyncTask<A, B, C> {
    private boolean mIsCanceled = false;

    public void cancelTask() {
        mIsCanceled = true;
    }

    public boolean isTaskCanceled() {
        return mIsCanceled;
    }

    public static interface AsyncTaskCallback {
        public boolean doInBackground(Void... params);

        public void onPreExecute();

        public void onProgressUpdate(String... progress);

        public void onPostExecute(Boolean isSuccess);
    }
}
