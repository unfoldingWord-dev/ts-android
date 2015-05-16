package com.door43.translationstudio.tasks;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import com.door43.translationstudio.R;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.FileUtilities;
import com.door43.util.Logger;
import com.door43.util.ServerUtilities;
import com.door43.util.tasks.ManagedTask;

import org.apache.commons.io.FileUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This task submits the latest crash report to github
 */
public class UploadCrashReportTask extends ManagedTask {

    private final String mMessage;
    private int mMaxProgress = 100;

    public UploadCrashReportTask(String message) {
        mMessage = message;
    }

    @Override
    public void start() {
        File dir = new File(AppContext.context().getExternalCacheDir(), AppContext.context().STACKTRACE_DIR);
        File logFile = new File(AppContext.context().getExternalCacheDir(), "log.txt");
        String[] files = dir.list(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return !new File(file, s).isDirectory();
            }
        });
        if (files.length > 0) {
            File archiveDir =  new File(dir, "archive");
            archiveDir.mkdirs();
            for(int i = 0; i < files.length; i ++) {
                File traceFile = new File(dir, files[i]);

                // submit crash report to Github
                try {
                    JSONObject json = new JSONObject();
                    String title;
                    // generate title
                    if(mMessage.length() < 30 && !mMessage.isEmpty()) {
                        title = mMessage;
                    } else if(!mMessage.isEmpty()) {
                        title = mMessage.substring(0, 29) + "...";
                    } else {
                        title = "crash report";
                    }

                    // record environment details
                    PackageInfo pInfo = null;
                    StringBuffer infoBuf = new StringBuffer();
                    if(!mMessage.isEmpty()) {
                        infoBuf.append("Notes\n======\n");
                        infoBuf.append(mMessage + "\n");
                    }
                    infoBuf.append("\nEnvironment\n======\n");
                    try {
                        pInfo = AppContext.context().getPackageManager().getPackageInfo(AppContext.context().getPackageName(), 0);
                        infoBuf.append("version: " + pInfo.versionName + "\n");
                        infoBuf.append("build: " + pInfo.versionCode + "\n");
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                    infoBuf.append("UDID: " + AppContext.udid() + "\n");
                    infoBuf.append("Android Release: " + Build.VERSION.RELEASE + "\n");
                    infoBuf.append("Androind SDK: " + Build.VERSION.SDK_INT + "\n");
                    infoBuf.append("Brand: " + Build.BRAND + "\n");
                    infoBuf.append("Device: " + Build.DEVICE + "\n");
                    infoBuf.append("Stack trace\n======\n");
                    infoBuf.append(FileUtilities.getStringFromFile(traceFile));

                    if(logFile.exists() && logFile.length() > 0) {
                        infoBuf.append("Log history\n======\n");
                        infoBuf.append(FileUtilities.getStringFromFile(logFile));
                        // empty the log.
                        FileUtils.write(logFile, "");
                    }

                    // build payload
                    try {
                        json.put("title", title);
                        json.put("body", infoBuf.toString());
                        JSONArray labels = new JSONArray();
                        labels.put("crash report");
                        if(pInfo != null) {
                            labels.put(pInfo.versionName);
                        }
                        json.put("labels", labels);
                    } catch (JSONException e) {
                        continue;
                    }

                    List<NameValuePair> headers = new ArrayList<>();
                    headers.add(new BasicNameValuePair("Authorization", "token " + AppContext.context().getResources().getString(R.string.github_oauth2)));
                    headers.add(new BasicNameValuePair("Content-Type", "application/json"));
                    String response = ServerUtilities.post("https://api.github.com/repos/Door43/translationStudio2/issues", headers, json.toString());
                } catch (IOException e) {
                    Logger.w(this.getClass().getName(), "failed to upload traces", e);
                    // archive stack trace for later use
                    FileUtilities.moveOrCopy(traceFile, new File(archiveDir, files[i]));
                }

                // clean up traces
                if(traceFile.exists()) {
                    traceFile.delete();
                }
            }
        }
    }

    @Override
    public int maxProgress() {
        return mMaxProgress;
    }
}
