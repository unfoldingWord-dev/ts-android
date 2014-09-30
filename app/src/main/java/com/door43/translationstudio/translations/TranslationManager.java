package com.door43.translationstudio.translations;

import android.util.Log;

import com.door43.delegate.DelegateSender;
import com.door43.translationstudio.util.TCPClient;
import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.translations.tasks.ProgressCallback;
import com.door43.translationstudio.translations.tasks.repo.AddTask;
import com.door43.translationstudio.translations.tasks.repo.PushTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

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
     * Saves the translation into a folder structure based on the project, chapter, frame, and language.
     * Note: we don't explicitly pass in the chapter id because it is combined with the frame id.
     * Saving is cheap and should be performed often.
     * @param translation the translated text
     */
    public void save(String translation, String projectSlug, String langCode, String chapterFrameId) {
        String repoPath = buildRepositoryFilePath(projectSlug, langCode);
        String relativeFilePath = buildLocalTranslationFilePath(chapterFrameId);

        // init the repository
        Repo repo = new Repo(repoPath);

        if(relativeFilePath != null) {
            // write the file
            File file = new File(repoPath + "/" + relativeFilePath);

            // create new folder structure
            if(!file.exists()) {
                file.getParentFile().mkdir();
            }

            try {
                file.createNewFile();
                PrintStream ps = new PrintStream(file);
                ps.print(translation);
                ps.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

            // add to repo
            // TODO: we should probably only stage changes just before pushes and before the app closes.
            AddTask add = new AddTask(repo, file.getAbsolutePath());
            add.executeTask();
        } else {
            mContext.showToastMessage(R.string.error_can_not_save_file);
        }
    }

    /**
     * Initiates sharing with nearby devices. Or a simple file export that can be emailed, shared over external storage, etc.
     */
    public void share() {
        mContext.showToastMessage("Sharing hasn't been set up yet!");
    }

    /**
     * Initiates a git sync with the server. This will forcebly push all local changes to the server
     * and discard any discrepencies.
     */
    public void sync() {
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
            pushRepos();
        }
    }

    /**
     * Pushes the local repositories to the server
     */
    private void pushRepos() {
        // push the local repositories to the server
        // TODO: need to push all repositories or allow the user to choose which projects to push. Might be good to only push the selected project and language.
        String repoPath = buildRepositoryFilePath("obs", "en");
        String remotePath = buildRemotePath(mContext.getUserPreferences().getString(SettingsActivity.KEY_PREF_GIT_SERVER, mContext.getResources().getString(R.string.pref_default_git_server)), "obs", "en");
        Repo repo = new Repo(repoPath);
        PushTask push = new PushTask(repo, remotePath, true, true, new ProgressCallback(R.string.push_msg_init));
        push.executeTask();

        // TODO: we need to check the errors from the push task. If auth fails then we need to re-register.
    }

    /**
     * Generates the remote path for a local repo
     * @param server
     * @param projectSlug
     * @param langCode
     * @return
     */
    private String buildRemotePath(String server, String projectSlug, String langCode) {
        return server + ":tS/" + mContext.getUDID() + "/" + mParentProjectSlug + "-" + projectSlug + "-" + langCode;
    }

    /**
     * Submits the client public ssh key to the server so we can push updates
     */
    private void register() {
        if(mContext.hasKeys()) {
            mContext.showProgressDialog("Transferring security keys...");
            JSONObject json = new JSONObject();
            try {
                String key = getStringFromFile(mContext.getPublicKey().getAbsolutePath()).trim();
                json.put("key", key);
                json.put("udid", mContext.getUDID());
                // TODO: provide support for using user names
//                json.put("username", "");
                Log.d(TAG, json.toString());
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

    /**
     * Returns the translated content for a given frame
     * @param projectSlug the project in which the translation exists
     * @param langCode the language code
     * @param chapterFrameId the chapter and frame in which the translation exists e.g. 01-02 is chapter 1 frame 2.
     * @return
     */
    public String getTranslation(String projectSlug, String langCode, String chapterFrameId) {
        String repoPath = buildRepositoryFilePath(projectSlug, langCode);
        String filePath = buildLocalTranslationFilePath(chapterFrameId);

        if(filePath != null) {
            String path = repoPath + "/" + filePath;
            try {
                return getStringFromFile(path);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } else {
            Log.w(TAG, "The translation file path could not be determined");
            return null;
        }
    }

    /**
     * Converts an input stream into a string
     * @param is the input stream
     * @return
     * @throws Exception
     */
    private static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    /**
     * Returns the contents of a file as a string
     * @param filePath the path to the file
     * @return
     * @throws Exception
     */
    private static String getStringFromFile (String filePath) throws Exception {
        File fl = new File(filePath);
        if (fl.exists()) {
            FileInputStream fin = new FileInputStream(fl);
            String ret = convertStreamToString(fin);
            //Make sure you close all streams.
            fin.close();
            return ret;
        } else {
            return null;
        }
    }

    /**
     * Generates the local path to the translation file using the frame id
     * @param chapterFrameId
     * @return
     */
    private String buildLocalTranslationFilePath(String chapterFrameId) {
        String[] parts;
        parts = chapterFrameId.split("-");
        if(parts.length != 2) {
            return null;
        } else {
            return  parts[0]+"/"+parts[1]+".txt";
        }
    }

    /**
     * Generates the absolute path to the repository directory
     * @param projectSlug the project slug
     * @param langCode the language code
     * @return
     */
    private String buildRepositoryFilePath(String projectSlug, String langCode) {
        return mContext.getFilesDir() + "/" + mContext.getResources().getString(R.string.git_repository_dir) + "/"+mParentProjectSlug+"-" + projectSlug + "-" + langCode;
    }


    @Override
    public void onConnectionEstablished() {
        // submit key to the server
        register();
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
