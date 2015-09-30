package com.door43.translationstudio.core;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;

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

    /**
     * Converts a json array to a string array
     * @param json
     * @return
     */
    public static String[] jsonArrayToString(JSONArray json) throws JSONException {
        String[] values = new String[json.length()];
        for(int i = 0; i < json.length(); i ++) {
            values[i] = json.getString(i);
        }
        return values;
    }
}
