package com.door43.translationstudio.tasks;

import org.unfoldingword.tools.logger.GithubReporter;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;

import org.unfoldingword.tools.logger.Logger;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import java.io.File;

/**
 * This task submits the latest crash report to github
 */
public class UploadCrashReportTask extends ManagedTask {
    public static final String TASK_ID = "upload_crash_report_task";
    private final String mMessage;
    private int mMaxProgress = 100;

    public UploadCrashReportTask(String message) {
        mMessage = message;
    }

    @Override
    public void start() {
        File logFile = new File(App.getPublicDirectory(), "log.txt");
        int githubTokenIdentifier = App.context().getResources().getIdentifier("github_oauth2", "string", App.context().getPackageName());
        String githubUrl = App.context().getResources().getString(R.string.github_bug_report_repo);

        // TRICKY: make sure the github_oauth2 token has been set
        if(githubTokenIdentifier != 0) {
            GithubReporter reporter = new GithubReporter(App.context(), githubUrl, App.context().getResources().getString(githubTokenIdentifier));
            File[] stacktraces = Logger.listStacktraces();
            if (stacktraces.length > 0) {
                // upload most recent stacktrace
                reporter.reportCrash(mMessage, stacktraces[0], logFile);

                // empty the log
                Logger.flush();
            }
        }
    }

    @Override
    public int maxProgress() {
        return mMaxProgress;
    }
}
