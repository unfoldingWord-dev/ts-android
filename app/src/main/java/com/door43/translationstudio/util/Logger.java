package com.door43.translationstudio.util;

import android.media.MediaScannerConnection;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * A logger that uses the standard Android Log class to log exceptions, and also logs them to a
 * file on the device. Requires permission WRITE_EXTERNAL_STORAGE in AndroidManifest.xml.
 * @author Cindy Potvin
 * http://www.codeproject.com/Articles/738115/Creating-logs-in-Android-applications
 */
public class Logger
{
    /**
     * Sends an error message to LogCat and to a log file.
     * @param logMessageTag A tag identifying a group of log messages. Should be a constant in the
     *                      class calling the logger.
     * @param logMessage The message to add to the log.
     */
    public static void e(String logMessageTag, String logMessage)
    {
        try {
            int logResult = Log.e(logMessageTag, logMessage);
            if (logResult > 0) logToFile(logMessageTag, logMessage);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a warning message to LogCat and to a log file.
     * @param logMessageTag A tag identifying a group of log messages. Should be a constant in the
     *                      class calling the logger.
     * @param logMessage The message to add to the log.
     */
    public static void w(String logMessageTag, String logMessage)
    {
        try {
            int logResult = Log.w(logMessageTag, logMessage);
            if (logResult > 0) logToFile(logMessageTag, logMessage);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends an info message to LogCat and to a log file.
     * @param logMessageTag A tag identifying a group of log messages. Should be a constant in the
     *                      class calling the logger.
     * @param logMessage The message to add to the log.
     */
    public static void i(String logMessageTag, String logMessage)
    {
        try {
            int logResult = Log.i(logMessageTag, logMessage);
            if (logResult > 0) logToFile(logMessageTag, logMessage);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends an error message and the exception to LogCat and to a log file.
     * @param logMessageTag A tag identifying a group of log messages. Should be a constant in the
     *                      class calling the logger.
     * @param logMessage The message to add to the log.
     * @param throwableException An exception to log
     */
    public static void e(String logMessageTag, String logMessage, Throwable throwableException)
    {
        try {
            int logResult = Log.e(logMessageTag, logMessage, throwableException);
            if (logResult > 0)
                logToFile(logMessageTag, logMessage + "\r\n" + Log.getStackTraceString(throwableException));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a message and the exception to LogCat and to a log file.
     * @param logMessageTag A tag identifying a group of log messages. Should be a constant in the
     *                      class calling the logger.
     * @param logMessage The message to add to the log.
     * @param throwableException An exception to log
     */
    public static void w(String logMessageTag, String logMessage, Throwable throwableException)
    {
        try {
            int logResult = Log.w(logMessageTag, logMessage, throwableException);
            if (logResult > 0)
                logToFile(logMessageTag, logMessage + "\r\n" + Log.getStackTraceString(throwableException));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets a stamp containing the current date and time to write to the log.
     * @return The stamp for the current date and time.
     */
    private static String getDateTimeStamp()
    {
        Date dateNow = Calendar.getInstance().getTime();
        return (DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.ENGLISH).format(dateNow));
    }

    /**
     * Writes a message to the log file on the device.
     * @param logMessageTag A tag identifying a group of log messages.
     * @param logMessage The message to add to the log.
     */
    private static void logToFile(String logMessageTag, String logMessage)
    {
        try
        {
            // Gets the log file from the root of the primary storage. If it does
            // not exist, the file is created.
            // TODO: this path should be some place global
            File logFile = new File(MainContext.getContext().getExternalCacheDir(), "log.txt");
            if (!logFile.exists()) {
                logFile.getParentFile().mkdirs();
                logFile.createNewFile();
            }
            // Write the message to the log with a timestamp
            RandomAccessFile writer = new RandomAccessFile(logFile, "rw");
            writer.seek(0);
            writer.write(String.format("%1s [%2s]:%3s\r\n", getDateTimeStamp(), logMessageTag, logMessage).getBytes());
            writer.close();

            // truncate the log if it gets too big. we cut it in half so we don't end up having to do this all the time
            // TODO: this should be a user setting.
            long maxLogSize = 1024*200; // 200KB
            if(logFile.length() > maxLogSize) {
                FileChannel outChan = new FileOutputStream(logFile, true).getChannel();
                outChan.truncate(maxLogSize/2);
                outChan.close();
            }

            // Refresh the data so it can seen when the device is plugged in a
            // computer. You may have to unplug and replug to see the latest
            // changes
            MediaScannerConnection.scanFile(MainContext.getContext(), new String[]{logFile.toString()}, null, null);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}