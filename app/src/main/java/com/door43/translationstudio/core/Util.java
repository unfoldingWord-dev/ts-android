package com.door43.translationstudio.core;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by joel on 9/2/2015.
 */
public class Util {
    public static String readStream(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public static void writeStream(InputStream is, File output) throws Exception{
        output.getParentFile().mkdirs();
        try {
            FileOutputStream outputStream = new FileOutputStream(output);
            try {
                byte[] buf = new byte[1024];
                int len;
                while ((len = is.read(buf)) > 0) {
                    outputStream.write(buf, 0, len);
                }
            } finally {
                outputStream.close();
            }
        } finally {
            is.close();
        }
    }
}
