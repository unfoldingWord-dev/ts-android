package com.door43.translationstudio.tasks;

import com.door43.translationstudio.util.AppContext;
import com.door43.util.FileUtilities;
import com.door43.util.threads.ManagedTask;

import java.io.File;
import java.io.FilenameFilter;

/**
 * This task archives the latest crash report
 */
public class ArchiveCrashReportTask extends ManagedTask {

    @Override
    public void start() {
        File dir = new File(AppContext.context().getExternalCacheDir(), AppContext.context().STACKTRACE_DIR);
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
                // archive stack trace for later use
                FileUtilities.moveOrCopy(traceFile, new File(archiveDir, files[i]));
                // clean up traces
                if(traceFile.exists()) {
                    traceFile.delete();
                }
            }
        }
    }
}
