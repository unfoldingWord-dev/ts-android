package com.door43.translationstudio.tasks;

import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.Profile;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import org.unfoldingword.gogsclient.GogsAPI;
import org.unfoldingword.gogsclient.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * Retrieves a list of target translations that have been backed up to the server
 */
public class GetUserRepositoriesTask extends ManagedTask {
    public static final String TASK_ID = "get_target_translation_backups";
    private List<Repository> repositories = new ArrayList<>();

    @Override
    public void start() {
        if(AppContext.context().isNetworkAvailable()) {
            GogsAPI api = new GogsAPI(AppContext.getUserString(SettingsActivity.KEY_PREF_GOGS_API, R.string.pref_default_gogs_api));
            Profile profile = AppContext.getProfile();

            if(profile != null && profile.gogsUser != null) {
                this.repositories = api.listRepos(profile.gogsUser);
            }
        }
    }

    public List<Repository> getRepositories() {
        return repositories;
    }
}
