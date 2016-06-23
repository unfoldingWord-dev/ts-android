package com.door43.translationstudio.tasks;

import android.content.ContentResolver;
import android.net.Uri;

import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.core.ArchiveDetails;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Locale;

/**
 * Created by blm on 5/13/16.
 */

public class ExamineImportsForCollisionsTask extends ManagedTask {

    public static final String TASK_ID = "search_imports_for_collisions_task";
    public static final String TAG = ExamineImportsForCollisionsTask.class.getSimpleName();

    public boolean mSuccess;
    private ContentResolver resolver;
    public Uri mContentUri;
    public File mProjectsFolder;
    public boolean mAlreadyPresent;
    public String mProjectsFound;

    /**
     *
     */
    public ExamineImportsForCollisionsTask(ContentResolver resolver, Uri mContentUri) {
        mSuccess = false;
        mAlreadyPresent = false;
        this.resolver = resolver;
        this.mContentUri = mContentUri;
    }

    public void cleanup() {
        if(mProjectsFolder != null) {
            FileUtils.deleteQuietly(mProjectsFolder);
        }
        mProjectsFolder = null;
    }

    @Override
    public void start() {
        mSuccess = false;
        try {
            mProjectsFolder = File.createTempFile("targettranslation", "." + Translator.ARCHIVE_EXTENSION);
            FileUtils.copyInputStreamToFile(resolver.openInputStream(mContentUri), mProjectsFolder);
            ArchiveDetails details = ArchiveDetails.newInstance(mProjectsFolder, Locale.getDefault().getLanguage(), App.getLibrary());
            mProjectsFound = "";
            mAlreadyPresent = false;
            for (ArchiveDetails.TargetTranslationDetails td : details.targetTranslationDetails) {
                mProjectsFound += td.projectName + " - " + td.targetLanguageName + ", ";

                String targetTranslationId = td.targetTranslationSlug;
                TargetTranslation localTargetTranslation = App.getTranslator().getTargetTranslation(targetTranslationId);
                if ((localTargetTranslation != null)) {
                    mAlreadyPresent = true;
                }
            }
            mProjectsFound = mProjectsFound.replaceAll(", $", "");
            mSuccess = true;
        } catch (Exception e) {
            Logger.e(TAG,"Error processing input file: " + mContentUri.toString());
        }
    }
}

