package com.door43.translationstudio.tasks;

import org.unfoldingword.tools.http.Request;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Profile;
import com.door43.translationstudio.ui.SettingsActivity;
import com.door43.util.EmailReporter;

import org.unfoldingword.tools.logger.Logger;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import java.io.File;

/**
 * This task submits the latest crash report to github
 */
public class UploadCrashReportTask extends ManagedTask {
    public static final String TASK_ID = "upload_crash_report_task";
    private final String mMessage;
    private final String mEmail;
    private int mMaxProgress = 100;
    private int mResponseCode = -1;

    public UploadCrashReportTask(String email, String message) {
        mEmail = email;
        mMessage = message;
    }

    /**
     * determine if upload was success
     * @return
     */
    public boolean isSuccess() {
        return (mResponseCode >= 200) && (mResponseCode <= 202);
    }

    /**
     * get response code from server
     * @return
     */
    public int getResponseCode() {
        return mResponseCode;
    }

    @Override
    public void start() {
        mResponseCode = -1;
        File logFile = new File(App.publicDir(), "log.txt");

        // TRICKY: make sure the helpdesk token has been set
        int helpdeskTokenIdentifier = App.context().getResources().getIdentifier("helpdesk_token", "string", App.context().getPackageName());
        String helpdeskEmail = App.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_HELP_EMAIL, App.context().getResources().getString(R.string.pref_default_helpdesk_email));

//        int githubTokenIdentifier = App.context().getResources().getIdentifier("github_oauth2", "string", App.context().getPackageName());
//        String githubUrl = App.context().getResources().getString(R.string.github_bug_report_repo);

        // TRICKY: make sure the github_oauth2 token has been set
        if(helpdeskTokenIdentifier != 0) {
            EmailReporter reporter = new EmailReporter(App.context().getResources().getString(helpdeskTokenIdentifier), helpdeskEmail);
//            GithubReporter reporter = new GithubReporter(App.context(), githubUrl, App.context().getResources().getString(githubTokenIdentifier));
            File[] stacktraces = Logger.listStacktraces();
            if (stacktraces.length > 0) {
                try {
                    String senderEmail = mEmail;
                    String senderName = mEmail;
                    Profile profile = App.getProfile();
                    if(profile != null) {
                        if (senderEmail.isEmpty() && profile.gogsUser != null) {
                            senderEmail = profile.gogsUser.email;
                        }
                        senderName = profile.getFullName();
                    }
                    if(senderEmail.isEmpty()) {
                        senderEmail = helpdeskEmail;
                        senderName = "Help Desk";
                    }

                    // upload most recent stacktrace
                    Request request = reporter.reportCrash(senderName, senderEmail, mMessage, stacktraces[0], logFile, App.context());
                    mResponseCode = request.getResponseCode();
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "Failed to submit feedback: " + e.getMessage());
                    e.printStackTrace();
                }

                if(!isSuccess()) {
                    Logger.e(UploadCrashReportTask.class.getSimpleName(), "Failed to upload crash report.  Code: " + mResponseCode);
                } else { // success
                    // empty the log
                    Logger.flush();
                }
            }
        }
    }

    @Override
    public int maxProgress() {
        return mMaxProgress;
    }
}
