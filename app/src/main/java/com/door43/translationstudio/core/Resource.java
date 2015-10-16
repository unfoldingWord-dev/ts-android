package com.door43.translationstudio.core;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by joel on 9/10/2015.
 */
public class Resource {
    private final int mCheckingLevel;
    private final String mTitle;
    private final String mId;
    private final String mVersion;
    private final int mDateModified;
    private final String sourceCatalogUrl;
    private final int sourceDateModified;
    private final String notesCatalogUrl;
    private final int notesDateModified;
    private final String termsCatalogUrl;
    private final int termsDateModified;
    private final String termAssignmentsCatalogUrl;
    private final int termAssignmentsDateModified;
    private final String questionsCatalogUrl;
    private final int questionsDateModified;

    private Resource(String name, String slug, int checkingLevel, String version, int dateModified,
                     String sourceCatalogUrl, int sourceDateModified, String notesCatalogUrl, int notesDateModified,
                     String termsCatalogUrl, int termsDateModified, String termAssignmentsCatalogUrl,
                     int termAssignmentsDateModified, String questionsCatalogUrl, int questionsDateModified) {
        mTitle = name;
        mId = slug;
        mCheckingLevel = checkingLevel;
        mVersion = version;
        mDateModified = dateModified;

        this.sourceCatalogUrl = sourceCatalogUrl;
        this.sourceDateModified = sourceDateModified;
        this.notesCatalogUrl = notesCatalogUrl;
        this.notesDateModified = notesDateModified;
        this.termsCatalogUrl = termsCatalogUrl;
        this.termsDateModified = termsDateModified;
        this.termAssignmentsCatalogUrl = termAssignmentsCatalogUrl;
        this.termAssignmentsDateModified = termAssignmentsDateModified;
        this.questionsCatalogUrl = questionsCatalogUrl;
        this.questionsDateModified = questionsDateModified;
    }

    /**
     * Generates a new resource from json
     * @param json
     * @return
     * @throws JSONException
     */
    public static Resource generate(JSONObject json) throws Exception {
        if(json == null) {
            return null;
        }
        JSONObject statusJson = json.getJSONObject("status");
        String sourceCatalog = json.getString("source");
        int sourceModified = Integer.parseInt(sourceCatalog.split("\\?")[1]);
        String notesCatalog = json.getString("notes");
        int notesModified = Integer.parseInt(notesCatalog.split("\\?")[1]);
        String termsCatalog = json.getString("terms");
        int termsModified = Integer.parseInt(termsCatalog.split("\\?")[1]);
        String questionsCatalog = json.getString("checking_questions");
        int questiosnModified = Integer.parseInt(questionsCatalog.split("\\?")[1]);
        String termAssignmentsCatalog = json.getString("tw_cat");
        int termAssignmentsModified = Integer.parseInt(termAssignmentsCatalog.split("\\?")[1]);

        return new Resource(
                json.getString("name"),
                json.getString("slug"),
                statusJson.getInt("checking_level"),
                statusJson.getString("version"),
                json.getInt("date_modified"),
                sourceCatalog,
                sourceModified,
                notesCatalog,
                notesModified,
                termsCatalog,
                termsModified,
                termAssignmentsCatalog,
                termAssignmentsModified,
                questionsCatalog,
                questiosnModified
        );
    }

    /**
     * Returns the id of the resource
     * @return
     */
    public String getId() {
        return mId;
    }

    /**
     * Returns the checking level of the resource
     * @return
     */
    public int getCheckingLevel() {
        return mCheckingLevel;
    }

    /**
     * Returns the title of the resource
     * @return
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Returns the version of the resource
     * @return
     */
    public String getVersion() {
        return mVersion;
    }

    /**
     * Returns the date the resource was last modified
     * @return
     */
    public int getDateModified() {
        return mDateModified;
    }

    public String getSourceCatalogUrl() {
        return sourceCatalogUrl;
    }

    public int getSourceDateModified() {
        return sourceDateModified;
    }

    public String getNotesCatalogUrl() {
        return notesCatalogUrl;
    }

    public int getNotesDateModified() {
        return notesDateModified;
    }

    public String getWordsCatalogUrl() {
        return termsCatalogUrl;
    }

    public int getWordsDateModified() {
        return termsDateModified;
    }

    public String getWordAssignmentsCatalogUrl() {
        return termAssignmentsCatalogUrl;
    }

    public int getWordAssignmentsDateModified() {
        return termAssignmentsDateModified;
    }

    public String getQuestionsCatalogUrl() {
        return questionsCatalogUrl;
    }

    public int getQuestionsDateModified() {
        return questionsDateModified;
    }
}
