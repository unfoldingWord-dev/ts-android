package com.door43.translationstudio.util;

import android.media.MediaScannerConnection;
import android.util.Log;

import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
//    private static Level mLoggingLevel = Level.Info;
    public static enum Level {
        Info(0),
        Warning(1),
        Error(2);

        Level(int i) {
            this.level = i;
        }

        private int level;

        public int getLevel() {
            return level;
        }
    }

    /**
     * Specifies the minimum level included in the logs
     * @param level
     */
//    public static void setLoggingLevel(Level level) {
//        if(level == null) {
//            mLoggingLevel = Level.Info;
//        } else {
//            mLoggingLevel = level;
//        }
//    }

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
            if (logResult > 0) logToFile(Level.Error, logMessageTag, logMessage);
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
            if (logResult > 0) logToFile(Level.Warning, logMessageTag, logMessage);
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
            if (logResult > 0) logToFile(Level.Info, logMessageTag, logMessage);
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
                logToFile(Level.Error, logMessageTag, logMessage + "\r\n" + Log.getStackTraceString(throwableException));
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
                logToFile(Level.Warning, logMessageTag, logMessage + "\r\n" + Log.getStackTraceString(throwableException));
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
    private static void logToFile(Level level, String logMessageTag, String logMessage)
    {
        // filter out logging levels
        int minLevel = Integer.parseInt(AppContext.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_LOGGING_LEVEL, AppContext.context().getResources().getString(R.string.pref_default_logging_level)));
        if(level.getLevel() < minLevel) return;

        try
        {
            // Gets the log file from the root of the primary storage. If it does
            // not exist, the file is created.
            // TODO: this path should be some place global
            File logFile = new File(AppContext.context().getExternalCacheDir(), "log.txt");
            if (!logFile.exists()) {
                logFile.getParentFile().mkdirs();
                logFile.createNewFile();
            }
            // Write the message to the log with a timestamp
            String flag = "";
            switch(level) {
                case Error:
                    flag = "E";
                    break;
                case Warning:
                    flag = "W";
                    break;
                case Info:
                default:
                    flag = "I";
            }

            // append log message
            String log = FileUtils.readFileToString(logFile);
            log = String.format("%1s %2s/%3s: %4s\r\n%5s", getDateTimeStamp(), flag, logMessageTag, logMessage, log);
            logFile.delete();
            FileUtils.writeStringToFile(logFile, log);

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
            MediaScannerConnection.scanFile(AppContext.context(), new String[]{logFile.toString()}, null, null);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}