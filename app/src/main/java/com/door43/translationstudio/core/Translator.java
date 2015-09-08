package com.door43.translationstudio.core;

import org.apache.commons.io.FileUtils;

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
                TargetTranslation translation = getTargetTranslation(filename);
                if(translation != null) {
                    translations.add(translation);
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
        if(targetTranslationId != null) {
            try {
                String projectId = TargetTranslation.getProjectIdFromId(targetTranslationId);
                String targetLanguageId = TargetTranslation.getTargetLanguageIdFromId(targetTranslationId);
                return getTargetTranslation(targetLanguageId, projectId);
            } catch (StringIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Deletes a target translation
     * @param targetTranslationId
     */
    public void deleteTargetTranslation(String targetTranslationId) {
        if(targetTranslationId != null) {
            try {
                String projectId = TargetTranslation.getProjectIdFromId(targetTranslationId);
                String targetLanguageId = TargetTranslation.getTargetLanguageIdFromId(targetTranslationId);
                File dir = TargetTranslation.generateTargetTranslationDir(targetLanguageId, projectId, mRootDir);
                FileUtils.deleteQuietly(dir);
            } catch (StringIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    }
}
