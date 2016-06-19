package com.door43.util.network;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by joel on 6/18/16.
 */
public class PatchRequest extends Request {
    private final String data;

    /**
     * Prepares the patch request
     * @param url
     * @param data
     */
    private PatchRequest(URL url, String data) {
        super(url, "PATCH");
        this.data = data;
    }

    /**
     * Creates a new patch request
     * @param uri
     * @param data
     * @return
     * @throws MalformedURLException
     */
    public static PatchRequest newInstance(String uri, String data) throws MalformedURLException {
        URL url = new URL(uri);
        return new PatchRequest(url, data);
    }

    @Override
    protected String onSubmit(HttpURLConnection conn) throws IOException {
        sendData(data);
        return readResponse();
    }
}
