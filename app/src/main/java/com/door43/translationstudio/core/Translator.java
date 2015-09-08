package com.door43.translationstudio.core;

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
     * @param projectId the id of the project that will be translated
     * @return
     */
    public TargetTranslation createTargetTranslation(TargetLanguage targetLanguage, String projectId) {
        try {
            return TargetTranslation.generate(targetLanguage, projectId, mRootDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns a target translation if it exists
     * @param targetLanguageId
     * @param projectId
     * @return
     */
    public TargetTranslation getTargetTranslation(String targetLanguageId, String projectId) {
        File dir = TargetTranslation.generateTargetTranslationDir(targetLanguageId, projectId, mRootDir);
        if(dir.exists()) {
            return new TargetTranslation(targetLanguageId, projectId, mRootDir);
        } else {
            return null;
        }
    }

    /**
     * Returns a target translation if it exists
     * @param targetTranslationId
     * @return
     */
    public TargetTranslation getTargetTranslation(String targetTranslationId) {
        try {
            String projectId = TargetTranslation.getProjectIdFromId(targetTranslationId);
            String targetLanguageId = TargetTranslation.getTargetLanguageIdFromId(targetTranslationId);
            return getTargetTranslation(targetLanguageId, projectId);
        } catch (StringIndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        return null;
    }
}
