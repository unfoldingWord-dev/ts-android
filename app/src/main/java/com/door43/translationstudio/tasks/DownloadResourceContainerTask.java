package com.door43.translationstudio.tasks;

import com.door43.translationstudio.App;
import com.door43.translationstudio.core.SourceTranslation;

import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.tools.taskmanager.ManagedTask;

/**
 * Created by joel on 9/30/16.
 */

public class DownloadResourceContainerTask extends ManagedTask {
    public final Translation translation;

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
            App.getLibrary().download(translation.language.slug, translation.project.slug, translation.resource.slug);
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(success) {
            // also download helps
            if(!translation.resource.slug.equals("tw") || !translation.resource.slug.equals("tn") || !translation.resource.slug.equals("tq")) {
                // TODO: 11/2/16 only download these if there is an update
                try {
                    App.getLibrary().download(translation.language.slug, translation.project.slug, "tw");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    App.getLibrary().download(translation.language.slug, translation.project.slug, "tn");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    App.getLibrary().download(translation.language.slug, translation.project.slug, "tq");
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
}
