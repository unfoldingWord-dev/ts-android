package com.door43.translationstudio.tasks;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.Util;
import com.door43.translationstudio.git.SSHSession;
import com.door43.util.tasks.ManagedTask;
import com.jcraft.jsch.Channel;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Retrieves a list of target translations that have been backed up to the server
 */
public class GetCloudBackupsTask extends ManagedTask {
    public static final String TASK_ID = "get_target_translation_backups";
    private List<String> targetTranslationSlugs = new ArrayList<>();
    public static final String AUTH_FAIL = "Auth fail";
    private boolean mUploadSucceeded = true;
    private boolean mUploadAuthFailure = false;

    @Override
    public void start() {
        String[] userserver = AppContext.getUserString(SettingsActivity.KEY_PREF_GIT_SERVER, R.string.pref_default_git_server).split("@");
        int port = Integer.parseInt(AppContext.getUserString(SettingsActivity.KEY_PREF_GIT_SERVER_PORT, R.string.pref_default_git_server_port));
        try {
            Channel channel = SSHSession.openSession(userserver[0], userserver[1], port);
            OutputStream os = channel.getOutputStream();
            InputStream is = channel.getInputStream();
            os.write(0);
            String response = Util.readStream(is);
            if (response != null) {
                String[] lines = response.split("\n");
                for (String line : lines) {
                    if (line.trim().indexOf("R") == 0 && line.contains(AppContext.udid())) {
                        String[] parts = line.split("/");
                        targetTranslationSlugs.add(parts[parts.length - 1]);
                    }
                }
            }
        } catch (Exception e) {
            Logger.e(this.getClass().getName(), "Failed to retrieve the list of backups on the server", e);
            mUploadSucceeded = false;
            Throwable cause = e.getCause();
            if(cause != null) {
                Throwable subException = cause.getCause();
                if(subException != null) {
                    String detail = subException.getMessage();
                    if (AUTH_FAIL.equals(detail)) {
                        mUploadAuthFailure = true; // we do special handling for auth failure
                    }
                }
            }
        }
    }

    public String[] getTargetTranslationSlugs() {
        return targetTranslationSlugs.toArray(new String[targetTranslationSlugs.size()]);
    }

    public boolean uploadSucceeded() {
        return mUploadSucceeded;
    }

    public boolean uploadAuthFailure() {
        return mUploadAuthFailure;
    }
}
