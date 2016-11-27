package com.door43.translationstudio.tasks;

import com.door43.translationstudio.App;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.logger.Logger;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * searches resources to find new and updated resources.
 */


public class FindNewAndUpdatedSourcesTask extends ManagedTask {
    public static final String TASK_ID = "find_new_and_updated_sources_task";
    public static final String TAG = UpdateCatalogsTask.class.getName();
    private int maxProgress = 0;
    private boolean success = false;
    private int addedCnt = 0;
    private String prefix;
    private List<Translation> changedSources;

    @Override
    public void start() {
        addedCnt = 0;
        success = false;
        changedSources = new ArrayList<>();
        int count = 0;
        Calendar calendar = Calendar.getInstance();

        publishProgress(-1, "");

        File containersDir = new File(App.publicDir(), "resource_containers");

        Door43Client library = App.getLibrary();
        List<Translation> availableTranslations = library.index.findTranslations(null, null, null, "book", null, App.MIN_CHECKING_LEVEL, -1);

        int length = availableTranslations.size();
        for (Translation sourceTranslation : availableTranslations) {
            publishProgress((float)count++/length, "");

            // TODO: 11/26/16 add unchanged items to list
            boolean isDownloaded = library.exists(sourceTranslation.resourceContainerSlug);
            if(!isDownloaded) {
                changedSources.add(sourceTranslation);
            } else { // check change
                File directory = new File(containersDir, sourceTranslation.resourceContainerSlug);
                File archive = new File(directory + "." + ResourceContainer.fileExtension);
                if(!archive.exists()) {
                    changedSources.add(sourceTranslation);
                } else {
                    long modified = archive.lastModified();
                    calendar.setTimeInMillis(modified);

                    int lastUpdated = calendar.get(Calendar.YEAR) * 10000
                            + calendar.get(Calendar.MONTH) * 100
                            + calendar.get(Calendar.DAY_OF_MONTH);
                    int lastModifiedOnServer = library.getResourceContainerLastModified(sourceTranslation.language.slug, sourceTranslation.project.slug, sourceTranslation.resource.slug);
                    if(lastModifiedOnServer > lastUpdated) {
                        changedSources.add(sourceTranslation);
                    }
                    if(lastModifiedOnServer < 0) {
                        Logger.i(TASK_ID,"Could not get server modified time: " + sourceTranslation.resourceContainerSlug);
                    }
                }
            }
        }

        success = true;
    }

    @Override
    public int maxProgress() {
        return maxProgress;
    }

    public boolean isSuccess() {
        return success;
    }

    public List<Translation> getChangedSources() {
        return changedSources;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix + "  ";
    }
}
