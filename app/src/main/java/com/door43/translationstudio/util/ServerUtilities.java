package com.door43.translationstudio.util;

import android.util.Log;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.ByteArrayBuffer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;

/**
 * Provides some server utilities such as uploading and downloading
 * Created by joel on 12/2/2014.
 */
public class ServerUtilities {

    /**
     *
     * @param url
     * @param parameters
     */
    public static String post(String url, List<NameValuePair> parameters) {
        return post(url, null, null, parameters);
    }

    /**
     *
     * @param url
     * @param headers
     * @param body
     */
    public static String post(String url, List<NameValuePair> headers, String body) {
        return post(url, headers, body, null);
    }

    /**
     *
     * @param url
     * @param headers
     * @param body
     * @param parameters
     */
    public static String post(String url, List<NameValuePair> headers, String body, List<NameValuePair> parameters) {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(url);
        try {
            if(parameters != null) {
                httpPost.setEntity(new UrlEncodedFormEntity(parameters, HTTP.UTF_8));
            }
            if(body != null) {
                httpPost.setEntity(new StringEntity(body));
            }
            if(headers != null) {
                for(NameValuePair h:headers) {
                    httpPost.setHeader(h.getName(), h.getValue());
                }
            }
            HttpResponse response = httpClient.execute(httpPost);
            return response.toString();
        } catch (IOException e) {
            Logger.e(ServerUtilities.class.getName(), "failed to send post request", e);
            return "";
        }
    }

    /**
     * Downloads a file from a url and stores it on the device
     * @param url the url that will be downloaded
     * @param destFile the destination file
     */
    public static boolean downloadFile(URL url, File destFile) {
        if(!destFile.exists()) {
            destFile.getParentFile().mkdirs();
        }
        try {
            URLConnection conn = url.openConnection();
            conn.setReadTimeout(5000);
            conn.setConnectTimeout(5000);

            InputStream is = conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);

            ByteArrayBuffer bab = new ByteArrayBuffer(5000);
            int current = 0;
            while ((current = bis.read()) != -1) {
                bab.append((byte) current);
            }
            FileOutputStream fos = new FileOutputStream(destFile);
            fos.write(bab.toByteArray());
            fos.flush();
            fos.close();
        } catch(IOException e) {
            return false;
        }
        return true;
    }
}
