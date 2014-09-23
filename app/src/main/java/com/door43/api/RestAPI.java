package com.door43.api;

import android.content.Context;

import java.util.Map;

/**
 * Created by joel on 7/8/2014.
 * Use this class to perform api requests.
 */
public class RestAPI {
    private String baseUrl = null;

    public RestAPI(String url) {
        this.baseUrl = url;
    }

    /**
     * Performs a POST request against the api
     * @param server the api server
     * @param params extra parameters to send to the server
     * @param listener the callback method
     */
    public void post(String server, Map<String, String> params, ApiRequestCompleted listener) {
        ApiRestHandler call = new ApiRestHandler(ApiRestHandler.RequestMethod.POST, server, params, null, listener);
        call.execute();
    }

    /**
     * Performs a GET request against the api
     * @param server the api server
     * @param listener the callback method
     */
    public void get(String server, ApiRequestCompleted listener) {
        ApiRestHandler call = new ApiRestHandler(ApiRestHandler.RequestMethod.GET, server, null, null, listener);
        call.execute();
    }
//
//    /**
//     * Performs a get request to download an image from the api
//     * @param command the api command to execute
//     * @param listener the callback method
//     */
//    public void getImage(String command, Map<String, String> params, Context context, ApiRequestCompleted listener) {
//        ApiRestHandler call = new ApiRestHandler(this.baseUrl, params, context, listener);
//        call.execute(command, "image");
//    }
}
