package com.door43.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONObject;
import org.unfoldingword.tools.http.PostRequest;
import org.unfoldingword.tools.http.Request;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * This class submits information to a github repository
 */
public class EmailReporter {
    private static final String SEND_GRID_API = "https://api.sendgrid.com/v3/mail/send";
    private final String helpDeskToken;
    private final String helpDeskEmail;

    /**
     * Generates a new github reporter
     *
     * @param helpDeskToken
     * @param helpDeskEmail
     */
    public EmailReporter(String helpDeskToken, String helpDeskEmail) {
        this.helpDeskToken = helpDeskToken;
        this.helpDeskEmail = helpDeskEmail;
    }

    /**
     * Submit the data
     *
     * @param data
     * @return
     * @throws IOException
     */
    private Request submit(String data) throws IOException {
        PostRequest request = new PostRequest(new URL(SEND_GRID_API), data);
        request.setAuth(helpDeskToken, "Bearer");
        request.setContentType("application/json");
        request.read();
        return request;
    }

    /**
     * Creates a crash issue on github.
     *
     * @param notes          notes supplied by the user
     * @param stacktraceFile the stacktrace file
     * @return the request object
     */
    public Request reportCrash(String name, String email, String notes, File stacktraceFile, Context context) throws Exception {
        String stacktrace = FileUtilities.readFileToString(stacktraceFile);
        return reportCrash(name, email, notes, stacktrace, null, context);
    }

    /**
     * Creates a crash issue on github.
     *
     * @param notes          notes supplied by the user
     * @param stacktraceFile the stacktrace file
     * @param logFile        the log file
     * @return the request object
     */
    public Request reportCrash(String name, String email, String notes, File stacktraceFile, File logFile, Context context) throws Exception {
        String log = null;
        if (logFile.exists()) {
            try {
                log = FileUtilities.readFileToString(logFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String stacktrace = FileUtilities.readFileToString(stacktraceFile);
        return reportCrash(name, email, notes, stacktrace, log, context);
    }

    /**
     * Creates a crash issue on github.
     *
     * @param notes      notes supplied by the user
     * @param stacktrace the stracktrace
     * @param log        information from the log
     * @return the request object
     */
    public Request reportCrash(String name, String email, String notes, String stacktrace, String log, Context context) throws Exception {
        PackageInfo environment = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        StringBuffer bodyBuf = new StringBuffer();
        bodyBuf.append(getNotesBlock(notes));
        bodyBuf.append(getEnvironmentBlock(environment));
        bodyBuf.append(getStacktraceBlock(stacktrace));
        bodyBuf.append(getLogBlock(log));

        return submit(generatePayload(name, email, bodyBuf.toString(), "Crash Report"));
    }

    /**
     * Creates a bug issue on github
     *
     * @param notes notes supplied by the user
     * @return the request object
     */
    public Request reportBug(String name, String email, String notes, Context context) throws Exception {
        return reportBug(name, email, notes, "", context);
    }

    /**
     * Creates a bug issue on github
     *
     * @param notes   notes supplied by the user
     * @param logFile the log file
     * @return the request object
     */
    public Request reportBug(String name, String email, String notes, File logFile, Context context) throws Exception {
        String log = null;
        if (logFile != null && logFile.exists()) {
            try {
                log = FileUtilities.readFileToString(logFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return reportBug(name, email, notes, log, context);
    }

    /**
     * Creates a bug issue on github
     *
     * @param notes notes supplied by the user
     * @param log   information from the log
     * @return the request object
     */
    public Request reportBug(String name, String email, String notes, String log, Context context) throws Exception {
        PackageInfo environment = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        StringBuffer bodyBuf = new StringBuffer();
        bodyBuf.append(getNotesBlock(notes));
        bodyBuf.append(getEnvironmentBlock(environment));
        bodyBuf.append(getLogBlock(log));

        String category = "General Feedback";
        return submit(generatePayload(name, email, bodyBuf.toString(), category));
    }

    /**
     * Generates the json payload that will be set to the github server.
     *
     * @return
     */
    public String generatePayload(String name, String email, String message, String category) throws Exception {
        if (email == null) email = "";
        if (name == null) name = "";
        String userEmail = email;
        String userName = name;
        if (userEmail.isEmpty()) {
            userEmail = helpDeskEmail;
            userName = "Help Desk";
        } else if (userName.isEmpty()) {
            userName = userEmail;
        }

        JSONObject fromContact = new JSONObject();
        fromContact.put("email", userEmail);
        fromContact.put("name", userName);

        JSONObject toContact = new JSONObject();
        toContact.put("email", helpDeskEmail);
        toContact.put("name", "Help Desk");

        JSONObject json = new JSONObject();

        // sender
        json.put("from", fromContact);
        json.put("reply_to", fromContact);

        // build content structure
        JSONArray content = new JSONArray();
        JSONObject textMessage = new JSONObject();
        textMessage.put("type", "text/plain");
        textMessage.put("value", message);
        content.put(textMessage);
        JSONObject htmlMessage = new JSONObject();
        htmlMessage.put("type", "text/html");
        htmlMessage.put("value", message.replaceAll("\n", "<br>"));
        content.put(htmlMessage);
        json.put("content", content);

        // personalize
        JSONObject personalization = new JSONObject();
        personalization.put("to", new JSONArray().put(toContact));
        personalization.put("subject", "tS Android: " + category);
        json.put("personalizations", new JSONArray().put(personalization));
        return json.toString();
    }

    /**
     * Generates the notes block.
     *
     * @param log
     * @return
     */
    private String getLogBlock(String log) {
        StringBuffer logBuf = new StringBuffer();
        if (log != null && !log.isEmpty()) {
            logBuf.append("\n# Log history\n");
            logBuf.append("------begin log history------\n");
            logBuf.append(log.trim() + "\n");
            logBuf.append("------end log history------\n");
        }
        return logBuf.toString();
    }

    /**
     * Generates the stacktrace block
     *
     * @param stacktrace the stacktrace text
     * @return
     */
    private static String getStacktraceBlock(String stacktrace) {
        StringBuffer stacktraceBuf = new StringBuffer();
        if (stacktrace != null && !stacktrace.isEmpty()) {
            stacktraceBuf.append("# Stack trace\n");
            stacktraceBuf.append("------begin stacktrace------\n");
            stacktraceBuf.append(stacktrace + "\n");
            stacktraceBuf.append("------end stacktrace------\n");
        }
        return stacktraceBuf.toString();
    }

    /**
     * Generates the notes block
     *
     * @param notes notes supplied by the user
     * @return
     */
    private static String getNotesBlock(String notes) {
        StringBuffer notesBuf = new StringBuffer();
        if (!notes.isEmpty()) {
            notesBuf.append("# Notes\n");
            notesBuf.append(notes + "\n");
        }
        return notesBuf.toString();
    }


    /**
     * Generates the environment block
     *
     * @return
     */
    private String getEnvironmentBlock(PackageInfo environment) {
        StringBuffer environmentBuf = new StringBuffer();
        environmentBuf.append("\n# Environment\n");
        if(environment != null) {
            environmentBuf.append("version: " + environment.versionName + "\n");
            environmentBuf.append("build: " + environment.versionCode + "\n");
        }
        environmentBuf.append("Android Release: " + Build.VERSION.RELEASE + "\n");
        environmentBuf.append("Android SDK: " + Build.VERSION.SDK_INT + "\n");
        environmentBuf.append("Brand: " + Build.BRAND + "\n");
        environmentBuf.append("Device: " + Build.DEVICE + "\n");
        return environmentBuf.toString();
    }
}