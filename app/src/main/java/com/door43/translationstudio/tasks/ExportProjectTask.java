package com.door43.translationstudio.tasks;

import android.app.Activity;
import android.net.Uri;
import android.os.Process;
import android.support.v4.provider.DocumentFile;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.util.FileUtilities;
import com.door43.util.SdUtils;

import org.eclipse.jgit.api.errors.NoHeadException;
import org.unfoldingword.tools.logger.Logger;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import java.io.File;
import java.io.OutputStream;

/**
 * Created by blm on 2/22/17.
 */

public class ExportProjectTask extends ManagedTask {

    public static final String TASK_ID = "export_project_task";
    public static final String TAG = ExportProjectTask.class.getSimpleName();
    final private String filename;
    final private Uri path;
    private String message = "";
    final private TargetTranslation targetTranslation;

    public ExportProjectTask(Activity activity, String filename, Uri path, TargetTranslation targetTranslation) {
        setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
        this.filename = filename;
        this.path = path;
        this.targetTranslation = targetTranslation;
        message = activity.getString(R.string.please_wait);
    }

    @Override
    public void start() {
        String filePath = null;
        DocumentFile sdCardFile = null;
        OutputStream out = null;
        boolean success = false;
        publishProgress(-1, message);
        boolean isOutputToDocumentFile = !SdUtils.isRegularFile(path);

        try {
            if (isOutputToDocumentFile) {
                sdCardFile = SdUtils.documentFileCreate(path, filename);
                filePath = SdUtils.getPathString(sdCardFile);
                out = SdUtils.createOutputStream(sdCardFile);
                try {
                    App.getTranslator().exportArchive(targetTranslation, out, filename);
                    success = true;
                } catch (NoHeadException e) {
                    // fix corrupt repo and try again
                    App.recoverRepo(targetTranslation);
                    App.getTranslator().exportArchive(targetTranslation, out, filename);
                    success = true;
                }

            } else {
                File exportFile = new File(path.getPath(), filename);
                filePath = exportFile.toString();
                App.getTranslator().exportArchive(targetTranslation, exportFile);
                success = exportFile.exists();
            }
        } catch (Exception e) {
            success = false;
            Logger.e(TAG, "Failed to export the target translation " + targetTranslation.getId(), e);
            if(sdCardFile != null) {
                try {
                    if(null != out) {
                        FileUtilities.closeQuietly(out);
                    }
                    sdCardFile.delete();
                } catch(Exception e2) {
                    Logger.e(TAG, "Cleanup failed", e2);
                }
            }
        }

        setResult(new ExportResults(filePath, success));
    }

    /**
     * returns the import results which includes:
     *   the human readable filePath
     *   the success flag
     */
    public class ExportResults {
        public final String filePath;
        public final boolean success;

        ExportResults(String filePath, boolean success) {
            this.filePath = filePath;
            this.success = success;
        }
    }
}
