package com.door43.translationstudio;

import android.content.Context;

import com.door43.translationstudio.util.AppContext;
import com.door43.util.FileUtilities;
import com.door43.util.tasks.ManagedTask;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by joel on 2/25/2015.
 */
public class Util {
    /**
     * Utiltiy to load a signature
     * @param sig
     * @return
     * @throws Exception
     */
    public static String loadSig(Context context, String sig) throws Exception {
        InputStream sigStream = context.getAssets().open(sig);
        String sigJson = FileUtilities.convertStreamToString(sigStream);
        JSONArray json = new JSONArray(sigJson);
        JSONObject sigObj = json.getJSONObject(0);
        return sigObj.getString("sig");
    }

    /**
     * Executes a task
     * @param task
     */
    public static void runTask(ManagedTask task) {
        task.start();
    }

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

    public static void copyStreamToCache(Context context, InputStream is, File output) throws Exception{
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
