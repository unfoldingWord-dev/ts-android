package com.door43.tools.reporting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * Created by joel on 3/25/2016.
 */
public class FileUtils {

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
}
