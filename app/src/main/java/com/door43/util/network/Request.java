package com.door43.util.network;

import android.util.Base64;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by joel on 6/18/16.
 */
abstract class Request {
    private final URL url;
    private final String requestMethod;
    private String token;
    private String username;
    private String password;
    private String contentType = null;

    /**
     * Prepare a new network request
     * @param url The url that will receive the request
     * @param requestMethod the method of request e.g. POST, GET, PUT, etc.
     */
    public Request(URL url, String requestMethod) {
        this.url = url;
        this.requestMethod = requestMethod.toUpperCase();
    }

    /**
     * Sets the token used for authenticating the post request
     * Tokens take precedence over credentials
     * Token authentication.
     * @param token
     */
    public void setAuthorization(String token) {
        this.token = token;
    }

    /**
     * Sets the credentials used for authenticating the report
     * Basic authentication.
     * @param username
     * @param password
     */
    public void setAuthorization(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Generates and returns the auth information if available
     * @return
     */
    protected String getAuth() {
        if(this.token != null) {
            return "token " + this.token;
        } else if(this.username != null && this.password != null){
            String credentials = this.username + ":" + this.password;
            try {
                return "Basic " + Base64.encodeToString(credentials.getBytes("UTF-8"), Base64.NO_WRAP);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Sets the content type to be used in the request
     * @param contentType
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Creates a new connection object
     * @return
     * @throws IOException
     */
    protected HttpURLConnection openConnection() throws IOException {
        HttpURLConnection conn;
        if(url.getProtocol() == "https") {
            conn = (HttpsURLConnection)url.openConnection();
        } else {
            conn = (HttpURLConnection)url.openConnection();
        }
        String auth = getAuth();
        if(auth != null) {
            conn.setRequestProperty("Authorization", auth);
        }
        if(contentType != null) {
            conn.setRequestProperty("Content-Type", contentType);
        }
        conn.setRequestMethod(requestMethod);
        return conn;
    }

    /**
     * Reads the response from the connection as a string
     * @param conn the connection to be read
     * @return
     * @throws IOException
     */
    protected static String readResponse(HttpURLConnection conn) throws IOException {
        InputStream is = conn.getInputStream();
        BufferedInputStream bis = new BufferedInputStream(is);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int current;
        while ((current = bis.read()) != -1) {
            baos.write((byte) current);
        }
        conn.disconnect();
        return baos.toString("UTF-8");
    }

    /**
     * Submits data to the connection.
     * Such as in a POST or PUT request.
     * @param conn the connection receiving the data
     * @param data the data to be sent
     * @throws IOException
     */
    protected void sendData(HttpURLConnection conn, String data) throws IOException {
        conn.setDoOutput(true);
        DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
        dos.writeBytes(data);
        dos.flush();
        dos.close();
    }

    /**
     * Submits the request
     * @return
     * @throws IOException
     */
    abstract String submit() throws IOException;
}
