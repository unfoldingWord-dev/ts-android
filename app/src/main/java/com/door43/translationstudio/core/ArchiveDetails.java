package com.door43.translationstudio.core;

import android.content.ContentResolver;
import android.content.Context;
import android.support.v4.provider.DocumentFile;

import com.door43.util.Zip;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds details about the translation archive
 * TODO: this duplicates a lot of code from ArchiveImporter. Eventually it might be nice to refactor both so that there is less duplication.
 */
public class ArchiveDetails {
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
     * @param archive
     * @return
     * @throws IOException
     */
    public static ArchiveDetails newInstance(File archive, String preferredLocale, Library library) throws Exception {
        if(archive != null && archive.exists()) {
            String rawManifest = Zip.read(archive, "manifest.json");
            if(rawManifest != null) {
                JSONObject json = new JSONObject(rawManifest);
                if(json.has("package_version")) {
                    int manifestVersion = json.getInt("package_version");
                    switch (manifestVersion) {
                        case 1:
                            return parseV1Manifest(json);
                        case 2:
                            return parseV2Manifest(json, preferredLocale, library);
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
            String rawManifest = Zip.readInputStream(context.getContentResolver().openInputStream(archive.getUri()), "manifest.json");
            if(rawManifest != null) {
                JSONObject json = new JSONObject(rawManifest);
                if(json.has("package_version")) {
                    int manifestVersion = json.getInt("package_version");
                    switch (manifestVersion) {
                        case 1:
                            return parseV1Manifest(json);
                        case 2:
                            return parseV2Manifest(json, preferredLocale, library);
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

    private static ArchiveDetails parseV2Manifest(JSONObject manifest, String preferredLocale, Library library) throws JSONException {
        List<TargetTranslationDetails> targetDetails = new ArrayList<>();
        long timestamp = manifest.getLong("timestamp");
        JSONArray translationsJson = manifest.getJSONArray("target_translations");
        for(int i = 0; i < translationsJson.length(); i ++) {
            JSONObject json = translationsJson.getJSONObject(i);
            String targetTranslationId = json.getString("id");
            String targetLanguageName = null;
            if(json.has("target_language_name")) {
                targetLanguageName = json.getString("target_language_name");
            } else {
                TargetLanguage tl = library.getTargetLanguage(TargetTranslation.getTargetLanguageIdFromId(targetTranslationId));
                if(tl != null) {
                    targetLanguageName = tl.name;
                } else {
                    // use the target translation id if nothing else can be found
                    targetLanguageName = targetTranslationId.toUpperCase();
                }
            }
            String targetLanguageSlug = TargetTranslation.getTargetLanguageIdFromId(targetTranslationId);
            String projectSlug = TargetTranslation.getProjectIdFromId(targetTranslationId);
            String projectName = null;
            if(json.has("project_name")) {
                projectName = json.getString("project_name");
            } else {
                Project project = library.getProject(projectSlug, preferredLocale);
                if(project != null) {
                    projectName = project.name;
                } else {
                    projectName = projectSlug;
                }
            }
            String directionString = json.getString("direction");
            String commit = json.getString("commit_hash");
            LanguageDirection direction = LanguageDirection.get(directionString);
            if(direction == null) {
                direction = LanguageDirection.LeftToRight;
            }
            targetDetails.add(new TargetTranslationDetails(targetTranslationId, targetLanguageSlug, targetLanguageName, projectSlug, projectName, direction, commit));
        }
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
