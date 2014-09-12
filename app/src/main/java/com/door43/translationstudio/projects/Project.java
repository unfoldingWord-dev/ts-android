package com.door43.translationstudio.projects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Projects are translation containers. They may also be called "Books".
 * Projects will first look for information (by slug) within the installed package, and will augment
 * any existing/missing information with data found on the server (requires internet connection).
 */
public class Project {

    private Map<Integer,Chapter> mChapters = new HashMap<Integer, Chapter>();
    private final String mTitle;
    private final String mSlug;
    private final String mDescription;
    private int mSelectedChapter;

    /**
     * Create a new project definition
     * @param title The human readable title of the project.
     * @param slug The machine readable slug identifying the project.
     * @param description A short description of the project.
     */
    public Project(String title, String slug, String description) {
        mTitle = title;
        mSlug = slug;
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
     * Get the project slug
     * @return
     */
    public String getSlug() {
        return mSlug;
    }

    /**
     * Get the project description
     * @return
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     * Returns the number of chapters in this project
     * @return
     */
    public int numChapters() {
        return mChapters.size();
    }

    /**
     * Get a chapter by index
     * @param id the id of the chapter starting at 0
     * @return
     */
    public Chapter getChapter(Integer id) {
        if(mChapters.containsKey(id)) {
            return mChapters.get(id);
        } else {
            // out of bounds
            return null;
        }
    }

    /**
     * Sets the currently selected chapter in the application
     * @param id
     * @return boolean return true of the index is valid
     */
    public boolean setSelectedChapter(Integer id) {
        if (mChapters.containsKey(id)) {
            mSelectedChapter = id;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Gets the currently selected chapter in the application
     * @return
     */
    public Chapter getSelectedChapter() {
        Chapter selectedChapter = getChapter(mSelectedChapter);
        if(selectedChapter == null) {
            // atuo select the first project if no other project has been selected yet.
            Integer key = (Integer) getChaptersKeySet().get(0);
            setSelectedChapter(key);
            return getChapter(key);
        } else {
            return selectedChapter;
        }
    }

    /**
     * Adds a chapter to the project
     * @param c the chapter to add
     * @return
     */
    public Chapter addChapter(Chapter c) {
        if(!this.mChapters.containsKey(c.getId())) {
            this.mChapters.put(c.getId(), c);
            return c;
        } else {
            // TODO: is this nessesary? need to double check that the object signatures are different. If they are the same we should just always return the input chapter.
            return getChapter(c.getId());
        }
    }

    /**
     * Returns a keyset of chapter keys so list adapters can use indexes to identify chapters.
     * @return
     */
    public List getChaptersKeySet() {
        return new ArrayList(mChapters.keySet());
    }
}
