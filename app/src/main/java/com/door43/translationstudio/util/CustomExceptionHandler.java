package com.door43.translationstudio.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * http://stackoverflow.com/questions/601503/how-do-i-obtain-crash-data-from-my-android-application
 */
public class CustomExceptionHandler implements Thread.UncaughtExceptionHandler {

    private Thread.UncaughtExceptionHandler defaultUEH;

    private String localPath;

    /*
     * if any of the parameters is null, the respective functionality
     * will not be used
     */
    public CustomExceptionHandler(File localPath) {
        if(!localPath.exists()) {
            localPath.mkdirs();
        }
        this.localPath = localPath.getAbsolutePath();
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    }

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

    private void writeToFile(String stacktrace, String filename) {
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