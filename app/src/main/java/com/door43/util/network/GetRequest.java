package com.door43.util.network;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by joel on 6/18/16.
 */
public class GetRequest extends Request {

    /**
     * Prepares the post request
     * @param url
     */
    private GetRequest(URL url) {
        super(url, "GET");
    }

    /**
     * Creates a new post request
     * @param uri
     * @return
     * @throws MalformedURLException
     */
    public static GetRequest newInstance(String uri) throws MalformedURLException {
        URL url = new URL(uri);
        return new GetRequest(url);
    }

    /**
     * Submits the get request
     *
     * @return
     * @throws IOException
     */
    @Override
    public String submit() throws IOException {
        HttpURLConnection conn = openConnection();
        return readResponse(conn);
    }
}
