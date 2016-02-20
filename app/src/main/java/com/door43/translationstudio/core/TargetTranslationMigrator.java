package com.door43.translationstudio.core;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.util.Manifest;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.Iterator;

/**
 * Created by joel on 11/4/2015.
 */
public class TargetTranslationMigrator {

    private static final String CHUNK_MERGE_MARKER = "\n----------\n";

    private TargetTranslationMigrator() {

    }

    /**
     * Performs necessary migration operations on a target translation
     * @param targetTranslationDir
     * @return
     */
    public static boolean migrate(File targetTranslationDir) {
        try {
            File manifestFile = new File(targetTranslationDir, "manifest.json");
            if (manifestFile.exists()) {
                JSONObject manifest = new JSONObject(FileUtils.readFileToString(manifestFile));
                switch (manifest.getInt("package_version")) {
                    case 2:
                        return v2(manifest, manifestFile);
                    case 3:
                        return v3(manifest, manifestFile);
                    case 4:
                        return v4(manifest, manifestFile);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * latest version
     * @param manifest
     * @param path
     * @return
     */
    private static boolean v4(JSONObject manifest, File path) {
        return true;
    }

    /**
     * We changed how the translator information is stored
     * we no longer store sensitive information like email and phone number
     * @param manifest
     * @param path
     * @return
     */
    private static boolean v3(JSONObject manifest, File path) throws Exception {
        if(manifest.has("translators")) {
            JSONArray legacyTranslators = manifest.getJSONArray("translators");
            JSONArray translators = new JSONArray();
            for(int i = 0; i < legacyTranslators.length(); i ++) {
                JSONObject obj = legacyTranslators.getJSONObject(i);
                translators.put(obj.getString("name"));
            }
            manifest.put("translators", translators);
            FileUtils.write(path, manifest.toString());
        }
        return true;
    }

    /**
     * upgrade from v2
     * @param manifest
     * @param path
     * @return
     */
    private static boolean v2(JSONObject manifest, File path) throws Exception {
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

        FileUtils.write(path, manifest.toString());
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
                    invalidChunks += frameTranslation.body + CHUNK_MERGE_MARKER;
                } else { // if last frame is not null, then append invalid chunk to it
                    FrameTranslation lastFrameTranslation = targetTranslation.getFrameTranslation(lastValidFrame);
                    targetTranslation.applyFrameTranslation(lastFrameTranslation, lastFrameTranslation.body + CHUNK_MERGE_MARKER + frameTranslation.body );
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
                targetTranslation.applyFrameTranslation(frameTranslation, invalidChunks + CHUNK_MERGE_MARKER + frameTranslation.body);
                targetTranslation.reopenFrame(lastValidFrame);
            }
        }

        return success;
    }
}
