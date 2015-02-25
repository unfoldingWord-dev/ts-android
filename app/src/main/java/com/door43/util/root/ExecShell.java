package com.door43.util.root;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

/**
 * This class suppliments Root to determine if the device is rooted
 * @author Kevin Kowalewski
 * http://stackoverflow.com/questions/1101380/determine-if-running-on-a-rooted-device
 */
public class ExecShell {

    private static String LOG_TAG = ExecShell.class.getName();

    public static enum SHELL_CMD {
        check_su_binary(new String[] { "/system/xbin/which", "su" });

        String[] command;

        SHELL_CMD(String[] command) {
            this.command = command;
        }
    }

    public ArrayList<String> executeCommand(SHELL_CMD shellCmd) {
        String line = null;
        ArrayList<String> fullResponse = new ArrayList<String>();
        Process localProcess = null;
        try {
            localProcess = Runtime.getRuntime().exec(shellCmd.command);
        } catch (Exception e) {
            return null;
        }
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                localProcess.getOutputStream()));
        BufferedReader in = new BufferedReader(new InputStreamReader(
                localProcess.getInputStream()));
        try {
            while ((line = in.readLine()) != null) {
                Log.d(LOG_TAG, "--> Line received: " + line);
                fullResponse.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(LOG_TAG, "--> Full response was: " + fullResponse);
        return fullResponse;
    }
}