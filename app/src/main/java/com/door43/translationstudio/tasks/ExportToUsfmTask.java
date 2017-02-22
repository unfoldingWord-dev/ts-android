package com.door43.translationstudio.tasks;

import android.app.Activity;
import android.net.Uri;
import android.os.Process;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.ExportUsfm;
import com.door43.translationstudio.core.TargetTranslation;
import org.unfoldingword.tools.taskmanager.ManagedTask;

/**
 * Created by blm on 2/22/17.
 */

public class ExportToUsfmTask extends ManagedTask {

    public static final String TASK_ID = "export_to_usfm_task";
    public static final String TAG = ExportProjectTask.class.getSimpleName();
    final private String filename;
    final private Uri path;
    private String message = "";
    final private TargetTranslation targetTranslation;
    final private boolean outputToDocumentFile;

    public ExportToUsfmTask(Activity activity, TargetTranslation targetTranslation, Uri path, String filename, boolean outputToDocumentFile) {
        setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
        this.filename = filename;
        this.path = path;
        this.targetTranslation = targetTranslation;
        this.outputToDocumentFile = outputToDocumentFile;
        message = activity.getString(R.string.please_wait);
    }

    @Override
    public void start() {
        publishProgress(-1, message);
        Uri exportFile = ExportUsfm.saveToUSFM( targetTranslation, path, filename, outputToDocumentFile);
        setResult(exportFile);
    }
}