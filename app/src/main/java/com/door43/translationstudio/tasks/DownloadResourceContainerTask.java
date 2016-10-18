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
    }

    /**
     * Checks if the task was completed successfully
     * @return
     */
    public boolean success() {
        return this.success;
    }
}
