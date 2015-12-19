package com.door43.translationstudio.core;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
     * Merges chunks found in the target translation that do not exist in the source translation
     * to a sibling chunk so that no data is lost.
     * @param library
     * @param targetTranslation project to translate
     * @return
     */
    public static boolean mergeInvalidChunks(Library library, TargetTranslation targetTranslation) throws Exception {

        boolean success = true;

        final String CHUNK_EXT = "txt";

        final String targetTranslationId = targetTranslation.getPath().getName();
        final String sourceTranslationID = AppContext.getSelectedSourceTranslationId(targetTranslationId);;

        final SourceTranslation sourceTranslation = library.getSourceTranslation(sourceTranslationID);
        if(null == sourceTranslation)  {
            Logger.w(TargetTranslationMigrator.class.getName(), "no source translations found for " + targetTranslation.getProjectId());
            return false;
        }

        final Chapter[] chapters = library.getChapters( sourceTranslation);

        final File localDir = targetTranslation.getPath();
        if(localDir.exists()) {

            // merge folders in translation
            final File[] newFiles = localDir.listFiles();
            for(File localFolder:newFiles) {
                if(localFolder.isDirectory()) {

                    // merge the folders

                    final String chapterID = getFileBase(localFolder.getName());
                    Chapter currentChapter = null;

                    for(Chapter chapter: chapters) {
                        if(chapter.getId().equals(chapterID)) {
                            currentChapter = chapter;
                            break;
                        }
                    }

                    if(null != currentChapter) {

                        Frame[] frames = library.getFrames(sourceTranslation, currentChapter.getId());

                        File[] chunks = localFolder.listFiles();
                        Arrays.sort(chunks);

                         for (File chunk : chunks) {

                            if (getFileExt(chunk.getName()).equals(CHUNK_EXT)) {

                                String chunkBase = getFileBase(chunk.getName());

                                Frame previousFrame = null;
                                Frame matchFrame = null;
                                for(Frame frame:frames) {

                                    int compare = frame.getId().compareTo(chunkBase);
                                    if(compare == 0) { // found match
                                        matchFrame = frame;
                                        break;
                                    } else if (compare > 0) { // gone past
                                        break;
                                    }

                                    previousFrame = frame;
                                }

                                if(null == matchFrame) { // if frame is not found
                                    // append content to previous chunk

                                    if(null != previousFrame) {
                                        try {
                                            targetTranslation.reopenFrame(previousFrame);

                                            long fileSize = chunk.length();
                                            if(fileSize <= 0) { // if file is empty
                                                continue; // skip to next file
                                            }

                                            File previousChunk = new File(localFolder, previousFrame.getId() + "." + CHUNK_EXT);

                                            // merge frames
                                            String extraData = FileUtils.readFileToString(chunk);
                                            String previousData = FileUtils.readFileToString(previousChunk);
                                            FileUtils.write(previousChunk, previousData + " " + extraData);

                                            //clear extra chunk
                                            FileUtils.write(chunk, "");

                                        } catch (IOException e) {
                                            Logger.w(TargetTranslationMigrator.class.getName(), "merge of frames failed", e);
                                            success = false;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
        return success;
    }

    /**
     * Merges chunks found in the target translation that do not exist in the source translation
     * to a sibling chunk so that no data is lost.
     * @param library
     * @param baseDir the base folder
     * @param translationSlugs projects to translate
     * @return
     */
    public static boolean mergeInvalidChunksFromProjects(Translator translator, Library library, File baseDir, String[] translationSlugs) {
        boolean mergeSuccess = true;
        for (String translationSlug : translationSlugs) {
            try {
                TargetTranslation targetTranslation = translator.getTargetTranslation(translationSlug);

                boolean success = mergeInvalidChunks( library,  targetTranslation);
                mergeSuccess = mergeSuccess && success;

            } catch (Exception e) {
                Logger.w(TargetTranslationMigrator.class.getName(), "merge of " + translationSlug + " failed", e);
                mergeSuccess = false;
            }
        }

        return mergeSuccess;
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
