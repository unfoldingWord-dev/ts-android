package com.door43.util.network;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by joel on 6/18/16.
 */
public class PutRequest extends Request {
    private final String data;

    /**
     * Prepares the put request
     * @param url
     * @param data
     */
    private PutRequest(URL url, String data) {
        super(url, "PUT");
        this.data = data;
    }

    /**
     * Creates a new put request
     * @param uri
     * @param data
     * @return
     * @throws MalformedURLException
     */
    public static PutRequest newInstance(String uri, String data) throws MalformedURLException {
        URL url = new URL(uri);
        return new PutRequest(url, data);
    }

    @Override
    protected String onSubmit(HttpURLConnection conn) throws IOException {
        sendData(data);
        return readResponse();
    }
}
