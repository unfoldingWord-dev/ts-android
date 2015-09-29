package com.door43.translationstudio.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.newui.home.HomeActivity;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.AppContext;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This services runs in the background to provide automatic backups for translations.
 * For now this service is backup the translations to two locations for added peace of mind.
 */
public class BackupService extends Service {
    private final Timer sTimer = new Timer();
    private boolean mFirstRun = true;
    private static boolean sRunning = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Logger.i(this.getClass().getName(), "starting backup service");
    }

    @Override
    public void onDestroy() {
        stopService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplication());
        int backupIntervalMinutes = Integer.parseInt(pref.getString(SettingsActivity.KEY_PREF_BACKUP_INTERVAL, getResources().getString(R.string.pref_default_backup_interval)));
        if(backupIntervalMinutes > 0) {
            int backupInterval = backupIntervalMinutes * 60 * 1000;
            Logger.i(this.getClass().getName(), "Backups running every " + backupIntervalMinutes + " minute/s");
            sRunning = true;
            sTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (!mFirstRun) {
                        runBackup();
                    }
                    mFirstRun = false;
                }
            }, 0, backupInterval);
            return START_STICKY;
        } else {
            Logger.i(this.getClass().getName(), "Backups are disabled");
            sRunning = true;
            return START_STICKY;
        }
    }

    /**
     * Checks if the service is running
     * @return
     */
    public static boolean isRunning() {
        return sRunning;
    }

    /**
     * Stops the service
     */
    private void stopService() {
        if(sTimer != null) {
            sTimer.cancel();
        }
        sRunning = false;
        Logger.i(this.getClass().getName(), "stopping backup service");
    }

    /**
     * Performs the backup if nessesary
     */
    private void runBackup() {
        boolean backupPerformed = false;
        Translator translator = AppContext.getTranslator();
        TargetTranslation[] targetTranslations = translator.getTargetTranslations();
        for(TargetTranslation t:targetTranslations) {

            // retreive commit hash
            String tag;
            try {
                tag = t.commitHash();
            } catch (Exception e) {
                Logger.e(this.getClass().getName(), "Failed to read commit hash", e);
                continue;
            }

            // check if backup is required
            if(tag != null) {
                File primaryBackupDir = new File(AppContext.getPublicDirectory(), "backups/" + t.getId() + "/");
                File primaryBackupFile = new File(primaryBackupDir, tag + "." + Project.PROJECT_EXTENSION);
                File downloadBackupDir = new File(AppContext.getPublicDownloadsDirectory(), "backups/" + t.getId() + "/");
                File downloadBackupFile = new File(downloadBackupDir, tag + "." + Project.PROJECT_EXTENSION);
                // e.g. ../../backups/uw-obs-de/[commit hash].tstudio
                if (!downloadBackupFile.exists()) {

                    // peform backup
                    File archive = new File(AppContext.getPublicDownloadsDirectory(), t.getId() + ".temp." + Project.PROJECT_EXTENSION);
                    translator.export(t, archive);
                    if(archive.exists() && archive.isFile()) {
                        // move into backup
                        FileUtils.deleteQuietly(downloadBackupDir);
                        FileUtils.deleteQuietly(primaryBackupDir);
                        downloadBackupDir.mkdirs();
                        primaryBackupDir.mkdirs();
                        try {
                            // backup to downloads directory
                            FileUtils.copyFile(archive, downloadBackupFile);
                            // backup to a slightly less public area (used for auto restore)
                            FileUtils.copyFile(archive, primaryBackupFile);
                            backupPerformed = true;
                        } catch (IOException e) {
                            Logger.e(this.getClass().getName(), "Failed to copy the backup archive for target translation: " + t.getId(), e);
                        }
                        archive.delete();
                    } else {
                        Logger.w(this.getClass().getName(), "Failed to export the target translation: " + t.getId());
                    }
                }
            } else {
                Logger.w(this.getClass().getName(), "Could not find the commit hash");
            }
        }

        if(backupPerformed) {
            onBackupComplete();
        }
    }

    /**
     * Notifies the user that a backup was made
     */
    private void onBackupComplete() {
        CharSequence noticeText = "Translations backed up";

        // activity to open when clicked
        // TODO: instead of the home activity we need a backup activity where the user can view their backups.
        Intent notificationIntent = new Intent(getApplicationContext(), HomeActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);

        // build notification
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_stat_notify_msg)
                        .setContentTitle(noticeText)
                        .setAutoCancel(true)
                        .setContentIntent(intent)
                        .setNumber(0);

        // build big notification
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle(noticeText);
        mBuilder.setStyle(inboxStyle);

        // issue notification
        NotificationManager mNotifyMgr = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mNotifyMgr.notify(0, mBuilder.build());
        } else {
            mNotifyMgr.notify(0, mBuilder.getNotification());
        }
    }
}
