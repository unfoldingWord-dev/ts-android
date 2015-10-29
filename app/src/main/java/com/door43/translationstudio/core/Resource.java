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
    private final int sourceServerDateModified;
    private final int notesServerDateModified;
    private final int termsServerDateModified;
    private final int termAssignmentsServerDateModified;
    private final int questionsServerDateModified;
    private long DBId = -1;
    private boolean isDownloaded;

    public Resource(String name, String slug, int checkingLevel, String version, boolean isDownloaded, int dateModified,
                    String sourceCatalogUrl, int sourceDateModified, int sourceServerDateModified,
                    String notesCatalogUrl, int notesDateModified, int notesServerDateModified,
                    String termsCatalogUrl, int termsDateModified, int termsServerDateModified,
                    String termAssignmentsCatalogUrl, int termAssignmentsDateModified, int termAssignmentsServerDateModified,
                    String questionsCatalogUrl, int questionsDateModified, int questionsServerDateModified) {
        mTitle = name;
        mId = slug;
        mCheckingLevel = checkingLevel;
        mVersion = version;
        mDateModified = dateModified;
        this.isDownloaded = isDownloaded;

        this.sourceCatalogUrl = sourceCatalogUrl;
        this.sourceDateModified = sourceDateModified;
        this.sourceServerDateModified = sourceServerDateModified;

        this.notesCatalogUrl = notesCatalogUrl;
        this.notesDateModified = notesDateModified;
        this.notesServerDateModified = notesServerDateModified;

        this.termsCatalogUrl = termsCatalogUrl;
        this.termsDateModified = termsDateModified;
        this.termsServerDateModified = termsServerDateModified;

        this.termAssignmentsCatalogUrl = termAssignmentsCatalogUrl;
        this.termAssignmentsDateModified = termAssignmentsDateModified;
        this.termAssignmentsServerDateModified = termAssignmentsServerDateModified;

        this.questionsCatalogUrl = questionsCatalogUrl;
        this.questionsDateModified = questionsDateModified;
        this.questionsServerDateModified = questionsServerDateModified;
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
        int sourceModified = Util.getDateFromUrl(sourceCatalog);

        String notesCatalog = "";
        int notesModified = 0;
        if(json.has("notes")) {
            notesCatalog = json.getString("notes");
            notesModified = Util.getDateFromUrl(notesCatalog);
        }

        String wordsCatalog = "";
        int wordsModified = 0;
        if(json.has("terms")) {
            wordsCatalog = json.getString("terms");
            wordsModified = Util.getDateFromUrl(wordsCatalog);
        }

        String questionsCatalog = "";
        int questionsDateModified = 0;
        if(json.has("checking_questions")) {
            questionsCatalog = json.getString("checking_questions");
            questionsDateModified = Util.getDateFromUrl(questionsCatalog);
        }

        String termAssignmentsCatalog = "";
        int termAssignmentsModified = 0;
        if(json.has("tw_cat")) {
            termAssignmentsCatalog = json.getString("tw_cat");
            termAssignmentsModified = Util.getDateFromUrl(termAssignmentsCatalog);
        }

        return new Resource(
                json.getString("name"),
                json.getString("slug"),
                statusJson.getInt("checking_level"),
                statusJson.getString("version"),
                false,
                json.getInt("date_modified"),
                sourceCatalog,
                sourceModified,
                0,
                notesCatalog,
                notesModified,
                0,
                wordsCatalog,
                wordsModified,
                0,
                termAssignmentsCatalog,
                termAssignmentsModified,
                0,
                questionsCatalog,
                questionsDateModified,
                0
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
     * Returns the date the resource was last modified.
     * The local date modified is always the lowest date modified available
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

    public int getSourceServerDateModified() {
        return sourceServerDateModified;
    }

    public int getNotesServerDateModified() {
        return notesServerDateModified;
    }

    public int getTermsServerDateModified() {
        return termsServerDateModified;
    }

    public int getTermAssignmentsServerDateModified() {
        return termAssignmentsServerDateModified;
    }

    public int getQuestionsServerDateModified() {
        return questionsServerDateModified;
    }

    /**
     * Checks if updates are available for this resource on the server
     * @return
     */
    public boolean hasUpdates() {
        boolean hasUpdates = sourceDateModified < sourceServerDateModified;

        if(notesCatalogUrl != null && !notesCatalogUrl.isEmpty()) {
            hasUpdates = notesDateModified < notesServerDateModified ? true : hasUpdates;
        }
        if(questionsCatalogUrl != null && !questionsCatalogUrl.isEmpty()) {
            hasUpdates = questionsDateModified < questionsServerDateModified ? true : hasUpdates;
        }
        if(termsCatalogUrl != null && !termsCatalogUrl.isEmpty()) {
            hasUpdates = termsDateModified < termsServerDateModified ? true : hasUpdates;
        }
        if(termAssignmentsCatalogUrl != null && !termAssignmentsCatalogUrl.isEmpty()) {
            hasUpdates = termAssignmentsDateModified < termAssignmentsServerDateModified ? true : hasUpdates;
        }
        return hasUpdates;
    }

    /**
     * Checks if the content for this resource has been downloaded
     * @return
     */
    public boolean isDownloaded() {
        return this.isDownloaded;
    }

    public void setDBId(long DBId) {
        this.DBId = DBId;
    }

    public long getDBId() {
        return this.DBId;
    }
}
