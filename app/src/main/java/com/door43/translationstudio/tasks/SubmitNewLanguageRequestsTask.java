package com.door43.translationstudio.tasks;

import com.door43.tools.reporting.FileUtils;
import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.NewLanguageRequest;
import com.door43.util.tasks.ManagedTask;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Submits new language requests to the server for processing.
 * Requests already submitted will be skipped
 */
public class SubmitNewLanguageRequestsTask extends ManagedTask {

    private List<NewLanguageRequest> requests = new ArrayList<>();

    public SubmitNewLanguageRequestsTask() {
        // load requests that have not been submitted
        File newLanguagesDir = new File(AppContext.getPublicDirectory(), "new_languages/");
        File[] requestFiles = newLanguagesDir.listFiles();
        if(requestFiles != null && requestFiles.length > 0) {
            for(File f:requestFiles) {
                try {
                    String data = FileUtils.readFileToString(f);
                    NewLanguageRequest request = NewLanguageRequest.generate(data);
                    if(request != null && request.getSubmittedAt() == 0) {
                        this.requests.add(request);
                    }
                } catch (IOException e) {
                    Logger.e(this.getClass().getName(), "Failed to read the language request file", e);
                }
            }
        }
    }

    @Override
    public void start() {
        if(AppContext.context().isNetworkAvailable()) {
            // TODO: 6/3/16 can we reuse the connection multiple times?
            publishProgress(-1, AppContext.context().getResources().getString(R.string.submitting_new_language_requests));
            for (NewLanguageRequest request : requests) {
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

                    request.setSubmittedAt(System.currentTimeMillis());
                    File requestFile = new File(AppContext.getPublicDirectory(), "new_languages/" + request.tempLanguageCode + ".json");
                    FileUtils.writeStringToFile(requestFile, request.toJson());
                    // TODO: 6/3/16 we need to update target translations using this language code

                    Logger.i(this.getClass().getName(), "new language request '" + request.tempLanguageCode + "' successfully submitted");
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "Failed to submit the new language request", e);
                }
            }
        }
    }
}
