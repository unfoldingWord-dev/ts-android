package com.door43.translationstudio.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.door43.translationstudio.MainActivity;
import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Sharing;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.Logger;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by joel on 7/2/2015.
 */
public class BackupManager extends Service {
    private static final long BACKUP_INTERVAL = 5 * 60 * 1000;
    private static final Timer sTimer = new Timer();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d("BackupManager", "starting backup service");
    }

    @Override
    public void onDestroy() {
        stopService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        sTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runBackup();
            }
        }, 0, BACKUP_INTERVAL);
        return START_STICKY;
    }

    /**
     * Stops the service
     */
    private void stopService() {
        if(sTimer != null) {
            sTimer.cancel();
        }
        Log.d("BackupManager", "stopping backup service");
    }

    /**
     * Performs the backup if nessesary
     */
    private void runBackup() {
        // TODO: we need to look in the settings for the project repo path and manually check if a backup is required
        // TODO: need to set up time period in which backups will exist.
        // there are any translations that need to be backed up.
        for(Project p:AppContext.projectManager().getProjects()) {
            if(p.isTranslatingGlobal()) {
                try {
                    String archivePath = Sharing.export(p, new SourceLanguage[]{}, p.getActiveTargetLanguages());
                    File archiveFile = new File(archivePath);
                    if (archiveFile.exists()) {
                        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyy'T'HH:mm:ss.SSSZ");
                        File backupFile = new File(AppContext.getPublicDownloadsDirectory(), "backups/" + Project.GLOBAL_PROJECT_SLUG + "-" + p.getId() + "-" + format.format(new Date()) + "-backup." + Project.PROJECT_EXTENSION);
                        FileUtils.copyFile(archiveFile, backupFile);
                        archiveFile.delete();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Logger.e(this.getClass().getName(), "Failed to backup the project " + p.getId(), e);
                }
            }
        }
        onBackupComplete();
    }

    /**
     * Notifies the user that a backup was made
     */
    private void onBackupComplete() {
        SimpleDateFormat format = new SimpleDateFormat("h:mm a");
        CharSequence noticeText = "Translations last backed up at " + format.format(new Date());

        // activity to open when clicked
        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
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
