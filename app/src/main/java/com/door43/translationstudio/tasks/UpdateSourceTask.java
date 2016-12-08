package com.door43.translationstudio.tasks;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.ui.SettingsActivity;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.tools.logger.Logger;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Indexes all of the source meta from the api
 */
public class UpdateSourceTask extends ManagedTask {
    public static final String TASK_ID = "update-source-task";
    private int maxProgress = 0;
    private boolean success = false;
    private int updatedCnt = 0;
    private int addedCnt = 0;
    private String prefix;

    @Override
    public void start() {
        updatedCnt = 0;
        addedCnt = 0;
        success = false;
        int count = 0;

        publishProgress(-1, "");

        Door43Client library = App.getLibrary();
        List<Translation> availableTranslationsAll = library.index.findTranslations(null, null, null, "book", null, App.MIN_CHECKING_LEVEL, -1);
        Map<String,Integer> previouslyUpdated = new HashMap<>();

        maxProgress = availableTranslationsAll.size();
        for (Translation t : availableTranslationsAll) {

            if( ++count % 16 == 0) {
                publishProgress((float)count/maxProgress, "");

                if(UpdateSourceTask.this.isCanceled()) {
                    success = false;
                    return;
                }
            }

            String id = t.resourceContainerSlug;
            int lastModifiedOnServer = library.getResourceContainerLastModified(t.language.slug, t.project.slug, t.resource.slug);
            previouslyUpdated.put(id, lastModifiedOnServer);
        }

        availableTranslationsAll = null; // free up memory while downloading

        publishProgress(-1, "");

        try {
            String server = App.getPref(SettingsActivity.KEY_PREF_MEDIA_SERVER, App.getRes(R.string.pref_default_media_server));
            String rootApiUrl = server + App.getRes(R.string.root_catalog_api);
            App.getLibrary().updateSources(rootApiUrl, new org.unfoldingword.door43client.OnProgressListener() {
                @Override
                public boolean onProgress(String tag, long max, long complete) {
                    maxProgress = (int)max;

                    if(prefix != null) {
                        tag = prefix + tag;
                    }

                    publishProgress((float)complete/(float)max, tag);

                    if(UpdateSourceTask.this.isCanceled()) {
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

        if(success) { // check for changes
            publishProgress(-1, "");

            library = App.getLibrary();
            availableTranslationsAll = library.index.findTranslations(null, null, null, "book", null, App.MIN_CHECKING_LEVEL, -1);

            maxProgress = availableTranslationsAll.size();
            count = 0;

            for (Translation t : availableTranslationsAll) {
                if(UpdateSourceTask.this.isCanceled()) {
                    break;
                }

                if( ++count % 16 == 0) {
                    publishProgress((float)count/maxProgress, "");

                    if(UpdateSourceTask.this.isCanceled()) {
                        success = false;
                        return;
                    }
                }

                String id = t.resourceContainerSlug;
                if(previouslyUpdated.containsKey(id)) {
                    try {
                        int lastModifiedOnServer = library.getResourceContainerLastModified(t.language.slug, t.project.slug, t.resource.slug);
                        Integer previousUpdate = previouslyUpdated.get(id);
                        if (lastModifiedOnServer > previousUpdate) {
                            updatedCnt++; // update times have changed
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    addedCnt++; // new entry
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

    public int getUpdatedCnt() {
        return updatedCnt;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix + "  ";
    }
}
