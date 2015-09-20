package com.door43.translationstudio.core;

import android.text.Editable;
import android.text.SpannedString;

import com.door43.util.Manifest;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by joel on 8/29/2015.
 */
public class TargetTranslation {
    private static final long COMMIT_DELAY = 5000;
    private final String mTargetLanguageId;
    private final String mProjectId;
    private static final String GLOBAL_PROJECT_ID = "uw";
    private final File mTargetTranslationDirectory;
    private final Manifest mManifest;
    private final String mTargetTranslationName;
    private Timer mApplyFrameTimer;

    public TargetTranslation(String targetLanguageId, String projectId, File rootDir) {
        mTargetLanguageId = targetLanguageId;
        mProjectId = projectId;
        mTargetTranslationDirectory = generateTargetTranslationDir(targetLanguageId, projectId, rootDir);;
        mManifest = Manifest.generate(mTargetTranslationDirectory);
        String name = targetLanguageId;
        try {
            name = mManifest.getJSONObject("target_language").getString("name");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mTargetTranslationName = name;
    }

    /**
     * Returns the id of the target translation
     * @return
     */
    public String getId() {
        return generateTargetTranslationId(mTargetLanguageId, mProjectId);
    }

    /**
     * Returns the name of the target language
     * @return
     */
    public String getTargetLanguageName() {
        return mTargetTranslationName;
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
     * @param projectId the id of the project that will be translated
     * @param mRootDir the parent directory in which the target translation directory will be created
     * @return
     */
    public static TargetTranslation generate(TargetLanguage targetLanguage, String projectId, File mRootDir) throws Exception {
        // create folder
        File translationDir = generateTargetTranslationDir(targetLanguage.getId(), projectId, mRootDir);

        if(!translationDir.exists()) {
            // build new manifest
            Manifest manifest = Manifest.generate(translationDir);
            manifest.put("slug", projectId);
            JSONObject targetLangaugeJson = new JSONObject();
            targetLangaugeJson.put("direction", targetLanguage.direction.toString());
            targetLangaugeJson.put("slug", targetLanguage.code);
            targetLangaugeJson.put("name", targetLanguage.name);
            // TODO: we should restructure this output to match what we see in the api.
            // also the target language should have a toJson method that will do all of this.
            manifest.put("target_language", targetLangaugeJson);
        }

        return new TargetTranslation(targetLanguage.getId(), projectId, translationDir);
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

    /**
     * Returns the id of the project of the target translation
     * @param targetTranslationId the target translation id
     * @return
     */
    public static String getProjectIdFromId(String targetTranslationId) throws StringIndexOutOfBoundsException{
        String[] complexId = targetTranslationId.split("-", 3);
        if(complexId.length == 3) {
            return complexId[1];
        } else {
            throw new StringIndexOutOfBoundsException("malformed target translation id" + targetTranslationId);
        }
    }

    /**
     * Returns the id of the target lanugage of the target translation
     * @param targetTranslationId the target translation id
     * @return
     */
    public static String getTargetLanguageIdFromId(String targetTranslationId) throws StringIndexOutOfBoundsException {
        String[] complexId = targetTranslationId.split("-", 3);
        if(complexId.length == 3) {
            return complexId[2];
        } else {
            throw new StringIndexOutOfBoundsException("malformed target translation id" + targetTranslationId);
        }
    }

    /**
     * Generates the file to the directory where the target translation is located
     *
     * @param targetLanguageId the language to which the project is being translated
     * @param projectId the id of the project that is being translated
     * @param rootDir the directory where the target translations are stored
     * @return
     */
    public static File generateTargetTranslationDir(String targetLanguageId, String projectId, File rootDir) {
        String targetTranslationId = generateTargetTranslationId(targetLanguageId, projectId);
        return new File(rootDir, targetTranslationId);
    }

    /**
     * Adds a source translation to the list of used sources
     * This is used for tracking what source translations are used to create a target translation
     *
     * @param sourceTranslation
     * @throws JSONException
     */
    public void addSourceTranslation(SourceTranslation sourceTranslation) throws JSONException {
        JSONObject sourceTranslationsJson = mManifest.getJSONObject("source_translations");
        JSONObject translationJson = new JSONObject();
        translationJson.put("checking_level", sourceTranslation.getCheckingLevel());
        translationJson.put("date_modified", sourceTranslation.getDateModified());
        translationJson.put("version", sourceTranslation.getVersion());
        sourceTranslationsJson.put(sourceTranslation.getId(), translationJson);
    }

    /**
     * Returns the translation of a frame
     *
     * @param frame
     * @return
     */
    public FrameTranslation getFrameTranslation(Frame frame) {

        File frameFile = getFrameFile(frame.getChapterId(), frame.getId());
        if(frameFile.exists()) {
            try {
                String body = FileUtils.readFileToString(frameFile);
                return new FrameTranslation(frame.getId(), frame.getChapterId(), body, frame.getFormat(), isFrameFinished(frame));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // give empty translation
        return new FrameTranslation(frame.getId(), frame.getChapterId(), "", frame.getFormat(), false);
    }

    /**
     * Returns the translation of a chapter
     * This includes the chapter title and reference
     *
     * @param chapter
     * @return
     */
    public ChapterTranslation getChapterTranslation(Chapter chapter) {
        File referenceFile = new File(mTargetTranslationDirectory, chapter.getId() + "/reference.txt");
        File titleFile = new File(mTargetTranslationDirectory, chapter.getId() + "/title.txt");
        String reference = "";
        String title = "";
        if(referenceFile.exists()) {
            try {
                reference = FileUtils.readFileToString(referenceFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(titleFile.exists()) {
            try {
                title = FileUtils.readFileToString(titleFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new ChapterTranslation(title, reference, chapter.getId());
    }

    /**
     * Returns the translation of a project
     * This is just for the project title
     *
     * @return
     */
    public ProjectTranslation getProjectTranslation() {
        // TODO: this is not supported yet but we need to be able to provide the translation of the project title
        return null;
    }

    /**
     * Stages a frame translation to be saved
     * @param frameTranslation
     * @param translatedText
     */
    public void applyFrameTranslation(final FrameTranslation frameTranslation, final String translatedText) {
        // testing this performance. it will make a lot of things eaiser if we don't have to use a timeout for performance.
        try {
            commitFrameTranslation(frameTranslation, translatedText);
        } catch (IOException e) {
            e.printStackTrace();
        }

//        if(mApplyFrameTimer != null) {
//            mApplyFrameTimer.cancel();
//        }
//        mApplyFrameTimer = new Timer();
//        mApplyFrameTimer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                try {
//                    commitFrameTranslation(frameTranslation, translatedText);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }, COMMIT_DELAY);
    }

    /**
     * Saves a frame translation to the disk
     * if the translated text is null the frame will be removed
     * @param frameTranslation
     * @param translatedText
     */
    public void commitFrameTranslation(FrameTranslation frameTranslation, String translatedText) throws IOException {
        File frameFile = getFrameFile(frameTranslation.getChapterId(), frameTranslation.getId());
        if(translatedText.isEmpty()) {
            frameFile.delete();
        } else {
            frameFile.getParentFile().mkdirs();
            FileUtils.write(frameFile, translatedText);
        }
        // TODO: add and commit git
    }

    /**
     * Returns the frame file
     * @param chapterId
     * @param frameId
     * @return
     */
    private File getFrameFile(String chapterId, String frameId) {
        return new File(mTargetTranslationDirectory, chapterId + "/" + frameId + ".txt");
    }

    /**
     * Marks the translation of a frame as complete
     * @param frame
     * @return returns true if the translation actually exists and the updated was successful
     */
    public boolean finishFrame(Frame frame) {
        // TODO: we may want to change this to just have a list of "finished_frames"
        // rather than having a multi level json object. Then we could just check to see if the
        // frame id exist in the json array.
        File file = getFrameFile(frame.getChapterId(), frame.getId());
        if(file.exists()) {
            JSONObject framesJson = mManifest.getJSONObject("frames");
            try {
                if (!framesJson.has(frame.getComplexId())) {
                    framesJson.put(frame.getComplexId(), new JSONObject());
                }
                JSONObject frameJson = framesJson.getJSONObject(frame.getComplexId());
                frameJson.put("finished", true);
                mManifest.put("frames", framesJson);
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Marks the translation of a frame as not complete
     * @param frame
     * @return
     */
    public boolean reopenFrame(Frame frame) {
        JSONObject framesJson = mManifest.getJSONObject("frames");
        try {
            if (!framesJson.has(frame.getComplexId())) {
                framesJson.put(frame.getComplexId(), new JSONObject());
            }
            JSONObject frameJson = framesJson.getJSONObject(frame.getComplexId());
            frameJson.put("finished", false);
            mManifest.put("frames", framesJson);
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Check if the translation of a frame is complete
     * @param frame
     * @return
     */
    private boolean isFrameFinished(Frame frame) {
        JSONObject framesJson = mManifest.getJSONObject("frames");
        if(framesJson.has(frame.getComplexId())) {
            try {
                JSONObject frameJson = framesJson.getJSONObject(frame.getComplexId());
                if(frameJson.has("finished")) {
                    return frameJson.getBoolean("finished");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
