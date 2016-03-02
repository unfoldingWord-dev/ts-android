package com.door43.tools.reporting;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.List;

/**
 * Created by joel on 8/21/2015.
 */
public class Github {

    private final String mApiUrl;

    public Github(String apiUrl) {
        mApiUrl = apiUrl;
    }

    public String getLatestRelease() {
        return getRequest("releases/latest");
    }

    private String getRequest(String apiMethod) {
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(mApiUrl + '/' + apiMethod);
        try {
            HttpResponse response = httpClient.execute(httpGet);
            if(response != null) {
                return convertStreamToString(response.getEntity().getContent());
            } else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Converts the input stream to a string
     * @param inputStream
     * @return
     * @throws IOException
     */
    private static String convertStreamToString(InputStream inputStream) throws IOException {
        if (inputStream != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"),1024);
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                inputStream.close();
            }
            return writer.toString();
        } else {
            return null;
        }
    }

    /**
     * Performs a post request against the github api.
     * @param apiMethod
     * @param headers
     * @param payload the data to submit to the server
     * @return null if the postRequest fails or the response
     */
    private String postRequest(String apiMethod, List<NameValuePair> headers, JSONObject payload) {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(mApiUrl + '/' + apiMethod);
        if(payload != null) {
            try {
                httpPost.setEntity(new StringEntity(payload.toString()));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if (headers != null) {
            for (NameValuePair h : headers) {
                httpPost.setHeader(h.getName(), h.getValue());
            }
        }
        try {
            HttpResponse response = httpClient.execute(httpPost);
            return response.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
