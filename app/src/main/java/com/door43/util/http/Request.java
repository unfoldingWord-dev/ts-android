package com.door43.util.http;

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
 * Represents a network request
 */
@Deprecated
abstract class Request {
    private final URL url;
    private final String requestMethod;
    private String token;
    private String username;
    private String password;
    private String contentType = null;
    private HttpURLConnection connection;
    private int responseCode = -1;

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
    protected void openConnection() throws IOException {
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
        this.connection = conn;
    }

    /**
     * Reads the response from the connection as a string
     * @return
     * @throws IOException
     */
    protected String readResponse() throws IOException {
        InputStream is = connection.getInputStream();
        BufferedInputStream bis = new BufferedInputStream(is);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int current;
        while ((current = bis.read()) != -1) {
            baos.write((byte) current);
        }
        connection.disconnect();
        return baos.toString("UTF-8");
    }

    /**
     * Submits data to the connection.
     * Such as in a POST or PUT request.
     * @param data the data to be sent
     * @throws IOException
     */
    protected void sendData(String data) throws IOException {
        connection.setDoOutput(true);
        DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
        dos.writeBytes(data);
        dos.flush();
        dos.close();
    }

    /**
     * Submits the request.
     * The connection is opened before delegating additional processing to the request Method.
     * @return
     * @throws IOException
     */
    public final String submit() throws IOException {
        openConnection();
        String response = onSubmit(connection);
        responseCode = connection.getResponseCode();
        return response;
    }

    /**
     * Returns the response code for this request
     * @return
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     * Perform methods specific actions
     * @return
     * @throws IOException
     */
    protected abstract String onSubmit(HttpURLConnection conn) throws IOException;
}
