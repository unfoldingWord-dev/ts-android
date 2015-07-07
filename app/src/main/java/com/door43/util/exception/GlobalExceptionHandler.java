package com.door43.util.exception;

import android.content.Context;

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
    private Thread.UncaughtExceptionHandler defaultUEH;
    private String localPath;

    /*
     * if any of the parameters is null, the respective functionality
     * will not be used
     */
    public GlobalExceptionHandler(File localPath) {
        if(!localPath.exists()) {
            localPath.mkdirs();
        }
        this.localPath = localPath.getAbsolutePath();
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    }

    /**
     * Registers the exception handler as the global exception handler for the app
     * @param context the application context
     * @param stacktraceDirectory the directory where the stacktrace files will be stored
     */
    public static void register(Context context, String stacktraceDirectory) {
        if(!(Thread.getDefaultUncaughtExceptionHandler() instanceof com.door43.util.CustomExceptionHandler)) {
            File dir = new File(context.getExternalCacheDir(), stacktraceDirectory);
            Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler(dir));
        }
    }

    /**
     * Returns a list of stacktrace files
     * @param stacktraceDirectory
     * @return
     */
    public static String[] getStacktraces(String stacktraceDirectory) {
        File dir = new File(stacktraceDirectory);
        return dir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return new File(dir, filename).isFile();
            }
        });
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
        String filename = timestamp + ".stacktrace";

        if (localPath != null) {
            writeToFile(stacktrace, filename);
        }

        defaultUEH.uncaughtException(t, e);
    }

    /**
     * Writes the stacktrace to the log directory
     * @param stacktrace
     * @param filename
     */
    public void writeToFile(String stacktrace, String filename) {
        try {
            BufferedWriter bos = new BufferedWriter(new FileWriter(
                    localPath + "/" + filename));
            bos.write(stacktrace);
            bos.flush();
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}