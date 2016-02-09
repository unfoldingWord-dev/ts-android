package com.door43.translationstudio.core;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.support.annotation.Nullable;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.git.Repo;
import com.door43.util.Manifest;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.DeleteBranchCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.ListTagCommand;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.TagCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.FetchResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;

/**
 * Created by joel on 8/29/2015.
 */
public class TargetTranslation {
    private static final String TAG = TargetTranslation.class.getSimpleName();
    private static final int PACKAGE_VERSION = 3; // the version of the manifest
    public static final String PARENT_DRAFT_RESOURCE_ID = "parent_draft_resource_id";
    private final String mTargetLanguageId;
    private final String mProjectId;
    private static final String GLOBAL_PROJECT_ID = "uw";
    public static final String TRANSLATORS = "translators";
    public static final String NAME = "name";
    public static final String PHONE = "phone";
    public static final String EMAIL = "email";
    private final File mTargetTranslationDirectory;
    private final Manifest mManifest;
    private final String mTargetTranslationName;
    private final LanguageDirection mDirection;
    private Timer mApplyFrameTimer;

    public TargetTranslation(String targetLanguageId, String projectId, File rootDir) {
        mTargetLanguageId = targetLanguageId;
        mProjectId = projectId;
        mTargetTranslationDirectory = generateTargetTranslationDir(generateTargetTranslationId(targetLanguageId, projectId), rootDir);
        mManifest = Manifest.generate(mTargetTranslationDirectory);
        String name = targetLanguageId;
        try {
            name = mManifest.getJSONObject("target_language").getString("name");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mTargetTranslationName = name;
        LanguageDirection direction = LanguageDirection.LeftToRight;
        try {
            direction = LanguageDirection.get(mManifest.getJSONObject("target_language").getString("direction"));
            if(direction == null) {
                direction = LanguageDirection.LeftToRight;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mDirection = direction;
    }

    /**
     * Returns the directory to this target translation
     * @return
     */
    public File getPath() {
        return mTargetTranslationDirectory;
    }

    /**
     * Returns the language direction of the target language
     * @return
     */
    public LanguageDirection getTargetLanguageDirection() {
        return mDirection;
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
     * @param rootDir the parent directory in which the target translation directory will be created
     * @return
     */
    public static TargetTranslation create(Context context, TargetLanguage targetLanguage, String projectId, File rootDir) throws Exception {
        // generate new target translation if it does not exist
        File translationDir = generateTargetTranslationDir(generateTargetTranslationId(targetLanguage.getId(), projectId), rootDir);
        if(!translationDir.exists()) {
            // build new manifest
            Manifest manifest = Manifest.generate(translationDir);
            manifest.put("project_id", projectId);
            JSONObject generatorJson = new JSONObject();
            generatorJson.put("name", "ts-android");
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            generatorJson.put("build", pInfo.versionCode);
            manifest.put("generator", generatorJson);
            manifest.put("package_version", PACKAGE_VERSION);
            JSONObject targetLangaugeJson = new JSONObject();
            targetLangaugeJson.put("direction", targetLanguage.direction.toString());
            targetLangaugeJson.put("id", targetLanguage.code);
            targetLangaugeJson.put("name", targetLanguage.name);
            // TODO: we should restructure this output to match what we see in the api. if we do we'll need to migrate all the old manifest files.
            // also the target language should have a toJson method that will do all of this.
            manifest.put("target_language", targetLangaugeJson);
            // TODO: 1/5/2016 we should only add the open profile (e.g. the current user) as a translator
            JSONArray translatorsJson = encodeTranslators(NativeSpeaker.nativeSpeakersFromProfiles(AppContext.getProfiles()));
            manifest.put(TRANSLATORS, translatorsJson);
        }
        // load the target translation (new or otherwise)
        return new TargetTranslation(targetLanguage.getId(), projectId, rootDir);
    }

    /**
     * Updates the recorded information about the generator of the target translation
     * @param context
     * @param targetTranslation
     * @throws Exception
     */
    public static void updateGenerator(Context context, TargetTranslation targetTranslation) throws Exception{
        JSONObject generatorJson = new JSONObject();
        generatorJson.put("name", "ts-android");
        PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        generatorJson.put("build", pInfo.versionCode);
        targetTranslation.mManifest.put("generator", generatorJson);
    }

    /**
     * Returns a properly formatted target translation id
     * @param targetLanguageId
     * @param projectId
     * @return
     */
    public static String generateTargetTranslationId(String targetLanguageId, String projectId) {
        return GLOBAL_PROJECT_ID + "-" + projectId + "-" + targetLanguageId;
    }

    /**
     * Returns the id of the project of the target translation
     * @param targetTranslationId the target translation id
     * @return
     */
    public static String getProjectIdFromId(String targetTranslationId) throws StringIndexOutOfBoundsException {
        String[] complexId = targetTranslationId.split("-", 3);
        if(complexId.length == 3) {
            return complexId[1];
        } else {
            throw new StringIndexOutOfBoundsException("malformed target translation id " + targetTranslationId);
        }
    }

    /**
     * Returns the id of the target lanugage of the target translation
     * @param targetTranslationId the target translation id
     * @return
     */
    public static String getTargetLanguageIdFromId(String targetTranslationId) throws StringIndexOutOfBoundsException {
        String[] complexId = targetTranslationId.split("-");
        if(complexId.length >= 3) {
            // TRICKY: target language id's can have dashes in them.
            String targetLanguageId = complexId[2];
            for(int i = 3; i < complexId.length; i ++) {
                targetLanguageId += "-" + complexId[i];
            }
            return targetLanguageId;
        } else {
            throw new StringIndexOutOfBoundsException("malformed target translation id" + targetTranslationId);
        }
    }

    /**
     * Generates the file to the directory where the target translation is located
     *
     * @param targetTranslationId the language to which the project is being translated
     * @param rootDir the directory where the target translations are stored
     * @return
     */
    public static File generateTargetTranslationDir(String targetTranslationId, File rootDir) {
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
        mManifest.put("source_translations", sourceTranslationsJson);
    }

    /**
     * Adds a native speaker as a translator
     * @param translator
     */
    public boolean addTranslator(NativeSpeaker translator) {
        ArrayList<NativeSpeaker> translators = getTranslators();

        int foundAt = getTranslatorByName(translator.name);
        if(foundAt >= 0) { // if found, update data
            translators.set(foundAt, translator);
        } else { // if new translator then add
            translators.add(translator);
        }

        return saveTranslators(translators);
    }

    /**
     * remove a translator
     * @param name
     */
    public boolean removeTranslator(String name) {
        ArrayList<NativeSpeaker> translators = getTranslators();

            int foundAt = getTranslatorByName(name);
            if(foundAt < 0) { // if not found, skip
                return false;
            } else { // if new translator then add
                translators.remove(foundAt);
            }

            return saveTranslators(translators);
    }

    /**
     * save updated list of translators
     * @param translators
     */
    public boolean saveTranslators(ArrayList<NativeSpeaker> translators) {

        try {
            JSONArray translatorsJson = encodeTranslators(translators);
            mManifest.put(TRANSLATORS,translatorsJson);
        } catch (Exception e) {
            Logger.e(TargetTranslation.class.getName(), "failed save translators", e);
            return false;
        }

        return true;
    }

    /**
     * Encodes the native speakers into json
     * @param translators
     * @return
     * @throws JSONException
     */
    private static JSONArray encodeTranslators(List<NativeSpeaker> translators) throws JSONException {
        JSONArray translatorsJson = new JSONArray();

        for(int i = 0; i < translators.size(); i++) {
            JSONObject translatorJSON = new JSONObject();
            NativeSpeaker currentTranslator = translators.get(i);
            translatorJSON.put(NAME,currentTranslator.name);
            translatorJSON.put(EMAIL,currentTranslator.email);
            translatorJSON.put(PHONE,currentTranslator.phone);

            translatorsJson.put(translatorJSON);
        }
        return translatorsJson;
    }

    /**
     * Finds index of translator with name match
     * @param name
     */
    public int getTranslatorByName(String name) {
        ArrayList<NativeSpeaker> translators = getTranslators();

        for (int i = 0; i < translators.size(); i++) {
            NativeSpeaker speaker = translators.get(i);
            if (speaker.name.equals(name)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Returns an array of native speakers who have worked on this translation
     * @return
     */
    public ArrayList<NativeSpeaker> getTranslators() {

        JSONArray translatorsJson = mManifest.getJSONArray(Manifest.TRANSLATORS);

        ArrayList<NativeSpeaker> translators = new ArrayList<>();

        if(translatorsJson.length() > 0) {

            try {
                for (int i = 0; i < translatorsJson.length(); i++) {
                    JSONObject contact = translatorsJson.getJSONObject(i);
                    if (contact.has(NAME)) {
                        NativeSpeaker profile = new NativeSpeaker(contact.getString(NAME),
                                contact.getString(EMAIL), contact.getString(PHONE));

                        translators.add(profile);
                    }
                }
            } catch (Exception e) {
                Logger.e(TargetTranslation.class.getName(), "failed to fetch translators", e);
            }
        }

        return translators;
    }

    /**
     * If there are no translators specified from a project, this sets them from the defaults.
     * If there are translators specified, this does nothing.
     */
    public void applyDefaultTranslatorsIfNoneSpecified() {
        if (getTranslators().isEmpty()) {
            // TODO: 1/7/2016 we should only add the open profile (e.g. the current user) as a translator
            List<Profile> profiles = AppContext.getProfiles();
            List<NativeSpeaker> translators = NativeSpeaker.nativeSpeakersFromProfiles(profiles);
            for (NativeSpeaker translator : translators) {
                addTranslator(translator);
            }
        }
    }

    /**
     * Returns the translation of a frame
     *
     * @param frame
     * @return
     */
    public FrameTranslation getFrameTranslation(Frame frame) {
        if(frame == null) {
            return null;
        }
        return getFrameTranslation(frame.getChapterId(), frame.getId(), frame.getFormat());
    }

    /**
     * Returns the translation of a frame
     * @param chapterId
     * @param frameId
     * @param format
     * @return
     */
    public FrameTranslation getFrameTranslation(String chapterId, String frameId, TranslationFormat format) {
        File frameFile = getFrameFile(chapterId, frameId);
        if(frameFile.exists()) {
            try {
                String body = FileUtils.readFileToString(frameFile);
                return new FrameTranslation(frameId, chapterId, body, format, isFrameFinished(chapterId + "-" + frameId));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // give empty translation
        return new FrameTranslation(frameId, chapterId, "", format, false);
    }

    /**
     * Returns the translation of a chapter
     * This includes the chapter title and reference
     *
     * @param chapter
     * @return
     */
    public ChapterTranslation getChapterTranslation(Chapter chapter) {
        if(chapter == null) {
            return null;
        }
        return getChapterTranslation(chapter.getId());
    }

    /**
     * Returns the translation of a chapter
     * @param chapterSlug
     * @return
     */
    public ChapterTranslation getChapterTranslation(String chapterSlug) {
        File referenceFile = getChapterReferenceFile(chapterSlug);
        File titleFile = getChapterTitleFile(chapterSlug);
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
        return new ChapterTranslation(title, reference, chapterSlug, isChapterTitleFinished(chapterSlug), isChapterReferenceFinished(chapterSlug));
    }

    /**
     * Returns the translation of a project
     * This is just for the project title
     *
     * @return
     */
    public ProjectTranslation getProjectTranslation() {
        File titleFile = getProjectTitleFile();
        String title = "";
        if(titleFile.exists()) {
            try {
                title = FileUtils.readFileToString(titleFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new ProjectTranslation(title, isProjectComponentFinished("title"));
    }

    /**
     * Stages a frame translation to be saved
     * @param frameTranslation
     * @param translatedText
     */
    public void applyFrameTranslation(final FrameTranslation frameTranslation, final String translatedText) {
        // testing this performance. it will make a lot of things easier if we don't have to use a timeout for performance.
        try {
            saveFrameTranslation(frameTranslation, translatedText);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves the project title translation
     * @param translatedText
     */
    public void applyProjectTitleTranslation(String translatedText) throws IOException {
        File titleFile = getProjectTitleFile();
        if(translatedText.isEmpty()) {
            titleFile.delete();
        } else {
            titleFile.getParentFile().mkdirs();
            FileUtils.write(titleFile, translatedText);
        }
    }

    /**
     * Saves a frame translation to the disk
     * if the translated text is null the frame will be removed
     * @param frameTranslation
     * @param translatedText
     */
    private void saveFrameTranslation(FrameTranslation frameTranslation, String translatedText) throws IOException {
        File frameFile = getFrameFile(frameTranslation.getChapterId(), frameTranslation.getId());
        if(translatedText.isEmpty()) {
            frameFile.delete();
        } else {
            frameFile.getParentFile().mkdirs();
            FileUtils.write(frameFile, translatedText);
        }
    }

    /**
     * Saves a chapter reference translation to the disk
     * if the translated text is null the reference will be removed
     * @param chapterTranslation
     * @param translatedText
     * @throws IOException
     */
    private void saveChapterReferenceTranslation(ChapterTranslation chapterTranslation, String translatedText) throws IOException {
        File chapterReferenceFile = getChapterReferenceFile(chapterTranslation.getId());
        if(translatedText.isEmpty()) {
            chapterReferenceFile.delete();
        } else {
            chapterReferenceFile.getParentFile().mkdirs();
            FileUtils.write(chapterReferenceFile, translatedText);
        }
    }

    /**
     * Saves a chapter title translation to the disk
     * if the translated text is null the title will be removed
     * @param chapterTranslation
     * @param translatedText
     * @throws IOException
     */
    private void saveChapterTitleTranslation(ChapterTranslation chapterTranslation, String translatedText) throws IOException {
        File chapterTitleFile = getChapterTitleFile(chapterTranslation.getId());
        if(translatedText.isEmpty()) {
            chapterTitleFile.delete();
        } else {
            chapterTitleFile.getParentFile().mkdirs();
            FileUtils.write(chapterTitleFile, translatedText);
        }
    }

    /**
     * Returns the frame file
     * @param chapterId
     * @param frameId
     * @return
     */
    public File getFrameFile(String chapterId, String frameId) {
        return new File(mTargetTranslationDirectory, chapterId + "/" + frameId + ".txt");
    }

    /**
     * Returns the chapter reference file
     * @param chapterId
     * @return
     */
    public File getChapterReferenceFile(String chapterId) {
        return new File(mTargetTranslationDirectory, chapterId + "/reference.txt");
    }

    /**
     * Returns the chapter title file
     * @param chapterId
     * @return
     */
    public File getChapterTitleFile(String chapterId) {
        return new File(mTargetTranslationDirectory, chapterId + "/title.txt");
    }

    /**
     * Returns the project title file
     * @return
     */
    public File getProjectTitleFile() {
        return new File(mTargetTranslationDirectory, "title.txt");
    }

    /**
     * Marks the project title as finished
     * @return
     */
    public boolean finishProjectTitle() {
        File file = getProjectTitleFile();
        if(file.exists()) {
            return finishProjectComponent("title");
        }
        return false;
    }

    /**
     * Marks the project title as not finished
     * @return
     */
    public boolean reopenProjectTitle() {
        return reopenProjectComponent("title");
    }

    /**
     * Checks if the translation of a component of a project has been marked as done
     * @param component
     * @return
     */
    private boolean isProjectComponentFinished(String component) {
        JSONArray finishedProjectComponents = mManifest.getJSONArray("finished_project_components");
        try {
            for (int i = 0; i < finishedProjectComponents.length(); i++) {
                if(finishedProjectComponents.getString(i).equals(component)) {
                    return true;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Reopens a project component
     * @param component
     * @return
     */
    private boolean reopenProjectComponent(String component) {
        JSONArray finishedProjectComponents = mManifest.getJSONArray("finished_project_components");
        JSONArray updatedComponents = new JSONArray();
        try {
            for (int i = 0; i < finishedProjectComponents.length(); i++) {
                String finishedComponent = finishedProjectComponents.getString(i);
                if(!finishedComponent.equals(component)) {
                    updatedComponents.put(finishedProjectComponents.getString(i));
                }
            }
            mManifest.put("finished_project_components", updatedComponents);
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Marks a component of the project as finished
     * @param component
     * @return
     */
    private boolean finishProjectComponent(String component) {
        JSONArray finishedProjectComponents = mManifest.getJSONArray("finished_project_components");
        boolean isFinished = false;
        try {
            for (int i = 0; i < finishedProjectComponents.length(); i++) {
                String completedComponent = finishedProjectComponents.getString(i);
                if(completedComponent.equals(component)) {
                    isFinished = true;
                    break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if(!isFinished) {
            finishedProjectComponents.put(component);
            mManifest.put("finished_project_components", finishedProjectComponents);
        }
        return true;
    }

    /**
     * Marks the translation of a chapter title as complete
     * @param chapter
     * @return returns true if the translation actually exists and the update was successful
     */
    public boolean finishChapterTitle(Chapter chapter) {
        File file = getChapterTitleFile(chapter.getId());
        if(file.exists()) {
            JSONArray finishedTitles = mManifest.getJSONArray(Manifest.FINISHED_TITLES);
            boolean isFinished = false;
            try {
                for (int i = 0; i < finishedTitles.length(); i++) {
                    String chapterSlug = finishedTitles.getString(i);
                    if(chapterSlug.equals(chapter.getId())) {
                        isFinished = true;
                        break;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if(!isFinished) {
                finishedTitles.put(chapter.getId());
                mManifest.put(Manifest.FINISHED_TITLES, finishedTitles);
            }
            return true;
        }
        return false;
    }

    /**
     * Marks the translation of a chapter title as not complete
     * @param chapter
     * @return
     */
    public boolean reopenChapterTitle(Chapter chapter) {
        JSONArray finishedTitles = mManifest.getJSONArray(Manifest.FINISHED_TITLES);
        JSONArray updatedTitles = new JSONArray();
        try {
            for (int i = 0; i < finishedTitles.length(); i++) {
                String chapterSlug = finishedTitles.getString(i);
                if(!chapterSlug.equals(chapter.getId())) {
                    updatedTitles.put(finishedTitles.getString(i));
                }
            }
            mManifest.put(Manifest.FINISHED_TITLES, updatedTitles);
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Checks if the translation of a chapter title has been marked as done
     * @param chapter
     * @return
     */
    private boolean isChapterTitleFinished(Chapter chapter) {
        return isChapterTitleFinished(chapter.getId());
    }

    /**
     * Checks if the translation of a chapter title has been marked as done
     * @param chapterSlug
     * @return
     */
    private boolean isChapterTitleFinished(String chapterSlug) {
        JSONArray finishedTitles = mManifest.getJSONArray(Manifest.FINISHED_TITLES);
        try {
            for (int i = 0; i < finishedTitles.length(); i++) {
                if(finishedTitles.getString(i).equals(chapterSlug)) {
                    return true;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Marks the translation of a chapter title as complete
     * @param chapter
     * @return returns true if the translation actually exists and the update was successful
     */
    public boolean finishChapterReference(Chapter chapter) {
        File file = getChapterReferenceFile(chapter.getId());
        if(file.exists()) {
            JSONArray finishedReferences = mManifest.getJSONArray(Manifest.FINISHED_REFERENCES);
            boolean isFinished = false;
            try {
                for (int i = 0; i < finishedReferences.length(); i++) {
                    String chapterSlug = finishedReferences.getString(i);
                    if(chapterSlug.equals(chapter.getId())) {
                        isFinished = true;
                        break;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if(!isFinished) {
                finishedReferences.put(chapter.getId());
                mManifest.put(Manifest.FINISHED_REFERENCES, finishedReferences);
            }
            return true;
        }
        return false;
    }

    /**
     * Marks the translation of a chapter title as not complete
     * @param chapter
     * @return
     */
    public boolean reopenChapterReference(Chapter chapter) {
        JSONArray finishedReferences = mManifest.getJSONArray(Manifest.FINISHED_REFERENCES);
        JSONArray updatedReferences = new JSONArray();
        try {
            for (int i = 0; i < finishedReferences.length(); i++) {
                String chapterSlug = finishedReferences.getString(i);
                if(!chapterSlug.equals(chapter.getId())) {
                    updatedReferences.put(finishedReferences.getString(i));
                }
            }
            mManifest.put(Manifest.FINISHED_REFERENCES, updatedReferences);
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Checks if the translation of a chapter title has been marked as done
     * @param chapter
     * @return
     */
    private boolean isChapterReferenceFinished(Chapter chapter) {
        return isChapterReferenceFinished(chapter.getId());
    }

    /**
     * Checks if the translation of a chapter title has been marked as done
     * @param chapterSlug
     * @return
     */
    private boolean isChapterReferenceFinished(String chapterSlug) {
        JSONArray finishedReferences = mManifest.getJSONArray(Manifest.FINISHED_REFERENCES);
        try {
            for (int i = 0; i < finishedReferences.length(); i++) {
                if(finishedReferences.getString(i).equals(chapterSlug)) {
                    return true;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Marks the translation of a frame as complete
     * @param frame
     * @return returns true if the translation actually exists and the update was successful
     */
    public boolean finishFrame(Frame frame) {
        File file = getFrameFile(frame.getChapterId(), frame.getId());
        if(file.exists()) {
            JSONArray finishedFrames = mManifest.getJSONArray(Manifest.FINISHED_FRAMES);
            boolean isFinished = false;
            try {
                for (int i = 0; i < finishedFrames.length(); i++) {
                    String complexSlug = finishedFrames.getString(i);
                    if(complexSlug.equals(frame.getComplexId())) {
                        isFinished = true;
                        break;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if(!isFinished) {
                finishedFrames.put(frame.getComplexId());
                mManifest.put(Manifest.FINISHED_FRAMES, finishedFrames);
            }
            return true;
        }
        return false;
    }

    /**
     * Marks the translation of a frame as not complete
     * @param frame
     * @return
     */
    public boolean reopenFrame(Frame frame) {
        JSONArray finishedFrames = mManifest.getJSONArray(Manifest.FINISHED_FRAMES);
        JSONArray updatedFrames = new JSONArray();
        try {
            for (int i = 0; i < finishedFrames.length(); i++) {
                String complexSlug = finishedFrames.getString(i);
                if(!complexSlug.equals(frame.getComplexId())) {
                    updatedFrames.put(finishedFrames.getString(i));
                }
            }
            mManifest.put(Manifest.FINISHED_FRAMES, updatedFrames);
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Checks if the translation of a frame has been marked as done
     * @param frame
     * @return
     */
    private boolean isFrameFinished(Frame frame) {
        return isFrameFinished(frame.getComplexId());
    }

    /**
     * Checks if the translation of a frame has been marked as done
     * @param frameComplexId
     * @return
     */
    private boolean isFrameFinished(String frameComplexId) {
        JSONArray finishedFrames = mManifest.getJSONArray(Manifest.FINISHED_FRAMES);
        try {
            for (int i = 0; i < finishedFrames.length(); i++) {
                if(finishedFrames.getString(i).equals(frameComplexId)) {
                    return true;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Stages and commits changes to the repository
     * @throws Exception
     */
    public void commit() throws Exception {
        commit(".", null);
    }

    /**
     * Stages and commits changes to the repository
     * @param listener
     * @throws Exception
     */
    public void commit(OnCommitListener listener) throws Exception {
        commit(".", listener);
    }

    /**
     * Stages and commits changes to the repository
     * @param filePattern the file pattern that will be used to match files for staging
     */
    private void commit(String filePattern, final OnCommitListener listener) throws Exception {
        Git git = getRepo().getGit();

        // check if dirty
        try {
            if(git.status().call().isClean()) {
                if(listener != null) {
                    listener.onCommit(true);
                }
                return;
            }
        } catch (GitAPIException e) {
            e.printStackTrace();
        }

        // stage changes
        AddCommand add = git.add();
        add.addFilepattern(filePattern).call();

        // commit changes
        final CommitCommand commit = git.commit();
        commit.setAll(true);
        commit.setMessage("auto save");


        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    commit.call();
                    if(listener != null) {
                        listener.onCommit(true);
                    }
                } catch (Exception e) {
                    Logger.e(TargetTranslation.class.getName(), "Failed to commit changes", e);
                    if(listener != null) {
                        listener.onCommit(false);
                    }
                }
            }
        };
        thread.start();
    }

    /**
     * sets publish tag in the repository
     * @return true if successful
     */
    public void setPublishTag(final OnTagListener listener)  {
        try {
            Git git = getRepo().getGit();
            final TagCommand tag = git.tag();
            String name = "published-" + System.currentTimeMillis();
            tag.setName(name);

            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        tag.call();
                        if(listener != null) {
                            listener.onTag(true);
                        }
                    } catch (Exception e) {
                        Logger.e(TargetTranslation.class.getName(), "Failed to commit changes", e);
                        if(listener != null) {
                            listener.onTag(false);
                        }
                    }
                }
            };
            thread.start();

        } catch (Exception e) {
            Logger.e(this.getClass().toString(), "error setting publish tag", e);
            if(listener != null) {
                listener.onTag(false);
            }
        }
    }

    /**
     * gets last publish tag in the repository
     * @return true if successful
     */
    public RevCommit getLastPublishTag() throws IOException, GitAPIException {
        try {
            Git git = getRepo().getGit();
            Repository repository = git.getRepository();
            ListTagCommand tags = git.tagList();
            List<Ref> refs = tags.call();
            for (int i=refs.size()-1; i >= 0; i--) {
                Ref ref = refs.get(i);
                Logger.i(this.getClass().toString(), "Tag: " + ref + " " + ref.getName() + " " + ref.getObjectId().getName());

                // fetch all commits for this tag
                LogCommand log = git.log();
                log.setMaxCount(1);

                Ref peeledRef = repository.peel(ref);
                if(peeledRef.getPeeledObjectId() != null) {
                    log.add(peeledRef.getPeeledObjectId());
                } else {
                    log.add(ref.getObjectId());
                }

                Iterable<RevCommit> logs = log.call();
                for (RevCommit rev : logs) {
                    Logger.i(this.getClass().toString(), "....Commit: " + rev /* + ", name: " + rev.getName() + ", id: " + rev.getId().getName() */);
                    return rev;
                }
            }

        } catch (GitAPIException|IOException e) {
            Logger.w(this.getClass().toString(), "error setting publish tag", e);
            throw e;
        }
        return null;
    }

    /**
     * sets publish tag in the repository
     * @return true if successful
     */
    public PublishStatus getPublishStatus()  {

        try {
            RevCommit lastTag = getLastPublishTag();
            if(null == lastTag) {
                return PublishStatus.NOT_PUBLISHED;
            }

            RevCommit head = getGitHead(getRepo());
            if(null == head) {
                return PublishStatus.QUERY_ERROR;
            }

            if(head.getCommitTime() > lastTag.getCommitTime()) {
                return PublishStatus.NOT_CURRENT;
            }

            return PublishStatus.IS_CURRENT;

        } catch (Exception e) {
            Logger.w(this.getClass().toString(), "error setting publish tag", e);
        }

        return PublishStatus.QUERY_ERROR;
    }

    /**
     * Sets the draft that is a parent of this target translation
     * @param draftTranslation
     */
    public void setParentDraft(SourceTranslation draftTranslation) {
        mManifest.put(PARENT_DRAFT_RESOURCE_ID, draftTranslation.resourceSlug);
    }

    /**
     * Returns the draft translation that is a parent of this target translation
     */
    public SourceTranslation getParentDraft () {
        String resourceSlug = mManifest.getString(PARENT_DRAFT_RESOURCE_ID);
        if(!resourceSlug.isEmpty()) {
            return SourceTranslation.simple(getProjectId(), getTargetLanguageId(), resourceSlug);
        } else {
            return null;
        }
    }

    /**
     * Merges a local repository into this one
     * @param newDir
     * @return boolean false if there were merge conflicts
     * @throws Exception
     */
    public boolean merge(File newDir) throws Exception {
        // TODO: 2/9/2016 retain original manifest and merge manually
//        Repo newRepo = new Repo(newDir.getAbsolutePath());
        Repo repo = getRepo();

        // attach remote
        repo.deleteRemote("new");
        repo.setRemote("new", newDir.getAbsolutePath());
        FetchCommand fetch = repo.getGit().fetch();
        fetch.setRemote("new");
        FetchResult fetchResult = fetch.call();

        // create branch for new changes
        DeleteBranchCommand deleteBranch = repo.getGit().branchDelete();
        deleteBranch.setBranchNames("new");
        deleteBranch.setForce(true);
        deleteBranch.call();
        CreateBranchCommand branch = repo.getGit().branchCreate();
        branch.setName("new");
        branch.setStartPoint("new/master");
        branch.call();

        // perform merge
        MergeCommand merge = repo.getGit().merge();
        merge.setFastForward(MergeCommand.FastForwardMode.NO_FF);
        merge.include(repo.getGit().getRepository().getRef("new"));

        MergeResult result = merge.call();
        if (result.getMergeStatus().equals(MergeResult.MergeStatus.CONFLICTING)) {
            System.out.println(result.getConflicts().toString());
            return false;
        }
        return true;
    }

    public enum PublishStatus {
        IS_CURRENT,
        NOT_CURRENT,
        NOT_PUBLISHED,
        QUERY_ERROR
    }

    /**
     * Returns the repository for this target translation
     * @return
     */
    public Repo getRepo() {
        return new Repo(mTargetTranslationDirectory.getAbsolutePath());
    }

    /**
     * Returns the commit hash of the repo HEAD
     * @return
     * @throws Exception
     */
    public String getCommitHash() throws Exception {
        String tag = null;
        RevCommit commit = getGitHead(getRepo());
        if(commit != null) {
            String[] pieces = commit.toString().split(" ");
            tag = pieces[1];
        } else {
            tag = null;
        }

        return tag;
    }

    /**
     * Returns the commit HEAD
     * @param repo the repository who's HEAD is returned
     * @return
     * @throws GitAPIException, IOException
     */
    @Nullable
    private RevCommit getGitHead(Repo repo) throws GitAPIException, IOException {
        Iterable<RevCommit> commits = repo.getGit().log().setMaxCount(1).call();
        RevCommit commit = null;
        for(RevCommit c : commits) {
            commit = c;
        }
        return commit;
    }

    /**
     * Sets whether or not this target translation is publishable
     * @param publishable
     * @throws Exception
     */
    public void setPublishable(boolean publishable) throws Exception {
        setPublishable(publishable, null);
    }

    /**
     * Sets whether or not this target translation is publishable
     * @param publishable
     * @param listener
     * @throws Exception
     */
    public void setPublishable(boolean publishable, OnCommitListener listener) throws Exception {
        File readyFile = new File(mTargetTranslationDirectory, "READY");
        if(publishable) {
            try {
                readyFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            readyFile.delete();
        }
        commit(listener);
    }

    /**
     * Checks if this target translation is publishable
     * @return
     */
    public boolean getPublishable() {
        File readyFile = new File(mTargetTranslationDirectory, "READY");
        return readyFile.exists();
    }

    /**
     * Stages a chapter reference to be saved
     * @param chapterTranslation
     * @param translatedText
     */
    public void applyChapterReferenceTranslation(ChapterTranslation chapterTranslation, String translatedText) {
        try {
            saveChapterReferenceTranslation(chapterTranslation, translatedText);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Stages a chapter title to be saved
     * @param chapterTranslation
     * @param translatedText
     */
    public void applyChapterTitleTranslation(ChapterTranslation chapterTranslation, String translatedText) {
        try {
            saveChapterTitleTranslation(chapterTranslation, translatedText);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the number of items that have been translated
     * @return
     */
    public int numTranslated() {
        int numFiles = 0;
        File[] chapterDirs = mTargetTranslationDirectory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory() && !pathname.getName().equals(".git") && !pathname.getName().equals("manifest.json");
            }
        });
        if(chapterDirs != null) {
            for (File dir : chapterDirs) {
                String[] files = dir.list();
                if (files != null) {
                    numFiles += files.length;
                }
            }
        }
        return numFiles;
    }

    /**
     * Returns the number of items that have been marked as finished
     * @return
     */
    public int numFinished() {
        JSONArray finishedFrames = mManifest.getJSONArray(Manifest.FINISHED_FRAMES);
        JSONArray finishedTitles = mManifest.getJSONArray(Manifest.FINISHED_TITLES);
        JSONArray finishedReferences = mManifest.getJSONArray(Manifest.FINISHED_REFERENCES);
        return finishedFrames.length() + finishedTitles.length() + finishedReferences.length();
    }

    /**
     * Returns an array of chapter translations
     * @return
     */
    public ChapterTranslation[] getChapterTranslations() {
        String[] chapterSlugs = mTargetTranslationDirectory.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return new File(dir, filename).isDirectory() && !filename.equals(".git");
            }
        });
        Arrays.sort(chapterSlugs);
        List<ChapterTranslation> chapterTranslations = new ArrayList<>();
        if(chapterSlugs != null) {
            for (String slug : chapterSlugs) {
                ChapterTranslation c = getChapterTranslation(slug);
                if (c != null) {
                    chapterTranslations.add(c);
                }
            }
        }
        return chapterTranslations.toArray(new ChapterTranslation[chapterTranslations.size()]);
    }

    // TODO: 2/15/2016 Once the new api (v3) is built we can base all the translatable items off a ChunkTranslation object so we just need one method in place of the 4 below

    public FileHistory getFrameHistory(FrameTranslation frameTranslation) {
        try {
            return new FileHistory(getRepo(), getFrameFile(frameTranslation.getChapterId(), frameTranslation.getId()));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public FileHistory getChapterTitleHistory(ChapterTranslation chapterTranslation) {
        try {
            return new FileHistory(getRepo(), getChapterTitleFile(chapterTranslation.getId()));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public FileHistory getChapterReferenceHistory(ChapterTranslation chapterTranslation) {
        try {
            return new FileHistory(getRepo(), getChapterReferenceFile(chapterTranslation.getId()));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public FileHistory getProjectTitleHistory() {
        try {
            return new FileHistory(getRepo(), getProjectTitleFile());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns an array of frame translations for the chapter
     * @param chapterSlug
     * @return
     */
    public FrameTranslation[] getFrameTranslations(String chapterSlug, TranslationFormat frameTranslationformat) {
        String[] frameFileNames = new File(mTargetTranslationDirectory, chapterSlug).list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return !filename.equals("reference.txt") && !filename.equals("title.txt");
            }
        });
        Arrays.sort(frameFileNames);
        List<FrameTranslation> frameTranslations = new ArrayList<>();
        if(frameFileNames != null) {
            for (String fileName : frameFileNames) {
                String[] slug = fileName.split("\\.txt");
                if(slug.length == 1) {
                    FrameTranslation f = getFrameTranslation(chapterSlug, slug[0], frameTranslationformat);
                    if (f != null) {
                        frameTranslations.add(f);
                    }
                }
            }
        }
        return frameTranslations.toArray(new FrameTranslation[frameTranslations.size()]);
    }

    public interface OnCommitListener {
        void onCommit(boolean success);
    }

    public interface OnTagListener {
        void onTag(boolean success);
    }
}
