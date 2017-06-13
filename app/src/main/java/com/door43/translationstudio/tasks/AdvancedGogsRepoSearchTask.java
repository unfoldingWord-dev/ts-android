package com.door43.translationstudio.tasks;

import com.door43.translationstudio.App;

import org.unfoldingword.tools.taskmanager.ManagedTask;

import org.unfoldingword.gogsclient.Repository;
import org.unfoldingword.gogsclient.User;

import java.util.ArrayList;
import java.util.List;

/**
 * This task performs a hybrid search on gogs that includes for repositories by username as well as repository name
 */
public class AdvancedGogsRepoSearchTask extends ManagedTask {
    public static final String TASK_ID = "advanced_gogs_search";
    private final User authUser;
    private final String userQuery;
    private final String repoQuery;
    private final int limit;
    private List<Repository> repositories = new ArrayList<>();

    /**
     *
     * @param authUser the user authenticating the request
     * @param userQuery the username query
     * @param repoQuery the repository query
     * @param limit the maximum number of results
     */
    public AdvancedGogsRepoSearchTask(User authUser, String userQuery, String repoQuery, int limit) {

        this.authUser = authUser;
        this.userQuery = userQuery;
        this.limit = limit;

        if(repoQuery.isEmpty()) {
            // wild card
            this.repoQuery = "_";
        } else {

            this.repoQuery = repoQuery;
        }
    }

    @Override
    public void start() {
        if(App.isNetworkAvailable()) {
            // submit new language requests
            delegate(new SubmitNewLanguageRequestsTask());

            // user search or user and repo search
            if((!userQuery.isEmpty() && repoQuery.equals("_"))
                    || (!userQuery.isEmpty() && !repoQuery.equals("_"))) {
                // start by searching user
                SearchGogsUsersTask searchUsersTask = new SearchGogsUsersTask(this.authUser, this.userQuery, this.limit);
                delegate(searchUsersTask);
                List<User> users = searchUsersTask.getUsers();
                for(User user:users) {
                    // search by repo
                    SearchGogsRepositoriesTask searchReposTask = new SearchGogsRepositoriesTask(this.authUser, user.getId(), this.repoQuery, this.limit);
                    delegate(searchReposTask);
                    this.repositories.addAll(searchReposTask.getRepositories());
                }
            // repo search or any search
            } else {
                // just search repos
                SearchGogsRepositoriesTask searchReposTask = new SearchGogsRepositoriesTask(this.authUser, 0, this.repoQuery, this.limit);
                delegate(searchReposTask);
                this.repositories = searchReposTask.getRepositories();
            }
        }
    }

    /**
     * Returns the repositories that were found
     * @return
     */
    public List<Repository> getRepositories() {
        return this.repositories;
    }
}
