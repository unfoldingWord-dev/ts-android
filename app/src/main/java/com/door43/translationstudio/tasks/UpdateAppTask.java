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

        lastVersionCode = 114; // TODO: 3/8/16 remove

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
        // perform migrations
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
        if(lastVersion < 111) {
            upgradePre111();
        }
        if(lastVersion < 112) {
            upgradePre112();
        }
        if(lastVersion < 113) {
            upgradePre113();
        }
        if(lastVersion < 115) {
            upgradePre115();
        }
        updateTargetTranslations();
        updateBuildNumbers();
    }

    /**
     * Updates the target translations
     * NOTE: we used to do this manually but now we run this every time so we don't have to manually
     * add a new migration path each time
     */
    private void updateTargetTranslations() {
        TargetTranslation[] targetTranslations = AppContext.getTranslator().getTargetTranslations();
        for(TargetTranslation tt:targetTranslations) {
            if(!TargetTranslationMigrator.migrate(tt.getPath())) {
                Logger.w(this.getClass().getName(), "Failed to migrate the target translation " + tt.getId());
            }
            try {
                tt.commitSync();
            } catch (Exception e) {
                Logger.e(this.getClass().getName(), "Failed to commit migration changes to target translation " + tt.getId());
            }
        }
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
     * We updated the source
     */
    private void upgradePre115() {
        AppContext.context().deleteDatabase(Library.DATABASE_NAME);
    }

    /**
     * We updated the source
     */
    private void upgradePre113() {
        AppContext.context().deleteDatabase(Library.DATABASE_NAME);
    }

    /**
     * We made some updates to the db schema and updated the source.
     */
    private void upgradePre112() {
        AppContext.context().deleteDatabase(Library.DATABASE_NAME);
    }
    
    /**
     * We moved the target translations to the public files directory so that they persist when the
     * app is uninstalled
     */
    private void upgradePre111() {
        File legacyTranslationsDir = new File(mContext.getFilesDir(), "translations");
        File translationsDir = AppContext.getTranslator().getPath();

        if(legacyTranslationsDir.exists()) {
            translationsDir.mkdirs();
            File[] oldFiles = legacyTranslationsDir.listFiles();
            boolean errors = false;
            for(File file:oldFiles) {
                File newFile = new File(translationsDir, file.getName());
                try {
                    if(file.isDirectory()) {
                        FileUtils.copyDirectory(file, newFile);
                    } else {
                        FileUtils.copyFile(file, newFile);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Logger.e(this.getClass().getName(), "Failed to move targetTranslation", e);
                    errors = true;
                }
            }
            // remove old files if there were no errors
            if(!errors) {
                FileUtils.deleteQuietly(legacyTranslationsDir);
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
                tt.commitSync();
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
