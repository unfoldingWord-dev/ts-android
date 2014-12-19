package com.door43.translationstudio.projects;

/**
 * Resources are content that is available for translation.
 * A resource is actually comprised of several different items: source, notes, and terms.
 */
public class Resource {
    private final String mSlug;
    private final String mName;
    private final int mDateModified;

    public Resource(String slug, String name, int dateModified) {
        mSlug = slug;
        mName = name;
        mDateModified = dateModified;
    }

    /**
     * Returns the resource slug
     * @return
     */
    public String getId() {
        return mSlug;
    }

    /**
     * Returns the translated name of the resource
     * @return
     */
    public String getName() {
        return mName;
    }

    /**
     * Returns the date when the resource was last modified.
     * @return
     */
    public int getDateModified() {
        return mDateModified;
    }
}
