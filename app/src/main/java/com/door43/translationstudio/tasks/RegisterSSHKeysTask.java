package com.door43.translationstudio.tasks;

import android.os.Process;

import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.Profile;
import com.door43.util.FileUtilities;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import org.unfoldingword.gogsclient.GogsAPI;
import org.unfoldingword.gogsclient.PublicKey;
import org.unfoldingword.gogsclient.Response;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by joel on 4/18/16.
 */
public class RegisterSSHKeysTask extends ManagedTask {

    public static final String TASK_ID = "register_ssh_public_key_task";
    private final boolean force;
    private final String keyName;
    private boolean success = false;

    /**
     *
     * @param force indicates if existing ssh keys should be regenerated
     */
    public RegisterSSHKeysTask(boolean force) {
        setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
        this.force = force;
        this.keyName = App.context().getResources().getString(R.string.gogs_public_key_name) + " " + App.udid();
    }

    @Override
    public void start() {
        if(App.isNetworkAvailable()) {
            publishProgress(-1, "Authenticating");
            GogsAPI api = new GogsAPI(App.getUserString(SettingsActivity.KEY_PREF_GOGS_API, R.string.pref_default_gogs_api));
            Profile profile = App.getProfile();
            if (profile != null && profile.gogsUser != null) {
                if (!App.hasSSHKeys() || this.force) {
                    App.generateSSHKeys();
                }
                String keyString = null;
                try {
                    keyString = FileUtilities.readFileToString(new File(App.getPublicKey().getAbsolutePath())).trim();
                } catch (IOException e) {
                    e.printStackTrace();
                    Logger.e(this.getClass().getName(), "Failed to retreive the public key", e);
                    return;
                }

                PublicKey keyTemplate = new PublicKey(keyName, keyString);
                // delete old key
                List<PublicKey> keys = api.listPublicKeys(profile.gogsUser);
                for(PublicKey k:keys) {
                    if(k.getTitle().equals(keyTemplate.getTitle())) {
                        api.deletePublicKey(k, profile.gogsUser);
                        break;
                    }
                }
                // create new key
                PublicKey key = api.createPublicKey(keyTemplate, profile.gogsUser);
                if (key != null) {
                    success = true;
                } else {
                    Response response = api.getLastResponse();
                    Logger.w(this.getClass().getName(), "Failed to register the public key. Gogs responded with " + response.code + ": " + response.data, response.exception);
                }
            }
        }
    }

    public boolean isSuccess() {
        return success;
    }
}
