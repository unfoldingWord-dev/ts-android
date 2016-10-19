package com.door43.util.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by joel on 6/18/16.
 */
@Deprecated
public class GetRequest extends Request {

    /**
     * Prepares the get request
     * @param url
     */
    private GetRequest(URL url) {
        super(url, "GET");
    }

    /**
     * Creates a new get request
     * @param uri
     * @return
     * @throws MalformedURLException
     */
    public static GetRequest newInstance(String uri) throws MalformedURLException {
        URL url = new URL(uri);
        return new GetRequest(url);
    }

    @Override
    protected String onSubmit(HttpURLConnection conn) throws IOException {
        return readResponse();
    }
}
