package com.door43.translationstudio.core;

/**
 * Created by joel on 12/2/2015.
 */
@Deprecated
public class TranslationArticle {
    private final String text;
    private final String volume;
    private final String manual;
    private final String slug;
    private final String title;
    private final String reference;
    private long DBId = 0;

    public TranslationArticle(String volume, String manual, String slug, String title, String text, String reference) {
        this.volume = volume;
        this.manual = manual;
        this.slug = slug;
        this.title = title;
        this.text = text;
        this.reference = reference;
    }

    /**
     * Returns the slug of the article
     * @return
     */
    public String getId() {
        return this.slug;
    }

    /**
     * Returns the title of the article
     * @return
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the database id of the article
     * @param DBId
     */
    public void setDBId(long DBId) {
        this.DBId = DBId;
    }

    /**
     * Returns the database id of the article
     * @return
     */
    public long getDBId() {
        return DBId;
    }

    /**
     * Returns the body of the article
     * @return
     */
    public String getBody() {
        return text;
    }

    public String getManual() {
        return manual;
    }

    public String getVolume() {
        return volume;
    }

    public String getReference() {
        return reference;
    }
}
