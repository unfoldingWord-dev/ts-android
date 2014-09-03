package com.door43.translationstudio;

/**
 * Created by joel on 9/2/2014.
 */
public class Chapter {
    private String mTitle;
    private String mDescription;

    /**
     * Creates a new chapter.
     * TODO: may need to define chapter number here rather than rely on order of add
     * @param title the human readable title of the chapter
     * @param description a short description of the chapter
     */
    public Chapter(String title, String description) {
        mTitle = title;
        mDescription = description;
    }

    /**
     * Get the chapter title
     * @return
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Get the chapter description
     * @return
     */
    public String getDescription() {
        return mDescription;
    }
}
