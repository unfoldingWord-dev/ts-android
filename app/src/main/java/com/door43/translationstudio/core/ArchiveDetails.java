package com.door43.translationstudio.core;

import android.content.Context;
import android.support.v4.provider.DocumentFile;

import com.door43.util.Zip;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds details about the translation archive
 * TODO: this duplicates a lot of code from ArchiveImporter. Eventually it might be nice to refactor both so that there is less duplication.
 */
public class ArchiveDetails {
    public static final String MANIFEST_JSON = "manifest.json";
    public static final String PACKAGE_VERSION = "package_version";
    public final long createdAt;
    public final TargetTranslationDetails[] targetTranslationDetails;

    /**
     * Creates a new instance of the archive details
     * @param timestamp
     */
    private ArchiveDetails(long timestamp, TargetTranslationDetails[] targetTranslationDetails) {
        this.createdAt = timestamp;
        this.targetTranslationDetails = targetTranslationDetails;
    }

    /**
     * Reads the details from a translationStudio archive
     * @param archiveStream
     * @param preferredLocale
     * @param library
     * @return
     * @throws Exception
     */
    public static ArchiveDetails newInstance(InputStream archiveStream, String preferredLocale, Library library) throws Exception {
        if(archiveStream != null) {
            File tempFile = File.createTempFile("targettranslation", "." + Translator.ARCHIVE_EXTENSION);
            FileUtils.copyInputStreamToFile(archiveStream, tempFile);

            String rawManifest = Zip.read(tempFile, MANIFEST_JSON);
            if (rawManifest != null) {
                JSONObject json = new JSONObject(rawManifest);
                if (json.has(PACKAGE_VERSION)) {
                    int manifestVersion = json.getInt(PACKAGE_VERSION);
                    switch (manifestVersion) {
                        case 1:
                            return parseV1Manifest(json);
                        case 2:
                            return parseV2Manifest(new FileInputStream(tempFile), json, preferredLocale, library);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Reads the details from a translationStudio archive
     * @param archive
     * @return
     * @throws IOException
     */
    public static ArchiveDetails newInstance(File archive, String preferredLocale, Library library) throws Exception {
        if(archive != null && archive.exists()) {
            String rawManifest = Zip.read(archive, MANIFEST_JSON);
            if(rawManifest != null) {
                JSONObject json = new JSONObject(rawManifest);
                if(json.has(PACKAGE_VERSION)) {
                    int manifestVersion = json.getInt(PACKAGE_VERSION);
                    switch (manifestVersion) {
                        case 1:
                            return parseV1Manifest(json);
                        case 2:
                            return parseV2Manifest(new FileInputStream(archive), json, preferredLocale, library);
                    }
                }
            }
            return null;
        } else {
            return null;
        }
    }

    /**
     * Reads the details from a translationStudio archive
     * @param archive
     * @return
     * @throws IOException
     */
    public static ArchiveDetails newInstance(Context context, DocumentFile archive, String preferredLocale, Library library) throws Exception {
        if(archive != null && archive.exists()) {
            InputStream ais = context.getContentResolver().openInputStream(archive.getUri());
            String rawManifest = Zip.readInputStream(ais, MANIFEST_JSON);
            if (rawManifest != null) {
                JSONObject json = new JSONObject(rawManifest);
                if (json.has(PACKAGE_VERSION)) {
                    int manifestVersion = json.getInt(PACKAGE_VERSION);
                    switch (manifestVersion) {
                        case 1:
                            return parseV1Manifest(json);
                        case 2:
                            return parseV2Manifest(context.getContentResolver().openInputStream(archive.getUri()), json, preferredLocale, library);
                    }
                }
            }
            return null;
        } else {
            return null;
        }
    }

    private static ArchiveDetails parseV1Manifest(JSONObject json) {
        return null;
    }

    private static ArchiveDetails parseV2Manifest(InputStream ais, JSONObject archiveManifest, String preferredLocale, Library library) throws JSONException, IOException {
        List<TargetTranslationDetails> targetDetails = new ArrayList<>();
        long timestamp = archiveManifest.getLong("timestamp");
        JSONArray translationsJson = archiveManifest.getJSONArray("target_translations");
        for(int i = 0; i < translationsJson.length(); i ++) {
            JSONObject translationRecordJson = translationsJson.getJSONObject(i);
            String path = translationRecordJson.getString("path");
            String rawTranslationManifest = Zip.readInputStream(ais, path.replaceAll("/+$", "") + "/manifest.json");
            if(rawTranslationManifest != null) {
                JSONObject manifest = new JSONObject(rawTranslationManifest);

                // migrate the manifest
                manifest = TargetTranslationMigrator.migrateManifest(manifest);

                if (manifest != null) {
                    JSONObject targetLanguageJson = manifest.getJSONObject("target_language");
                    JSONObject projectJson = manifest.getJSONObject("project");

                    // get target language
                    String targetLanguageName = null;
                    String targetLanguageSlug = targetLanguageJson.getString("id");
                    LanguageDirection targetLangaugeDirection = LanguageDirection.get(targetLanguageJson.getString("direction"));
                    if (targetLangaugeDirection == null) {
                        targetLangaugeDirection = LanguageDirection.LeftToRight;
                    }
                    TargetLanguage tl = library.getTargetLanguage(targetLanguageSlug);
                    if (tl != null) {
                        targetLanguageName = tl.name;
                    } else {
                        targetLanguageName = targetLanguageSlug.toUpperCase();
                    }

                    // get project
                    String projectName = null;
                    String projectSlug = projectJson.getString("id");
                    Project project = library.getProject(projectSlug, preferredLocale);
                    if (project != null) {
                        projectName = project.name;
                    } else {
                        projectName = projectSlug.toUpperCase();
                    }

                    // git commit hash
                    String commit = translationRecordJson.getString("commit_hash");

                    // translation type
                    TranslationType translationType = TranslationType.get(manifest.getJSONObject("type").getString("id"));
                    if (translationType == null) {
                        translationType = TranslationType.TEXT;
                    }

                    // resource
                    String resourceSlug = null;
                    if (manifest.has("resource")) {
                        resourceSlug = manifest.getJSONObject("resource").getString("id");
                    }

                    // build id
                    String targetTranslationId = TargetTranslation.generateTargetTranslationId(targetLanguageSlug, projectSlug, translationType, resourceSlug);

                    targetDetails.add(new TargetTranslationDetails(targetTranslationId, targetLanguageSlug, targetLanguageName, projectSlug, projectName, targetLangaugeDirection, commit));
                }
            }
        }
        ais.close();
        return new ArchiveDetails(timestamp, targetDetails.toArray(new TargetTranslationDetails[targetDetails.size()]));
    }

    /**
     * Returns an empty archive
     * @return
     */
    public static ArchiveDetails newDummyInstance() {
        return new ArchiveDetails(0, new TargetTranslationDetails[0]);
    }

    /**
     * Contains details about a target translation in the archive
     */
    public static class TargetTranslationDetails {
        public final String targetTranslationSlug;
        public final String targetLanguageName;
        public final String projectSlug;
        public final String targetLanguageSlug;
        public final String projectName;
        public final LanguageDirection direction;
        public final String commitHash;

        /**
         * Creates a new instance of the archive details
         * @param targetTranslationSlug
         * @param targetLanguageSlug
         * @param targetLanguageName
         * @param projectSlug
         * @param projectName
         */
        private TargetTranslationDetails(String targetTranslationSlug, String targetLanguageSlug, String targetLanguageName, String projectSlug, String projectName, LanguageDirection direction, String commitHash) {
            this.targetTranslationSlug = targetTranslationSlug;
            this.targetLanguageName = targetLanguageName;
            this.projectSlug = projectSlug;
            this.targetLanguageSlug = targetLanguageSlug;
            this.projectName = projectName;
            this.direction = direction;
            this.commitHash = commitHash;
        }
    }
}
