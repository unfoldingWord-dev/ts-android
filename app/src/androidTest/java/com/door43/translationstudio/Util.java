package com.door43.translationstudio;

import com.door43.translationstudio.util.AppContext;
import com.door43.util.FileUtilities;
import com.door43.util.tasks.ManagedTask;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
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
    public static String loadSig(String sig) throws Exception {
        InputStream sigStream = AppContext.context().getAssets().open(sig);
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
}
