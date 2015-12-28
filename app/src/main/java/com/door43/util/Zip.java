package com.door43.util;

import android.support.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * This class handles zipping and un-zipping files and directories
 */
public class Zip {
    /**
     * Creates a zip archive
     * http://stackoverflow.com/questions/6683600/zip-compress-a-folder-full-of-files-on-android
     * @param sourcePath
     * @param destPath
     * @throws java.io.IOException
     */
    public static void zip(String sourcePath, String destPath) throws IOException {
        final int BUFFER = 2048;
        File sourceFile = new File(sourcePath);
        BufferedInputStream origin = null;
        FileOutputStream dest = new FileOutputStream(destPath);
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
        if (sourceFile.isDirectory()) {
            // TRICKY: we add 1 to the base path length to exclude the leading path separator
            zipSubFolder(out, sourceFile, sourceFile.getParent().length() + 1);
        } else {
            byte data[] = new byte[BUFFER];
            FileInputStream fi = new FileInputStream(sourcePath);
            origin = new BufferedInputStream(fi, BUFFER);
            String[] segments = sourcePath.split("/");
            String lastPathComponent = segments[segments.length - 1];
            ZipEntry entry = new ZipEntry(lastPathComponent);
            out.putNextEntry(entry);
            int count;
            while ((count = origin.read(data, 0, BUFFER)) != -1) {
                out.write(data, 0, count);
            }
        }
        out.close();
    }

    /**
     * Adds a file to a zip archive
     * http://stackoverflow.com/questions/3048669/how-can-i-add-entries-to-an-existing-zip-file-in-java
     * @param zipFile
     * @param files
     * @throws IOException
     */
    @Deprecated
    public static void addFilesToExistingZip(File zipFile, File[] files) throws IOException {
        // get a temp file
        File tempFile = File.createTempFile(zipFile.getName(), null);
        // delete it, otherwise you cannot rename your existing zip to it.
        tempFile.delete();

        boolean renameOk=zipFile.renameTo(tempFile);
        if (!renameOk)
        {
            throw new RuntimeException("could not rename the file "+zipFile.getAbsolutePath()+" to "+tempFile.getAbsolutePath());
        }
        byte[] buf = new byte[1024];

        ZipInputStream zin = new ZipInputStream(new FileInputStream(tempFile));
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));

        ZipEntry entry = zin.getNextEntry();
        while (entry != null) {
            String name = entry.getName();
            boolean notInFiles = true;
            for (File f : files) {
                if (f.getName().equals(name)) {
                    notInFiles = false;
                    break;
                }
            }
            if (notInFiles) {
                // Add ZIP entry to output stream.
                out.putNextEntry(new ZipEntry(name));
                // Transfer bytes from the ZIP file to the output file
                int len;
                while ((len = zin.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
            entry = zin.getNextEntry();
        }
        // Close the streams
        zin.close();
        // Compress the files
        for (int i = 0; i < files.length; i++) {
            InputStream in = new FileInputStream(files[i]);
            // Add ZIP entry to output stream.
            out.putNextEntry(new ZipEntry(files[i].getName()));
            // Transfer bytes from the file to the ZIP file
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            // Complete the entry
            out.closeEntry();
            in.close();
        }
        // Complete the ZIP file
        out.close();
        tempFile.delete();
    }

    /**
     * Zips up a list of files to file
     * @param files
     * @param archivePath - destination file
     */
    public static void zip(File[] files, File archivePath) throws IOException {
        FileOutputStream dest = new FileOutputStream(archivePath);
        zipToStream(files, dest);
    }

    /**
     * Zips up a list of files to output stream
     * @param files
     * @param dest - destination output stream
     */
    public static void zipToStream(File[] files, OutputStream dest) throws IOException {
        final int BUFFER = 2048;
        BufferedInputStream origin = null;
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
                dest));

        for(File f:files) {
            if (f.isDirectory()) {
                // TRICKY: we add 1 to the base path length to exclude the leading path separator
                zipSubFolder(out, f, f.getParent().length() + 1);
            } else {
                byte data[] = new byte[BUFFER];
                FileInputStream fi = new FileInputStream(f);
                origin = new BufferedInputStream(fi, BUFFER);
                String[] segments = f.getAbsolutePath().split("/");
                String lastPathComponent = segments[segments.length - 1];
                ZipEntry entry = new ZipEntry(lastPathComponent);
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
            }
        }

        out.close();
    }

    /**
     * Zips up a list of files that are mapped to a relative directory in the resulting archive.
     * TODO: we'd like to begin using this so we don't have to organize everything into a directory before zipping
     * @param files
     * @param archivePath
     * @throws IOException
     */
    public static void zip(Map<File, String> files, File archivePath) throws IOException {
        final int BUFFER = 2048;
        BufferedInputStream origin = null;
        FileOutputStream dest = new FileOutputStream(archivePath);
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));

        Iterator it = files.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<File, String> pair = (Map.Entry)it.next();
            // clean the target path
            pair.setValue(pair.getValue().replaceAll("^/+", "").replace("/*$", "") + "/");
            if (pair.getKey().isDirectory()) {
                // TRICKY: we add 1 to the base path length to exclude the leading path separator
                zipSubFolder(out, pair.getKey(), pair.getValue() + pair.getKey().getName());
            } else {
                byte data[] = new byte[BUFFER];
                FileInputStream fi = new FileInputStream(pair.getKey());
                origin = new BufferedInputStream(fi, BUFFER);
                String relativePath = pair.getValue() + pair.getKey().getName();
                ZipEntry entry = new ZipEntry(pair.getValue());
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
            }
        }

        out.close();
    }

    /**
     * Zips up a sub folder
     * @param out
     * @param folder
     * @param basePathLength
     * @throws IOException
     */
    private static void zipSubFolder(ZipOutputStream out, File folder, int basePathLength) throws IOException {
        final int BUFFER = 2048;
        File[] fileList = folder.listFiles();
        BufferedInputStream origin = null;
        for (File file : fileList) {
            if (file.isDirectory()) {
                zipSubFolder(out, file, basePathLength);
            } else {
                byte data[] = new byte[BUFFER];
                String unmodifiedFilePath = file.getPath();
                String relativePath = unmodifiedFilePath.substring(basePathLength);
                FileInputStream fi = new FileInputStream(unmodifiedFilePath);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(relativePath);
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }
        }
    }

    /**
     * Zips up a sub folder
     * @param out
     * @param folder
     * @param relativePath
     * @throws IOException
     */
    private static void zipSubFolder(ZipOutputStream out, File folder, String relativePath) throws IOException {
        final int BUFFER = 2048;
        File[] fileList = folder.listFiles();
        BufferedInputStream origin = null;
        for (File file : fileList) {
            if (file.isDirectory()) {
                zipSubFolder(out, file, relativePath + "/" + file.getName());
            } else {
                byte data[] = new byte[BUFFER];
                FileInputStream fi = new FileInputStream(file.getPath());
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(relativePath + "/" + file.getName());
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }
        }
    }

    /**
     * Extracts a zip archive
     * @param zipPath
     * @throws IOException
     */
    public static void unzip(String zipPath, String destPath) throws IOException {
        InputStream is;
        ZipInputStream zis;
        String filename;
        ZipEntry ze;
        int count;
        byte[] buffer = new byte[1024];
        is = new FileInputStream(zipPath);
        zis = new ZipInputStream(new BufferedInputStream(is));

        File destDir = new File(destPath);
        destDir.mkdirs();

        while ((ze = zis.getNextEntry()) != null) {
            filename = ze.getName();
            File f = new File(destPath, filename);
            if (ze.isDirectory()) {
                f.mkdirs();
                continue;
            }
            f.getParentFile().mkdirs();
            f.createNewFile();
            FileOutputStream fout = new FileOutputStream(f.getAbsolutePath());
            while ((count = zis.read(buffer)) != -1) {
                fout.write(buffer, 0, count);
            }
            fout.close();
            zis.closeEntry();
        }
        zis.close();
    }

    /**
     * Extracts a zip archive
     * @param zipArchive
     * @param destDir - place to store unzipped file
     * @throws IOException
     */
    public static void unzip(File zipArchive, File destDir) throws IOException {
        InputStream is;
        ZipInputStream zis;
        String filename;
        ZipEntry ze;
        int count;
        is = new FileInputStream(zipArchive);
        unzipFromStream(is, destDir);
    }

    /**
     * Extracts a zip archive from a stream
     * @param is - input stream of zip file
     * @param destDir - place to store unzipped file
     * @throws IOException
     */
    public static void unzipFromStream(InputStream is, File destDir) throws IOException {
        byte[] buffer = new byte[1024];
        ZipInputStream zis;
        ZipEntry ze;
        String filename;
        int count;
        zis = new ZipInputStream(new BufferedInputStream(is));

        destDir.mkdirs();

        while ((ze = zis.getNextEntry()) != null) {
            filename = ze.getName();
            File f = new File(destDir, filename);
            if (ze.isDirectory()) {
                f.mkdirs();
                continue;
            }
            f.getParentFile().mkdirs();
            f.createNewFile();
            FileOutputStream fout = new FileOutputStream(f.getAbsolutePath());
            while ((count = zis.read(buffer)) != -1) {
                fout.write(buffer, 0, count);
            }
            fout.close();
            zis.closeEntry();
        }
        zis.close();
    }

    /**
     * Lists the contents of the zip file
     * @param zipArchive
     * @return
     * @throws IOException
     */
    public static String[] list(File zipArchive) throws IOException {
        InputStream is;
        ZipInputStream zis;
        ZipEntry ze;
        is = new FileInputStream(zipArchive);
        zis = new ZipInputStream(new BufferedInputStream(is));

        List<String> files = new ArrayList<>();
        while ((ze = zis.getNextEntry()) != null) {
            files.add(ze.getName());
            if (ze.isDirectory()) {
                continue;
            }
            zis.closeEntry();
        }
        zis.close();
        return files.toArray(new String[files.size()]);
    }

    /**
     * Reads the contents of a file from the zip archive
     * @param zipArchive
     * @param path
     * @return
     */
    public static String read(File zipArchive, String path) throws IOException {
        InputStream is;
        is = new FileInputStream(zipArchive);
        return readInputStream(is, path);
    }

    /**
     * Reads the contents of a file from the zip archive
     * @param zipStream
     * @param path
     * @return
     */    @Nullable
    public static String readInputStream(InputStream zipStream, String path) throws IOException {
        String contents = null;
        ZipInputStream zis;
        ZipEntry ze;
        zis = new ZipInputStream(new BufferedInputStream(zipStream));

        while ((ze = zis.getNextEntry()) != null) {
            if (ze.isDirectory()) {
                continue;
            }
            if(ze.getName().equalsIgnoreCase(path)) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(zis));
                StringBuilder sb = new StringBuilder();

                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                contents = sb.toString();
            }
            zis.closeEntry();
            if(contents != null) {
                break;
            }
        }
        zis.close();
        return contents;
    }
}
