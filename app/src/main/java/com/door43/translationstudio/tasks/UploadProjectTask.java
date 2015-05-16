package com.door43.translationstudio.tasks;

import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.git.Repo;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.FileUtilities;
import com.door43.util.threads.ManagedTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

/**
 * This task will upload the device public ssh key to the server
 */
public class UploadProjectTask extends ManagedTask {

    private final Project mProject;
    private final Language mLanguage;
    private final String mAuthServer;
    private final int mAuthServerPort;

    public UploadProjectTask(Project p, Language target) {
        mProject = p;
        mLanguage = target;
        mAuthServer = AppContext.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_AUTH_SERVER, AppContext.context().getResources().getString(R.string.pref_default_auth_server));
        mAuthServerPort = Integer.parseInt(AppContext.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_AUTH_SERVER_PORT, AppContext.context().getResources().getString(R.string.pref_default_auth_server_port)));
    }

    @Override
    public void start() {
        if(AppContext.context().isNetworkAvailable()) {
            publishProgress(-1, AppContext.context().getResources().getString(R.string.loading));
            if(!AppContext.context().hasRegisteredKeys()) {
                if(AppContext.context().hasKeys()) {
                    // open tcp connection with server
                    try {
                        InetAddress serverAddr = InetAddress.getByName(mAuthServer);
                        Socket socket = new Socket(serverAddr, mAuthServerPort);
                        PrintWriter out = null;
                        InputStream in = null;
                        try {
                            //send the message to the server
                            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                            in = socket.getInputStream();

                            // submit key
                            JSONObject json = new JSONObject();
                            try {
                                String key = FileUtilities.getStringFromFile(AppContext.context().getPublicKey().getAbsolutePath()).trim();
                                json.put("key", key);
                                json.put("udid", AppContext.udid());
                                // TODO: provide support for using user names
//                              json.put("username", "");
                                out.println(json.toString());

                                // read response
                                boolean messageReceived = false;
                                String serverMessage = null;
                                while(!interrupted() && !messageReceived) {
                                    byte[] buffer = new byte[4096];
                                    int read = 0;
                                    try {
                                        read = in.read(buffer, 0, 4096);
                                    } catch (IOException e) {
                                        processError(e);
                                        break;
                                    }

                                    // receive data
                                    // TODO: right now we just read the first buffer. We should probably create a way to determine how much data has actually been sent. Probably need to have the server specify how many bytes have been sent.
                                    if (read != -1) {
                                        byte[] tempdata = new byte[read];
                                        System.arraycopy(buffer, 0, tempdata, 0, read);
                                        serverMessage = new String(tempdata);
                                    }

                                    // handle response from the server
                                    if (serverMessage != null) {
                                        processResponse(serverMessage);
                                        messageReceived = true;
                                    }
                                    serverMessage = null;
                                }
                            } catch (JSONException e) {
                                processError(e);
                            } catch (Exception e) {
                                processError(e);
                            }
                        } catch (Exception e) {
                            processError(e);
                        } finally {
                            socket.close();

                            // stop everything
                            if(in != null) {
                                try {
                                    in.close();
                                } catch (IOException e) {
                                    processError(e);
                                }
                            }
                            if(out != null) {
                                out.close();
                            }
                        }
                    } catch (Exception e) {
                        processError(e);
                    }
                } else {
                    processError(R.string.error_missing_ssh_keys);
                }
            } else {
                pushProject();
            }
        } else {
            processError(R.string.internet_not_available);
        }

    }

    /**
     * Processes the response from the server
     * @param response
     */
    private void processResponse(String response) {
        try {
            JSONObject json = new JSONObject(response);
            if (json.has("ok")) {
                AppContext.context().setHasRegisteredKeys(true);
                pushProject();
            } else {
                processError(new Exception(json.getString("error")));
            }
        } catch (JSONException e) {
            processError(e);
        }
    }

    /**
     * Uploads pushes the project repository to the server
     */
    private void pushProject() {
        // TODO: we need to push the project and profile repositories
        Repo repo = new Repo(mProject.getRepositoryPath());
        
    }

    private void processError(Exception e) {
        // TODO: log error message for user
    }

    private void processError(int resId) {
        // TODO: log error message for user
    }
}
