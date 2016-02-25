package com.door43.gogs;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

/**
 * The main gogs api controller
 */
public class GogsAPI {

    private int readTimeout = 5000;
    private int connectionTimeout = 5000;
    private final String baseUrl;
    private Response lastResponse = null;

    public GogsAPI(String baseUrl) {
        this.baseUrl = baseUrl.replaceAll("/+$", "/");
    }

    /**
     * Returns the last reponse
     * @return
     */
    public Response getLastResponse() {
        return this.lastResponse;
    }

    /**
     * Changes the read timeout
     * @param timeout
     */
    public void setReadTimeout(int timeout) {
        this.readTimeout = timeout;
    }

    /**
     * Sets the connection timeout
     * @param timeout
     */
    public void setConnectionTimeout(int timeout) {
        this.connectionTimeout = timeout;
    }

    /**
     * Performs a get request against the api
     * @param partialUrl
     */
    private Response get(String partialUrl, User user) {
        Response response = null;
        try {
            URL url = new URL(this.baseUrl + partialUrl.replaceAll("^/+", ""));
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            if(user != null) {
                String auth = encodeUserAuth(user);
                if(auth != null) {
                    conn.addRequestProperty("Authorization", auth);
                }
            }
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setReadTimeout(this.readTimeout);
            conn.setConnectTimeout(this.connectionTimeout);
            InputStream is = conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int current;
            while ((current = bis.read()) != -1) {
                baos.write((byte) current);
            }
            String data = baos.toString("UTF-8");
            response = new Response(conn.getResponseCode(), data);
            this.lastResponse = response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    /**
     * Performs a post request against the api
     * @param partialUrl
     * @param user
     * @param data
     * @return
     */
    private Response post(String partialUrl, User user, String data) {
        Response response = null;
        try {
            URL url = new URL(this.baseUrl + partialUrl.replaceAll("^/+", ""));
            HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
            if(user != null) {
                String auth = encodeUserAuth(user);
                if(auth != null) {
                    conn.addRequestProperty("Authorization", auth);
                }
            }
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setReadTimeout(this.readTimeout);
            conn.setConnectTimeout(this.connectionTimeout);

            // post
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
            dos.writeBytes(data);
            dos.flush();
            dos.close();

            InputStream is = conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int current;
            while ((current = bis.read()) != -1) {
                baos.write((byte) current);
            }
            String responseData = baos.toString("UTF-8");
            response = new Response(conn.getResponseCode(), responseData);
            this.lastResponse = response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    /**
     * Generates the authentication parameter for the user
     * @param user
     * @return
     */
    private String encodeUserAuth(User user) {
        if(user != null) {
            if(user.getToken() != null) {
                return "token " + user.getToken();
            } else if(!user.getUsername().isEmpty() && !user.getPassword().isEmpty()) {
                String credentials = user.getUsername() + ":" + user.getPassword();
                try {
                    return "Basic " + Base64.encodeToString(credentials.getBytes("UTF-8"), Base64.NO_WRAP);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * Creates a new user
     * @param user
     * @return
     */
    public User createUser(User user) {
        return null;
    }

    /**
     * Retrieves a user
     * @return
     */
    public User getUser() {
        return null;
    }

    /**
     * Deletes a user
     * @param user
     */
    public void deleteUser(User user) {

    }

    /**
     * Searches for public repositories that match the query
     * @param query
     * @param uid user whose repositories will be searched. 0 will search all
     * @param limit limit results to this quantity.
     * @return
     */
    public List<Repository> searchRepos(String query, int uid, int limit) {
        List<Repository> repos = new ArrayList<>();
        if(query != null && !query.trim().isEmpty()) {
            Response response = get(String.format("/repos/search?q=%s&uid=%d&limit=%d", query.trim(), uid, limit), null);
            if(response != null) {
                try {
                    JSONObject json = new JSONObject(response.getData());
                    JSONArray data = json.getJSONArray("data");
                    for(int i = 0; i < data.length(); i ++) {
                        Repository repo = Repository.parse(data.getJSONObject(i));
                        if(repo != null) {
                            repos.add(repo);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return repos;
    }

    /**
     * Lists all repositories that are accessible to the user
     * @param user
     * @return
     */
    public List<Repository> listRepos(User user) {
        List<Repository> repos = new ArrayList<>();
        if(user != null) {
            Response response = get("/user/repos", user);
            if(response != null) {
                try {
                    JSONArray data = new JSONArray(response.getData());
                    for(int i = 0; i < data.length(); i ++) {
                        Repository repo = Repository.parse(data.getJSONObject(i));
                        if(repo != null) {
                            repos.add(repo);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return repos;
    }

    /**
     * Creates a new repository for the user
     * @param repo
     * @param user
     * @return
     */
    public Repository createRepo(Repository repo, User user) {
        if(repo != null && user != null) {
            JSONObject json = new JSONObject();
            try {
                json.put("name", repo.getName());
                json.put("description", repo.getDescription());
                json.put("private", repo.getIsPrivate());
                Response response = post("/user/repos", user, json.toString());
                if(response != null) {
                    return Repository.parse(new JSONObject(response.getData()));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Returns a lsit of tokens the user has
     * @param user
     * @return
     */
    public List<Token> listTokens(User user) {
        List<Token> tokens = new ArrayList<>();
        if(user != null) {
            Response response = get(String.format("/users/%s/tokens", user.getUsername()), user);
            if(response != null) {
                try {
                    JSONArray data = new JSONArray(response.getData());
                    for(int i = 0; i < data.length(); i ++) {
                        Token token = Token.parse(data.getJSONObject(i));
                        if(token != null) {
                            tokens.add(token);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return tokens;
    }
}
