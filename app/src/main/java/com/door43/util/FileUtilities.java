package com.door43.util;

import android.support.annotation.Nullable;

import org.unfoldingword.tools.logger.Logger;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides some utility methods for handling files
 */
public class FileUtilities {

    /**
     * Converts an input stream into a string
     * @param is
     * @return
     * @throws Exception
     */
    public static String readStreamToString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    /**
     * Returns the contents of a file as a string
     * @param file
     * @return
     * @throws Exception
     */
    public static String readFileToString(File file) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            String contents = readStreamToString(fis);
            fis.close();
            return contents;
        } finally {
            if(fis != null) {
                fis.close();
            }
        }
    }

    /**
     * Writes a string to a file
     * @param file
     * @param contents
     * @throws IOException
     */
    public static void writeStringToFile(File file, String contents) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file.getAbsolutePath());
            fos.write(contents.getBytes());
        } finally {
            if(fos != null) {
                fos.close();
            }
        }
    }

    public static void copyInputStreamToFile(InputStream source, File destination) throws IOException {
        try {
            FileOutputStream output = openOutputStream(destination);

            try {
                copy(source, output);
                output.close();
            } finally {
                closeQuietly(output);
            }
        } finally {
            closeQuietly(source);
        }

    }

    public static FileOutputStream openOutputStream(File file) throws IOException {
        return openOutputStream(file, false);
    }

    public static FileOutputStream openOutputStream(File file, boolean append) throws IOException {
        if(file.exists()) {
            if(file.isDirectory()) {
                throw new IOException("File \'" + file + "\' exists but is a directory");
            }

            if(!file.canWrite()) {
                throw new IOException("File \'" + file + "\' cannot be written to");
            }
        } else {
            File parent = file.getParentFile();
            if(parent != null && !parent.mkdirs() && !parent.isDirectory()) {
                throw new IOException("Directory \'" + parent + "\' could not be created");
            }
        }

        return new FileOutputStream(file, append);
    }

    public static int copy(InputStream input, OutputStream output) throws IOException {
        long count = copyLarge(input, output);
        return count > 2147483647L?-1:(int)count;
    }

    /**
     * Returns the extension of the file.
     * If no delimiter is found or there is no extension the result is an empty string
     * @param path
     * @return
     */
    public static String getExtension(String path) {
        int index = path.lastIndexOf(".");
        if(index == -1 || index == path.length() - 1) {
            return "";
        }
        return path.substring(index + 1);
    }

    public static long copyLarge(InputStream input, OutputStream output) throws IOException {
        return copyLarge(input, output, new byte[4096]);
    }

    public static long copyLarge(InputStream input, OutputStream output, byte[] buffer) throws IOException {
        long count = 0L;

        int n1;
        for(boolean n = false; -1 != (n1 = input.read(buffer)); count += (long)n1) {
            output.write(buffer, 0, n1);
        }

        return count;
    }

    /**
     * Recursively deletes a directory or just deletes the file
     * @param fileOrDirectory
     */
    public static boolean deleteQuietly(File fileOrDirectory) {
        if(fileOrDirectory != null) {
            if (fileOrDirectory.isDirectory()) {
                for (File child : fileOrDirectory.listFiles()) {
                    if(!deleteQuietly(child)) {
                        return false;
                    }
                }
            }
            if (fileOrDirectory.exists()) {
                try {
                    fileOrDirectory.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Attempts to move a file or directory. If moving fails it will try to copy instead.
     * @param sourceFile
     * @param destFile
     * @return
     */
    public static boolean moveOrCopyQuietly(File sourceFile, File destFile) {
        if(sourceFile.exists()) {
            // first try to move
            if (!sourceFile.renameTo(destFile)) {
                // try to copy
                try {
                    if (sourceFile.isDirectory()) {
                        copyDirectory(sourceFile, destFile, null);
                    } else {
                        copyFile(sourceFile, destFile);
                    }
                    return true;
                } catch (IOException e) {
                    Logger.e(FileUtilities.class.getName(), "Failed to copy the file", e);
                }
            } else {
                return true; // successful move
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
            if (file.isDirectory()) {
                FileUtilities.moveOrCopyQuietly(file, new File(temp, file.getName()));
            } else {
                FileUtilities.moveOrCopyQuietly(file, temp);
            }
            FileUtilities.deleteQuietly(file); // just in case the move failed
            FileUtilities.deleteQuietly(temp);
        }
    }

    public static void copyDirectory(File srcDir, File destDir, FileFilter filter) throws IOException {
        if(srcDir == null) {
            throw new NullPointerException("Source must not be null");
        } else if(destDir == null) {
            throw new NullPointerException("Destination must not be null");
        } else if(!srcDir.exists()) {
            throw new FileNotFoundException("Source \'" + srcDir + "\' does not exist");
        } else if(!srcDir.isDirectory()) {
            throw new IOException("Source \'" + srcDir + "\' exists but is not a directory");
        } else if(srcDir.getCanonicalPath().equals(destDir.getCanonicalPath())) {
            throw new IOException("Source \'" + srcDir + "\' and destination \'" + destDir + "\' are the same");
        } else {
            ArrayList exclusionList = null;
            if(destDir.getCanonicalPath().startsWith(srcDir.getCanonicalPath())) {
                File[] srcFiles = filter == null?srcDir.listFiles():srcDir.listFiles(filter);
                if(srcFiles != null && srcFiles.length > 0) {
                    exclusionList = new ArrayList(srcFiles.length);
                    File[] arr$ = srcFiles;
                    int len$ = srcFiles.length;

                    for(int i$ = 0; i$ < len$; ++i$) {
                        File srcFile = arr$[i$];
                        File copiedFile = new File(destDir, srcFile.getName());
                        exclusionList.add(copiedFile.getCanonicalPath());
                    }
                }
            }

            doCopyDirectory(srcDir, destDir, filter, exclusionList);
        }
    }

    private static void doCopyDirectory(File srcDir, File destDir, FileFilter filter, List<String> exclusionList) throws IOException {
        File[] srcFiles = filter == null?srcDir.listFiles():srcDir.listFiles(filter);
        if(srcFiles == null) {
            throw new IOException("Failed to list contents of " + srcDir);
        } else {
            if(destDir.exists()) {
                if(!destDir.isDirectory()) {
                    throw new IOException("Destination \'" + destDir + "\' exists but is not a directory");
                }
            } else if(!destDir.mkdirs() && !destDir.isDirectory()) {
                throw new IOException("Destination \'" + destDir + "\' directory cannot be created");
            }

            if(!destDir.canWrite()) {
                throw new IOException("Destination \'" + destDir + "\' cannot be written to");
            } else {
                File[] arr$ = srcFiles;
                int len$ = srcFiles.length;

                for(int i$ = 0; i$ < len$; ++i$) {
                    File srcFile = arr$[i$];
                    File dstFile = new File(destDir, srcFile.getName());
                    if(exclusionList == null || !exclusionList.contains(srcFile.getCanonicalPath())) {
                        if(srcFile.isDirectory()) {
                            doCopyDirectory(srcFile, dstFile, filter, exclusionList);
                        } else {
                            doCopyFile(srcFile, dstFile);
                        }
                    }
                }

                // reserve date
                destDir.setLastModified(srcDir.lastModified());
            }
        }
    }

    /**
     * Copies a file or directory
     * @param srcFile
     * @param destFile
     */
    public static void copyFile(File srcFile, File destFile) throws IOException {
        if(srcFile == null) {
            throw new NullPointerException("Source must not be null");
        } else if(destFile == null) {
            throw new NullPointerException("Destination must not be null");
        } else if(!srcFile.exists()) {
            throw new FileNotFoundException("Source \'" + srcFile + "\' does not exist");
        } else if(srcFile.isDirectory()) {
            throw new IOException("Source \'" + srcFile + "\' exists but is a directory");
        } else if(srcFile.getCanonicalPath().equals(destFile.getCanonicalPath())) {
            throw new IOException("Source \'" + srcFile + "\' and destination \'" + destFile + "\' are the same");
        } else {
            File parentFile = destFile.getParentFile();
            if(parentFile != null && !parentFile.mkdirs() && !parentFile.isDirectory()) {
                throw new IOException("Destination \'" + parentFile + "\' directory cannot be created");
            } else if(destFile.exists() && !destFile.canWrite()) {
                throw new IOException("Destination \'" + destFile + "\' exists but is read-only");
            } else {
                doCopyFile(srcFile, destFile);
            }
        }
    }

    private static void doCopyFile(File srcFile, File destFile) throws IOException {
        if(destFile.exists() && destFile.isDirectory()) {
            throw new IOException("Destination \'" + destFile + "\' exists but is a directory");
        } else {
            FileInputStream fis = null;
            FileOutputStream fos = null;
            FileChannel input = null;
            FileChannel output = null;

            try {
                fis = new FileInputStream(srcFile);
                fos = new FileOutputStream(destFile);
                input = fis.getChannel();
                output = fos.getChannel();
                long size = input.size();
                long pos = 0L;

                for(long count = 0L; pos < size; pos += output.transferFrom(input, pos, count)) {
                    count = size - pos > 31457280L?31457280L:size - pos;
                }
            } finally {
                closeQuietly(output);
                closeQuietly(fos);
                closeQuietly(input);
                closeQuietly(fis);
            }

            if(srcFile.length() != destFile.length()) {
                throw new IOException("Failed to copy full contents from \'" + srcFile + "\' to \'" + destFile + "\'");
            } else {
                // preserve date
                destFile.setLastModified(srcFile.lastModified());
            }
        }
    }

    /**
     * closes the closable without throwing an exception
     * @param closable
     */
    public static void closeQuietly(Closeable closable) {
        try {
            closable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void forceMkdir(File directory) throws IOException {
        String message;
        if(directory.exists()) {
            if(!directory.isDirectory()) {
                message = "File " + directory + " exists and is " + "not a directory. Unable to create directory.";
                throw new IOException(message);
            }
        } else if(!directory.mkdirs() && !directory.isDirectory()) {
            message = "Unable to create directory " + directory;
            throw new IOException(message);
        }

    }
}
