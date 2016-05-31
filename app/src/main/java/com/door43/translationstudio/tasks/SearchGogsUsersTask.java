package com.door43.translationstudio.tasks;

import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.util.tasks.ManagedTask;

import org.unfoldingword.gogsclient.GogsAPI;
import org.unfoldingword.gogsclient.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Searches for users in gogs
 */
public class SearchGogsUsersTask extends ManagedTask {
    public static final String TASK_ID = "search_gogs_users";
    private final String query;
    private final User authUser;
    private final int limit;
    private List<User> users = new ArrayList<>();

    public SearchGogsUsersTask(User authUser, String query, int limit) {
        this.query = query == null ? "" : query;
        this.authUser = authUser;
        this.limit = limit;
    }

    @Override
    public void start() {
        if(AppContext.context().isNetworkAvailable()) {
            GogsAPI api = new GogsAPI(AppContext.getUserString(SettingsActivity.KEY_PREF_GOGS_API, R.string.pref_default_gogs_api));
            this.users = api.searchUsers(this.query, this.limit, this.authUser);
        }
    }

    /**
     * Returns a list of users that were found
     * @return
     */
    public List<User> getUsers() {
        return this.users;
    }
}
