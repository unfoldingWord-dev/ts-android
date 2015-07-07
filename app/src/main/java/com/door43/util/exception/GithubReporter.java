package com.door43.util.exception;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import com.door43.translationstudio.util.AppContext;
import com.door43.util.ServerUtilities;

import org.apache.commons.io.FileUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class submits information to a github repository
 */
public class GithubReporter {

    private static final int MAX_TITLE_LENGTH = 30;
    private static final String DEFAULT_CRASH_TITLE = "crash report";
    private static final String DEFAULT_BUG_TITLE = "bug report";
    private final String sRepositoryUrl;
    private final String sGithubOauth2Token;
    private final File sStacktraceDirectory;
    private final File sLogFile;

    public GithubReporter(String repositoryUrl, String githubOauth2Token, File stacktraceDirectory, File logFile) {
        sRepositoryUrl = repositoryUrl;
        sGithubOauth2Token = githubOauth2Token;
        sStacktraceDirectory = stacktraceDirectory;
        sLogFile = logFile;
    }

    /**
     * Creates a crash issue on github
     * @param notes
     * @param e
     */
    public void reportCrash(String notes, Throwable e) {
        String title = getTitle(notes, DEFAULT_CRASH_TITLE);
    }

    /**
     * Creates a crash issue on github
     * @param notes
     * @param stacktraceFile
     */
    public void reportCrash(String notes, File stacktraceFile) {

    }

    /**
     * Creates a bug issue on github
     * @param notes
     */
    public void reportBug(String notes) {
        // body
        String title = getTitle(notes, DEFAULT_BUG_TITLE);
        StringBuffer bodyBuf = new StringBuffer();
        bodyBuf.append(getNotesBlock(notes));
        bodyBuf.append(getEnvironmentBlock());
        bodyBuf.append(getLogBlock());

        // payload
        JSONObject json = new JSONObject();
        try {
            json.put("title", title);
            json.put("body", bodyBuf.toString());
            JSONArray labelsJson = new JSONArray();
            labelsJson.put("crash report");
            try {
                PackageInfo pInfo = AppContext.context().getPackageManager().getPackageInfo(AppContext.context().getPackageName(), 0);
                labelsJson.put(pInfo.versionName);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            json.put("labels", labelsJson);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String respose = submit(json);
    }

    private String submit(JSONObject json) {
        // headers
        List<NameValuePair> headers = new ArrayList<>();
        headers.add(new BasicNameValuePair("Authorization", "token " + sGithubOauth2Token));
        headers.add(new BasicNameValuePair("Content-Type", "application/json"));

        // TODO: place the code in here so we have a few dependencies as possible.
        return ServerUtilities.post(sRepositoryUrl, headers, json.toString());
    }

    /**
     * Generates the ntoes block
     * @param notes
     * @return
     */
    private static String getNotesBlock(String notes) {
        StringBuffer notesBuf = new StringBuffer();
        if (!notes.isEmpty()) {
            notesBuf.append("Notes\n======\n");
            notesBuf.append(notes + "\n");
        }
        return notesBuf.toString();
    }

    /**
     * Generates the notes block
     * @return
     */
    private String getLogBlock() {
        StringBuffer logBuf = new StringBuffer();
        if (sLogFile != null && sLogFile.exists() && sLogFile.length() > 0) {
            logBuf.append("Log history\n======\n");
            try {
                logBuf.append(FileUtils.readFileToString(sLogFile));
                // empty the log.
                FileUtils.write(sLogFile, "");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return logBuf.toString();
    }

    /**
     * Generates the stacktrace block
     * @param stacktrace
     * @return
     */
    private static String getStacktraceBlock(String stacktrace) {
        StringBuffer stacktraceBuf = new StringBuffer();
        stacktraceBuf.append("Stack trace\n======\n");
        stacktraceBuf.append(stacktrace);
        return stacktraceBuf.toString();
    }

    /**
     * Generates the environment block
     * @return
     */
    private static String getEnvironmentBlock() {
        PackageInfo pInfo = null;
        StringBuffer environmentBuf = new StringBuffer();
        environmentBuf.append("\nEnvironment\n======\n");
        try {
            pInfo = AppContext.context().getPackageManager().getPackageInfo(AppContext.context().getPackageName(), 0);
            environmentBuf.append("version: " + pInfo.versionName + "\n");
            environmentBuf.append("build: " + pInfo.versionCode + "\n");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        environmentBuf.append("UDID: " + AppContext.udid() + "\n");
        environmentBuf.append("Android Release: " + Build.VERSION.RELEASE + "\n");
        environmentBuf.append("Androind SDK: " + Build.VERSION.SDK_INT + "\n");
        environmentBuf.append("Brand: " + Build.BRAND + "\n");
        environmentBuf.append("Device: " + Build.DEVICE + "\n");
        return environmentBuf.toString();
    }

    /**
     * Generates the title from the notes
     * @param notes
     * @return
     */
    private static String getTitle(String notes, String defaultTitle) {
        String title = defaultTitle;
        if (notes.length() < MAX_TITLE_LENGTH && !notes.isEmpty()) {
            title = notes;
        } else if (!notes.isEmpty()) {
            title = notes.substring(0, MAX_TITLE_LENGTH - 3) + "...";
        }
        return title;
    }
}
