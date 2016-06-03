package com.door43.translationstudio.newui.newlanguage;

import android.app.Activity;
import android.content.Context;
import android.util.Base64;
import android.view.View;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.NewLanguagePackage;
import com.door43.translationstudio.core.NewLanguageQuestion;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.dialogs.CustomAlertDialog;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.unfoldingword.gogsclient.Response;
import org.unfoldingword.gogsclient.User;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import android.os.Handler;
import android.os.Looper;

/**
 * Created by blm on 3/21/16.
 */
public class NewLanguageAPI {
    public static final String TAG = "";//NewLanguageAPI.class.getSimpleName();
    private int readTimeout = 5000;
    private int connectionTimeout = 5000;
    private Response lastResponse = null;

    /**
     * Performs a request against the api
     * @param urlStr the api command
     * @param user the user authenticating this request. Requires token or username and pasword
     * @param postData if not null the request will POST the data (key,value) otherwise it will be a GET request
     * @param requestMethod if null the request method will default to POST or GET
     * @return
     */
    public Response doRequest(String urlStr, User user, HashMap<String, String> postData, String requestMethod) {
        int responseCode = -1;
        String responseData = null;
        Exception exception = null;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn;
            if(url.getProtocol() == "https") {
                conn = (HttpsURLConnection)url.openConnection();
            } else {
                conn = (HttpURLConnection)url.openConnection();
            }

            conn.setRequestProperty("Content-Type", "application/json");
            conn.setReadTimeout(this.readTimeout);
            conn.setConnectTimeout(this.connectionTimeout);

            // custom request method
            if(requestMethod != null) {
                conn.setRequestMethod(requestMethod.toUpperCase());
            }

            if(postData != null) {
                // post
                if(requestMethod == null) {
                    conn.setRequestMethod("POST");
                }

                // build multi-part post

                String lineEnd = "\r\n";
                String hyphens = "--";
                String boundary = AppContext.udid() ;
                String postString = "";
                for(Map.Entry<String, String> entry : postData.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();

                    String parameterStr = hyphens + boundary + lineEnd;
                    parameterStr += "Content-Disposition: form-data; name=\"" + key + "\"" + lineEnd;
                    parameterStr += lineEnd;
                    parameterStr += value;
                    parameterStr += lineEnd;

                    postString += parameterStr;
                }

                postString += hyphens + boundary + lineEnd;

//                Logger.i(TAG, "post data:\n" + postString);

                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary="+boundary);

                conn.setDoOutput(true);
                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(postString);
                dos.flush();
                dos.close();
            }

            responseCode = conn.getResponseCode();

            try {
                // read response
                InputStream is = conn.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int current;
                while ((current = bis.read()) != -1) {
                    baos.write((byte) current);
                }
                responseData = baos.toString("UTF-8");
            } catch (Exception e) {
                //no response data
            }

        } catch (Exception e) {
            exception = e;
        }
        this.lastResponse = new Response(responseCode, responseData, exception);
        return this.lastResponse;
    }


}
