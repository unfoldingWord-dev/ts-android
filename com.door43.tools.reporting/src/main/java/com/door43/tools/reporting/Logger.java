package com.door43.tools.reporting;

import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Logs messages using the android Log class and also records logs to a file if configured.
 * Requires permission WRITE_EXTERNAL_STORAGE in AndroidManifest.xml.
 */
public class Logger {

    /**
     * The pattern to match the leading log line
     */
    public final static String PATTERN = "(\\d+\\/\\d+\\/\\d+\\s+\\d+:\\d+\\s+[A|P]M)\\s+([A-Z|])\\/(((?!:).)*):(.*)";
    private final File mLogFile;
    private final Level mMinLoggingLevel;
    private final long mMaxLogFileSize;
    private static Logger sInstance;
    private static final long DEFAULT_MAX_LOG_FILE_SIZE = 1024 * 200;

    static {
        sInstance = new Logger(null, Level.Info);
    }

    /**
     * @param logFile
     * @param minLogingLevel
     */
    private Logger(File logFile, Level minLogingLevel) {
        mLogFile = logFile;
        if (minLogingLevel == null) {
            mMinLoggingLevel = Level.Info;
        } else {
            mMinLoggingLevel = minLogingLevel;
        }
        mMaxLogFileSize = DEFAULT_MAX_LOG_FILE_SIZE;
    }

    /**
     * @param logFile
     * @param minLogingLevel
     * @param maxLogFileSize
     */
    private Logger(File logFile, Level minLogingLevel, long maxLogFileSize) {
        mLogFile = logFile;
        if (minLogingLevel == null) {
            mMinLoggingLevel = Level.Info;
        } else {
            mMinLoggingLevel = minLogingLevel;
        }
        mMaxLogFileSize = maxLogFileSize;
    }

    /**
     * Configures the logger to write log messages to a file
     *
     * @param logFile        the file where logs will be written
     * @param minLogingLevel the minimum level a log must be before it is recorded to the log file
     */
    public static void configure(File logFile, Level minLogingLevel) {
        sInstance = new Logger(logFile, minLogingLevel);
    }

    /**
     * Configures the logger to write log messages to a file
     *
     * @param logFile        the file where logs will be written
     * @param minLogingLevel the minimum level a log must be before it is recorded to the log file
     * @param maxLogFileSize the maximum size the log file may become before old logs are truncated
     */
    public static void configure(File logFile, Level minLogingLevel, long maxLogFileSize) {
        sInstance = new Logger(logFile, minLogingLevel, maxLogFileSize);
    }

    public enum Level {
        Info(0, "I"),
        Warning(1, "W"),
        Error(2, "E");

        Level(int i, String label) {
            this.level = i;
            this.label = label;
        }

        private int level;
        private String label;

        public int getIndex() {
            return level;
        }

        public String getLabel() {
            return label;
        }

        /**
         * Returns a level by it's label
         *
         * @param label the case insensitive label of the level
         * @return null if the level does not exist
         */
        public static Level getLevel(String label) {
            for (Level l : Level.values()) {
                if (l.getLabel().toLowerCase().equals(label.toLowerCase())) {
                    return l;
                }
            }
            return null;
        }

        /**
         * Returns a level by it's index
         *
         * @param index the level index
         * @return null if the level does not exist
         */
        public static Level getLevel(int index) {
            for (Level l : Level.values()) {
                if (l.getIndex() == index) {
                    return l;
                }
            }
            return null;
        }
    }

    /**
     * Sends an error message to LogCat and to a log file.
     *
     * @param logMessageTag A tag identifying a group of log messages. Should be a constant in the
     *                      class calling the logger.
     * @param logMessage    The message to add to the log.
     */
    public static void e(String logMessageTag, String logMessage) {
        try {
            int logResult = Log.e(logMessageTag, logMessage);
            if (logResult > 0) {
                sInstance.logToFile(Level.Error, logMessageTag, logMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a warning message to LogCat and to a log file.
     *
     * @param logMessageTag A tag identifying a group of log messages. Should be a constant in the
     *                      class calling the logger.
     * @param logMessage    The message to add to the log.
     */
    public static void w(String logMessageTag, String logMessage) {
        try {
            int logResult = Log.w(logMessageTag, logMessage);
            if (logResult > 0) {
                sInstance.logToFile(Level.Warning, logMessageTag, logMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends an info message to LogCat and to a log file.
     *
     * @param logMessageTag A tag identifying a group of log messages. Should be a constant in the
     *                      class calling the logger.
     * @param logMessage    The message to add to the log.
     */
    public static void i(String logMessageTag, String logMessage) {
        try {
            int logResult = Log.i(logMessageTag, logMessage);
            if (logResult > 0) {
                sInstance.logToFile(Level.Info, logMessageTag, logMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends an error message and the exception to LogCat and to a log file.
     *
     * @param logMessageTag      A tag identifying a group of log messages. Should be a constant in the
     *                           class calling the logger.
     * @param logMessage         The message to add to the log.
     * @param throwableException An exception to log
     */
    public static void e(String logMessageTag, String logMessage, Throwable throwableException) {
        try {
            int logResult = Log.e(logMessageTag, logMessage, throwableException);
            if (logResult > 0) {
                sInstance.logToFile(Level.Error, logMessageTag, logMessage + "\r\n" + Log.getStackTraceString(throwableException));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a message and the exception to LogCat and to a log file.
     *
     * @param logMessageTag      A tag identifying a group of log messages. Should be a constant in the
     *                           class calling the logger.
     * @param logMessage         The message to add to the log.
     * @param throwableException An exception to log
     */
    public static void w(String logMessageTag, String logMessage, Throwable throwableException) {
        try {
            int logResult = Log.w(logMessageTag, logMessage, throwableException);
            if (logResult > 0) {
                sInstance.logToFile(Level.Warning, logMessageTag, logMessage + "\r\n" + Log.getStackTraceString(throwableException));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Empties the log file
     */
    public static void flush() {
        if (sInstance.mLogFile != null) {
            sInstance.mLogFile.delete();
        } else {
            Log.w(Logger.class.getName(), "The log file has not been configured and cannot be deleted");
        }
    }

    /**
     * Gets a stamp containing the current date and time to write to the log.
     *
     * @return The stamp for the current date and time.
     */
    private static String getDateTimeStamp() {
        Date dateNow = Calendar.getInstance().getTime();
        return (DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.ENGLISH).format(dateNow));
    }

    /**
     * Writes a message to the log file on the device.
     *
     * @param logMessageTag A tag identifying a group of log messages.
     * @param logMessage    The message to add to the log.
     */
    private void logToFile(Level level, String logMessageTag, String logMessage) {
        // filter out logging levels
        if (level.getIndex() >= mMinLoggingLevel.getIndex() && mLogFile != null) {
            try {
                if (!mLogFile.exists()) {
                    mLogFile.getParentFile().mkdirs();
                    mLogFile.createNewFile();
                }

                // append log message
                String log = FileUtils.readFileToString(mLogFile);
                log = String.format("%1s %2s/%3s: %4s\r\n%5s", getDateTimeStamp(), level.getLabel(), logMessageTag, logMessage, log);
                mLogFile.delete();
                FileUtils.writeStringToFile(mLogFile, log);

                // truncate the log if it gets too big.
                if (mLogFile.length() > mMaxLogFileSize) {
                    FileChannel outChan = new FileOutputStream(mLogFile, true).getChannel();
                    outChan.truncate(mMaxLogFileSize * (long) 0.8);
                    outChan.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns a list of log entries
     * @return
     */
    public static List<Entry> getLogEntries() {
        List<Entry> logs = new ArrayList<>();
        if (sInstance.mLogFile != null) {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(sInstance.mLogFile)));
                StringBuilder sb = new StringBuilder();
                String line;
                Pattern pattern = Pattern.compile(Logger.PATTERN);
                Entry log = null;
                while ((line = br.readLine()) != null) {
                    if (Thread.interrupted()) break;
                    Matcher match = pattern.matcher(line);
                    if (match.find()) {
                        // save log
                        if (log != null) {
                            log.setDetails(sb.toString().trim());
                            logs.add(log);
                            sb.setLength(0);
                        }
                        // start new log
                        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yy hh:mm a");
                        log = new Entry(format.parse(match.group(1)), Level.getLevel(match.group(2)), match.group(3), match.group(5));
                    } else {
                        // build log details
                        sb.append(line);
                    }
                }
                // save the last log
                if (log != null) {
                    log.setDetails(sb.toString().trim());
                    logs.add(log);
                    sb.setLength(0);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.w(Logger.class.getName(), "The log file has not been configured and cannot be read");
        }
        return logs;
    }

    public static class Entry {
        public final Date date;
        public final Level level;
        public final String classPath;
        public final String message;
        private String mDetails;

        /**
         * Creates a new error object
         *
         * @param date
         * @param level
         * @param classPath
         * @param message
         */
        public Entry(Date date, Level level, String classPath, String message) {
            this.date = date;
            this.level = level;
            this.classPath = classPath;
            this.message = message;
        }

        /**
         * Creates a new error object
         *
         * @param date
         * @param level
         * @param classPath
         * @param message
         * @param details
         */
        public Entry(Date date, Level level, String classPath, String message, String details) {
            this.date = date;
            this.level = level;
            this.classPath = classPath;
            this.message = message;
            mDetails = details;
        }

        /**
         * Sets the error log details
         *
         * @param details
         */
        public void setDetails(String details) {
            mDetails = details;
        }

        /**
         * Returns the error log details
         *
         * @return
         */
        public String getDetails() {
            return mDetails;
        }
    }
}