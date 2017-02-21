package com.door43.translationstudio.services;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
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
public class BackupService extends Service implements Foreground.Listener {
    public static final String TAG = BackupService.class.getName();
    private final Timer sTimer = new Timer();
    private static boolean sRunning = false;
    private boolean isPaused = false;
    private boolean executingBackup = false;
    private Foreground foreground;
    private Handler handler;
    private Runnable runner;
    private HandlerThread handlerThread;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.i(this.getClass().getName(), "starting backup service");
        handlerThread = new HandlerThread("BackupServiceHandler");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplication());
        int backupIntervalMinutes = Integer.parseInt(pref.getString(SettingsActivity.KEY_PREF_BACKUP_INTERVAL, getResources().getString(R.string.pref_default_backup_interval)));
        if(backupIntervalMinutes > 0) {
            int backupInterval = backupIntervalMinutes * 1000 * 60;
            Logger.i(this.getClass().getName(), "Backups running every " + backupIntervalMinutes + " minute/s");
            sRunning = true;
            sTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try {
                        runBackup(isPaused);
                    } catch(Exception e) {
                        // recover from exceptions
                        executingBackup = false;
                        e.printStackTrace();
                    }
                }
            }, backupInterval, backupInterval);
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
        Logger.i(TAG, "stopping backup service");
    }

    /**
     * Performs the backup if necessary
     */
    private void runBackup(boolean paused) {
        if(paused || this.executingBackup) return;

        this.executingBackup = true;
        boolean backupPerformed = false;
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            Translator translator = App.getTranslator();
            Logger.i(TAG, "Checking for changes");
            String[] targetTranslations = translator.getTargetTranslationFileNames();
            for (String filename : targetTranslations) {

                try {
                    // add delay to ease background processing and also slow the memory thrashing in background
                    Thread.sleep(1000);
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
                    t.commitSync(".", false);
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
        this.isPaused = false;
        Log.i(TAG, "backups resumed");
        if(runner != null) handler.removeCallbacks(runner);
    }

    @Override
    public void onBecameBackground() {
        if(runner != null) handler.removeCallbacks(runner);
        Log.i(TAG, "backups paused");
        isPaused = true;
        handler.postDelayed(runner = new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "performing single run before pause");
                runBackup(false);
            }
        }, 1000);
    }
}
