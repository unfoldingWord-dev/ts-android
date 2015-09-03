package com.door43.translationstudio.core;

import android.content.SharedPreferences;

import java.io.File;

/**
 * Created by joel on 8/29/2015.
 */
public class Translator {
    private final SharedPreferences mPreferences;
    private final File mRootDir;

    public Translator(SharedPreferences preferences, File rootDir) {
        mPreferences = preferences;
        mRootDir = rootDir;
    }

    /**
     * Returns the translation that was last opened
     * @return
     */
    public TargetTranslation getLastTranslation() {
        return null;
    }

    /**
     * Returns an array of all active translations
     * @return
     */
    public TargetTranslation[] getTranslations() {
        return null;
    }

    /**
     * Returns a project
     * @param translation
     * @return
     */
    public Project getProject(SourceTranslation translation) {
        return null;
    }

    /**
     * Returns the list of all available projects
     * These projects are loaded with their source language set to one of the preferred locals
     * so that the user can read them
     *
     * @param preferredLocales
     * @return
     */
    public Project[] getProjects(String[] preferredLocales) {
        return null;
    }

    /**
     * Returns a target language
     * @param targetLanguageId
     * @return
     */
    public TargetLanguage getTargetLanguage(String targetLanguageId) {
        return null;
    }

    /**
     * Returns an array of all available target languages
     * @return
     */
    public TargetLanguage[] getTargetLanguages() {
        return null;
    }

    /**
     * Returns all the source languages available for the project
     * @param project
     * @return
     */
    public SourceLanguage[] getSourceLanguages(Project project) {
        return null;
    }
}
