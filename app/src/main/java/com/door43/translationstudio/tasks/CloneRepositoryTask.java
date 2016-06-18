package com.door43.translationstudio.tasks;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.git.TransportCallback;
import com.door43.util.FileUtilities;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;

import java.io.File;

/**
 * Clones a repository from the server
 */
public class CloneRepositoryTask extends ManagedTask {

    public static final String TASK_ID = "clone_target_translation";
    private final File destDir;
    private final String cloneUrl;
    private Status status = Status.UNKNOWN;

    public CloneRepositoryTask(String cloneUrl, File dest) {
        this.destDir = dest;
        this.cloneUrl = cloneUrl;
    }

    /**
     * Returns the path where the repository was cloned
     * @return
     */
    public File getDestDir() {
        return destDir;
    }

    @Override
    public void start() {
        if(AppContext.context().isNetworkAvailable()) {
            publishProgress(-1, AppContext.context().getResources().getString(R.string.downloading));
            FileUtilities.deleteRecursive(destDir);

            try {
                // prepare destination
                destDir.mkdirs();

                CloneCommand cloneCommand = Git.cloneRepository()
                        .setURI(cloneUrl)
                        .setTransportConfigCallback(new TransportCallback())
                        .setDirectory(destDir);
                try {
                    Git result = cloneCommand.call();
                    result.getRepository().close();
                    this.status = Status.SUCCESS;
                } catch (TransportException e) {
                    Logger.e(this.getClass().getName(), e.getMessage(), e);
                    Throwable cause = e.getCause();
                    if(cause != null) {
                        Throwable subException = cause.getCause();
                        if(subException != null) {
                            String detail = subException.getMessage();
                            if ("Auth fail".equals(detail)) {
                                this.status = Status.AUTH_FAILURE;
                            }
                        } else if(cause instanceof NoRemoteRepositoryException) {
                            this.status = Status.NO_REMOTE_REPO;
                        } else if(cause.getMessage().contains("not permitted")) {
                            this.status = Status.AUTH_FAILURE;
                        }
                    }
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), e.getMessage(), e);
                } catch (OutOfMemoryError e) {
                    Logger.e(this.getClass().getName(), e.getMessage(), e);
                    this.status = Status.OUT_OF_MEMORY;
                } catch (Throwable e) {
                    Logger.e(this.getClass().getName(), e.getMessage(), e);
                }
            } catch (Exception e) {
                Logger.e(this.getClass().getName(), "Failed to clone the repository " + cloneUrl, e);
                FileUtils.deleteQuietly(destDir);
                stop();
            }
        }
    }

    public Status getStatus() {
        return status;
    }

    public String getCloneUrl() {
        return cloneUrl;
    }

    public enum Status {
        NO_REMOTE_REPO,
        UNKNOWN,
        AUTH_FAILURE,
        OUT_OF_MEMORY,
        SUCCESS
    }
}
