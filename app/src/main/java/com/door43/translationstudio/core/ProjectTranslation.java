package com.door43.translationstudio.core;

/**
 * Created by joel on 9/16/2015.
 *
 */
public class ProjectTranslation {

    private final String title;
    private final boolean isTitleFinished;

    public ProjectTranslation(String title, boolean isTitleFinished) {
        this.title = title;
        this.isTitleFinished = isTitleFinished;
    }

    /**
     * Returns the title of the project
     * @return
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * Returns a description of the project
     * @return
     */
    public String getDescription() {
        return "";
    }

    /**
     * Checks if the project title translation has been marked as finished
     * @return
     */
    public boolean isTitleFinished() {
        return isTitleFinished;
    }
}
