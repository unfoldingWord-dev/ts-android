package com.door43.translationstudio.tasks;

import com.door43.translationstudio.App;

import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import java.util.ArrayList;
import java.util.List;

/**
 * to download multiple resource containers
 */

public class DownloadResourceContainersTask extends ManagedTask {
    public final List<String> translationIDs;
    private List<ResourceContainer> downloadedContainers = new ArrayList<>();
    private List<Translation> failedDownloads = new ArrayList<>();
    private int maxProgress = 0;

    /**
     * For keeping track of who this task is being performed for
     */
    public int TAG = -1;

    private boolean success = false;

    public DownloadResourceContainersTask(List<String> resourceContainerSlugs) {
        translationIDs = resourceContainerSlugs;
    }

    @Override
    public void start() {
        success = true;
        downloadedContainers.clear();
        failedDownloads.clear();
        maxProgress = translationIDs.size();
        publishProgress(-1, "");

        for (int i = 0; i < maxProgress; i++) {
            String resourceContainerSlug = translationIDs.get(i);
            Translation translation = null;
            boolean passSuccess = false;

            publishProgress((float)i/maxProgress, resourceContainerSlug);

            try {
                translation = App.getLibrary().index.getTranslation(resourceContainerSlug);
                if (interrupted() || this.isCanceled()) return;
                ResourceContainer rc = App.getLibrary().download(translation.language.slug, translation.project.slug, translation.resource.slug);
                downloadedContainers.add(rc);
                passSuccess = true;
            } catch (Exception e) {
                failedDownloads.add(translation);
                e.printStackTrace();
            }

            if (passSuccess) {
                // also download helps
                if (!translation.resource.slug.equals("tw") || !translation.resource.slug.equals("tn") || !translation.resource.slug.equals("tq")) {
                    // TODO: 11/2/16 only download these if there is an update
                    try {
                        ResourceContainer rc = App.getLibrary().download(translation.language.slug, translation.project.slug, "tn");
                        downloadedContainers.add(rc);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        ResourceContainer rc = App.getLibrary().download(translation.language.slug, translation.project.slug, "tq");
                        downloadedContainers.add(rc);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            if(!passSuccess){
                success = false;
            }
        }
        publishProgress((float)1.0, "");
    }

    @Override
    public int maxProgress() {
        return maxProgress;
    }

    /**
     * Checks if the task was completed successfully
     * @return
     */
    public boolean success() {
        return this.success;
    }

    /**
     * Returns the resource containers that were downloaded
     * @return
     */
    public List<ResourceContainer> getDownloadedContainers() {
        return downloadedContainers;
    }

    /**
     * Returns the translations that failed to download
     * @return
     */
    public List<Translation> getFailedDownloads() {
        return failedDownloads;
    }
}
