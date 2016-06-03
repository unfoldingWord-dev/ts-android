package com.door43.translationstudio.tasks;

import com.door43.tools.reporting.FileUtils;
import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.NewLanguageRequest;
import com.door43.util.tasks.ManagedTask;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Submits new language requests to the server for processing.
 * Requests already submitted will be skipped
 */
public class SubmitNewLanguageRequestsTask extends ManagedTask {

    private List<NewLanguageRequest> requests = new ArrayList<>();
    private int mMaxProgress = 1;

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
            String message = AppContext.context().getResources().getString(R.string.submitting_new_language_requests);
            mMaxProgress = requests.size();
            publishProgress(-1, message);
            for (int i = 0; i < requests.size(); i ++) {
                NewLanguageRequest request = requests.get(i);
                String data = request.toJson();

                // TODO: eventually we'll be able to get the server url from the db

                try {
                    // TRICKY: django needs to have the trailing slash for the post to work.
                    URL url = new URL("http://td-demo.unfoldingword.org/api/questionnaire/");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("Content-Disposition", "form-data");
                    conn.setReadTimeout(10000); // 10 seconds
                    conn.setConnectTimeout(10000); // 10 seconds
                    conn.setRequestMethod("POST");

                    // send payload as form-data
                    String formData = generateFormDataParameter("request_id", request.requestUUID);
                    formData += generateFormDataParameter("temp_code", request.tempLanguageCode);
                    formData += generateFormDataParameter("questionnaire_id", request.questionnaireId + "");
                    formData += generateFormDataParameter("app", request.app);
                    formData += generateFormDataParameter("requester", request.requester);
                    JSONArray answersJson = new JSONArray();
                    Map<Long, String> answers = request.getAnswers();
                    for(long key : answers.keySet()) {
                        JSONObject answer = new JSONObject();
                        answer.put("question_id", key);
                        answer.put("text", answers.get(key));
                        answersJson.put(answer);
                    }
                    formData += generateFormDataParameter("answers", answersJson.toString());
                    formData += formBoundary();
                    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + AppContext.udid());
                    conn.setDoOutput(true);
                    DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                    dos.writeBytes(formData);
                    dos.flush();
                    dos.close();


                    // send payload as raw json
//                    conn.setDoOutput(true);
//                    DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
//                    dos.writeBytes(data);
//                    dos.flush();
//                    dos.close();

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

                    // process response
                    JSONObject responseJson = new JSONObject(response);
                    if(responseJson.has("status") && responseJson.getString("status").equals("success")) {
                        request.setSubmittedAt(System.currentTimeMillis());
                        File requestFile = new File(AppContext.getPublicDirectory(), "new_languages/" + request.tempLanguageCode + ".json");
                        FileUtils.writeStringToFile(requestFile, request.toJson());
                        // TODO: 6/3/16 we need to update target translations using this language code

                        Logger.i(this.getClass().getName(), "new language request '" + request.tempLanguageCode + "' successfully submitted");
                    } else if(responseJson.has("message")) {
                        Logger.w(this.getClass().getName(), responseJson.getString("message"));
                    }
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "Failed to submit the new language request", e);
                }
                publishProgress((float)(i + 1)/(float)mMaxProgress, message);
            }
        }
    }

    /**
     * Generates a form data parameter.
     * This is temporary until the server supports raw json.
     * @param key
     * @param value
     * @return
     */
    private String generateFormDataParameter(String key, String value) {
        String parameter = formBoundary();
        parameter += "Content-Disposition: form-data; name=\"" + key + "\"\r\n";
        parameter += "\r\n";
        parameter += value;
        parameter += "\r\n";
        return parameter;
    }

    private String formBoundary() {
        return "----" + AppContext.udid() + "\r\n";
    }

    @Override
    public int maxProgress() {
        return mMaxProgress;
    }
}
