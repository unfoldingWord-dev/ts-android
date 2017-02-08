package com.door43.translationstudio.tasks;

import com.door43.translationstudio.App;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.logger.Logger;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * to download multiple resource containers
 */

public class DownloadResourceContainersTask extends ManagedTask {
    public final List<String> translationIDs;
    private List<ResourceContainer> downloadedContainers = new ArrayList<>();
    private List<String> failedDownloads = new ArrayList<>();
    private Map<String, String> failureMessages = new HashMap<>();
    private List<String> failedNotesDownloads = new ArrayList<>();
    private List<String> failedQuestionsDownloads = new ArrayList<>();
    private int maxProgress = 0;

    public static String TAG = DownloadResourceContainersTask.class.getSimpleName();

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

        Door43Client library = App.getLibrary();
        for (int i = 0; i < maxProgress; i++) {
            String resourceContainerSlug = translationIDs.get(i);
            Translation translation = null;
            boolean passSuccess = false;

            publishProgress((float)i/maxProgress, resourceContainerSlug);

            Logger.i(TAG, "Loading ID: " + resourceContainerSlug);

            try {
                translation = library.index.getTranslation(resourceContainerSlug);
                if (interrupted() || this.isCanceled()) return;
                ResourceContainer rc = library.download(translation.language.slug, translation.project.slug, translation.resource.slug);
                downloadedContainers.add(rc);
                Logger.i(TAG, "download Success: " + translation.resourceContainerSlug);
                passSuccess = true;
            } catch (Exception e) {
                e.printStackTrace();
                failureMessages.put(resourceContainerSlug, e.getMessage());
                failedDownloads.add(resourceContainerSlug);
                Logger.i(TAG, "download Failed: " + resourceContainerSlug);
            }

            if (passSuccess) {
                // also download helps
                String resourceSlug = translation.resource.slug;
                if (!resourceSlug.equals("tw") && !resourceSlug.equals("tn") && !resourceSlug.equals("tq") && !resourceSlug.equals("udb")) {
                    // TODO: 11/2/16 only download these if there is an update
                    try {
                        if (interrupted() || this.isCanceled()) return;
                        // check if notes present before trying to download
                        List<Translation> helps = library.index.findTranslations(translation.language.slug, translation.project.slug, "tn", null, null, App.MIN_CHECKING_LEVEL, -1);
                        for (Translation help : helps) {
                            Logger.i(TAG, "Loading notes ID: " + help.resourceContainerSlug);
                            ResourceContainer rc = library.download(help.language.slug, help.project.slug, help.resource.slug);
                            downloadedContainers.add(rc);
                            Logger.i(TAG, "notes download Success: " + rc.slug);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        String resource = translation.language.slug + "_" + translation.project.slug + "_tn";
                        Logger.i(TAG, "notes download Failed: " + resource);
                        failedNotesDownloads.add(resource);
                    }
                    try {
                        if (interrupted() || this.isCanceled()) return;
                        // check if questions present before trying to download
                        List<Translation> helps = library.index.findTranslations(translation.language.slug, translation.project.slug, "tq", null, null, App.MIN_CHECKING_LEVEL, -1);
                        for (Translation help : helps) {
                            Logger.i(TAG, "Loading question ID: " + help.resourceContainerSlug);
                            ResourceContainer rc = library.download(help.language.slug, help.project.slug, help.resource.slug);
                            downloadedContainers.add(rc);
                            Logger.i(TAG, "questions download Success: " + rc.slug);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        String resource = translation.language.slug + "_" + translation.project.slug + "_tq";
                        Logger.i(TAG, "quotes download Failed: " + resource);
                        failedQuestionsDownloads.add(resource);
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
    public List<String> getFailedDownloads() {
        return failedDownloads;
    }

    /**
     * Returns the notes that failed to download
     * @return
     */
    public List<String> getFailedNotesDownloads() {
        return failedNotesDownloads;
    }

    /**
     * Returns the questions that failed to download
     * @return
     */
    public List<String> getFailedQuestionsDownloads() {
        return failedQuestionsDownloads;
    }

    public String getFailureMessage(String translationID) {
        if(failureMessages.containsKey(translationID)) {
            return failureMessages.get(translationID);
        } else {
            return null;
        }
    }
}
