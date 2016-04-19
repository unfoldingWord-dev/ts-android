package com.door43.translationstudio.tasks;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.util.FileUtilities;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by blm on 2/16/16.
 */
@Deprecated
public class KeyRegistration {
    private boolean mUploadSucceeded = false;
    private String mAuthServer;
    private int mAuthServerPort;
    private OnRegistrationFinishedListener mListener = null;
    static private String TAG = KeyRegistration.class.getSimpleName();

    public void registerKeys(OnRegistrationFinishedListener listener) {
        mListener = listener;
        mUploadSucceeded = true;
        mAuthServer = AppContext.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_AUTH_SERVER, AppContext.context().getResources().getString(R.string.pref_default_auth_server));
        mAuthServerPort = Integer.parseInt(AppContext.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_AUTH_SERVER_PORT, AppContext.context().getResources().getString(R.string.pref_default_auth_server_port)));

        Logger.i(TAG, "Registering ssh key with " + mAuthServer);
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
                    String key = FileUtilities.readFileToString(new File(AppContext.context().getPublicKey().getAbsolutePath())).trim();
                    json.put("key", key);
                    json.put("udid", AppContext.udid());
                    // TODO: provide support for using user names
//                              json.put("username", "");
                    out.println(json.toString());

                    // read response
                    boolean messageReceived = false;
                    String serverMessage = null;
                    while(!messageReceived) {
                        byte[] buffer = new byte[4096];
                        int read = 0;
                        try {
                            read = in.read(buffer, 0, 4096);
                        } catch (IOException e) {
                            Logger.e(TAG, "Could not read response from server while registering keys", e);
                            mUploadSucceeded = false;
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
                    Logger.e(TAG, "Failed to build key registration request", e);
                    mUploadSucceeded = false;
                }
            } catch (Exception e) {
                Logger.e(TAG, "Failed to submit the key registration request to the server", e);
                mUploadSucceeded = false;
            } finally {
                socket.close();

                // stop everything
                if(in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        Logger.e(TAG, "Failed to close tcp connection with key server", e);
                        mUploadSucceeded = false;
                    }
                }
                if(out != null) {
                    out.close();
                }
            }
        } catch (Exception e) {
            Logger.e(TAG, "Failed to open a tcp connection with the key server", e);
            mUploadSucceeded = false;
        }

        if(!mUploadSucceeded) {  // report error to listener
            if(mListener != null) {
                mListener.onRestoreFinish(mUploadSucceeded);
            }
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
            } else {
                Logger.e(TAG, "Key registration was refused", new Exception(json.getString("error")));
                mUploadSucceeded = false;
            }
        } catch (JSONException e) {
            Logger.e(TAG, "Failed to parse response from keys server", e);
            mUploadSucceeded = false;
        }

        if(mListener != null) {
            mListener.onRestoreFinish(mUploadSucceeded);
        }
    }

    /**
     * callback interface for when undo/redo operation is completed
     */
    public interface OnRegistrationFinishedListener {
        /**
         * Called when a view has been clicked.
         */
        void onRestoreFinish(boolean registrationSuccess);
    }
}
