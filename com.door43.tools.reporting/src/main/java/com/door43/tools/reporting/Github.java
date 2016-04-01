package com.door43.tools.reporting;

import android.util.Pair;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Created by joel on 8/21/2015.
 */
public class Github {

    private final String mApiUrl;

    public Github(String apiUrl) {
        mApiUrl = apiUrl;
    }

    /**
     * Returns the latest release in this repository
     * @return
     * @throws IOException
     */
    public String getLatestRelease() throws IOException {
        return getRequest("releases/latest");
    }

    /**
     * Performs a get request
     * @param apiMethod
     * @return
     * @throws IOException
     */
    private String getRequest(String apiMethod) throws IOException {
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(mApiUrl + '/' + apiMethod);
            urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            String response = convertStreamToString(in);
            urlConnection.disconnect();
            return response;
        } finally {
            if(urlConnection != null) {
                urlConnection.disconnect();
            }
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
    public String postRequest(String apiMethod, List<Pair<String, String>> headers, String payload) throws IOException {
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(mApiUrl + '/' + apiMethod);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");

            // headers
            if(headers != null) {
                for (Pair<String, String> h : headers) {
                    urlConnection.setRequestProperty(h.first, h.second);
                }
            }

            // payload
            if(payload != null) {
                urlConnection.setDoOutput(true);
                DataOutputStream out = new DataOutputStream(urlConnection.getOutputStream());
                out.writeBytes(payload);
                out.flush();
                out.close();
            }

            int responseCode = urlConnection.getResponseCode();

            // response
            BufferedInputStream bis = new BufferedInputStream(urlConnection.getInputStream());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int current;
            while ((current = bis.read()) != -1) {
                baos.write((byte) current);
            }
            String response = baos.toString("UTF-8");
            return response;
        } finally {
            if(urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }
}
