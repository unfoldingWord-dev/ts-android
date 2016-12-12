package com.door43.translationstudio.tasks;

import com.door43.translationstudio.App;

import org.unfoldingword.door43client.models.TargetLanguage;
import org.unfoldingword.tools.logger.Logger;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Indexes all of the global catalogs in the api
 */

public class UpdateCatalogsTask extends ManagedTask {
    public static final String TASK_ID = "update-catalogs-task";
    public static final String TAG = UpdateCatalogsTask.class.getName();
    private int maxProgress = 0;
    private boolean success = false;
    private int addedCnt = 0;
    private String prefix;

    @Override
    public void start() {
        addedCnt = 0;
        success = false;

        publishProgress(-1, "");

        List<TargetLanguage> targetLanguages = App.getLibrary().index.getTargetLanguages();
        Set<String> initialLanguages = new HashSet<>();
        for (TargetLanguage l : targetLanguages) {
            initialLanguages.add(l.slug);
        }

        Logger.i(TAG,"Initial target languages count: " + targetLanguages.size());
        Logger.i(TAG,"Unigue target languages slug count: " + initialLanguages.size());

        targetLanguages = null; // free up memory while downloading

        try {
            App.getLibrary().updateCatalogs(new org.unfoldingword.door43client.OnProgressListener() {
                @Override
                public boolean onProgress(String tag, long max, long complete) {
                    maxProgress = (int)max;

                    if(prefix != null) {
                        tag = prefix + tag;
                    }

                    publishProgress((float)complete/(float)max, tag);

                    if(UpdateCatalogsTask.this.isCanceled()) {
                        Logger.i(this.getClass().getSimpleName(), "Download Cancelled");
                        return false;
                    }
                    return true;
                }
            });
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(success) {
            targetLanguages = App.getLibrary().index.getTargetLanguages();
            Logger.i(TAG,"Final target languages count: " + targetLanguages.size());
            for (TargetLanguage l : targetLanguages) {
                if(UpdateCatalogsTask.this.isCanceled()) {
                    break;
                }

                if(!initialLanguages.contains(l.slug)) {
                    addedCnt++;
                    Logger.i(TAG,"New target languages " + addedCnt + ": " + l.slug);
                }
            }
        }
    }

    @Override
    public int maxProgress() {
        return maxProgress;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getAddedCnt() {
        return addedCnt;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix + "  ";
    }
}
