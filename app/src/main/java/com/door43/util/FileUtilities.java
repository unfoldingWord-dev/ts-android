package com.door43.util;

import android.content.Context;

import com.door43.tools.reporting.Logger;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * This class provides some utility methods for handling files
 */
public class FileUtilities {
    /**
     * Converts an input stream into a string
     * @param is the input stream
     * @return
     * @throws Exception
     */
    public static String convertStreamToString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    /**
     * Returns the contents of a file as a string
     * @param f
     * @return
     * @throws Exception
     */
    public static String getStringFromFile(File f) throws IOException {
        return getStringFromFile(f.getAbsolutePath());
    }

    /**
     * Returns the contents of a file as a string
     * @param filePath the path to the file
     * @return
     * @throws Exception
     */
    public static String getStringFromFile (String filePath) throws IOException {
        File fl = new File(filePath);
        if (fl.exists()) {
            FileInputStream fin = new FileInputStream(fl);
            String ret = convertStreamToString(fin);
            //Make sure you close all streams.
            fin.close();
            return ret;
        } else {
            return null;
        }
    }

    /**
     * Recursively deletes a direcotry or just deletes the file
     * @param fileOrDirectory
     */
    public static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }

    /**
     * Attempts to move a file or directory. If moving fails it will try to copy instead.
     * @param sourceFile
     * @param destFile
     * @return
     */
    public static boolean moveOrCopy(File sourceFile, File destFile) {
        if(sourceFile.exists()) {
            // first try to move
            if (!sourceFile.renameTo(destFile)) {
                // try to copy
                try {
                    if (sourceFile.isDirectory()) {
                        FileUtils.copyDirectory(sourceFile, destFile);
                    } else {
                        FileUtils.copyFile(sourceFile, destFile);
                    }
                    return true;
                } catch (IOException e) {
                    Logger.e(FileUtilities.class.getName(), "Failed to copy the file", e);
                }
            }
        }
        return false;
    }

    /**
     * Deletes a file/directory by first moving it to a temporary location then deleting it.
     * This avoids an issue with FAT32 on some devices where you cannot create a file
     * with the same name right after deleting it
     * @param file
     */
    public static void safeDelete(File file) {
        if(file != null && file.exists()) {
            File temp = new File(file.getParentFile(), System.currentTimeMillis() + ".trash");
            file.renameTo(temp);
            try {
                if (file.isDirectory()) {
                    FileUtils.moveDirectoryToDirectory(file, temp, true);
                } else {
                    FileUtils.moveFile(file, temp);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            FileUtils.deleteQuietly(file); // just in case the move failed
            FileUtils.deleteQuietly(temp);
        }
    }
}
