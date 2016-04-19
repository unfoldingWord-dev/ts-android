package com.door43.translationstudio.tasks;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.git.TransportCallback;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;

import java.io.File;

/**
 * Clones a repository from the server
 */
public class CloneRepositoryTask extends SshTask {

    public static final String TASK_ID = "clone_target_translation";
    private final File localPath;
    private final String remote;
    private final String targetTranslationSlug;

    public CloneRepositoryTask(String targetTranslationSlug, File dest) {
        this.localPath = dest;
        this.targetTranslationSlug = targetTranslationSlug;
        String server = AppContext.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_GIT_SERVER, AppContext.context().getResources().getString(R.string.pref_default_git_server));
        this.remote = server + ":tS/" + AppContext.udid() + "/" + targetTranslationSlug;
    }

    /**
     * Returns the slug of the target translation that was cloned
     * @return
     */
    public String getTargetTranslationSlug() {
        return targetTranslationSlug;
    }

    /**
     * Returns the path where the repository was cloned
     * @return
     */
    public File getLocalPath() {
        return localPath;
    }

    @Override
    protected void onRegistered() {
        publishProgress(-1, AppContext.context().getResources().getString(R.string.downloading));
        try {
            // prepare destination
            localPath.mkdirs();

            Git result = Git.cloneRepository()
                    .setURI(remote)
                    .setTransportConfigCallback(new TransportCallback())
                    .setDirectory(localPath)
                    .call();
            result.getRepository().close();
        } catch (Exception e) {
            Logger.e(this.getClass().getName(), "Failed to clone the repository " + remote, e);
            FileUtils.deleteQuietly(localPath);
            stop();
        }
    }
}
