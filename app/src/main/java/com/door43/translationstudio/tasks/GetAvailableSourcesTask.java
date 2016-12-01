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


public class GetAvailableSourcesTask extends ManagedTask {
    public static final String TASK_ID = "get_available_sources_task";
    public static final String TAG = GetAvailableSourcesTask.class.getName();
    private int maxProgress = 0;
    private boolean success = false;
    private List<Translation> availableTranslations;

    @Override
    public void start() {
        success = false;
        int count = 0;

        publishProgress(-1, "");

        Door43Client library = App.getLibrary();
        availableTranslations = library.index.findTranslations(null, null, null, "book", null, App.MIN_CHECKING_LEVEL, -1);

        maxProgress = availableTranslations.size();

        success = true;
    }

    @Override
    public int maxProgress() {
        return maxProgress;
    }

    public boolean isSuccess() {
        return success;
    }

    public List<Translation> getSources() {
        return availableTranslations;
    }
 }
