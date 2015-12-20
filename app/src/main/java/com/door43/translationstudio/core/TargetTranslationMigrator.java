package com.door43.translationstudio.core;

import android.support.annotation.Nullable;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Created by joel on 11/4/2015.
 */
public class TargetTranslationMigrator {

    final static String CHUNK_EXT = "txt";
    final static String CHUNK_FULL_EXT = "." + CHUNK_EXT;

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
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
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
     * Currently latest version.
     * @param manifest
     * @param path
     * @return
     */
    private static boolean v3(JSONObject manifest, File path) {
        return true;
    }

   /**
     * Merges chunks found in the target translation projects that do not exist in the source translation
     * to a sibling chunk so that no data is lost.
     * @param library
     * @param translationSlugs projects to translate
     * @return
     */
    public static boolean mergeInvalidChunksFromProjects(final Translator translator, final Library library, final String[] translationSlugs) {
        boolean mergeSuccess = true;
        for (String translationSlug : translationSlugs) {

            final TargetTranslation targetTranslation = translator.getTargetTranslation(translationSlug);
            boolean success = mergeInvalidChunksFromProject(library, targetTranslation);
            mergeSuccess = mergeSuccess && success;
        }
        return mergeSuccess;
    }

    /**
     * Merges chunks found in the target translation projects that do not exist in the source translation
     * to a sibling chunk so that no data is lost.
     * @param library
     * @param targetTranslations projects to translate
     * @return
     */
    public static boolean mergeInvalidChunksFromProjects(final Library library, final TargetTranslation[] targetTranslations) {
        boolean mergeSuccess = true;
        for (TargetTranslation targetTranslation : targetTranslations) {
            boolean success = mergeInvalidChunksFromProject(AppContext.getLibrary(), targetTranslation);
            mergeSuccess = mergeSuccess && success;
        }
        return mergeSuccess;
    }

    /**
     * Merges chunks found in a target translation Project that do not exist in the source translation
     * to a sibling chunk so that no data is lost.
     * @param library
     * @param targetTranslation project to translate
     * @return
     */
    public static boolean mergeInvalidChunksFromProject(final Library library, final TargetTranslation targetTranslation)  {
        try {
            Logger.i(TargetTranslationMigrator.class.getName(), "merging project " + targetTranslation.getProjectId());

            final String targetTranslationID = targetTranslation.getPath().getName();
            final String sourceTranslationID = AppContext.getSelectedSourceTranslationId(targetTranslationID);
            if(null == sourceTranslationID) { // likely a new import and no sources have been selected
                Logger.w(TargetTranslationMigrator.class.getName(), "no source translation ID found for " + targetTranslation.getProjectId());
                return false;
            }

            Logger.i(TargetTranslationMigrator.class.getName(), "source Translation ID: " + sourceTranslationID);

            final SourceTranslation sourceTranslation = library.getSourceTranslation(sourceTranslationID);
            if(null == sourceTranslation)  {
                Logger.w(TargetTranslationMigrator.class.getName(), "no source translations found for " + targetTranslation.getProjectId());
                return false;
            }

            final File localDir = targetTranslation.getPath();
            if(localDir.exists()) {
                return mergeInvalidChunksInChapters(library, sourceTranslation, targetTranslation);
            }

        } catch (Exception e) {
            Logger.e(TargetTranslationMigrator.class.getName(), "Failed to merge the chunks in the target translation " + targetTranslation.getProjectId());
        }
        return false;
    }

    /**
     * Merges chunks found in the target translation that do not exist in the source translation
     * to a sibling chunk so that no data is lost.
     * @param library
     * @param targetTranslation project to translate
     * @return
     */
    private static boolean mergeInvalidChunksInChapters(final Library library, final SourceTranslation sourceTranslation, final TargetTranslation targetTranslation)  {
        boolean mergeSuccess = true;
        final File localDir = targetTranslation.getPath();
        final File[] newFiles = localDir.listFiles();
        for(File localFolder:newFiles) {
            if(localFolder.isDirectory()) {
                Chapter chapter = getChapter(library, sourceTranslation, getFileBase(localFolder.getName()));
                if(null != chapter) {
                    boolean success = mergeInvalidChunksInChapter(library, sourceTranslation, targetTranslation, localFolder, chapter);
                    mergeSuccess = mergeSuccess && success;
                }
            }
        }
        return mergeSuccess;
    }

    private static boolean mergeInvalidChunksInChapter(final Library library, final SourceTranslation sourceTranslation, final TargetTranslation targetTranslation, final File localFolder, final Chapter chapter) {
        boolean success = true;

        Logger.i(TargetTranslationMigrator.class.getName(), "searching chapter " + chapter.getId());

        final Frame[] frames = library.getFrames(sourceTranslation, chapter.getId());
        File[] chunks = localFolder.listFiles();
        Arrays.sort(chunks);
        for (File chunk : chunks) {
            String chunkName = chunk.getName();
            if (getFileExt(chunkName).equals(CHUNK_EXT)) {
                String chunkBase = getFileBase(chunkName);
                Frame mergeFrame = getMergeFrame(frames, chunkBase);

                if(null != mergeFrame) {
                    if(!mergeFrame.getId().equals(chunkBase)) { // if merge frame is not same as current frame
                        try {
                            mergeExtraChunkIntoValidFrame(targetTranslation, localFolder, chunk, mergeFrame);

                        } catch (IOException e) {
                            Logger.w(TargetTranslationMigrator.class.getName(), "merge of frames failed", e);
                            success = false;
                        }
                    }
                } else {
                    Logger.w(TargetTranslationMigrator.class.getName(), "no merge frame found for " + chunkBase);
                }
            }
        }
        return success;
    }

    private static void mergeExtraChunkIntoValidFrame(TargetTranslation targetTranslation, File localFolder, File extraChunk, Frame mergeFrame) throws IOException {
        if(extraChunk.length() <= 0) {
            Logger.i(TargetTranslationMigrator.class.getName(), "skipping merge of empty file: " + extraChunk.toString());
            return;
        } // skip if file is empty

        Logger.i(TargetTranslationMigrator.class.getName(), "merging '" + extraChunk.toString() +"' into '" + mergeFrame.getId() + "' in folder: " + localFolder.toString());

        File mergeChunk = new File(localFolder, mergeFrame.getId() + CHUNK_FULL_EXT);

        // merge extra chunk into target chunk
        String extraData = FileUtils.readFileToString(extraChunk);
        String previousData = FileUtils.readFileToString(mergeChunk);

        String mergedString;
        if(extraChunk.toString().compareTo(mergeChunk.toString()) >= 0) {
            mergedString = previousData + " " + extraData;
        } else {
            mergedString = extraData + " " + previousData;
        }

        Logger.i(TargetTranslationMigrator.class.getName(), "merged data: " + mergedString);

        FileUtils.write(mergeChunk, mergedString);
        FileUtils.write(extraChunk, ""); //clear extra chunk

        targetTranslation.reopenFrame(mergeFrame);
    }

    @Nullable
    private static Frame getMergeFrame(Frame[] frames, String chunkBase) {
        Frame previousFrame = null;
        for(Frame frame:frames) {

            if(null == previousFrame) { // always start with first valid frame
                previousFrame = frame;
            }

            int compare = frame.getId().compareTo(chunkBase);
            if(compare == 0) { // found match
                previousFrame = frame;
                break;
            } else if (compare > 0) { // gone past
                break;
            }

            previousFrame = frame;
        }
        return previousFrame;
    }

    @Nullable
    private static Chapter getChapter(final Library library, final SourceTranslation sourceTranslation, final String chapterID) {
        Chapter currentChapter = null;
        final Chapter[] chapters = library.getChapters( sourceTranslation);

        for(Chapter chapter: chapters) {
            if(chapter.getId().equals(chapterID)) {
                currentChapter = chapter;
                break;
            }
        }
        return currentChapter;
    }

    private static String getFileExt(String fileName) {
        return fileName.substring((fileName.lastIndexOf(".") + 1), fileName.length());
    }

    private static String getFileBase(String fileName) {

        int pos = fileName.lastIndexOf(".");

        if(pos < 0) { // no extension
            return fileName;
        }

        return fileName.substring(0, pos);
    }

    private static int getFileBaseAsNumber(String fileName) {
        try {
            String base = getFileBase(fileName);

            Integer value = Integer.parseInt(base);
            return value.intValue();

        } catch (NumberFormatException nfe) {
            return -1; // not number
        }
    }
}
