package com.door43.translationstudio.tasks;

import android.util.Pair;

import com.door43.tools.reporting.FileUtils;
import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TempLanguageRequest;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.util.tasks.ManagedTask;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
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

    private List<TempLanguageRequest> requests = new ArrayList<>();
    private int mMaxProgress = 1;

    public SubmitNewLanguageRequestsTask() {
        // load requests that have not been submitted
        File newLanguagesDir = new File(AppContext.getPublicDirectory(), "new_languages/");
        File[] requestFiles = newLanguagesDir.listFiles();
        if(requestFiles != null && requestFiles.length > 0) {
            for(File f:requestFiles) {
                try {
                    String data = FileUtils.readFileToString(f);
                    TempLanguageRequest request = TempLanguageRequest.generate(data);
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
            String progressMessage = AppContext.context().getResources().getString(R.string.submitting_new_language_requests);
            mMaxProgress = requests.size();
            publishProgress(-1, progressMessage);
            for (int i = 0; i < requests.size(); i ++) {
                TempLanguageRequest request = requests.get(i);

                // TODO: eventually we'll be able to get the server url from the db

                try {
                    // TRICKY: django needs to have the trailing slash for the post to work.
                    // TODO: 6/6/16 change this to production (td.) before releasing.
                    URL url = new URL("http://td-demo.unfoldingword.org/api/questionnaire/");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("Content-Disposition", "form-data");
                    conn.setReadTimeout(10000); // 10 seconds
                    conn.setConnectTimeout(10000); // 10 seconds
                    conn.setRequestMethod("POST");

                    // send payload as form-data. Later this chunk of code can be replaced by the commented out stuff below.
                    List<Pair<String, String>> params = new ArrayList<>();
                    params.add(new Pair<>("request_id", request.requestUUID));
                    params.add(new Pair<>("temp_code", request.tempLanguageCode));
                    params.add(new Pair<>("questionnaire_id", request.questionnaireId + ""));
                    params.add(new Pair<>("app", request.app));
                    params.add(new Pair<>("requester", request.requester));

                    JSONArray answersJson = new JSONArray();
                    Map<Long, String> answers = request.getAnswers();
                    for(long key : answers.keySet()) {
                        JSONObject answer = new JSONObject();
                        answer.put("question_id", key);
                        answer.put("text", answers.get(key));
                        answersJson.put(answer);
                    }
                    params.add(new Pair<>("answers", answersJson.toString()));

                    conn.setDoOutput(true);
                    DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                    dos.writeBytes(getQuery(params));
                    dos.flush();
                    dos.close();

                    // send payload as raw json once the server supports it.
//                    conn.setDoOutput(true);
//                    DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
//                    String data = request.toJson();
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
                    String status = "unknown";
                    String message = "";
                    if(responseJson.has("status")) {
                        status = responseJson.getString("status");
                    }
                    if(responseJson.has("message")) {
                        message = responseJson.getString("message");
                    }
                    if(status.equals("success")) {
                        Logger.i(this.getClass().getName(), "new language request '" + request.tempLanguageCode + "' successfully submitted");
                        sealRequest(request);
                    }  else if(status.endsWith("duplicate") || message.toLowerCase().contains("duplicate key value")) {
                        Logger.i(this.getClass().getName(), "new language request '" + request.tempLanguageCode + "' has already been submitted");
                        sealRequest(request);
                    } else if(!message.isEmpty()) {
                        Logger.w(this.getClass().getName(), responseJson.getString("message"));
                    }
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "Failed to submit the new language request", e);
                }
                publishProgress((float)(i + 1)/(float)mMaxProgress, progressMessage);
            }
        }
    }

    /**
     * Marks the request has submitted and updates any affected target translations
     * @param request
     */
    private void sealRequest(TempLanguageRequest request) throws IOException {
        Logger.i(this.getClass().getName(), "Sealing new language request '" + request.tempLanguageCode + "'");
        request.setSubmittedAt(System.currentTimeMillis());
        File requestFile = new File(AppContext.getPublicDirectory(), "new_languages/" + request.tempLanguageCode + ".json");
        FileUtils.writeStringToFile(requestFile, request.toJson());

        // updated affected target translations
        TargetTranslation[] translations = AppContext.getTranslator().getTargetTranslations();
        for(TargetTranslation t:translations) {
            if(t.getTargetLanguageId().equals(request.tempLanguageCode)) {
                Logger.i(this.getClass().getName(), "Updating language request in target translation '" + t.getId() + "'");
                t.setNewLanguageRequest(request);
            }
        }
    }

    /**
     * Generates the form-data query
     * @param params
     * @return
     */
    private String getQuery(List<Pair<String, String>> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (Pair<String, String> pair : params)
        {
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(pair.first, "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(pair.second, "UTF-8"));
        }

        return result.toString();
    }

    private String formBoundary() {
        return "----" + AppContext.udid() + "\r\n";
    }

    @Override
    public int maxProgress() {
        return mMaxProgress;
    }
}
