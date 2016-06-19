package com.door43.util.network;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by joel on 6/18/16.
 */
public class PostRequest extends Request {
    private final String data;

    /**
     * Prepares the post request
     * @param url
     * @param data
     */
    private PostRequest(URL url, String data) {
        super(url, "POST");
        this.data = data;
    }

    /**
     * Creates a new post request
     * @param uri
     * @param data
     * @return
     * @throws MalformedURLException
     */
    public static PostRequest newInstance(String uri, String data) throws MalformedURLException {
        URL url = new URL(uri);
        return new PostRequest(url, data);
    }

    @Override
    protected String onSubmit(HttpURLConnection conn) throws IOException {
        sendData(data);
        return readResponse();
    }
}
