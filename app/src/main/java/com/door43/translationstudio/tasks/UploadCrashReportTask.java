package com.door43.translationstudio.tasks;

import com.door43.tools.reporting.GithubReporter;
import com.door43.tools.reporting.GlobalExceptionHandler;
import com.door43.translationstudio.R;
import com.door43.translationstudio.AppContext;
import com.door43.util.FileUtilities;
import com.door43.util.tasks.ManagedTask;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

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
        File stacktraceDir = new File(AppContext.getPublicDirectory(), AppContext.context().STACKTRACE_DIR);
        File logFile = new File(AppContext.getPublicDirectory(), "log.txt");
        int githubTokenIdentifier = AppContext.context().getResources().getIdentifier("github_oauth2", "string", AppContext.context().getPackageName());
        String githubUrl = AppContext.context().getResources().getString(R.string.github_bug_report_repo);

        // TRICKY: make sure the github_oauth2 token has been set
        if(githubTokenIdentifier != 0) {
            GithubReporter reporter = new GithubReporter(AppContext.context(), githubUrl, AppContext.context().getResources().getString(githubTokenIdentifier));
            String[] stacktraces = GlobalExceptionHandler.getStacktraces(stacktraceDir);
            if (stacktraces.length > 0) {
                // upload most recent stacktrace
                reporter.reportCrash(mMessage, new File(stacktraces[0]), logFile);
                // empty the log
                try {
                    FileUtils.write(logFile, "");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // archive extra stacktraces
                File archiveDir = new File(stacktraceDir, "archive");
                archiveDir.mkdirs();
                for (String filePath:stacktraces) {
                    File traceFile = new File(filePath);
                    if (traceFile.exists()) {
                        FileUtilities.moveOrCopy(traceFile, new File(archiveDir, traceFile.getName()));
                        if(traceFile.exists()) {
                            traceFile.delete();
                        }
                    }
                }
            }
        }
    }

    @Override
    public int maxProgress() {
        return mMaxProgress;
    }
}
