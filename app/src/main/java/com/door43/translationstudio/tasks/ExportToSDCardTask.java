package com.door43.translationstudio.tasks;

import android.support.v4.provider.DocumentFile;

import com.door43.translationstudio.App;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.util.FileUtilities;
import com.door43.util.SdUtils;

import org.unfoldingword.tools.logger.Logger;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import java.io.File;
import java.io.OutputStream;

/**
 * Created by blm on 2/22/17.
 */

public class ExportToSDCardTask extends ManagedTask {

    public static final String TASK_ID = "export_to_sd_card_task";
    public static final String TAG = ExportToSDCardTask.class.getSimpleName();
    final private String filename;
    final private String title;
    final private TargetTranslation targetTranslation;

    public ExportToSDCardTask(String filename, TargetTranslation targetTranslation, String title) {
        this.filename = filename;
        this.title = title;
        this.targetTranslation = targetTranslation;
    }

    @Override
    public void start() {
        boolean canWriteToSdCardBackupLollipop = false;
        DocumentFile baseFolder = null;
        String filePath = null;
        DocumentFile sdCardFile = null;
        OutputStream out = null;
        boolean success = false;
        publishProgress(-1, title);

        try {
            if(SdUtils.isSdCardPresentLollipop()) {
                baseFolder = SdUtils.sdCardMkdirs(SdUtils.DOWNLOAD_TRANSLATION_STUDIO_FOLDER);
                canWriteToSdCardBackupLollipop = baseFolder != null;
            }

            if (canWriteToSdCardBackupLollipop) { // default to writing to SD card if available
                filePath = SdUtils.getPathString(baseFolder);
                if (baseFolder.canWrite()) {
                    sdCardFile = SdUtils.documentFileCreate(baseFolder, filename);
                    filePath = SdUtils.getPathString(sdCardFile);
                    out = SdUtils.createOutputStream(sdCardFile);
                    App.getTranslator().exportArchive(targetTranslation, out, filename);
                    success = true;
                }
            } else {
                File exportFile = new File(App.getPublicDownloadsDirectory(), filename);
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
