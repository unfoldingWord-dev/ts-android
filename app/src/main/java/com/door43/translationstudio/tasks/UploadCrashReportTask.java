package com.door43.translationstudio.tasks;

import org.unfoldingword.tools.http.Request;
import org.unfoldingword.tools.logger.GithubReporter;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;

import org.unfoldingword.tools.logger.Logger;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import java.io.File;
import java.io.IOException;

/**
 * This task submits the latest crash report to github
 */
public class UploadCrashReportTask extends ManagedTask {
    public static final String TASK_ID = "upload_crash_report_task";
    private final String mMessage;
    private int mMaxProgress = 100;
    private int mResponseCode = -1;

    public UploadCrashReportTask(String message) {
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
        int githubTokenIdentifier = App.context().getResources().getIdentifier("github_oauth2", "string", App.context().getPackageName());
        String githubUrl = App.context().getResources().getString(R.string.github_bug_report_repo);

        // TRICKY: make sure the github_oauth2 token has been set
        if(githubTokenIdentifier != 0) {
            GithubReporter reporter = new GithubReporter(App.context(), githubUrl, App.context().getResources().getString(githubTokenIdentifier));
            File[] stacktraces = Logger.listStacktraces();
            if (stacktraces.length > 0) {
                try {
                    // upload most recent stacktrace
                    Request request = reporter.reportCrash(mMessage, stacktraces[0], logFile);
                    mResponseCode = request.getResponseCode();
                } catch (IOException e) {
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
