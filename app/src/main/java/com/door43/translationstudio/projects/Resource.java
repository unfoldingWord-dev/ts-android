package com.door43.translationstudio.projects;

/**
 * Resources are content that is available for translation.
 * A resource is actually comprised of several different items: source, notes, and terms.
 */
public class Resource {
    private final String mSlug;
    private final String mName;
    private final int mDateModified;

    // resources may have custom urls from which source, terms, and notes are retrieved
    private String mSourceUrl;
    private String mTermsUrl;
    private String mNotesUrl;

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

    /**
     * Returns the url to the source or null
     * @return
     */
    public String getSourceUrl() {
        if(mSourceUrl != null && !mSourceUrl.isEmpty()) {
            return mSourceUrl;
        } else {
            return null;
        }
    }

    /**
     * Returns the url to the terms or null
     * @return
     */
    public String getTermsUrl() {
        if(mTermsUrl != null && !mTermsUrl.isEmpty()) {
            return mTermsUrl;
        } else {
            return null;
        }
    }

    /**
     * Returns the url to the terms or null
     * @return
     */
    public String getNotesUrl() {
        if(mNotesUrl != null && !mNotesUrl.isEmpty()) {
            return mNotesUrl;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return mName;
    }
}
