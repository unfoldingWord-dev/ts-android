package com.door43.translationstudio.tasks;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.resourcecontainer.Project;
import org.unfoldingword.resourcecontainer.Resource;
import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationFormat;
import com.door43.translationstudio.core.TranslationType;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.core.Typography;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import java.io.File;
import java.util.List;

/**
 * Created by joel on 1/21/2016.
 */
public class PrintPDFTask extends ManagedTask {
    public static final String TASK_ID = "print_pdf_task";
    private final TargetTranslation mTargetTranslation;
    private final File mDestFile;
    private final boolean includeImages;
    private final boolean includeIncompleteFrames;
    private final File imagesDir;
    private boolean success;
    private String message;

    public PrintPDFTask(String targetTranslationId, File destFile, boolean includeImages, boolean includeIncompleteFrames, File imagesDir) {
        mDestFile = destFile;
        this.includeImages = includeImages;
        this.includeIncompleteFrames = includeIncompleteFrames;
        this.imagesDir = imagesDir;
        mTargetTranslation = App.getTranslator().getTargetTranslation(targetTranslationId);
        message = "";
    }

    @Override
    public void start() {
        message = App.context().getString(R.string.printing) + mTargetTranslation.getId();
        publishProgress(-1, message);
        if(mTargetTranslation != null) {
            Door43Client library = App.getLibrary();
            Translator translator = App.getTranslator();
            try {
                Project p = App.getLibrary().index().getProject("en", mTargetTranslation.getProjectId(), true);
                List<Resource> resources = App.getLibrary().index().getResources(p.languageSlug, p.slug);
                String targetLanguageFontPath = Typography.getAssetPath(App.context(), TranslationType.TARGET);
                float targetLanguageFontSize = Typography.getFontSize(App.context(), TranslationType.TARGET);
                String licenseFontName = App.context().getString(R.string.pref_default_translation_typeface);
                String licenseFontPath = "assets/fonts/" + licenseFontName;
                translator.exportPdf(library, mTargetTranslation, mTargetTranslation.getFormat(), targetLanguageFontPath,
                                        targetLanguageFontSize, licenseFontPath, imagesDir, includeImages,
                                        includeIncompleteFrames, mDestFile, this);
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
        publishProgress(1, App.context().getString(R.string.printing));
    }

    public void updateProgress(double progress) {
        publishProgress(progress, message);
    }

    public boolean isSuccess() {
        return success;
    }
}
