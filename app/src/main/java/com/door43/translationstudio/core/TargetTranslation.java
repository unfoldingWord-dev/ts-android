package com.door43.translationstudio.core;

import com.door43.util.Manifest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/**
 * Created by joel on 8/29/2015.
 */
public class TargetTranslation {
    private final String mTargetLanguageId;
    private final String mProjectId;
    private static final String GLOBAL_PROJECT_ID = "uw";
    private final File mTranslationDirectory;
    private final Manifest mManifest;

    public TargetTranslation(String targetLanguageId, String projectId, File translationDirectory) {
        mTargetLanguageId = targetLanguageId;
        mProjectId = projectId;
        mTranslationDirectory = translationDirectory;
        mManifest = Manifest.generate(translationDirectory);
    }

    /**
     * Returns the id of the target translation
     * @return
     */
    public String getId() {
        return generateTargetTranslationId(mTargetLanguageId, mProjectId);
    }

    /**
     * Returns the id of the project being translated
     * @return
     */
    public String getProjectId() {
        return mProjectId;
    }

    /**
     * Returns the id of the target language the project is being translated into
     * @return
     */
    public String getTargetLanguageId() {
        return mTargetLanguageId;
    }

    /**
     * Creates a new target translation
     *
     * If the target translation already exists the existing one will be returned
     *
     * @param targetLanguage the target language the project will be translated into
     * @param project the project that will be translated
     * @param mRootDir the parent directory in which the target translation directory will be created
     * @return
     */
    public static TargetTranslation generate(TargetLanguage targetLanguage, Project project, File mRootDir) throws Exception {
        // create folder
        String targetTranslationId = generateTargetTranslationId(targetLanguage.getId(), project.getId());
        File translationDir = new File(mRootDir, targetTranslationId);

        if(!translationDir.exists()) {
            // build new manifest
            Manifest manifest = Manifest.generate(translationDir);
            manifest.put("slug", project.getId());
            JSONObject targetLangaugeJson = new JSONObject();
            targetLangaugeJson.put("direction", targetLanguage.direction.toString());
            targetLangaugeJson.put("slug", targetLanguage.code);
            targetLangaugeJson.put("name", targetLanguage.name);
            // TODO: we should restructure this output to match what we see in the api.
            // also the target language should have a toJson method that will do all of this.
            manifest.put("target_language", targetLangaugeJson);
        }

        return new TargetTranslation(targetLanguage.getId(), project.getId(), translationDir);
    }

    /**
     * Returns a properly formatted target language id
     * @param targetLanguageId
     * @param projectId
     * @return
     */
    private static String generateTargetTranslationId(String targetLanguageId, String projectId) {
        return GLOBAL_PROJECT_ID + "-" + projectId + "-" + targetLanguageId;
    }
}
