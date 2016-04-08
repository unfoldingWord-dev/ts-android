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
import com.door43.translationstudio.AppContext;

import java.util.Timer;
import java.util.TimerTask;

/**
 * This services runs in the background to provide automatic backups for translations.
 * For now this service is backup the translations to two locations for added peace of mind.
 */
public class BackupService extends Service {
    private final Timer sTimer = new Timer();
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
            int delay = 60 * 1000; // wait a minute for the app to finish booting
            Logger.i(this.getClass().getName(), "Backups running every " + backupIntervalMinutes + " minute/s");
            sRunning = true;
            sTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    runBackup();
                }
            }, delay, backupInterval);
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

            // commit pending changes
            try {
                t.commitSync();
            } catch (Exception e) {
                Logger.e(this.getClass().getName(), "Failed to commit changes before backing up", e);
                continue;
            }

            // run backup if there are translations
            if(t.numTranslated() > 0) {
                try {
                    backupPerformed = AppContext.backupTargetTranslation(t, false) ? true : backupPerformed;
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "Failed to backup the target translation " + t.getId(), e);
                }
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
