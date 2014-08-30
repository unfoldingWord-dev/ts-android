package com.door43.translationstudio;

/**
 * Created by joel on 8/29/2014.
 */
public class Project {

    private final String mTitle;
    private final String mDescription;

    public Project(String title, String description) {
        mTitle = title;
        mDescription = description;
    }

    /**
     * Get the project title
     * @return
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Get the project description
     * @return
     */
    public String getDescription() {
        return mDescription;
    }
}
