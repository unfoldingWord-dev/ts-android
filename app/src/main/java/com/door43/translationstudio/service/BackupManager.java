package com.door43.translationstudio.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Path;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.door43.translationstudio.MainActivity;
import com.door43.translationstudio.R;
import com.door43.translationstudio.git.Repo;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.ProjectManager;
import com.door43.translationstudio.projects.Sharing;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.FileUtilities;
import com.door43.tools.reporting.Logger;
import com.door43.util.Manifest;
import com.door43.util.Zip;

import org.eclipse.jgit.revwalk.RevCommit;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This services runs in the background to provide automatic backups for translations.
 */
public class BackupManager extends Service {
    private static final long BACKUP_INTERVAL = 5 * 60 * 1000;
    private static final Timer sTimer = new Timer();
    private static boolean mFirstRun = true;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Logger.i("BackupManager", "starting backup service");
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
                if (!mFirstRun) {
                    runBackup();
                }
                mFirstRun = false;
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
        Logger.i("BackupManager", "stopping backup service");
    }

    /**
     * Performs the backup if nessesary
     */
    private void runBackup() {
        // TODO: we need to look in the settings for the project repo path and manually check if a backup is required
        File projectsRoot = new File(AppContext.context().getFilesDir() + "/" + AppContext.context().getResources().getString(R.string.git_repository_dir) + "/");
        String projectDirs[] = projectsRoot.list();
        boolean backedUpTranslations = false;
        for(String filename:projectDirs) {
            File translationDir = new File(projectsRoot, filename);
            if(translationDir.isDirectory()) {
                // load the project and target language from the manifest
                Manifest manifest = Manifest.generate(translationDir);
                Language targetLanguage = null;
                JSONObject targetJson = manifest.getJSONObject("target_language");
                try {
                    String targetLanguageId = targetJson.getString("slug");
                    String targetLanguageName = targetJson.getString("name");
                    String targetLanguageDirection = targetJson.getString("direction");
                    targetLanguage = new Language(targetLanguageId, targetLanguageName, Language.Direction.get(targetLanguageDirection));
                } catch(JSONException e) {
                    Logger.e(this.getClass().getName(), "Failed to load the target language", e);
                    continue;
                }
                Project p = ProjectManager.getProject(manifest);

                // export the project
                if(p != null) {
                    try {
                        Sharing.export(p, new SourceLanguage[]{}, new Language[]{targetLanguage});
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Logger.w(this.getClass().getName(), "Failed to load the project at "+filename);
                }

                // TODO: finish this. need to check if the project needs to be backed up then we can export it on line 112 above.
                //old
                Repo repo = new Repo(translationDir.getAbsolutePath());
                String tag = null;
                try {
                    Iterable<RevCommit> commits = repo.getGit().log().setMaxCount(1).call();
                    RevCommit commit = null;
                    for(RevCommit c : commits) {
                        commit = c;
                    }
                    if(commit != null) {
                        String[] pieces = commit.toString().split(" ");
                        tag = pieces[1];
                    } else {
                        tag = null;
                    }
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "Failed to backup the translation " + translationDir.getName() + ". Missing commit tag", e);
                    continue;
                }
                File backupDir = new File(AppContext.getPublicDownloadsDirectory(), "backups/" + translationDir.getName() + "/");
                File backupFile = new File(backupDir, tag + "." + Project.PROJECT_EXTENSION);
                // TODO: export the translation so users can easily import it. This will require an updated project manager.
                if(!backupFile.exists()) {
                    FileUtilities.deleteRecursive(backupDir);
                    backupDir.mkdirs();
                    try {
                        Zip.zip(translationDir.getAbsolutePath(), backupFile.getAbsolutePath());
                        backedUpTranslations = true;
                    } catch (IOException e) {
                        Logger.e(this.getClass().getName(), "Failed to backup the translation " + translationDir.getName(), e);
                        continue;
                    }
                }
            }
        }
        if(backedUpTranslations) {
            onBackupComplete();
        }
    }

    /**
     * Notifies the user that a backup was made
     */
    private void onBackupComplete() {
        CharSequence noticeText = "Translations backed up";

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
