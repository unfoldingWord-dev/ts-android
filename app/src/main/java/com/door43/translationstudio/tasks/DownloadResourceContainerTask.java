package com.door43.translationstudio.tasks;

import com.door43.translationstudio.App;

import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 9/30/16.
 */

public class DownloadResourceContainerTask extends ManagedTask {
    public final Translation translation;
    private List<ResourceContainer> downloadedContainers = new ArrayList<>();

    /**
     * For keeping track of who this task is being performed for
     */
    public int TAG = -1;

    private boolean success = false;

    public DownloadResourceContainerTask(Translation translation) {
        this.translation = translation;
    }

    @Override
    public void start() {
        publishProgress(-1, "");
        try {
            if(interrupted()) return;
            ResourceContainer rc = App.getLibrary().download(translation.language.slug, translation.project.slug, translation.resource.slug);
            downloadedContainers.add(rc);
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(success) {
            // also download helps
            if(!translation.resource.slug.equals("tw") && !translation.resource.slug.equals("tn") && !translation.resource.slug.equals("tq")) {
                // TODO: 11/2/16 only download these if there is an update
                try {
                    if (translation.project.slug.equals("obs")) {
                        ResourceContainer rc = App.getLibrary().download(translation.language.slug, "bible-obs", "tw");
                        downloadedContainers.add(rc);
                    } else {
                        ResourceContainer rc = App.getLibrary().download(translation.language.slug, "bible", "tw");
                        downloadedContainers.add(rc);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
}
