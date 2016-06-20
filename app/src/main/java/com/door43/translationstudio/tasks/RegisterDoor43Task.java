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

/**
 * Created by joel on 4/15/16.
 */
public class RegisterDoor43Task extends ManagedTask {
    public static final String TASK_ID = "register_door43_account";
    private final String tokenName;
    private final String username;
    private final String password;
    private final String fullName;
    private final String email;
    private User user = null;
    private String error;

    public RegisterDoor43Task(String username, String password, String fullName, String email) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.tokenName = AppContext.context().getResources().getString(R.string.gogs_token_name);
    }

    @Override
    public void start() {
        int gogsAdminTokenIdentifier = AppContext.context().getResources().getIdentifier("gogs_admin_token", "string", AppContext.context().getPackageName());
        if(gogsAdminTokenIdentifier != 0) {
            GogsAPI api = new GogsAPI(AppContext.getUserString(SettingsActivity.KEY_PREF_GOGS_API, R.string.pref_default_gogs_api));

            // build admin user
            String tokenSha1 = AppContext.context().getResources().getString(gogsAdminTokenIdentifier);
            User authUser = new User("", "");
            authUser.token = new Token("", tokenSha1);

            // create user
            User userTemplate = new User(this.username, this.password);
            userTemplate.fullName = this.fullName;
            userTemplate.email = this.email;
            this.user = api.createUser(userTemplate, authUser, true);

            if(this.user == null) {
                Response response = api.getLastResponse();

                // check if username already exists
                User existingUser = api.getUser(userTemplate, null);
                if(existingUser != null) {
                    this.error = AppContext.context().getResources().getString(R.string.gogs_username_already_exists);
                }

                Logger.w(LoginDoor43Task.class.getName(), "gogs api responded with " + response.code + ": " + response.toString(), response.exception);
                return;
            }

            // create new token
            Token t = new Token(tokenName);
            this.user.token = api.createToken(t, userTemplate);

            // validate login
            if (this.user.token == null) {
                this.user = null;
                this.error = "";
                Response response = api.getLastResponse();
                Logger.w(LoginDoor43Task.class.getName(), "gogs api responded with " + response.code + ": " + response.toString(), response.exception);
            }
        } else {
            Logger.w(this.getClass().getName(), "The gogs admin token is missing");
        }
    }

    /**
     * Returns the newly registered user
     * @return
     */
    public User getUser() {
        return user;
    }

    /**
     * Returns the last error the user gave if an error occured
     * @return
     */
    public String getError() {
        return error;
    }
}
