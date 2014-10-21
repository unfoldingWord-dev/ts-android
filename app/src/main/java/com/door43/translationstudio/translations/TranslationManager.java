package com.door43.translationstudio.translations;

import android.util.Log;

import com.door43.delegate.DelegateSender;
import com.door43.translationstudio.git.Repo;
import com.door43.translationstudio.git.tasks.repo.AddTask;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.FileUtilities;
import com.door43.translationstudio.util.TCPClient;
import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.git.tasks.ProgressCallback;
import com.door43.translationstudio.git.tasks.repo.PushTask;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class handles the storage of translated content.
 */
public class TranslationManager extends DelegateSender implements TCPClient.TcpListener {
    private TranslationManager me = this;
    private MainApplication mContext;
    private final String TAG = "TranslationManager";
    private final String mParentProjectSlug = "uw"; //  NOTE: not sure if this will ever need to be dynamic
    private TCPClient mTcpClient;

    public TranslationManager(MainApplication context) {
        mContext = context;
    }

    /**
     * Initiates a git sync with the server. This will forcebly push all local changes to the server
     * and discard any discrepencies.
     */
    public void syncSelectedProject() {
        if(!mContext.hasRegisteredKeys()) {
            mContext.showProgressDialog("Establishing a connection...");
            // set up a tcp connection
            if(mTcpClient == null) {
                mTcpClient = new TCPClient(mContext.getUserPreferences().getString(SettingsActivity.KEY_PREF_AUTH_SERVER, mContext.getResources().getString(R.string.pref_default_auth_server)), Integer.parseInt(mContext.getUserPreferences().getString(SettingsActivity.KEY_PREF_AUTH_SERVER_PORT, mContext.getResources().getString(R.string.pref_default_auth_server_port))), me);
            } else {
                // TODO: update the sever and port if they have changed... Not sure if this task is applicable now
            }
            // connect to the server so we can submit our key
            mTcpClient.connect();
        } else {
            pushSelectedProjectRepo();
        }
    }

    /**
     * Pushes the currently selected project+language repo to the server
     */
    private void pushSelectedProjectRepo() {
        Project p = mContext.getSharedProjectManager().getSelectedProject();

        final String remotePath = getRemotePath(p, p.getSelectedTargetLanguage());
        final Repo repo = new Repo(p.getRepositoryPath());

        AddTask add = new AddTask(repo, ".", new AddTask.OnAddComplete() {
            @Override
            public void success() {
                PushTask push = new PushTask(repo, remotePath, true, true, new ProgressCallback(R.string.push_msg_init));
                push.executeTask();
                // TODO: we need to check the errors from the push task. If auth fails then we need to re-register.
            }

            @Override
            public void error() {
                mContext.showToastMessage("Changes could not be staged.");
            }
        });
        add.executeTask();
    }

    /**
     * Generates the remote path for a local repo
     * @param project
     * @param lang
     * @return
     */
    private String getRemotePath(Project project, Language lang) {
        String server = mContext.getUserPreferences().getString(SettingsActivity.KEY_PREF_GIT_SERVER, mContext.getResources().getString(R.string.pref_default_git_server));
        return server + ":tS/" + mContext.getUDID() + "/" + mParentProjectSlug + "-" + project.getId() + "-" + lang.getId();
    }

    /**
     * Submits the client public ssh key to the server so we can push updates
     */
    private void registerSSHKeys() {
        if(mContext.hasKeys()) {
            mContext.showProgressDialog("Loading security keys...");
            JSONObject json = new JSONObject();
            try {
                String key = FileUtilities.getStringFromFile(mContext.getPublicKey().getAbsolutePath()).trim();
                json.put("key", key);
                json.put("udid", mContext.getUDID());
                // TODO: provide support for using user names
//                json.put("username", "");
                Log.d(TAG, json.toString());
                mContext.showProgressDialog("Transferring security keys...");
                mTcpClient.sendMessage(json.toString());
            } catch (JSONException e) {
                mContext.showException(e);
            } catch (Exception e) {
                mContext.showException(e);
            }
        } else {
            mContext.closeProgressDialog();
            mContext.showException(new Throwable("The ssh keys have not been generated."));
        }
    }

    @Override
    public void onConnectionEstablished() {
        // submit key to the server
        registerSSHKeys();
    }

    @Override
    public void onMessageReceived(String message) {
        // check if we get an ok message
        mContext.closeProgressDialog();
        try {
            JSONObject json = new JSONObject(message);
            if(json.has("ok")) {
                mContext.setHasRegisteredKeys(true);
                me.issueDelegateResponse(new TranslationSyncResponse(true));
            } else {
                mContext.showException(new Throwable(json.getString("error")));
            }
        } catch (JSONException e) {
            mContext.showException(e);
        }
        mTcpClient.stop();
    }

    @Override
    public void onError(Throwable t) {
        mContext.showException(t);
    }
}
