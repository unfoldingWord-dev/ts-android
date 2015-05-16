package com.door43.translationstudio.tasks;

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.migration.UpdateManager;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.Logger;
import com.door43.util.tasks.ManagedTask;

/**
 * Created by joel on 5/4/2015.
 */
public class UpdateAppTask extends ManagedTask {
    public static final String TASK_ID = "update_app";
    private String mError = null;

    /**
     * Returns any error messages
     * @return
     */
    public String error() {
        return mError;
    }

    @Override
    public void start() {
        SharedPreferences settings = AppContext.context().getSharedPreferences(MainApplication.PREFERENCES_TAG, AppContext.context().MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        int lastVersionCode = settings.getInt("last_version_code", 0);
        PackageInfo pInfo = null;
        try {
            pInfo = AppContext.context().getPackageManager().getPackageInfo(AppContext.context().getPackageName(), 0);
            editor.putInt("last_version_code", pInfo.versionCode);
            editor.apply();
            if(pInfo.versionCode > lastVersionCode) {
                // update the app
                publishProgress(-1, "performing updates");
                UpdateManager updater = new UpdateManager(lastVersionCode, pInfo.versionCode);
                updater.run(new UpdateManager.OnProgressCallback() {
                    @Override
                    public void onProgress(double progress, String message) {
                        publishProgress(progress * 100, message);
                    }

                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onError(String message) {
                        mError = message;
                        Logger.w(this.getClass().getName(), message);
                    }
                });
            } else {
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int maxProgress() {
        return 100;
    }
}
