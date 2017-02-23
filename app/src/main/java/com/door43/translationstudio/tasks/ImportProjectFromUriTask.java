package com.door43.translationstudio.tasks;

import android.net.Uri;

import org.unfoldingword.tools.logger.Logger;
import org.unfoldingword.tools.taskmanager.ManagedTask;
import android.os.Process;

import com.door43.translationstudio.App;
import com.door43.translationstudio.core.MergeConflictsHandler;
import com.door43.translationstudio.core.Translator;
import com.door43.util.FileUtilities;
import com.door43.util.SdUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Created by blm on 2/23/17.
 */

public class ImportProjectFromUriTask extends ManagedTask {

    public static final String TASK_ID = "import_project_from_uri_task";
    public static final String TAG = ImportProjectFromUriTask.class.getSimpleName();
    final private Uri path;
    final private boolean mergeOverwrite;

    public ImportProjectFromUriTask(Uri path, boolean mergeOverwrite) {
        setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
        this.path = path;
        this.mergeOverwrite = mergeOverwrite;
    }

    @Override
    public void start() {
        boolean success = false;
        boolean mergeConflict = false;
        BufferedInputStream in = null;
        String readablePath = path.toString(); // default
        String importedSlug = "";
        boolean validExtension = FileUtilities.getExtension(path.getPath()).toLowerCase().equals(Translator.ARCHIVE_EXTENSION);
        boolean isDocumentFile = !SdUtils.isRegularFile(path);

        if(validExtension) {
            try {
                Translator translator = App.getTranslator();
                if (isDocumentFile) {
                    readablePath = SdUtils.getPathString(path.toString());
                    Logger.i(TAG, "Importing SD card: " + readablePath);
                    InputStream inputStream = App.context().getContentResolver().openInputStream(path);
                    in = new BufferedInputStream(inputStream);
                    Translator.ImportResults importResults = translator.importArchive(in, mergeOverwrite);
                    importedSlug = importResults.importedSlug;
                    success = importResults.isSuccess();
                    if (success && importResults.mergeConflict) {
                        mergeConflict = MergeConflictsHandler.isTranslationMergeConflicted(importResults.importedSlug); // make sure we have actual merge conflicts
                    }
                } else {
                    File file = new File(path.getPath());
                    Logger.i(TAG, "Importing internal file: " + file.toString());
                    FileInputStream inputStream = new FileInputStream(file);
                    in = new BufferedInputStream(inputStream);
                    Translator.ImportResults importResults = translator.importArchive(in, mergeOverwrite);
                    importedSlug = importResults.importedSlug;
                    success = importResults.isSuccess();
                    if(success && importResults.mergeConflict) {
                        mergeConflict = MergeConflictsHandler.isTranslationMergeConflicted(importResults.importedSlug); // make sure we have actual merge conflicts
                    }
                }

            } catch(Exception e) {
                Logger.e(TAG, "Exception Importing from SD card", e);
            }
            finally {
                if(in != null) {
                    FileUtilities.closeQuietly(in);
                }
            }
        }
        setResult(new ImportResults(path, readablePath, importedSlug, success, mergeConflict, !validExtension, isDocumentFile));
    }

    /**
     * returns the import results which includes:
     *   the human readable filePath
     *   the success flag
     */
    public class ImportResults {
        public final Uri filePath;
        public final String readablePath;
        public final String importedSlug;
        public final boolean mergeConflict;
        public final boolean invalidFileName;
        public final boolean isDocumentFile;
        public final boolean success;

        ImportResults(Uri filePath, String readablePath, String importedSlug, boolean success, boolean mergeConflict, boolean invalidFileName, boolean isDocumentFile) {
            this.filePath = filePath;
            this.success = success;
            this.mergeConflict = mergeConflict;
            this.invalidFileName = invalidFileName;
            this.isDocumentFile = isDocumentFile;
            this.readablePath = readablePath;
            this.importedSlug = importedSlug;
        }
    }
}
