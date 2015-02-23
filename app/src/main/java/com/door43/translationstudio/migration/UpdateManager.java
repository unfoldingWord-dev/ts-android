package com.door43.translationstudio.migration;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.door43.translationstudio.util.FileUtilities;
import com.door43.logging.Logger;
import com.door43.translationstudio.util.AppContext;

import java.io.File;

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
            onProgress(0, "performing migration from 1.x to 2.x");

            File db = new File("/data/data/com.translationstudio.androidapp/app_webview/databases/file__0/1");
            if(db.exists() && db.isFile()) {
                // perform migration
                PackageInfo pInfo;
                try {
                    pInfo = AppContext.context().getPackageManager().getPackageInfo(AppContext.context().getPackageName(), 0);
                } catch (PackageManager.NameNotFoundException e) {
                    Logger.e(this.getClass().getName(), "failed to identify package directory", e);
                    return;
                }
//                String databasePath = pInfo.applicationInfo.dataDir;
                SQLiteMigrationHelper migrationHelper = new SQLiteMigrationHelper(AppContext.context(), pInfo.applicationInfo.dataDir);
                try {
                    migrationHelper.migrateDatabase(new SQLiteMigrationHelper.OnProgressCallback() {
                        @Override
                        public void onProgress(double progress, String message) {
                            UpdateManager.this.onProgress(progress, message);
                        }
                    });
                } catch(Exception e) {
                    Logger.e(this.getClass().getName(), "migration failed", e);
                    UpdateManager.this.onProgress(100, "Migration may not have been successful.");
                }

                // backup database
                File backup = new File(AppContext.context().getFilesDir(), "1.x_backup.sqlite3");
                FileUtilities.moveOrCopy(db, backup);

                // clean up old 1.x files
                FileUtilities.deleteRecursive(new File(AppContext.context().getFilesDir(), "Documents"));
                FileUtilities.deleteRecursive(new File(pInfo.applicationInfo.dataDir, "app_database"));
                FileUtilities.deleteRecursive(new File(pInfo.applicationInfo.dataDir, "app_webview")); // this technically deletes 2.x stuff too, but it will be rebuilt automatically
            } else {
                onError("The database from version 1.x could not be found at " + db.getAbsolutePath());
            }
        }
        onSuccess();
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
    private void onProgress(double progress, String message) {
        if(mCallback != null) {
            mCallback.onProgress(progress, message);
        }
    }

    /**
     * lets the callback know we are done
     */
    private void onSuccess() {
        if(mCallback != null) {
            mCallback.onSuccess();
        }
    }

    /**
     * Sends an error message to the callback
     * @param message
     */
    private void onError(String message) {
        if(mCallback != null) {
            mCallback.onError(message);
        }
    }

    public interface OnProgressCallback {
        void onProgress(double progress, String message);
        void onSuccess();
        void onError(String message);
    }
}
