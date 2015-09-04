package com.door43.translationstudio.core;

import android.content.SharedPreferences;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 8/29/2015.
 */
public class Translator {
    private final File mRootDir;

    public Translator(File rootDir) {
        mRootDir = rootDir;
    }

    /**
     * Returns the translation that was last opened
     * @return
     */
    public TargetTranslation getLastTargetTranslation() {
        return null;
    }

    /**
     * Returns an array of all active translations
     * @return
     */
    public TargetTranslation[] getTargetTranslations() {
        final List<TargetTranslation> translations = new ArrayList<>();
        mRootDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                File file = new File(dir, filename);
                if(file.isDirectory()) {
                    String[] complexId = filename.split("-", 3);
                    if(complexId.length == 3) {
                        String projectId = complexId[1];
                        String targetLanguageId = complexId[2];
                        translations.add(new TargetTranslation(targetLanguageId, projectId, file));
                    }
                }
                return false;
            }
        });

        return translations.toArray(new TargetTranslation[translations.size()]);
    }

    /**
     * Initializes a new target translation
     * @param targetLanguage the target language the project will be translated into
     * @param project the project that will be translated
     * @return
     */
    public TargetTranslation createTargetTranslation(TargetLanguage targetLanguage, Project project) {
        try {
            return TargetTranslation.generate(targetLanguage, project, mRootDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

}
