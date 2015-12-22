package com.door43.translationstudio.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TargetTranslationMigrator;
import com.door43.util.tasks.ManagedTask;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * This tasks performs any upgrades that need to occur between app versions
 */
public class UpdateAppTask extends ManagedTask {
    public static final String TASK_ID = "update_app";
    private final Context mContext;
    private String mError = null;

    public UpdateAppTask(Context context) {
        mContext = context;
    }

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
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (pInfo != null) {
            // record latest version
            editor.putInt("last_version_code", pInfo.versionCode);
            editor.apply();

            // check if update is possible
            if (pInfo.versionCode > lastVersionCode) {
                performUpdates(lastVersionCode, pInfo.versionCode);
            }
        }
    }

    /**
     * Performs required updates between the two app versions
     * @param lastVersion
     * @param currentVersion
     */
    private void performUpdates(int lastVersion, int currentVersion) {
        if(lastVersion < 87) {
            upgradePre87();
        }
        if(lastVersion < 103) {
            upgradePre103();
        }
        if(lastVersion < 106) {
            upgradePre106();
        }
        if(lastVersion < 107) {
            upgradePre107();
        }
        if(lastVersion < 108) {
            upgradePre108();
        }
        if(lastVersion < 109) {
            upgradePre109();
        }
        if(lastVersion < 110) {
            upgradePre110();
        }

        updateBuildNumbers();
    }

    /**
     * Updates the generator information for the target translations
     */
    private void updateBuildNumbers() {
        TargetTranslation[] targetTranslations = AppContext.getTranslator().getTargetTranslations();
        for(TargetTranslation tt:targetTranslations) {
            try {
                TargetTranslation.updateGenerator(mContext, tt);
            } catch (Exception e) {
                Logger.e(this.getClass().getName(), "Failed to update the generator in the target translation " + tt.getId());
            }
        }
    }

    /**
     * We need to migrate chunks in targetTranslations because some no longer match up to the source.
     */
    private void upgradePre110() {
        AppContext.context().deleteDatabase(Library.DATABASE_NAME);

        // TRICKY: we deploy the new library in a different task but since we are using it we need to do so now
        try {
            AppContext.deployDefaultLibrary();
        } catch (Exception e) {
            Logger.e(this.getClass().getName(), "Failed to deploy the default index", e);
        }
        
        // migrate broken chunks
        TargetTranslation[] targetTranslations = AppContext.getTranslator().getTargetTranslations();
        TargetTranslationMigrator.migrateChunkChanges(AppContext.getLibrary(), targetTranslations);

    }

    /**
     * Updated db schema
     */
    private void upgradePre109() {
        AppContext.context().deleteDatabase(Library.DATABASE_NAME);
    }

    /**
     * Updated the source content
     */
    private void upgradePre108() {
        AppContext.context().deleteDatabase(Library.DATABASE_NAME);
    }

    /**
     * We made some changes to the target translation manifest structure
     */
    private void upgradePre107() {
        TargetTranslation[] targetTranslations = AppContext.getTranslator().getTargetTranslations();
        for(TargetTranslation tt:targetTranslations) {
            if(!TargetTranslationMigrator.migrate(tt.getPath())) {
                Logger.w(this.getClass().getName(), "Failed to migrate the target translation " + tt.getId());
            }
            try {
                tt.commit();
            } catch (Exception e) {
                Logger.e(this.getClass().getName(), "Failed to commit migration changes to target translation " + tt.getId());
            }
        }
    }

    /**
     * Minor schema changes and updated source content.
     */
    private void upgradePre106() {
        // clear old index so new index is loaded
        AppContext.context().deleteDatabase(Library.DATABASE_NAME);
    }

    /**
     * Major changes.
     * Moved to the new object management system.
     */
    private void upgradePre103() {
        publishProgress(-1, "Updating translations");
        Logger.i(this.getClass().getName(), "Upgrading source data management from pre 103");

        // migrate target translations and profile
        File oldTranslationsDir = new File(AppContext.context().getFilesDir(), "git");
        File newTranslationsDir = AppContext.getTranslator().getPath();
        newTranslationsDir.mkdirs();
        File oldProfileDir = new File(oldTranslationsDir, "profile");
        File newProfileDir = new File(AppContext.context().getFilesDir(), AppContext.PROFILES_DIR + "/profile");
        newProfileDir.getParentFile().mkdirs();
        try {
            if(oldProfileDir.exists()) {
                FileUtils.deleteQuietly(newProfileDir);
                FileUtils.moveDirectory(oldProfileDir, newProfileDir);
            }
        } catch (IOException e) {
            Logger.e(this.getClass().getName(), "Failed to migrate the profile", e);
        }
        try {
            if(oldTranslationsDir.exists() && oldTranslationsDir.list().length > 0) {
                FileUtils.deleteQuietly(newTranslationsDir);
                FileUtils.moveDirectory(oldTranslationsDir, newTranslationsDir);
            } else if(oldTranslationsDir.exists()) {
                FileUtils.deleteQuietly(oldTranslationsDir);
            }
        } catch (IOException e) {
            Logger.e(this.getClass().getName(), "Failed to migrate the target translations", e);
        }

        // remove old source
        File oldSourceDir = new File(AppContext.context().getFilesDir(), "assets");
        File oldTempSourceDir = new File(AppContext.context().getCacheDir(), "assets");
        File oldIndexDir = new File(AppContext.context().getCacheDir(), "index");
        FileUtils.deleteQuietly(oldSourceDir);
        FileUtils.deleteQuietly(oldTempSourceDir);
        FileUtils.deleteQuietly(oldIndexDir);

        // remove old caches
        File oldP2PDir = new File(AppContext.context().getExternalCacheDir(), "transferred");
        File oldExportDir = new File(AppContext.context().getCacheDir(), "exported");
        File oldImportDir = new File(AppContext.context().getCacheDir(), "imported");
        File oldSharingDir = new File(AppContext.context().getCacheDir(), "sharing");
        FileUtils.deleteQuietly(oldP2PDir);
        FileUtils.deleteQuietly(oldExportDir);
        FileUtils.deleteQuietly(oldImportDir);
        FileUtils.deleteQuietly(oldSharingDir);

        // clear old logs and crash reports
        Logger.flush();
        File stacktraceDir = new File(AppContext.context().getExternalCacheDir(), AppContext.context().STACKTRACE_DIR);
        FileUtils.deleteQuietly(stacktraceDir);
    }

    /**
     * Change default font to noto because most of the others do not work
     */
    private void upgradePre87() {
        publishProgress(-1, "Updating fonts");
        Logger.i(this.getClass().getName(), "Upgrading fonts from pre 87");
        SharedPreferences.Editor editor = AppContext.context().getUserPreferences().edit();
        editor.putString(SettingsActivity.KEY_PREF_TRANSLATION_TYPEFACE, AppContext.context().getString(R.string.pref_default_translation_typeface));
        editor.apply();
    }

    @Override
    public int maxProgress() {
        return 100;
    }
}
