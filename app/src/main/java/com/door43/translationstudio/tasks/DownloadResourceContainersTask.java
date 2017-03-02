package com.door43.translationstudio.tasks;

import com.door43.translationstudio.App;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.logger.Logger;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * to download multiple resource containers
 */

public class DownloadResourceContainersTask extends ManagedTask {
    public final List<String> translationIDs;
    private List<ResourceContainer> downloadedContainers = new ArrayList<>();
    private List<String> failedSourceDownloads = new ArrayList<>();
    private Map<String, String> failureMessages = new HashMap<>();
    private List<String> failedHelpsDownloads = new ArrayList<>();
    public List<String> downloadedTranslations = new ArrayList<>();
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
        failedSourceDownloads.clear();
        failureMessages.clear();
        failedHelpsDownloads.clear();
        maxProgress = translationIDs.size();
        downloadedTranslations.clear();
        Set<String> downloadedTwBibleLanguages = new HashSet<>();
        Set<String> downloadedTwObsLanguages = new HashSet<>();
        publishProgress(-1, "");

        Door43Client library = App.getLibrary();
        for (int i = 0; i < maxProgress; i++) {
            String resourceContainerSlug = translationIDs.get(i);
            Translation translation = null;
            boolean passSuccess = false;

            float progress = (float) i / maxProgress + 0.00001f; // add offset to make sure not exactly zero
            publishProgress(progress, resourceContainerSlug);

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
                failedSourceDownloads.add(resourceContainerSlug);
                Logger.i(TAG, "download Failed: " + resourceContainerSlug);
            }

            if (passSuccess) {
                // also download helps
                String resourceSlug = translation.resource.slug;
                String languageSlug = translation.language.slug;
                String projectSlug = translation.project.slug;
                if (!resourceSlug.equals("tw") && !resourceSlug.equals("tn") && !resourceSlug.equals("tq") && !resourceSlug.equals("udb")) {
                    // TODO: 11/2/16 only download these if there is an update
                    String resource = "";
                    try {
                        if (projectSlug.equals("obs")) {
                            boolean success = downloadTranlationWords(library, progress, resourceContainerSlug, downloadedTwObsLanguages, languageSlug, "bible-obs", "tw", "OBS Words");
                            passSuccess = passSuccess && success;
                        } else {
                            boolean success = downloadTranlationWords(library, progress, resourceContainerSlug, downloadedTwBibleLanguages, languageSlug, "bible", "tw", "Bible Words");
                            passSuccess = passSuccess && success;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (interrupted() || this.isCanceled()) return;
                    passSuccess = passSuccess && downloadHelps(library, progress, resourceContainerSlug, languageSlug, projectSlug, "tn", "Notes");
                    if (interrupted() || this.isCanceled()) return;
                    passSuccess = passSuccess && downloadHelps(library, progress, resourceContainerSlug, languageSlug, projectSlug, "tq", "Questions");
                }
            }

            if (!passSuccess) {
                success = false;
            } else {
                downloadedTranslations.add(resourceContainerSlug);
            }
        }
        publishProgress((float) 1.0, "");
    }

    /**
     * handles specific translation words download for resource, only downloads once for each language
     * @param library
     * @param progress
     * @param resourceContainerSlug
     * @param downloaded
     * @param languageSlug
     * @param projectSlug
     * @param resourceSlug
     * @param name
     * @return
     */
    public boolean downloadTranlationWords(Door43Client library, float progress, String resourceContainerSlug, Set<String> downloaded, String languageSlug, String projectSlug, String resourceSlug, String name) {
        boolean success = true;
        if (!downloaded.contains(languageSlug)) {
            success = downloadHelps(library, progress, resourceContainerSlug, languageSlug, projectSlug, resourceSlug, name);
            if (success) {
                downloaded.add(languageSlug);
            }
        } else {
            Logger.i(TAG, "'" + name + "' already downloaded for: " + languageSlug);
        }
        return success;
    }

    /**
     * handles specific helps download for resource
     * @param library
     * @param progress
     * @param resourceContainerSlug
     * @param languageSlug
     * @param projectSlug
     * @param resourceSlug
     * @param name
     * @return
     */
    public boolean downloadHelps(Door43Client library, float progress, String resourceContainerSlug, String languageSlug, String projectSlug, String resourceSlug, String name) {
        boolean passSuccess = true;
        try {
            if (interrupted() || this.isCanceled()) return false;
            // check if notes present before trying to download
            List<Translation> helps = library.index.findTranslations(languageSlug, projectSlug, resourceSlug, null, null, App.MIN_CHECKING_LEVEL, -1);
            if(helps.size() == 0) {
                Logger.i(TAG, "No '" + name + "' for: " + resourceContainerSlug);
            }
            for (Translation help : helps) {
                Logger.i(TAG, "Loading " + name + " ID: " + help.resourceContainerSlug);
                publishProgress(progress, help.resourceContainerSlug);
                ResourceContainer rc = library.download(help.language.slug, help.project.slug, help.resource.slug);
                downloadedContainers.add(rc);
                Logger.i(TAG, name + " download Success: " + rc.slug);
            }
        } catch (Exception e) {
            e.printStackTrace();
            String resource = languageSlug + "_" + projectSlug + "_" + resourceSlug;
            Logger.i(TAG, name + " download Failed: " + resource);
            failedHelpsDownloads.add(resource);
            failedSourceDownloads.add(resourceContainerSlug); // if helps download failed, then mark the source as error also
            passSuccess = false;
        }
        return passSuccess;
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
     * Returns the slugs for source languages where the source and all the resources where downloaded
     * @return
     */
    public List<String> getDownloadedTranslations() {
        return downloadedTranslations;
    }

    /**
     * Returns the translations that failed to download
     * @return
     */
    public List<String> getFailedSourceDownloads() {
        return failedSourceDownloads;
    }

    /**
     * Returns the notes that failed to download
     * @return
     */
    public List<String> getFailedHelpsDownloads() {
        return failedHelpsDownloads;
    }

    public String getFailureMessage(String translationID) {
        if(failureMessages.containsKey(translationID)) {
            return failureMessages.get(translationID);
        } else {
            return null;
        }
    }
}
