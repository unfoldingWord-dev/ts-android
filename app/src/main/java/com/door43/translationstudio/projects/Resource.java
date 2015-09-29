package com.door43.translationstudio.projects;

import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Resources are content that is available for translation.
 * A resource is actually comprised of several different items: source, notes, and terms.
 */
@Deprecated
public class Resource {
    private final String mSlug;
    private final String mName;
    private final int mCheckingLevel;
    private final Uri mQuestionsUri;
    private int mDateModified;
    private Uri mNotesUri = null;
    private Uri mSourceUri = null;
    private Uri mTermsUri = null;

    // resources may have custom urls from which source, terms, and notes are retrieved
//    private String mSourceUri;
//    private String mTermsUri;
//    private String mNotesUri;

    protected Resource(String slug, String name, int checkingLevel, int dateModified, String notesUrl, String sourceUrl, String termsUrl, String questionsUrl) {
        mSlug = slug;
        mName = name;
        mCheckingLevel = checkingLevel;
        mDateModified = dateModified;
        if(termsUrl != null) {
            mTermsUri = Uri.parse(termsUrl);
        } else {
            mTermsUri = null;
        }
        if(notesUrl != null) {
            mNotesUri = Uri.parse(notesUrl);
        } else {
            mNotesUri = null;
        }
        if(sourceUrl != null) {
            mSourceUri = Uri.parse(sourceUrl);
        } else {
            mSourceUri = null;
        }
        if(questionsUrl != null) {
            mQuestionsUri = Uri.parse(questionsUrl);
        } else {
            mQuestionsUri = null;
        }
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
            String notesCatalog = null;
            String sourceCatalog = null;
            String termsCatalog = null;
            String questionsCatalog = null;
            if(json.has("notes")) {
                notesCatalog = json.getString("notes");
            }
            if(json.has("source")) {
                sourceCatalog = json.getString("source");
            }
            if(json.has("terms")) {
                termsCatalog = json.getString("terms");
            }
            if(json.has("checking_questions") && !json.getString("checking_questions").isEmpty()) {
                questionsCatalog = json.getString("checking_questions");
            }
            return new Resource(json.getString("slug"), json.getString("name"), jsonStatus.getInt("checking_level"), json.getInt("date_modified"), notesCatalog, sourceCatalog, termsCatalog, questionsCatalog);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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

    /**
     * Returns the url to the questions catalog
     * @return
     */
    public Uri getQuestionsCatalog() {
        return mQuestionsUri;
    }

    /**
     * Serializes the resource
     * @return
     */
    public JSONObject serialize() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("date_modified", mDateModified);
        json.put("name", mName);
        json.put("notes", mNotesUri.toString());
        json.put("slug", mSlug);
        json.put("source", mSourceUri.toString());
        json.put("terms", mTermsUri.toString());
        JSONObject status = new JSONObject();
        status.put("checking_level", mCheckingLevel);
        json.put("status", status);
        return json;
    }
}
