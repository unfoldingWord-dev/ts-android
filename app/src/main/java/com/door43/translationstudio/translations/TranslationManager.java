package com.door43.translationstudio.translations;

import android.util.Log;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Project;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * This class handles the storage of translated content.
 */
public class TranslationManager {
    private GitSync mGitSync = new GitSync();
    private MainApplication mContext;
    private final String TAG = "TranslationManager";
    private String mActiveRepo;
    private String mActiveRepoPath;
    private final String mParentProjectSlug = "uw"; //  NOTE: not sure if this will ever need to be dynamic

    public TranslationManager(MainApplication context) {
        mContext = context;
    }

    private void openRepo(String projectSlug, String languageCode) {
        if(mActiveRepo != projectSlug) {
            mActiveRepo = projectSlug;
            mActiveRepoPath = buildRepositoryFilePath(projectSlug, languageCode);
            File repoDir = new File(mActiveRepoPath);
            repoDir.mkdir();
            mGitSync.openRepo(repoDir);
        }
    }

    /**
     * Saves the translation into a folder structure based on the project, chapter, frame, and language.
     * Note: we don't explicitly pass in the chapter id because it is combined with the frame id.
     * Saving is cheap and should be performed often.
     * @param translation the translated text
     */
    public void save(String translation, String projectSlug, String langCode, String chapterFrameId) {
        // open the repository
        openRepo(projectSlug, langCode);

        // build the file path
        String path = buildLocalTranslationFilePath(chapterFrameId);
        if(path != null) {
            // save the file
            mGitSync.updateFile(translation, path);
        } else {
            Log.d(TAG, "the fild could not be saved");
            // TODO: notify the user that the file could not be saved
        }
    }

    /**
     * Initiates sharing with nearby devices. Or a simple file export that can be emailed, shared over external storage, etc.
     */
    public void share() {
        Log.d(TAG, "need to share stuff!");
    }

    /**
     * Initiates a git sync with the server. This will pull down updates as well as push local changes.
     * Conflicts will need to be handled by the client before pushing to the server.
     */
    public void sync() {
        sync(false);
    }

    /**
     * Initiates a git sync with the server. This will pull down updates as well as push local changes.
     * Conflicts will need to be handled by the client before pushing to the server.
     * Syncing is not cheap and should only be done on a schedule or manually.
     * @param forcePush if set to true the app will perform a forced push to the server overriding any changes on the server and avoiding merge conflicts
     */
    public void sync(boolean forcePush) {
        // TODO: need to handle pulls and conflicts
        mGitSync.pushToRemote(mContext.getResources().getString(R.string.git_server));
    }

    /**
     * Returns the translated content for a given frame
     * @param projectSlug the project in which the translation exists
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
}
