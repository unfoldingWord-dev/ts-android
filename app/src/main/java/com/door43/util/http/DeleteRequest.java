package com.door43.util.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by joel on 6/18/16.
 */
public class DeleteRequest extends Request {

    /**
     * Prepares the delete request
     * @param url
     */
    private DeleteRequest(URL url) {
        super(url, "DELETE");
    }

    /**
     * Creates a new delete request
     * @param uri
     * @return
     * @throws MalformedURLException
     */
    public static DeleteRequest newInstance(String uri) throws MalformedURLException {
        URL url = new URL(uri);
        return new DeleteRequest(url);
    }

    @Override
    protected String onSubmit(HttpURLConnection conn) throws IOException {
        return readResponse();
    }
}
