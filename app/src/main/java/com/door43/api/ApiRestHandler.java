package com.door43.api;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by joel on 7/8/2014.
 */
public class ApiRestHandler extends AsyncTask<String, String, String> {
    private ApiRequestCompleted listener;
    private String result;
    private String baseUrl;
    private Map<String, String> params;
    private Context context;

    public ApiRestHandler(String url, Map<String, String> params, Context context, ApiRequestCompleted listener) {
        this.listener = listener;
        this.baseUrl = url;
        this.params = params;
        this.context = context;
    }

    @Override
    protected String doInBackground(String... params) {
        if (params.length >= 2) {
            if (params[1].compareTo("post") == 0) {
                return HTTPpost(params[0]);
            } else if(params[1].compareTo("get") == 0) {
                return HTTPget(params[0]);
            } else if(params[1].compareTo("image") == 0) {
                return HTTPgetImage(params[0]);
            }
        }
        return "";
    }

    private String HTTPpost(String apiCommand) {
        StringBuilder sb = new StringBuilder();
        String data = null;
        try
        {
            // prepare post data
            Iterator it = this.params.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry)it.next();
                data += "&" + URLEncoder.encode(pairs.getKey().toString(), "UTF-8") + "=" + URLEncoder.encode(pairs.getValue().toString(), "UTF-8");
            }
            data = data.substring(1);

            // prepare connection
            URL url = new URL(getRealUrl(apiCommand));
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write( data );
            wr.flush();

            // Get the server response
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = null;
            while((line = in.readLine()) != null)
            {
                sb.append(line);
            }
            in.close();
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
            return e.getMessage();
        }
        this.result = sb.toString();
        return "";
    }

    private String HTTPget(String apiCommand) {
        StringBuilder sb = new StringBuilder();
        String arguments = null;
        try {
            // prepare argument data
            Iterator it = this.params.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry)it.next();
                if(arguments == null) {
                    arguments = "?";
                } else {
                    arguments += "&";
                }
                arguments += URLEncoder.encode(pairs.getKey().toString(), "UTF-8") + "=" + URLEncoder.encode(pairs.getValue().toString(), "UTF-8");
            }

            URL url = new URL(getRealUrl(apiCommand+arguments));
            Log.d("Api", "get request: "+url.toString());
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = null;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            in.close();
        } catch (Exception e ) {
            System.out.println(e.getMessage());
            return e.getMessage();
        }
        this.result = sb.toString();
        return "";
    }

    private String HTTPgetImage(String apiCommand) {
        Bitmap bitmap = null;
        String arguments = null;
        try {
            // prepare argument data
            Iterator it = this.params.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry)it.next();
                if(arguments == null) {
                    arguments = "?";
                } else {
                    arguments += "&";
                }
                arguments += URLEncoder.encode(pairs.getKey().toString(), "UTF-8") + "=" + URLEncoder.encode(pairs.getValue().toString(), "UTF-8");
            }

            URL url = new URL(getRealUrl(apiCommand+""+arguments));
            Log.d("Api", "get image request: "+url.toString());
//            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//            connection.setDoInput(true);
//            connection.connect();
//            InputStream input = connection.getInputStream();
//            bitmap = BitmapFactory.decodeStream((InputStream)url.getContent());
            bitmap = BitmapFactory.decodeStream(url.openStream());
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return e.getMessage();
        }

        if(bitmap != null) {
            // save image to cache
            FileOutputStream fos = null;
            File image = new File(context.getCacheDir(), new SimpleDateFormat("ddMMyyy_HHmmssSSZ").format(new Date()));
            String fileError = null;
            try {
                fos = new FileOutputStream(image);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
                bos.flush();
                bos.close();
                bitmap.recycle();
            } catch (IOException e) {
                fileError =  e.getMessage();
            } finally {
                try {
                    if(fos != null) {
                        fos.close();
                    }
                } catch (IOException e) {
                    // TODO: will this result in a corrupt file?
                }
            }
            if(fileError != null) return fileError;

            // prepare response
            JSONObject json = new JSONObject();
            JSONObject values = new JSONObject();
            try {
                values.put("path", image.getAbsolutePath());
                json.put("v", "0");
                json.put("t", "0");
                json.put("ok", values);
            } catch (JSONException e) {
                return e.getMessage();
            }
            this.result = json.toString();
            return "";
        } else {
            // try to decode as a normal get request
            return this.HTTPget(apiCommand);
        }
    }

    /**
     * Returns the full api url
     * @param url the api command to execute
     * @return
     */
    private String getRealUrl(String url) {
        return this.baseUrl+url;
    }

    @Override
    protected void onPostExecute(String string) {
        if(this.result != null) {
            try {
                JSONObject json = new JSONObject(this.result);
                String version = json.get("v").toString();
                String timestamp = json.get("t").toString();
                ApiResponse response;
                if(json.has("ok")) {
                    response = new ApiResponse(version, Double.parseDouble(timestamp), json.get("ok").toString());
                } else if(json.has("error")) {
                    JSONObject error = json.getJSONObject("error");
                    response = new ApiResponse(error.get("type").toString(), error.get("message").toString());
                } else {
                    response = new ApiResponse("MalformedResponse", "Could not determine if the response was an error or success message");
                }
                listener.onRequestCompleted(response);
            } catch(JSONException e) {
                listener.onRequestCompleted(ApiResponse.generateError(e.getCause().toString(), e.getMessage()));
            }
        } else {
            listener.onRequestCompleted(ApiResponse.generateError("BadResponseError", string));
        }
    }
}