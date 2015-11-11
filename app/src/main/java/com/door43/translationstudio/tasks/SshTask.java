package com.door43.translationstudio.tasks;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.util.FileUtilities;
import com.door43.util.tasks.ManagedTask;

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
 * Created by joel on 11/11/2015.
 */
public abstract class SshTask extends ManagedTask {

    private final String server;
    private final int port;

    public SshTask() {
        setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT);
        this.server = AppContext.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_AUTH_SERVER, AppContext.context().getResources().getString(R.string.pref_default_auth_server));
        this.port = Integer.parseInt(AppContext.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_AUTH_SERVER_PORT, AppContext.context().getResources().getString(R.string.pref_default_auth_server_port)));
    }

    @Override
    public void start() {
        if(AppContext.context().isNetworkAvailable()) {
            if(!AppContext.context().hasRegisteredKeys()) {
                // register the keys
                if(AppContext.context().hasKeys()) {
                    // open tcp connection with server
                    publishProgress(-1, AppContext.context().getResources().getString(R.string.submitting_security_keys));
                    registerKeys();
                } else {
                    Logger.w(this.getClass().getName(), "The ssh keys have not been generated");
                    stop();
                }
            } else {
                onRegistered();
            }
        } else {
            Logger.w(this.getClass().getName(), "The task '" + this.getClass().getName() + "' could not be completed becuase the network is not available");
            stop();
        }
    }

    /**
     * Called after the device has been registered with the server
     */
    protected abstract void onRegistered();

    /**
     * Submits the device public ssh key to the server
     */
    private void registerKeys() {
        Logger.i(this.getClass().getName(), "Registering ssh key with " + this.server);
        // open tcp connection with server
        try {
            InetAddress serverAddr = InetAddress.getByName(this.server);
            Socket socket = new Socket(serverAddr, this.port);
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
                            Logger.e(this.getClass().getName(), "Could not read response from server while registering keys", e);
                            stop();
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
                            handleRegistrationResponse(serverMessage);
                            messageReceived = true;
                        }
                        serverMessage = null;
                    }
                } catch (JSONException e) {
                    Logger.e(this.getClass().getName(), "Failed to build key registration request", e);
                    stop();
                }
            } catch (Exception e) {
                Logger.e(this.getClass().getName(), "Failed to submit the key registration request to the server", e);
                stop();
            } finally {
                socket.close();

                // stop everything
                if(in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        Logger.e(this.getClass().getName(), "Failed to close tcp connection with key server", e);
                        stop();
                    }
                }
                if(out != null) {
                    out.close();
                }
            }
        } catch (Exception e) {
            Logger.e(this.getClass().getName(), "Failed to open a tcp connection with the key server", e);
            stop();
        }
    }

    /**
     * Handles the server's response
     * @param serverMessage
     */
    private void handleRegistrationResponse(String serverMessage) {
        try {
            JSONObject json = new JSONObject(serverMessage);
            if (json.has("ok")) {
                AppContext.context().setHasRegisteredKeys(true);
                onRegistered();
            } else {
                Logger.e(this.getClass().getName(), "Key registration was refused", new Exception(json.getString("error")));
                stop();
            }
        } catch (JSONException e) {
            Logger.e(this.getClass().getName(), "Failed to parse response from keys server", e);
            stop();
        }
    }
}
