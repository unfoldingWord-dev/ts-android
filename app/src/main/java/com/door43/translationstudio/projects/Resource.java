package com.door43.translationstudio.projects;

import org.json.JSONObject;

/**
 * Resources are content that is available for translation.
 * A resource is actually comprised of several different items: source, notes, and terms.
 */
public class Resource {
    private final String mSlug;
    private final String mName;
    private final int mCheckingLevel;
    private int mDateModified;
    private String mNotesUrl;
    private String mSourceUrl;
    private String mTermsUrl;

    // resources may have custom urls from which source, terms, and notes are retrieved
//    private String mSourceUrl;
//    private String mTermsUrl;
//    private String mNotesUrl;

    public Resource(String slug, String name, int checkingLevel, int dateModified) {
        mSlug = slug;
        mName = name;
        mCheckingLevel = checkingLevel;
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
     * Returns the checking level of this resource
     * @return
     */
    public int getCheckingLevel() {
        return mCheckingLevel;
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
//    public String getSourceUrl() {
//        if(mSourceUrl != null && !mSourceUrl.isEmpty()) {
//            return mSourceUrl;
//        } else {
//            return null;
//        }
//    }

    /**
     * Returns the url to the terms or null
     * @return
     */
//    public String getTermsUrl() {
//        if(mTermsUrl != null && !mTermsUrl.isEmpty()) {
//            return mTermsUrl;
//        } else {
//            return null;
//        }
//    }

    /**
     * Returns the url to the terms or null
     * @return
     */
//    public String getNotesUrl() {
//        if(mNotesUrl != null && !mNotesUrl.isEmpty()) {
//            return mNotesUrl;
//        } else {
//            return null;
//        }
//    }

    @Override
    public String toString() {
        return mName;
    }

    /**
     * Update the date the resource was last modified
     * @param dateModified
     */
    public void setDateModified(int dateModified) {
        mDateModified = dateModified;
    }

    /**
     * Generates a new resource object from json
     * @param json
     * @return
     */
    public static Resource generate(JSONObject json) {
        try {
            JSONObject jsonStatus = json.getJSONObject("status");
            Resource r = new Resource(json.getString("slug"), json.getString("name"), jsonStatus.getInt("checking_level"), json.getInt("date_modified"));
            if(json.has("notes")) {
                r.setNotesCatalog(json.getString("notes"));
            }
            if(json.has("source")) {
                r.setSourceCatalog(json.getString("source"));
            }
             if(json.has("terms")) {
                 r.setTermsCatalog(json.getString("terms"));
             }
            return r;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Sets the url to the terms catalog
     * @param termsUrl
     */
    private void setTermsCatalog(String termsUrl) {
        mTermsUrl = termsUrl;
    }

    /**
     * Sets the url to the notes catalog
     * @param notesUrl
     */
    private void setNotesCatalog(String notesUrl) {
        mNotesUrl = notesUrl;
    }

    /**
     * Sets the url to the source catalog
     * @param sourceUrl
     */
    public void setSourceCatalog(String sourceUrl) {
        mSourceUrl = sourceUrl;
    }

    /**
     * Returns the url to the terms catalog.
     * @return
     */
    public String getTermsCatalog() {
        return mTermsUrl;
    }

    /**
     * Returns the url to the notes catalog
     * @return
     */
    public String getNotesCatalog() {
        return mNotesUrl;
    }

    /**
     * Returns the url to the source catalog
     * @return
     */
    public String getSourceCatalog() {
        return mSourceUrl;
    }
}
