package com.door43.translationstudio.git.tasks;

import android.app.ProgressDialog;

import com.door43.translationstudio.util.MainContext;

/**
 * Displays a progress dialog to the user
 * TODO: add support to display as a progress bar
 */
public class ProgressCallback implements GitSyncAsyncTask.AsyncTaskCallback {
    private int mInitMsg;
    private ProgressDialog dialog;

    public ProgressCallback(int initMsg) {
        mInitMsg = initMsg;
        dialog = new ProgressDialog(MainContext.getContext().getCurrentActivity());
    }

    @Override
    public void onPreExecute() {
        dialog.setMessage(MainContext.getContext().getString(mInitMsg));
        dialog.show();
    }

    @Override
    public void onProgressUpdate(String... progress) {

    }

    @Override
    public void onPostExecute(Boolean isSuccess) {
        if(dialog.isShowing()) {
            dialog.dismiss();
        }
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
