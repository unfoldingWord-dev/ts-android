package com.door43.translationstudio.core;

import android.content.res.AssetManager;

import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.rendering.USXtoUSFMConverter;
import com.door43.util.FileUtilities;
import com.door43.util.Manifest;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Created by joel on 11/4/2015.
 */
public class TargetTranslationMigrator {

    private static final String MANIFEST_FILE = "manifest.json";
    public static final String LICENSE = "LICENSE";

    /**
     * Performs a migration on a manifest object.
     * We just throw it into a temporary directory and run the normal migration on it.
     * @param manifestJson
     * @return
     */
    public static JSONObject migrateManifest(JSONObject manifestJson) {
        File tempDir = new File(AppContext.context().getCacheDir(), System.currentTimeMillis() + "");
        // TRICKY: the migration can change the name of the translation dir so we nest it to avoid conflicts.
        File fakeTranslationDir = new File(tempDir, "translation");
        fakeTranslationDir.mkdirs();
        JSONObject migratedManifest = null;
        try {
            FileUtils.writeStringToFile(new File(fakeTranslationDir, "manifest.json"), manifestJson.toString());
            fakeTranslationDir = migrate(fakeTranslationDir);
            if(fakeTranslationDir != null) {
                migratedManifest = new JSONObject(FileUtils.readFileToString(new File(fakeTranslationDir, "manifest.json")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // clean up
            FileUtils.deleteQuietly(tempDir);
        }
        return migratedManifest;
    }

    /**
     * Performs necessary migration operations on a target translation
     * @param targetTranslationDir
     * @return the target translation dir. Null if the migration failed
     */
    public static File migrate(File targetTranslationDir) {
        File migratedDir = targetTranslationDir;
        File manifestFile = new File(targetTranslationDir, MANIFEST_FILE);
        try {
            JSONObject manifest = new JSONObject(FileUtils.readFileToString(manifestFile));
            int packageVersion = 2; // default to version 2 if no package version is available
            if(manifest.has("package_version")) {
                packageVersion = manifest.getInt("package_version");
            }
            switch (packageVersion) {
                case 2:
                    migratedDir = v2(migratedDir);
                    if (migratedDir == null) break;
                case 3:
                    migratedDir = v3(migratedDir);
                    if (migratedDir == null) break;
                case 4:
                    migratedDir = v4(migratedDir);
                    if (migratedDir == null) break;
                case 5:
                    migratedDir = v5(migratedDir);
                    if (migratedDir == null) break;
                case 6:
                    migratedDir = v6(migratedDir);
                    if (migratedDir == null) break;
                default:
                    if (migratedDir != null && !validateTranslationType(migratedDir)) {
                        migratedDir = null;
                    }
            }
        } catch (Exception e) {
            e.printStackTrace();
            migratedDir = null;
        }
        if(migratedDir != null) {
            // import new langauge requests
            TargetTranslation tt = TargetTranslation.open(targetTranslationDir);
            if(tt != null) {
                NewLanguageRequest newRequest = tt.getNewLanguageRequest();
                if(newRequest != null) {
                    NewLanguageRequest existingRequest = AppContext.getNewLanguageRequest(newRequest.tempLanguageCode);
                    if(existingRequest == null) {
                        TargetLanguage approvedTargetLanguage = AppContext.getLibrary().getApprovedTargetLanguage(newRequest.tempLanguageCode);
                        if(approvedTargetLanguage == null) {
                            AppContext.addNewLanguageRequest(newRequest);
                        } else {
                            // this language has alread been approved
                            try {
                                tt.setNewLanguageRequest(null);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            // TODO: 6/15/16 change the language of the target translation
                        }

                    } else {
                        if(existingRequest.getSubmittedAt() > 0 && newRequest.getSubmittedAt() == 0) {
                            // indicated imported language request has been submitted
                            newRequest.setSubmittedAt(existingRequest.getSubmittedAt());
                            try {
                                tt.setNewLanguageRequest(newRequest);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else if(existingRequest.getSubmittedAt() == 0 && newRequest.getSubmittedAt() > 0) {
                            // indicate existing language request has been submitted
                            existingRequest.setSubmittedAt(newRequest.getSubmittedAt());
                            AppContext.addNewLanguageRequest(existingRequest);
                        }
                    }
                }
            }
        }
        return migratedDir;
    }

    /**
     * current version
     * @param path
     * @return
     * @throws Exception
     */
    private static File v6(File path) throws Exception {
        return path;
    }

    /**
     * Updated the id format of target translations
     * @param path
     * @return
     */
    private static File v5(File path) throws Exception {
        File manifestFile = new File(path, MANIFEST_FILE);
        JSONObject manifest = new JSONObject(FileUtils.readFileToString(manifestFile));

        // pull info to build id
        String targetLanguageCode = manifest.getJSONObject("target_language").getString("id");
        String projectSlug = manifest.getJSONObject("project").getString("id");
        String translationTypeSlug = manifest.getJSONObject("type").getString("id");
        String resourceSlug = null;
        if(translationTypeSlug.equals("text")) {
            resourceSlug = manifest.getJSONObject("resource").getString("id");
        }

        // build new id
        String id = targetLanguageCode + "_" + projectSlug + "_" + translationTypeSlug;
        if(translationTypeSlug.equals("text") && resourceSlug != null) {
            id += "_" + resourceSlug;
        }

        // add license file
        File licenseFile = new File(path, "LICENSE.md");
        if(!licenseFile.exists()) {
            AssetManager am = AppContext.context().getAssets();
            InputStream is = am.open("LICENSE.md");
            if(is != null) {
                FileUtils.copyInputStreamToFile(is, licenseFile);
            } else {
                throw new Exception("Failed to open the template license file");
            }
        }

        // update package version
        manifest.put("package_version", 6);
        FileUtils.write(manifestFile, manifest.toString(2));

        // update target translation dir name
        File newPath = new File(path.getParentFile(), id.toLowerCase());
        FileUtilities.safeDelete(newPath);
        FileUtils.moveDirectory(path, newPath);
        return newPath;
    }

    /**
     * major restructuring of the manifest to provide better support for future front/back matter, drafts, rendering,
     * and resolves issues between desktop and android platforms.
     * @param path
     * @return
     */
    private static File v4(File path) throws Exception {
        File manifestFile = new File(path, MANIFEST_FILE);
        JSONObject manifest = new JSONObject(FileUtils.readFileToString(manifestFile));

        // type
        {
            String typeId = "text";
            if (manifest.has("project")) {
                try {
                    JSONObject projectJson = manifest.getJSONObject("project");
                    typeId = projectJson.getString("type");
                    projectJson.remove("type");
                    manifest.put("project", projectJson);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            JSONObject typeJson = new JSONObject();
            TranslationType translationType = TranslationType.get(typeId);
            typeJson.put("id", typeId);
            if(translationType != null) {
                typeJson.put("name", translationType.getName());
            } else {
                typeJson.put("name", "");
            }
            manifest.put("type", typeJson);
        }

        // update project
        // NOTE: this was actually in v3 but we missed it so we need to catch it here
        if(manifest.has("project_id")) {
            String projectId = manifest.getString("project_id");
            manifest.remove("project_id");
            JSONObject projectJson = new JSONObject();
            projectJson.put("id", projectId);
            projectJson.put("name", projectId.toUpperCase()); // we don't know the full name at this point
            manifest.put("project", projectJson);
        }

        // update resource
        if(manifest.getJSONObject("type").getString("id").equals("text")) {
            if (manifest.has("resource_id")) {
                String resourceId = manifest.getString("resource_id");
                manifest.remove("resource_id");
                JSONObject resourceJson = new JSONObject();
                // TRICKY: supported resource id's (or now types) are "reg", "obs", "ulb", and "udb".
                if (resourceId.equals("ulb")) {
                    resourceJson.put("name", "Unlocked Literal Bible");
                } else if (resourceId.equals("udb")) {
                    resourceJson.put("name", "Unlocked Dynamic Bible");
                } else if (resourceId.equals("obs")) {
                    resourceJson.put("name", "Open Bible Stories");
                } else {
                    // everything else changes to "reg"
                    resourceId = "reg";
                    resourceJson.put("name", "Regular");
                }
                resourceJson.put("id", resourceId);
                manifest.put("resource", resourceJson);
            } else if (!manifest.has("resource")) {
                // add missing resource
                JSONObject resourceJson = new JSONObject();
                JSONObject projectJson = manifest.getJSONObject("project");
                JSONObject typeJson = manifest.getJSONObject("type");
                if (typeJson.getString("id").equals("text")) {
                    String resourceId = projectJson.getString("id");
                    if (resourceId.equals("obs")) {
                        resourceJson.put("id", "obs");
                        resourceJson.put("name", "Open Bible Stories");
                    } else {
                        // everything else changes to reg
                        resourceJson.put("id", "reg");
                        resourceJson.put("name", "Regular");
                    }
                    manifest.put("resource", resourceJson);
                }
            }
        } else {
            // non-text translation types do not have resources
            manifest.remove("resource_id");
            manifest.remove("resource");
        }

        // update source translations
        if(manifest.has("source_translations")) {
            JSONObject oldSourceTranslationsJson = manifest.getJSONObject("source_translations");
            manifest.remove("source_translations");
            JSONArray newSourceTranslationsJson = new JSONArray();
            Iterator<String> keys = oldSourceTranslationsJson.keys();
            while (keys.hasNext()) {
                try {
                    String key = keys.next();
                    JSONObject oldObj = oldSourceTranslationsJson.getJSONObject(key);
                    JSONObject sourceTranslation = new JSONObject();
                    String[] parts = key.split("-", 2);
                    if (parts.length == 2) {
                        String languageResourceId = parts[1];
                        String[] pieces = languageResourceId.split("-");
                        if (pieces.length > 0) {
                            String resId = pieces[pieces.length - 1];
                            sourceTranslation.put("resource_id", resId);
                            sourceTranslation.put("language_id", languageResourceId.substring(0, languageResourceId.length() - resId.length() - 1));
                            sourceTranslation.put("checking_level", oldObj.getString("checking_level"));
                            sourceTranslation.put("date_modified", oldObj.getInt("date_modified"));
                            sourceTranslation.put("version", oldObj.getString("version"));
                            newSourceTranslationsJson.put(sourceTranslation);
                        }
                    }
                } catch (Exception e) {
                    // don't fail migration just because a source translation was invalid
                    e.printStackTrace();
                }
            }
            manifest.put("source_translations", newSourceTranslationsJson);
        }

        // update parent draft
        if(manifest.has("parent_draft_resource_id")) {
            JSONObject draftStatus = new JSONObject();
            draftStatus.put("resource_id", manifest.getString("parent_draft_resource_id"));
            draftStatus.put("checking_entity", "");
            draftStatus.put("checking_level", "");
            draftStatus.put("comments", "The parent draft is unknown");
            draftStatus.put("contributors", "");
            draftStatus.put("publish_date", "");
            draftStatus.put("source_text", "");
            draftStatus.put("source_text_version", "");
            draftStatus.put("version", "");
            manifest.put("parent_draft", draftStatus);
            manifest.remove("parent_draft_resource_id");
        }

        // update finished chunks
        if(manifest.has("finished_frames")) {
            JSONArray finishedFrames = manifest.getJSONArray("finished_frames");
            manifest.remove("finished_frames");
            manifest.put("finished_chunks", finishedFrames);
        }

        // remove finished titles
        if(manifest.has("finished_titles")) {
            JSONArray finishedChunks = manifest.getJSONArray("finished_chunks");
            JSONArray finishedTitles = manifest.getJSONArray("finished_titles");
            manifest.remove("finished_titles");
            for(int i = 0; i < finishedTitles.length(); i ++) {
                String chapterId = finishedTitles.getString(i);
                finishedChunks.put(chapterId + "-title");
            }
            manifest.put("finished_chunks", finishedChunks);
        }

        // remove finished references
        if(manifest.has("finished_references")) {
            JSONArray finishedChunks = manifest.getJSONArray("finished_chunks");
            JSONArray finishedReferences = manifest.getJSONArray("finished_references");
            manifest.remove("finished_references");
            for(int i = 0; i < finishedReferences.length(); i ++) {
                String chapterId = finishedReferences.getString(i);
                finishedChunks.put(chapterId + "-reference");
            }
            manifest.put("finished_chunks", finishedChunks);
        }

        // remove project components
        // NOTE: this was never quite official, just in android
        if(manifest.has("finished_project_components")) {
            JSONArray finishedChunks = manifest.getJSONArray("finished_chunks");
            JSONArray finishedProjectComponents = manifest.getJSONArray("finished_project_components");
            manifest.remove("finished_project_components");
            for(int i = 0; i < finishedProjectComponents.length(); i ++) {
                String component = finishedProjectComponents.getString(i);
                finishedChunks.put("00-" + component);
            }
            manifest.put("finished_chunks", finishedChunks);
        }

        // add format
        if(!Manifest.valueExists(manifest, "format") || manifest.getString("format").equals("usx") || manifest.getString("format").equals("default")) {
            String typeId = manifest.getJSONObject("type").getString("id");
            String projectId = manifest.getJSONObject("project").getString("id");
            if(!typeId.equals("text") || projectId.equals("obs")) {
                manifest.put("format", "markdown");
            } else {
                manifest.put("format", "usfm");
            }
        }

        // update where project title is saved.
        File oldProjectTitle = new File(path, "title.txt");
        File newProjectTitle = new File(path, "00/title.txt");
        if(oldProjectTitle.exists()) {
            newProjectTitle.getParentFile().mkdirs();
            FileUtils.moveFile(oldProjectTitle, newProjectTitle);
        }

        // update package version
        manifest.put("package_version", 5);

        FileUtils.write(manifestFile, manifest.toString(2));

        // migrate usx to usfm
        String format = manifest.getString("format");
        // TRICKY: we just added the new format field, anything marked as usfm may have residual usx and needs to be migrated
        if (format.equals("usfm")) {
            File[] chapterDirs = path.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isDirectory() && !pathname.getName().equals(".git");
                }
            });
            for(File cDir:chapterDirs) {
                File[] chunkFiles = cDir.listFiles();
                for(File chunkFile:chunkFiles) {
                    try {
                        String usx = FileUtils.readFileToString(chunkFile);
                        String usfm = USXtoUSFMConverter.doConversion(usx).toString();
                        FileUtils.writeStringToFile(chunkFile, usfm);
                    } catch (IOException e) {
                        // this conversion may have failed but don't stop the rest of the migration
                        e.printStackTrace();
                    }
                }
            }
        }

        return path;
    }

    /**
     * We changed how the translator information is stored
     * we no longer store sensitive information like email and phone number
     * @param path
     * @return
     */
    private static File v3(File path) throws Exception {
        File manifestFile = new File(path, MANIFEST_FILE);
        JSONObject manifest = new JSONObject(FileUtils.readFileToString(manifestFile));
        if(manifest.has("translators")) {
            JSONArray legacyTranslators = manifest.getJSONArray("translators");
            JSONArray translators = new JSONArray();
            for(int i = 0; i < legacyTranslators.length(); i ++) {
                Object obj = legacyTranslators.get(i);
                if(obj instanceof JSONObject) {
                    translators.put(((JSONObject)obj).getString("name"));
                } else if(obj instanceof String) {
                    translators.put(obj);
                }
            }
            manifest.put("translators", translators);
            manifest.put("package_version", 4);
            FileUtils.write(manifestFile, manifest.toString(2));
        }
        migrateChunkChanges(path);
        return path;
    }

    /**
     * upgrade from v2
     * @param path
     * @return
     */
    private static File v2( File path) throws Exception {
        File manifestFile = new File(path, MANIFEST_FILE);
        JSONObject manifest = new JSONObject(FileUtils.readFileToString(manifestFile));
        // fix finished frames
        if(manifest.has("frames")) {
            JSONObject legacyFrames = manifest.getJSONObject("frames");
            Iterator<String> keys = legacyFrames.keys();
            JSONArray finishedFrames = new JSONArray();
            while(keys.hasNext()) {
                String key = keys.next();
                JSONObject frameState = legacyFrames.getJSONObject(key);
                boolean finished = false;
                if(frameState.has("finished")) {
                    finished = frameState.getBoolean("finished");
                }
                if(finished) {
                    finishedFrames.put(key);
                }
            }
            manifest.remove("frames");
            manifest.put("finished_frames", finishedFrames);
        }
        // fix finished chapter titles and references
        if(manifest.has("chapters")) {
            JSONObject legacyChapters = manifest.getJSONObject("chapters");
            Iterator<String> keys = legacyChapters.keys();
            JSONArray finishedTitles = new JSONArray();
            JSONArray finishedReferences = new JSONArray();
            while(keys.hasNext()) {
                String key = keys.next();
                JSONObject chapterState = legacyChapters.getJSONObject(key);
                boolean finishedTitle = false;
                boolean finishedReference = false;
                if(chapterState.has("finished_title")) {
                    finishedTitle = chapterState.getBoolean("finished_title");
                }
                if(chapterState.has("finished_reference")) {
                    finishedTitle = chapterState.getBoolean("finished_reference");
                }
                if(finishedTitle) {
                    finishedTitles.put(key);
                }
                if(finishedReference) {
                    finishedReferences.put(key);
                }
            }
            manifest.remove("chapters");
            manifest.put("finished_titles", finishedTitles);
            manifest.put("finished_references", finishedReferences);
        }
        // fix project id
        if(manifest.has("slug")) {
            String projectSlug = manifest.getString("slug");
            manifest.remove("slug");
            manifest.put("project_id", projectSlug);
        }
        // fix target language id
        JSONObject targetLanguage = manifest.getJSONObject("target_language");
        if(targetLanguage.has("slug")) {
            String targetLanguageSlug = targetLanguage.getString("slug");
            targetLanguage.remove("slug");
            targetLanguage.put("id", targetLanguageSlug);
            manifest.put("target_language", targetLanguage);
        }

        manifest.put("package_version", 3);
        FileUtils.write(manifestFile, manifest.toString(2));
        return path;
    }

    /**
     * Merges chunks found in a target translation Project that do not exist in the source translation
     * to a sibling chunk so that no data is lost.
     * @param targetTranslationDir
     * @return
     */
    private static boolean migrateChunkChanges(File targetTranslationDir)  {
        // TRICKY: calling the AppContext here is bad practice, but we'll deprecate this soon anyway.
        final Library library = AppContext.getLibrary();
        final SourceTranslation sourceTranslation = library.getDefaultSourceTranslation(targetTranslationDir.getName(), "en");
        if(sourceTranslation == null) {
            // if there is no source we are done
            return true;
        }
        File[] chapterDirs = targetTranslationDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory() && !pathname.getName().equals(".git") && !pathname.getName().equals("00"); // 00 contains project title translations
            }
        });
        for(File cDir:chapterDirs) {
            mergeInvalidChunksInChapter(library, new File(targetTranslationDir, "manifest.json"), sourceTranslation, cDir);
        }
        return true;
    }

    /**
     * Merges invalid chunks found in the target translation with a valid sibling chunk in order
     * to preserve translation data. Merged chunks are marked as not finished to force
     * translators to review the changes.
     * @param library
     * @param manifestFile
     * @param sourceTranslation
     * @param chapterDir
     * @return
     */
    private static boolean mergeInvalidChunksInChapter(final Library library, File manifestFile, final SourceTranslation sourceTranslation, final File chapterDir) {
        JSONObject manifest;
        try {
            manifest = new JSONObject(FileUtils.readFileToString(manifestFile));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        final String chunkMergeMarker = "\n----------\n";
        File[] frameFiles = chapterDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return !pathname.getName().equals("title.txt") && !pathname.getName().equals("reference.txt");
            }
        });
        String invalidChunks = "";
        File lastValidFrameFile = null;
        String chapterId = chapterDir.getName();
        for(File frameFile:frameFiles) {
            String frameId = frameFile.getName();
            Frame frame = library.getFrame(sourceTranslation, chapterId, frameId);
            String frameBody = "";
            try {
                frameBody = FileUtils.readFileToString(frameFile).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(frame != null) {
                lastValidFrameFile =  frameFile;
                // merge invalid frames into the existing frame
                if(!invalidChunks.isEmpty()) {
                    try {
                        FileUtils.writeStringToFile(frameFile, invalidChunks + frameBody);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    invalidChunks = "";
                    try {
                        Manifest.removeValue(manifest.getJSONArray("finished_frames"), chapterId + "-" + frameId);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } else if(!frameBody.isEmpty()) {
                // collect invalid frame
                if(lastValidFrameFile == null) {
                    invalidChunks += frameBody + chunkMergeMarker;
                } else {
                    // append to last valid frame
                    String lastValidFrameBody = "";
                    try {
                        lastValidFrameBody = FileUtils.readFileToString(lastValidFrameFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        FileUtils.writeStringToFile(lastValidFrameFile, lastValidFrameBody + chunkMergeMarker + frameBody);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        Manifest.removeValue(manifest.getJSONArray("finished_frames"), chapterId + "-" + lastValidFrameFile.getName());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                // delete invalid frame
                FileUtils.deleteQuietly(frameFile);
            }
        }
        // clean up remaining invalid chunks
        if(!invalidChunks.isEmpty()) {
            // grab updated list of frames
            frameFiles = chapterDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return !pathname.getName().equals("title.txt") && !pathname.getName().equals("reference.txt");
                }
            });
            if(frameFiles != null && frameFiles.length > 0) {
                String frameBody = "";
                try {
                    frameBody = FileUtils.readFileToString(frameFiles[0]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    FileUtils.writeStringToFile(frameFiles[0], invalidChunks + chunkMergeMarker + frameBody);
                    try {
                        Manifest.removeValue(manifest.getJSONArray("finished_frames"), chapterId + "-" + frameFiles[0].getName());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    /**
     * Checks if the android app can support this translation type.
     * Example: ts-desktop can translate tW but ts-android cannot.
     * @param path
     * @return
     */
    private static boolean validateTranslationType(File path) throws Exception{
        JSONObject manifest = new JSONObject(FileUtils.readFileToString(new File(path, MANIFEST_FILE)));
        String typeId = manifest.getJSONObject("type").getString("id");
        // android only supports TEXT translations for now
        return TranslationType.get(typeId) == TranslationType.TEXT;
    }
}
