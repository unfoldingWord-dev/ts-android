package com.door43.translationstudio.tasks;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.Profile;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import org.unfoldingword.gogsclient.GogsAPI;
import org.unfoldingword.gogsclient.Repository;
import org.unfoldingword.gogsclient.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Searches for repositories in gogs
 */
public class SearchGogsRepositoriesTask extends ManagedTask {
    public static final String TASK_ID = "search_gogs_repositories";
    private final String query;
    private final int uid;
    private final User authUser;
    private final int limit;
    private List<Repository> repositories = new ArrayList<>();

    public SearchGogsRepositoriesTask(User authUser, int uid, String query, int limit) {
        this.query = query == null ? "" : query;
        this.uid = uid;
        this.authUser = authUser;
        this.limit = limit;
    }

    @Override
    public void start() {
        if(App.isNetworkAvailable()) {
            GogsAPI api = new GogsAPI(App.getUserString(SettingsActivity.KEY_PREF_GOGS_API, R.string.pref_default_gogs_api));
            Profile profile = App.getProfile();

            if(profile != null && profile.gogsUser != null) {
                List<Repository> repos = api.searchRepos(this.query, this.uid, this.limit);
                // fetch additional information about the repos (clone urls)
                for(Repository repo:repos) {
                    repo = api.getRepo(repo, this.authUser);
                    if(repo != null) {
                        this.repositories.add(repo);
                    }
                }
            }
        }
    }

    public List<Repository> getRepositories() {
        return this.repositories;
    }
}
