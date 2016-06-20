package com.door43.translationstudio.tasks;

import org.unfoldingword.tools.logger.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import org.unfoldingword.gogsclient.GogsAPI;
import org.unfoldingword.gogsclient.Response;
import org.unfoldingword.gogsclient.Token;
import org.unfoldingword.gogsclient.User;

import java.util.List;

/**
 * Created by joel on 4/15/16.
 */
public class LoginDoor43Task extends ManagedTask {
    public static final String TASK_ID = "login_door43";
    private final String password;
    private final String username;
    private final String tokenName;
    private final String fullName;
    private User user = null;

    /**
     * Logs into a door43 account
     * @param username
     * @param password
     */
    public LoginDoor43Task(String username, String password) {
        this.username = username;
        this.password = password;
        this.tokenName = AppContext.context().getResources().getString(R.string.gogs_token_name);
        this.fullName = null;
    }

    /**
     * Logs into a door43 account. If no full_name exists on the account the provided one will be added
     * @param username
     * @param password
     * @param fullName the name to add to the account if one does not areadly exist
     */
    public LoginDoor43Task(String username, String password, String fullName) {
        this.username = username;
        this.password = password;
        this.tokenName = AppContext.context().getResources().getString(R.string.gogs_token_name);
        this.fullName = fullName;
    }


    @Override
    public void start() {
        GogsAPI api = new GogsAPI(AppContext.getUserString(SettingsActivity.KEY_PREF_GOGS_API, R.string.pref_default_gogs_api));
        User authUser = new User(this.username, this.password);

        // get user
        this.user = api.getUser(authUser, authUser);
        if(this.user != null) {

            // retrieve existing token
            List<Token> tokens = api.listTokens(authUser);
            for (Token t : tokens) {
                if (t.getName().equals(tokenName)) {
                    this.user.token = t;
                    break;
                }
            }

            // create new token
            if (this.user.token == null) {
                Token t = new Token(tokenName);
                this.user.token = api.createToken(t, authUser);
            }

            // validate login
            if (this.user.token == null) {
                this.user = null;
                Response response = api.getLastResponse();
                Logger.w(LoginDoor43Task.class.getName(), "gogs api responded with " + response.code + ": " + response.toString(), response.exception);
                return;
            }

            // set missing full_name
            if(this.user.fullName == null || this.user.fullName.isEmpty()
                    && (this.fullName != null && !this.fullName.isEmpty())) {
                this.user.fullName = this.fullName;
                User updatedUser = api.editUser(this.user, authUser);
                if(updatedUser == null) {
                    Response response = api.getLastResponse();
                    Logger.w(LoginDoor43Task.class.getName(), "The full_name could not be updated gogs api responded with " + response.code + ": " + response.toString(), response.exception);
                }
            }
        }
    }

    /**
     * Returns the logged in user
     * @return
     */
    public User getUser() {
        return user;
    }
}
