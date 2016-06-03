package com.door43.translationstudio.tasks;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.core.NewLanguageRequest;
import com.door43.util.tasks.ManagedTask;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by joel on 6/3/16.
 */
public class SubmitNewLanguageRequestTask extends ManagedTask {

    private final NewLanguageRequest request;
    private boolean success = false;

    public SubmitNewLanguageRequestTask(NewLanguageRequest request) {
        this.request = request;
    }

    @Override
    public void start() {
        if(request != null) {
            String data = request.toJson();

            // TODO: eventually we'll be able to get the server url from the db

            try {
                URL url = new URL("http://td.unfoldingword.org/api/questionnaire");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setReadTimeout(10000); // 10 seconds
                conn.setConnectTimeout(10000); // 10 seconds
                conn.setRequestMethod("POST");

                // send payload
                conn.setDoOutput(true);
                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(data);
                dos.flush();
                dos.close();

                // read response
                int responsCode = conn.getResponseCode();
                InputStream is = conn.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int current;
                while ((current = bis.read()) != -1) {
                    baos.write((byte) current);
                }
                String response = baos.toString("UTF-8");

                // TODO: 6/3/16 process the response
                success = true;
            } catch (Exception e) {
                Logger.e(this.getClass().getName(), "Failed to submit the new language request", e);
            }
        }
    }

    /**
     * Checks if the submission was a success
     * @return
     */
    public boolean success() {
        return this.success;
    }
}
