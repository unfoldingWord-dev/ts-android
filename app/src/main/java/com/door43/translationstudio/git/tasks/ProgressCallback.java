package com.door43.translationstudio.git.tasks;

import android.app.ProgressDialog;

import com.door43.translationstudio.util.MainContext;

/**
 * Displays a progress dialog to the user
 * TODO: add support to display as a progress bar
 */
public class ProgressCallback implements GitSyncAsyncTask.AsyncTaskCallback {
    private int mInitMsg;

    public ProgressCallback(int initMsg) {
        mInitMsg = initMsg;
    }

    @Override
    public void onPreExecute() {
        MainContext.getContext().showProgressDialog(mInitMsg);
    }

    @Override
    public void onProgressUpdate(String... progress) {

    }

    @Override
    public void onPostExecute(Boolean isSuccess) {
        MainContext.getContext().closeProgressDialog();
    }

    @Override
    public boolean doInBackground(Void... params) {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            return false;
        }
        return true;
    }
}
