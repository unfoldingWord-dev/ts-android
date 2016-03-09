package com.door43.translationstudio.core;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.util.Manifest;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Created by joel on 11/4/2015.
 */
public class TargetTranslationMigrator {

    private TargetTranslationMigrator() {

    }

    /**
     * Performs necessary migration operations on a target translation
     * @param targetTranslationDir
     * @return
     */
    public static boolean migrate(File targetTranslationDir) {
        boolean success = false;
        File manifestFile = new File(targetTranslationDir, "manifest.json");
        try {

            if (manifestFile.exists()) {
                JSONObject manifest = new JSONObject(FileUtils.readFileToString(manifestFile));
                switch (manifest.getInt("package_version")) {
                    case 2:
                        success = v2(manifestFile);
                        if(!success) break;
                    case 3:
                        success = v3(manifestFile);
                        if(!success) break;
                    case 4:
                        success = v4(manifestFile);
                        if(!success) break;
                    case 5:
                        success = v5(manifestFile);
                        if(!success) break;
                    default:
                        if(success) {
                            success = validateTranslationType(manifestFile);
                        }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return success;
    }

    /**
     * latest version
     * @param path
     * @return
     */
    private static boolean v5(File path) {
        return true;
    }

    /**
     * major restructuring of the manifest to provide better support for future front/back matter, drafts, rendering,
     * and resolves issues between desktop and android platforms.
     * @param path
     * @return
     */
    private static boolean v4(File path) throws Exception {
        JSONObject manifest = new JSONObject(FileUtils.readFileToString(path));

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
            typeJson.put("id", typeId);
            typeJson.put("name", "");
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
        if(manifest.has("resource_id")) {
            String resourceId = manifest.getString("resource_id");
            manifest.remove("resource_id");
            JSONObject resourceJson = new JSONObject();
            // TRICKY: supported resource id's (or now types) are "reg", "ulb", and "udb".
            if (resourceId.equals("ulb")) {
                resourceJson.put("name", "Unlocked Literal Bible");
            } else if (resourceId.equals("udb")) {
                resourceJson.put("name", "Unlocked Dynamic Bible");
            } else {
                // everything else changes to "reg"
                resourceId = "reg";
                resourceJson.put("name", "Regular");
            }
            resourceJson.put("id", resourceId);
            manifest.put("resource", resourceJson);
        } else if(!manifest.has("resource")) {
            // add missing resource
            JSONObject resourceJson = new JSONObject();
            JSONObject projectJson = manifest.getJSONObject("project");
            JSONObject typeJson = manifest.getJSONObject("type");
            if(typeJson.getString("id").equals("text")) {
                if(projectJson.getString("id").equals("obs")) {
                    // obs switches to reg
                    resourceJson.put("id", "reg");
                    resourceJson.put("name", "Regular");
                } else {
                    resourceJson.put("id", "ulb");
                    resourceJson.put("name", "Unlocked Literal Bible");
                }
                manifest.put("resource", resourceJson);
            }
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
                            sourceTranslation.put("language_id", languageResourceId.substring(0, resId.length() - 1));
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

        // update where project title is saved.
        File oldProjectTitle = new File(path.getParentFile(), "title.txt");
        File newProjectTitle = new File(path.getParentFile(), "00/title.txt");
        if(oldProjectTitle.exists()) {
            newProjectTitle.getParentFile().mkdirs();
            FileUtils.moveFile(oldProjectTitle, newProjectTitle);
        }

        // update package version
        manifest.put("package_version", 5);

        FileUtils.write(path, manifest.toString(2));
        return true;
    }

    /**
     * We changed how the translator information is stored
     * we no longer store sensitive information like email and phone number
     * @param path
     * @return
     */
    private static boolean v3(File path) throws Exception {
        JSONObject manifest = new JSONObject(FileUtils.readFileToString(path));
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
            manifest.put("package_version", 3);
            FileUtils.write(path, manifest.toString(2));
        }
        return true;
    }

    /**
     * upgrade from v2
     * @param path
     * @return
     */
    private static boolean v2( File path) throws Exception {
        JSONObject manifest = new JSONObject(FileUtils.readFileToString(path));
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

        manifest.put("package_version", 2);
        FileUtils.write(path, manifest.toString(2));
        return true;
    }

   /**
    * Merges chunks found in the target translation projects that do not exist in the source translation
    * to a sibling chunk so that no data is lost.
    * @param library
    * @param translationSlugs target translations to merge
    * @return
    */
    public static boolean migrateChunkChanges(final Translator translator, final Library library, final String[] translationSlugs) {
        boolean mergeSuccess = true;
        for (String translationSlug : translationSlugs) {
            final TargetTranslation targetTranslation = translator.getTargetTranslation(translationSlug);
            boolean success = migrateChunkChanges(library, targetTranslation);
            mergeSuccess = mergeSuccess && success;
        }
        return mergeSuccess;
    }

    /**
     * Merges chunks found in the target translation projects that do not exist in the source translation
     * to a sibling chunk so that no data is lost.
     * @param library
     * @param targetTranslations target translations to merge
     * @return
     */
    public static boolean migrateChunkChanges(final Library library, final TargetTranslation[] targetTranslations) {
        boolean mergeSuccess = true;
        for (TargetTranslation targetTranslation : targetTranslations) {
            boolean success = migrateChunkChanges(AppContext.getLibrary(), targetTranslation);
            mergeSuccess = mergeSuccess && success;
        }
        return mergeSuccess;
    }

    /**
     * Merges chunks found in a target translation Project that do not exist in the source translation
     * to a sibling chunk so that no data is lost.
     * @param library
     * @param targetTranslation target translation to merge
     * @return
     */
    public static boolean migrateChunkChanges(final Library library, final TargetTranslation targetTranslation)  {
        try {
            Logger.i(TargetTranslationMigrator.class.getName(), "Migrating chunks in target translation " + targetTranslation.getProjectId());
            final SourceTranslation sourceTranslation = library.getDefaultSourceTranslation(targetTranslation.getProjectId(), "en");
            if(sourceTranslation == null) {
                Logger.w(TargetTranslationMigrator.class.getName(), "Could not find a source translation for the target translation " + targetTranslation.getId());
                return false;
            }
            if(targetTranslation.getPath().exists()) {
                boolean migrationSuccess = true;
                // perform the chunk migration on each chapter of the target translation
                for(ChapterTranslation chapterTranslation:targetTranslation.getChapterTranslations()) {
                    Chapter chapter = library.getChapter(sourceTranslation, chapterTranslation.getId());
                    if(chapter != null) {
                        boolean success = mergeInvalidChunksInChapter(library, sourceTranslation, targetTranslation, chapter);
                        migrationSuccess = migrationSuccess && success;
                    }
                }
                return migrationSuccess;
            }
        } catch (Exception e) {
            Logger.e(TargetTranslationMigrator.class.getName(), "Failed to merge the chunks in the target translation " + targetTranslation.getProjectId());
        }
        return false;
    }

    /**
     * Merges invalid chunks found in the target translation with a valid sibling chunk in order
     * to preserve translation data. Merged chunks are marked as not finished to force
     * translators to review the changes.
     * @param library
     * @param sourceTranslation
     * @param targetTranslation
     * @param chapter
     * @return
     */
    private static boolean mergeInvalidChunksInChapter(final Library library, final SourceTranslation sourceTranslation, final TargetTranslation targetTranslation, final Chapter chapter) {
        boolean success = true;
        final String chunkMergeMarker = "\n----------\n";
        Logger.i(TargetTranslationMigrator.class.getName(), "Searching chapter " + chapter.getId() + " for invalid chunks ");
        // TRICKY: the translation format doesn't matter for migrating
        FrameTranslation[] frameTranslations = targetTranslation.getFrameTranslations(chapter.getId(), TranslationFormat.DEFAULT);
        String invalidChunks = "";
        Frame lastValidFrame = null;
        for(FrameTranslation frameTranslation:frameTranslations) {
            Frame frame = library.getFrame(sourceTranslation, chapter.getId(), frameTranslation.getId());
            if(frame != null) {
                lastValidFrame =  frame;
                // merge invalid frames into the existing frame
                if(!invalidChunks.isEmpty()) {
                    targetTranslation.applyFrameTranslation(frameTranslation, invalidChunks + frameTranslation.body);
                    invalidChunks = "";
                    targetTranslation.reopenFrame(frame);
                }
            } else if(!frameTranslation.body.trim().isEmpty()) {
                if(lastValidFrame == null) {
                    // collect invalid frame
                    invalidChunks += frameTranslation.body + chunkMergeMarker;
                } else { // if last frame is not null, then append invalid chunk to it
                    FrameTranslation lastFrameTranslation = targetTranslation.getFrameTranslation(lastValidFrame);
                    targetTranslation.applyFrameTranslation(lastFrameTranslation, lastFrameTranslation.body + chunkMergeMarker + frameTranslation.body );
                    targetTranslation.reopenFrame(lastValidFrame);
                }
                targetTranslation.applyFrameTranslation(frameTranslation, "" ); // clear out old data
            }
        }
        // clean up remaining invalid chunks
        if(!invalidChunks.isEmpty()) {
            if(lastValidFrame == null) {
                // push remaining invalid chunks onto the first available frame
                String[] frameslugs = library.getFrameSlugs(sourceTranslation, chapter.getId());
                if(frameslugs.length > 0) {
                    lastValidFrame = library.getFrame(sourceTranslation, chapter.getId(), frameslugs[0]);
                } else {
                    Logger.w(TargetTranslationMigrator.class.getName(), "No frames were found for chapter " + chapter.getId());
                }
            }

            if(lastValidFrame != null) {
                FrameTranslation frameTranslation = targetTranslation.getFrameTranslation(lastValidFrame);
                targetTranslation.applyFrameTranslation(frameTranslation, invalidChunks + chunkMergeMarker + frameTranslation.body);
                targetTranslation.reopenFrame(lastValidFrame);
            }
        }

        return success;
    }

    /**
     * Checks if the android app can support this translation type.
     * Example: ts-desktop can translate tW but ts-android cannot.
     * @param path
     * @return
     */
    private static boolean validateTranslationType(File path) throws Exception{
        JSONObject manifest = new JSONObject(FileUtils.readFileToString(path));
        String typeId = manifest.getJSONObject("type").getString("id");
        // android only supports TEXT translations for now
        return TranslationType.get(typeId) == TranslationType.TEXT;
    }
}
