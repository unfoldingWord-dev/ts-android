package com.door43.translationstudio.tasks;

import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.Profile;
import com.door43.translationstudio.core.TargetTranslation;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import org.unfoldingword.gogsclient.GogsAPI;
import org.unfoldingword.gogsclient.Repository;
import org.unfoldingword.gogsclient.Response;

/**
 * Created by joel on 4/18/16.
 */
public class CreateRepositoryTask extends ManagedTask {
    public static final String TASK_ID = "create_repo_task";
    private final TargetTranslation targetTranslation;
    private boolean success = false;

    public CreateRepositoryTask(TargetTranslation targetTranslation) {
        this.targetTranslation = targetTranslation;
    }

    @Override
    public void start() {

        if(App.isNetworkAvailable()) {
            publishProgress(-1, "Preparing location on server");
            GogsAPI api = new GogsAPI(App.getUserString(SettingsActivity.KEY_PREF_GOGS_API, R.string.pref_default_gogs_api));
            Profile profile = App.getProfile();
            if(profile != null && profile.gogsUser != null) {
                Repository templateRepo = new Repository(targetTranslation.getId(), "", false);
                Repository repo = api.createRepo(templateRepo, profile.gogsUser);
                if(repo != null) {
                    success = true;
                } else {
                    Response response = api.getLastResponse();
                    Logger.w(this.getClass().getName(), "Failed to create repository " + targetTranslation.getId() + ". Gogs responded with " + response.code + ": " + response.data, response.exception);
                }
            }
        }
    }

    public boolean isSuccess() {
        return success;
    }
}
