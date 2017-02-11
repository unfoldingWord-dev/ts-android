package com.door43.translationstudio.services;

import android.Manifest;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.unfoldingword.tools.logger.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.ui.SettingsActivity;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.ui.home.HomeActivity;
import com.door43.translationstudio.App;
import com.door43.util.Foreground;

import java.util.Timer;
import java.util.TimerTask;

/**
 * This services runs in the background to provide automatic backups for translations.
 * For now this service is backup the translations to two locations for added peace of mind.
 */
public class BackupService extends IntentService implements Foreground.Listener {
    public static final String TAG = BackupService.class.getName();
    private final Timer sTimer = new Timer();
    private static boolean sRunning = false;
    private Foreground foreground;
    private boolean paused;
    private boolean executingBackup = false;

    public BackupService() {
        super("BackupService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplication());
        while(true) {
            // identify backup interval
            int backupIntervalMinutes = Integer.parseInt(pref.getString(SettingsActivity.KEY_PREF_BACKUP_INTERVAL, getResources().getString(R.string.pref_default_backup_interval)));
            int backupInterval = backupIntervalMinutes * 1000 * 60;
            if(backupInterval <= 0) {
                // backups are disabled. wait and check again
                try {
                    Thread.sleep(1000 * 60);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            // run backup
            try {
                Log.i(TAG, "backups will run in " + backupIntervalMinutes + " minute(s)");
                Thread.sleep(backupInterval);
                runBackup();
            } catch (Exception e) {
                // recover from exceptions
                e.printStackTrace();
                executingBackup = false;
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.i(this.getClass().getName(), "starting backup service");

        try {
            this.foreground = Foreground.get();
            this.foreground.addListener(this);
        } catch (IllegalStateException e) {
            Logger.i(TAG, "Foreground was not initialized");
        }
    }

    @Override
    public void onDestroy() {
        if(this.foreground != null) {
            this.foreground.removeListener(this);
        }
        stopService();
        super.onDestroy();
    }

    /**
     * Checks if the service is running
     * @return true if the service is running
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
        Logger.i(TAG, "stopping backup service");
    }

    /**
     * Performs the backup if necessary
     */
    private void runBackup() {
        if(this.paused || this.executingBackup) return;

        this.executingBackup = true;
        boolean backupPerformed = false;
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
            Translator translator = App.getTranslator();
            Logger.i(TAG, "Checking for changes");
            String[] targetTranslations = translator.getTargetTranslationFileNames();
            for (String filename : targetTranslations) {

                try {
                    Thread.sleep(100); // add delay to ease background processing and also slow the memory thrashing in background
                } catch (Exception e) {
                    Logger.e(TAG, "sleep problem", e);
                }

                TargetTranslation t = translator.getTargetTranslation(filename);
                if(t == null) { // skip if not valid
                    Logger.i(TAG, "Skipping invalid translation: " + filename);
                    continue;
                }

                // commit pending changes
                try {
                    t.commitSync();
                } catch (Exception e) {
                    Logger.w(TAG, "Could not commit changes to " + t.getId(), e);
                }

                // run backup if there are translations
                if (t.numTranslated() > 0) {
                    try {
                        boolean success = App.backupTargetTranslation(t, false);
                        if(success) {
                            Logger.i(TAG, t.getId() + " backed up");
                            backupPerformed = true;
                        }
                    } catch (Exception e) {
                        Logger.e(TAG, "Could not backup " + t.getId(), e);
                    }
                }
            }
            Logger.i(TAG, "Finished backup check.");
            if (backupPerformed) {
                onBackupComplete();
            }
        } else {
            Logger.e(TAG, "Missing permission to write to external storage. Automatic backups skipped.");
        }
        this.executingBackup = false;
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

    @Override
    public void onBecameForeground() {
        this.paused = false;
        Log.i(TAG, "backups resumed");
    }

    @Override
    public void onBecameBackground() {
        runBackup();
        Log.i(TAG, "backups paused");
        this.paused = true;
    }
}
