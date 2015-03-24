package com.door43.translationstudio.projects;

import android.net.Uri;

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
    private Uri mNotesUri = null;
    private Uri mSourceUri = null;
    private Uri mTermsUri = null;

    // resources may have custom urls from which source, terms, and notes are retrieved
//    private String mSourceUri;
//    private String mTermsUri;
//    private String mNotesUri;

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
        if(termsUrl != null) {
            mTermsUri = Uri.parse(termsUrl);
        } else {
            mTermsUri = null;
        }
    }

    /**
     * Sets the url to the notes catalog
     * @param notesUrl
     */
    private void setNotesCatalog(String notesUrl) {
        if(notesUrl != null) {
            mNotesUri = Uri.parse(notesUrl);
        } else {
            mNotesUri = null;
        }
    }

    /**
     * Sets the url to the source catalog
     * @param sourceUrl
     */
    public void setSourceCatalog(String sourceUrl) {
        if(sourceUrl != null) {
            mSourceUri = Uri.parse(sourceUrl);
        } else {
            mSourceUri = null;
        }
    }

    /**
     * Returns the url to the terms catalog.
     * @return
     */
    public Uri getTermsCatalog() {
        return mTermsUri;
    }

    /**
     * Returns the url to the notes catalog
     * @return
     */
    public Uri getNotesCatalog() {
        return mNotesUri;
    }

    /**
     * Returns the url to the source catalog
     * @return
     */
    public Uri getSourceCatalog() {
        return mSourceUri;
    }
}
