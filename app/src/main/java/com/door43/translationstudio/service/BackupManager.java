package com.door43.translationstudio.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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

import org.apache.commons.io.FileUtils;
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
        if(projectDirs != null) {
            for (String filename : projectDirs) {
                File translationDir = new File(projectsRoot, filename);
                if (translationDir.isDirectory()) {
                    // load the project and target language from the manifest
                    Manifest manifest = Manifest.generate(translationDir);
                    Language targetLanguage;
                    JSONObject targetJson = manifest.getJSONObject("target_language");
                    try {
                        String targetLanguageId = targetJson.getString("slug");
                        // the name and direction are optional because the backup doesn't need them
                        String targetLanguageName = "";
                        if (targetJson.has("name")) {
                            targetLanguageName = targetJson.getString("name");
                        }
                        String targetLanguageDirection = "";
                        if (targetJson.has("direction")) {
                            targetLanguageDirection = targetJson.getString("direction");
                        }
                        Language.Direction direction = Language.Direction.get(targetLanguageDirection);
                        if (direction == null) {
                            direction = Language.Direction.LeftToRight;
                        }
                        targetLanguage = new Language(targetLanguageId, targetLanguageName, direction);
                    } catch (JSONException e) {
                        Logger.e(this.getClass().getName(), "Failed to load the target language", e);
                        continue;
                    }
                    Project p = ProjectManager.getProject(manifest);

                    if (p != null) {
                        // check if backup is required
                        String tag = getRepoHeadTag(translationDir);
                        if (tag != null) {
                            File backupDir = new File(AppContext.getPublicDownloadsDirectory(), "backups/" + translationDir.getName() + "/");
                            File backupFile = new File(backupDir, tag + "." + Project.PROJECT_EXTENSION);

                            if (!backupFile.exists()) {
                                // export
                                String archivePath;
                                try {
                                    archivePath = Sharing.export(p, new SourceLanguage[]{}, new Language[]{targetLanguage});
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    continue;
                                }
                                File archiveFile = new File(archivePath);
                                if (archiveFile.exists()) {
                                    // replace existing backup
                                    FileUtilities.deleteRecursive(backupDir);
                                    backupDir.mkdirs();
                                    FileUtilities.moveOrCopy(archiveFile, backupFile);
                                    archiveFile.delete();
                                    backedUpTranslations = true;
                                } else {
                                    Logger.w(this.getClass().getName(), "Failed to export the project translation " + filename);
                                }
                            }
                        } else {
                            Logger.w(this.getClass().getName(), "Failed to get the git HEAD tag for " + filename);
                        }
                    } else {
                        Logger.w(this.getClass().getName(), "Failed to load the project at " + filename);
                    }
                }
            }
        }
        if(backedUpTranslations) {
            onBackupComplete();
        }
    }

    /**
     * Returns the commit tag for the repo HEAD
     * @param translationDir
     * @return
     */
    private static String getRepoHeadTag(File translationDir) {
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
            Logger.e(BackupManager.class.getName(), "Failed to backup the translation " + translationDir.getName() + ". Missing commit tag", e);
        }
        return tag;
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
