package com.door43.translationstudio.projects;

/**
 * Represents a piece of translated text
 */
@Deprecated
public class Translation {
    private Language mLanguage;
    private String mTranslationText;
    private Boolean mIsSaved;
    private int mLanguageSessionVersion;

    /**
     *
     * @param lang The language of the translation
     * @param translationText The text that has been translated
     */
    public Translation(Language lang, String translationText){
        mLanguage = lang;
        mLanguageSessionVersion = lang.getSessionVersion();
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
        if(mTranslationText == null) {
            return "";
        } else {
            return mTranslationText;
        }
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

    /**
     * Checks if the translation is in the given language and if the target translation has been updated on the disk
     * @param language
     */
    public boolean isLanguage(Language language) {
        return getLanguage().getId().equals(language.getId()) && mLanguageSessionVersion == language.getSessionVersion();
    }
}
