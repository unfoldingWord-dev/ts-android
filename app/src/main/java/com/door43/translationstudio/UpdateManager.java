package com.door43.translationstudio;

import android.util.Log;

/**
 * Handles updates from one version to another.
 * This may be process heavy so don't run this on the ui thread
 */
public class UpdateManager {
    private final int mOldVersionCode;
    private final int mNewVersionCode;
    private final String TAG = "UpdateManager";
    private OnProgressCallback mCallback;

    public UpdateManager(int oldVersionCode, int newVersionCode) {
        mOldVersionCode = oldVersionCode;
        mNewVersionCode = newVersionCode;
    }

    /**
     * Performs updates nessesary to migrate the app from mOldVersionCode to mNewVersionCode
     * @param callback the progress callback that will receive progress events
     */
    public void run(OnProgressCallback callback) {
        mCallback = callback;

        if(mOldVersionCode == 0) {
            // perform migration from 1.x to 2.x
            Log.d(TAG, "performing migration from 1.x to 2.x");
        }
    }

    /**
     * Performs updates nessesary to migrate the app from mOldVersionCode to mNewVersionCode
     */
    public void run() {
        run(null);
    }

    /**
     * Submits the progress to the callback if it is not null
     * @param progress
     */
    private void onProgress(double progress) {
        if(mCallback != null) {
            mCallback.onProgress(progress);
        }
    }

    public interface OnProgressCallback {
        void onProgress(double progress);
        void finished();
    }
}
