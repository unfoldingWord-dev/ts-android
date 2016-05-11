package com.door43.translationstudio.tasks;

import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.Profile;
import com.door43.util.tasks.ManagedTask;

import org.unfoldingword.gogsclient.GogsAPI;
import org.unfoldingword.gogsclient.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 5/10/16.
 */
public class SearchGogsRepositoriesTask extends ManagedTask {
    public static final String TASK_ID = "search_repositories";
    private final String query;
    private final int uid;
    private List<Repository> repositories = new ArrayList<>();

    public SearchGogsRepositoriesTask(int uid, String query) {
        this.query = query == null ? "" : query;
        this.uid = uid;
    }

    @Override
    public void start() {
        if(AppContext.context().isNetworkAvailable()) {
            GogsAPI api = new GogsAPI(AppContext.getUserString(SettingsActivity.KEY_PREF_GOGS_API, R.string.pref_default_gogs_api));
            Profile profile = AppContext.getProfile();

            if(profile != null && profile.gogsUser != null) {
                List<Repository> repos = api.searchRepos(query, uid, 20);
                // fetch additional information about the repos (clone urls)
                for(Repository repo:repos) {
                    repo = api.getRepo(repo, profile.gogsUser);
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
