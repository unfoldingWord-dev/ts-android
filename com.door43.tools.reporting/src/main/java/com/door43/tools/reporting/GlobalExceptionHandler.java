package com.door43.tools.reporting;

import android.os.Process;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * This class write exceptions to a file on disk before killing the app
 * This allows you to retrieve them later for debugging.
 * http://stackoverflow.com/questions/601503/how-do-i-obtain-crash-data-from-my-android-application
 */
public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static final String STACKTRACE_EXT = "stacktrace";
    private Thread.UncaughtExceptionHandler defaultUEH;
    private final String mStracktraceDir;

    /**
     * if any of the parameters is null, the respective functionality
     * will not be used
     * @param stacktraceDir
     */
    public GlobalExceptionHandler(File stacktraceDir) {
        if(!stacktraceDir.exists()) {
            stacktraceDir.mkdirs();
        }
        this.mStracktraceDir = stacktraceDir.getAbsolutePath();
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    }

    /**
     * Registers the exception handler as the global exception handler for the app
     * @param stacktraceDirectory the directory where the stacktrace files will be stored
     */
    public static void register(File stacktraceDirectory) {
        if(!(Thread.getDefaultUncaughtExceptionHandler() instanceof GlobalExceptionHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler(stacktraceDirectory));
        }
    }

    /**
     * Returns a list of stacktrace files found in the directory
     * @param stacktraceDir
     * @return
     */
    public static String[] getStacktraces(File stacktraceDir) {
        String[] files = stacktraceDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                String pieces[] = filename.split("\\.");
                String ext = pieces[pieces.length - 1];
                return new File(dir, filename).isFile() && ext.equals(STACKTRACE_EXT);
            }
        });
        if(files != null) {
            // build full path
            String[] stacktraces = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                stacktraces[i] = new File(stacktraceDir, files[i]).getAbsolutePath();
            }
            return stacktraces;
        } else {
            return new String[0];
        }
    }

    /**
     * Handles the uncaught exception
     * @param t
     * @param e
     */
    public void uncaughtException(Thread t, Throwable e) {
        Long tsLong = System.currentTimeMillis();
        String timestamp = tsLong.toString();
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        e.printStackTrace(printWriter);
        String stacktrace = result.toString();
        printWriter.close();
        String filename = timestamp + "." + STACKTRACE_EXT;

        if (mStracktraceDir != null) {
            writeToFile(stacktrace, filename);
        }

        defaultUEH.uncaughtException(t, e);

        // force shut down so we don't end up with un-initialized objects
        Process.killProcess(Process.myPid());
        System.exit(0);
    }

    /**
     * Writes the stacktrace to the log directory
     * @param stacktrace
     * @param filename
     */
    public void writeToFile(String stacktrace, String filename) {
        try {
            BufferedWriter bos = new BufferedWriter(new FileWriter(
                    mStracktraceDir + "/" + filename));
            bos.write(stacktrace);
            bos.flush();
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}