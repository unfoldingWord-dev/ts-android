package com.door43.translationstudio.core;

import com.door43.util.FileUtilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the importing of tstudio archives.
 * The importing is placed here to keep the Translator clean and organized.
 */
public class ArchiveImporter {
    private ArchiveImporter() {

    }

    /**
     * Prepares an archive for import with backwards compatible support.
     * @param expandedArchiveDir
     * @return an array of target translation directories that are ready and valid for import
     * @throws Exception
     */
    public static File[] importArchive(File expandedArchiveDir) throws Exception {
        // retrieve target translations from archive
        File manifestFile = new File(expandedArchiveDir, "manifest.json");
        File[] targetTranslationDirs;
        if(manifestFile.exists()) {
            JSONObject manifestJson = new JSONObject(FileUtilities.readFileToString(manifestFile));
            if(manifestJson.has("package_version")) {
                int packageVersion = manifestJson.getInt("package_version");
                switch (packageVersion) {
                    case 1:
                        targetTranslationDirs = v1(manifestJson, expandedArchiveDir); // just to keep the switch pretty
                        break;
                    case 2:
                        targetTranslationDirs = v2(manifestJson, expandedArchiveDir);
                        break;
                    default:
                        targetTranslationDirs = new File[0];
                }
            } else {
                targetTranslationDirs = v1(manifestJson, expandedArchiveDir);
            }
        } else {
            targetTranslationDirs = legacy(expandedArchiveDir);
        }

        // migrate target translations
        List<File> validTargetTranslations = new ArrayList<>();
        for(File dir:targetTranslationDirs) {
            File migratedDir = TargetTranslationMigrator.migrate(dir);
            if(migratedDir != null) {
                validTargetTranslations.add(migratedDir);
            }
        }
        return validTargetTranslations.toArray(new File[validTargetTranslations.size()]);
    }

    /**
     * translation dirs in the archive are named after their id
     * so we only need to return the path.
     * @param packageManifest
     * @param dir
     * @return
     * @throws JSONException
     */
    private static File[] v2(JSONObject packageManifest, File dir) throws JSONException {
        List<File> files = new ArrayList<>();
        JSONArray translationsJson = packageManifest.getJSONArray("target_translations");
        for(int i = 0; i < translationsJson.length(); i ++) {
            JSONObject translation = translationsJson.getJSONObject(i);
            files.add(new File(dir, translation.getString("path")));
        }
        return files.toArray(new File[files.size()]);
    }

    /**
     * targetTranslations are in directories labled by id
     * @param manifest
     * @param dir
     * @return
     * @throws JSONException
     */
    private static File[] v1(JSONObject manifest, File dir) throws JSONException {
        List<File> files = new ArrayList<>();
        JSONArray translationsJson = manifest.getJSONArray("projects");
        for(int i = 0; i < translationsJson.length(); i ++) {
            JSONObject translation = translationsJson.getJSONObject(i);
            files.add(new File(dir, translation.getString("path")));
        }
        return files.toArray(new File[files.size()]);
    }

    /**
     * todo: provide support for legacy archives.. if needed
     * @return
     */
    private static File[] legacy(File dir) {
//        String[] translationDirs = dir.list();
//        for(String targetTranslationId:translationDirs) {
//            targetTranslationId = StringUtilities.ltrim(targetTranslationId, '\\');
//            try {
//                String projectSlug = TargetTranslation.getProjectSlugFromId(targetTranslationId);
//                String targetLanguageSlug = TargetTranslation.getTargetLanguageSlugFromId(targetTranslationId);
//            } catch (Exception e) {
//                e.printStackTrace();
//                continue;
//            }
//        }
        return new File[0];
    }
}
