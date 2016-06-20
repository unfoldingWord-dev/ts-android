package com.door43.translationstudio.tasks;

import org.unfoldingword.tools.logger.GithubReporter;
import org.unfoldingword.tools.logger.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.AppContext;
import com.door43.util.FileUtilities;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import java.io.File;
import java.io.IOException;

/**
 * Uploads a bug report to github
 */
public class UploadBugReportTask extends ManagedTask {
    public static final String TASK_ID = "upload_bug_report_task";
    private final String mNotes;

    public UploadBugReportTask(String notes) {
        mNotes = notes;
    }

    @Override
    public void start() {
        File logFile = Logger.getLogFile();

        // TRICKY: make sure the github_oauth2 token has been set
        int githubTokenIdentifier = AppContext.context().getResources().getIdentifier("github_oauth2", "string", AppContext.context().getPackageName());
        String githubUrl = AppContext.context().getResources().getString(R.string.github_bug_report_repo);

        if(githubTokenIdentifier != 0) {
            GithubReporter reporter = new GithubReporter(AppContext.context(), githubUrl, AppContext.context().getResources().getString(githubTokenIdentifier));
            reporter.reportBug(mNotes, logFile);

            // empty the log
            try {
                FileUtilities.writeStringToFile(logFile, "");
            } catch (IOException e) {
                e.printStackTrace();
            }

            Logger.i(this.getClass().getName(), "Submitted bug report");
        } else if(githubTokenIdentifier == 0) {
            Logger.w(this.getClass().getName(), "the github oauth2 token is missing");
        }
    }
}
