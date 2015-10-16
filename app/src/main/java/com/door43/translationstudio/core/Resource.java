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
        int sourceModified = getDateFromUrl(sourceCatalog);

        String notesCatalog = "";
        int notesModified = 0;
        if(json.has("notes")) {
            notesCatalog = json.getString("notes");
            notesModified = getDateFromUrl(notesCatalog);
        }

        String wordsCatalog = "";
        int wordsModified = 0;
        if(json.has("terms")) {
            wordsCatalog = json.getString("terms");
            wordsModified = getDateFromUrl(wordsCatalog);
        }

        String questionsCatalog = "";
        int questionsDateModified = 0;
        if(json.has("checking_questions")) {
            questionsCatalog = json.getString("checking_questions");
            questionsDateModified = getDateFromUrl(questionsCatalog);
        }

        String termAssignmentsCatalog = "";
        int termAssignmentsModified = 0;
        if(json.has("tw_cat")) {
            termAssignmentsCatalog = json.getString("tw_cat");
            termAssignmentsModified = getDateFromUrl(termAssignmentsCatalog);
        }

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
                wordsCatalog,
                wordsModified,
                termAssignmentsCatalog,
                termAssignmentsModified,
                questionsCatalog,
                questionsDateModified
        );
    }

    /**
     * Returns the date_modified from a url
     * @param url
     * @return returns 0 if the date could not be parsed
     */
    private static int getDateFromUrl(String url) {
        String[] pieces = url.split("\\?");
        if(pieces.length > 1) {
            // date_modified=123456
            String attribute = pieces[1];
            pieces = attribute.split("=");
            if(pieces.length > 1) {
                try {
                    return Integer.parseInt(pieces[1]);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    return 0;
                }
            }
        }
        return 0;
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
