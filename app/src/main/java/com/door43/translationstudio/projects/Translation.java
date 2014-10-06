package com.door43.translationstudio.projects;

/**
 * Represents a piece of translated text
 */
public class Translation {
    private Language mLanguage;
    private String mTranslationText;
    private Boolean mIsSaved;

    /**
     *
     * @param lang The language of the translation
     * @param translationText The text that has been translated
     */
    public Translation(Language lang, String translationText){
        mLanguage = lang;
        mTranslationText = translationText;
        mIsSaved = false;
    }

    /**
     * Returns the language this translation is in
     * @return
     */
    public Language getLanguage() {
        return mLanguage;
    }

    /**
     * Returns the translated text
     * @return
     */
    public String getText() {
        return mTranslationText;
    }

    /**
     * Checks if the translation has been saved
     * @return
     */
    public boolean isSaved() {
        return mIsSaved;
    }

    /**
     * Sets whether or not the translation has been saved.
     * @param saved
     */
    public void isSaved(Boolean saved) {
        mIsSaved = saved;
    }
}
