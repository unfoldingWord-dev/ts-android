package com.door43.translationstudio.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.preference.PreferenceManager;

import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.ui.SettingsActivity;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TargetTranslationMigrator;
import com.door43.util.FileUtilities;

import org.unfoldingword.tools.taskmanager.ManagedTask;

import java.io.File;
import java.io.FileFilter;
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
        SharedPreferences settings = App.context().getSharedPreferences(App.PREFERENCES_TAG, App.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        int lastVersionCode = settings.getInt("last_version_code", 0);
        PackageInfo pInfo = null;
        try {
            pInfo = App.context().getPackageManager().getPackageInfo(App.context().getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (pInfo != null) {
            // use current version if fresh install
            lastVersionCode = lastVersionCode == 0 ? pInfo.versionCode : lastVersionCode;

            // record latest version
            editor.putInt("last_version_code", pInfo.versionCode);
            editor.apply();

            // check if update is possible
            if (pInfo.versionCode > lastVersionCode) {
                performUpdates(lastVersionCode, pInfo.versionCode);
            }
        }

        if(!App.isLibraryDeployed()) {
            try {
                App.deployDefaultLibrary();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        updateTargetTranslations();
        updateBuildNumbers();
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
        if(lastVersion < 111) {
            upgradePre111();
        }
        if(lastVersion < 122) {
            Looper.prepare();
            PreferenceManager.setDefaultValues(App.context(), R.xml.general_preferences, true);
        }
        if(lastVersion < 139) {
            // TRICKY: this was the old name of the database
            App.context().deleteDatabase("app");
        }
        if(lastVersion < 142) {
            // TRICKY: this was another old name of the database
            App.context().deleteDatabase("library");
        }

        // this should always be the latest version in which the library was updated
        if(lastVersion < 153) {
            App.deleteLibrary();
        }
    }

    /**
     * Updates the target translations
     * NOTE: we used to do this manually but now we run this every time so we don't have to manually
     * add a new migration path each time
     */
    private void updateTargetTranslations() {
        // TRICKY: we manually list the target translations because they won't be viewable until updated
        File translatorDir = App.getTranslator().getPath();
        File[] dirs = translatorDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory() && !pathname.getName().equals("cache");
            }
        });
        if(dirs != null) {
            for (File tt : dirs) {
                Logger.i(this.getClass().getSimpleName(),"Migrating: "+ tt);
                if (TargetTranslationMigrator.migrate(tt) == null) {
                    Logger.w(this.getClass().getName(), "Failed to migrate the target translation " + tt.getName());
                }
            }
        }

        // commit migration changes
        TargetTranslation[] translations = App.getTranslator().getTargetTranslations();
        for(TargetTranslation tt:translations) {
            try {
                tt.unlockRepo(); // TRICKY: prune dangling locks
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
        TargetTranslation[] targetTranslations = App.getTranslator().getTargetTranslations();
        for(TargetTranslation tt:targetTranslations) {
            try {
                TargetTranslation.updateGenerator(mContext, tt);
            } catch (Exception e) {
                Logger.e(this.getClass().getName(), "Failed to update the generator in the target translation " + tt.getId());
            }
        }
    }

    /**
     * We moved the target translations to the public files directory so that they persist when the
     * app is uninstalled
     */
    private void upgradePre111() {
        File legacyTranslationsDir = new File(mContext.getFilesDir(), "translations");
        File translationsDir = App.getTranslator().getPath();

        if(legacyTranslationsDir.exists()) {
            translationsDir.mkdirs();
            File[] oldFiles = legacyTranslationsDir.listFiles();
            boolean errors = false;
            for(File file:oldFiles) {
                File newFile = new File(translationsDir, file.getName());
                try {
                    if(file.isDirectory()) {
                        FileUtilities.copyDirectory(file, newFile, null);
                    } else {
                        FileUtilities.copyFile(file, newFile);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Logger.e(this.getClass().getName(), "Failed to move targetTranslation", e);
                    errors = true;
                }
            }
            // remove old files if there were no errors
            if(!errors) {
                FileUtilities.deleteQuietly(legacyTranslationsDir);
            }
        }
    }

    /**
     * Major changes.
     * Moved to the new object management system.
     */
    private void upgradePre103() {
        publishProgress(-1, "Updating translations");
        Logger.i(this.getClass().getName(), "Upgrading source data management from pre 103");

        // migrate target translations and profile
        File oldTranslationsDir = new File(App.context().getFilesDir(), "git");
        File newTranslationsDir = App.getTranslator().getPath();
        newTranslationsDir.mkdirs();
        File oldProfileDir = new File(oldTranslationsDir, "profile");
        File newProfileDir = new File(App.context().getFilesDir(), "profiles/profile");
        newProfileDir.getParentFile().mkdirs();
        if(oldProfileDir.exists()) {
            FileUtilities.deleteQuietly(newProfileDir);
            FileUtilities.moveOrCopyQuietly(oldProfileDir, newProfileDir);
        }
        if(oldTranslationsDir.exists() && oldTranslationsDir.list().length > 0) {
            FileUtilities.deleteQuietly(newTranslationsDir);
            FileUtilities.moveOrCopyQuietly(oldTranslationsDir, newTranslationsDir);
        } else if(oldTranslationsDir.exists()) {
            FileUtilities.deleteQuietly(oldTranslationsDir);
        }

        // remove old source
        File oldSourceDir = new File(App.context().getFilesDir(), "assets");
        File oldTempSourceDir = new File(App.context().getCacheDir(), "assets");
        File oldIndexDir = new File(App.context().getCacheDir(), "index");
        FileUtilities.deleteQuietly(oldSourceDir);
        FileUtilities.deleteQuietly(oldTempSourceDir);
        FileUtilities.deleteQuietly(oldIndexDir);

        // remove old caches
        File oldP2PDir = new File(App.context().getExternalCacheDir(), "transferred");
        File oldExportDir = new File(App.context().getCacheDir(), "exported");
        File oldImportDir = new File(App.context().getCacheDir(), "imported");
        File oldSharingDir = new File(App.context().getCacheDir(), "sharing");
        FileUtilities.deleteQuietly(oldP2PDir);
        FileUtilities.deleteQuietly(oldExportDir);
        FileUtilities.deleteQuietly(oldImportDir);
        FileUtilities.deleteQuietly(oldSharingDir);

        // clear old logs and crash reports
        Logger.flush();
    }

    /**
     * Change default font to noto because most of the others do not work
     */
    private void upgradePre87() {
        publishProgress(-1, "Updating fonts");
        Logger.i(this.getClass().getName(), "Upgrading fonts from pre 87");
        SharedPreferences.Editor editor = App.context().getUserPreferences().edit();
        editor.putString(SettingsActivity.KEY_PREF_TRANSLATION_TYPEFACE, App.context().getString(R.string.pref_default_translation_typeface));
        editor.apply();
    }

    @Override
    public int maxProgress() {
        return 100;
    }
}
