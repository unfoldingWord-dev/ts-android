package com.door43.translationstudio.tasks;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.core.Typography;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import java.io.File;

/**
 * Created by joel on 1/21/2016.
 */
public class PrintPDFTask extends ManagedTask {
    public static final String TASK_ID = "print_pdf_task";
    private final TargetTranslation mTargetTranslation;
    private final File mDestFile;
    private final boolean includeImages;
    private final boolean includeIncompleteFrames;
    private boolean success;

    public PrintPDFTask(String targetTranslationId, File destFile, boolean includeImages, boolean includeIncompleteFrames) {
        mDestFile = destFile;
        this.includeImages = includeImages;
        this.includeIncompleteFrames = includeIncompleteFrames;
        mTargetTranslation = AppContext.getTranslator().getTargetTranslation(targetTranslationId);
    }

    @Override
    public void start() {
        publishProgress(-1, AppContext.context().getString(R.string.printing));
        if(mTargetTranslation != null) {
            Library library = AppContext.getLibrary();
            Translator translator = AppContext.getTranslator();
            try {
                SourceTranslation sourceTranslation = AppContext.getLibrary().getDefaultSourceTranslation(mTargetTranslation.getProjectId(), "en");
                File imagesDir = library.getImagesDir();
                translator.exportPdf(library, mTargetTranslation, sourceTranslation.getFormat(), Typography.getAssetPath(AppContext.context()), imagesDir, includeImages, includeIncompleteFrames, mDestFile);
                if (mDestFile.exists()) {
                    success = true;
                } else {
                    success = false;
                }
            } catch (Exception e) {
                Logger.e(this.getClass().getName(), "Failed to export " + mTargetTranslation.getId() + " as pdf", e);
                success = false;
            }
        }
        publishProgress(1, AppContext.context().getString(R.string.printing));
    }

    public boolean isSuccess() {
        return success;
    }
}
