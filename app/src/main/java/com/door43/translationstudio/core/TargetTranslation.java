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
import org.eclipse.jgit.api.ListTagCommand;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.TagCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.FetchResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by joel on 8/29/2015.
 */
public class TargetTranslation {
    public static final String TAG = TargetTranslation.class.getSimpleName();
    public static final int PACKAGE_VERSION = 6; // the version of the target translation implementation
    public static final String LICENSE_FILE = "LICENSE.md";

    private static final String FIELD_PARENT_DRAFT = "parent_draft";
    private static final String FIELD_FINISHED_CHUNKS = "finished_chunks";
    private static final String FIELD_TRANSLATORS = "translators";
    private static final String FIELD_TARGET_LANGUAGE = "target_language";
    private static final String FIELD_FORMAT = "format";
    private static final String FIELD_RESOURCE = "resource";
    private static final String FIELD_SOURCE_TRANSLATIONS = "source_translations";
    private static final String FIELD_PACKAGE_VERSION = "package_version";
    private static final String FIELD_PROJECT = "project";
    private static final String FIELD_GENERATOR = "generator";
    private static final String FIELD_TRANSLATION_TYPE = "type";
    private static final String FIELD_TRANSLATION_FORMAT = "format";

    private final File targetTranslationDir;
    private final Manifest manifest;
    private final String targetLanguageId;
    private final String targetLanguageName;
    private final LanguageDirection targetLanguageDirection;
    private final String projectId;
    private final String projectName;
    private final TranslationType translationType;
    private final String translationTypeName;

    private String resourceSlug = null;
    private String resourceName = null;

    private TranslationFormat mTranslationFormat;
    private PersonIdent author = null;

    /**
     * Creates a new instance of the target translation
     * @param targetTranslationDir
     */
    private TargetTranslation(File targetTranslationDir) throws Exception {
        this.targetTranslationDir = targetTranslationDir;
        this.manifest = Manifest.generate(targetTranslationDir);

        // target language
        JSONObject targetLanguageJson = this.manifest.getJSONObject(FIELD_TARGET_LANGUAGE);
        this.targetLanguageId = targetLanguageJson.getString("id");
        this.targetLanguageName = Manifest.valueExists(targetLanguageJson, "name") ? targetLanguageJson.getString("name") : this.targetLanguageId.toUpperCase();
        this.targetLanguageDirection = LanguageDirection.get(targetLanguageJson.getString("direction"));

        // project
        JSONObject projectJson = this.manifest.getJSONObject(FIELD_PROJECT);
        this.projectId = projectJson.getString("id");
        this.projectName = Manifest.valueExists(projectJson, "name") ? projectJson.getString("name") : this.projectId.toUpperCase();

        // translation type
        JSONObject typeJson = this.manifest.getJSONObject("type");
        this.translationType = TranslationType.get(typeJson.getString("id"));
        this.translationTypeName = Manifest.valueExists(typeJson, "name") ? typeJson.getString("name") : this.translationType.toString().toUpperCase();

        if(this.translationType == TranslationType.TEXT) {
            // resource
            JSONObject resourceJson = this.manifest.getJSONObject("resource");
            this.resourceSlug = resourceJson.getString("id");
            this.resourceName = Manifest.valueExists(resourceJson, "name") ? resourceJson.getString("name") : this.resourceSlug.toUpperCase();
        }

        mTranslationFormat = readTranslationFormat();
    }

    /**
     * Returns the id of the target translation
     * @return
     */
    public String getId() {
        return generateTargetTranslationId(this.targetLanguageId, this.projectId, this.translationType, this.resourceSlug);
    }

    /**
     * Returns a properly formatted target translation id
     * @param targetLanguageSlug
     * @param projectSlug
     * @param translationType
     * @param resourceSlug
     * @return
     */
    public static String generateTargetTranslationId(String targetLanguageSlug, String projectSlug, TranslationType translationType, String resourceSlug) {
        String id = targetLanguageSlug + "_" + projectSlug + "_" + translationType;
        if(translationType == TranslationType.TEXT && resourceSlug != null) {
            id += "_" + resourceSlug;
        }
        return id.toLowerCase();
    }

    /**
     * Returns the resource slug
     * @return
     */
    public String getResourceSlug() {
        return this.resourceSlug;
    }

    /**
     * Returns the directory to this target translation
     * @return
     */
    public File getPath() {
        return targetTranslationDir;
    }

    /**
     * Returns the language direction of the target language
     * @return
     */
    public LanguageDirection getTargetLanguageDirection() {
        return targetLanguageDirection;
    }

    /**
     * Returns the name of the target language
     * @return
     */
    public String getTargetLanguageName() {
        return targetLanguageName;
    }

    /**
     * Returns the id of the project being translated
     * @return
     */
    public String getProjectId() {
        return projectId;
    }

    /**
     * Returns the id of the target language the project is being translated into
     * @return
     */
    public String getTargetLanguageId() {
        return targetLanguageId;
    }

    /**
     * get format of translation
     * @return
     */
    public TranslationFormat getFormat() {
        return mTranslationFormat;
    }

    /**
     * read the format of the translation
     * @return
     */
    private TranslationFormat readTranslationFormat() {
        TranslationFormat format = fetchTranslationFormat(manifest);
        if(null == format) {
            TranslationType translationType = fetchTranslationType(manifest);
            if(translationType != TranslationType.TEXT) {
                return TranslationFormat.MARKDOWN;
            } else {
                String projectIdStr = fetchProjectID(manifest);
                if("obs".equalsIgnoreCase(projectIdStr)) {
                    return TranslationFormat.MARKDOWN;
                }
                return TranslationFormat.USFM;
            }
        }
        return format;
    }

    private static TranslationFormat fetchTranslationFormat(Manifest manifest) {
        String formatStr = manifest.getString(FIELD_TRANSLATION_FORMAT);
        return TranslationFormat.get(formatStr);
    }

    public static String fetchProjectID(Manifest manifest) {
        String projectIdStr = "";
        JSONObject projectIdJson = manifest.getJSONObject(FIELD_PROJECT);
        if(projectIdJson != null) {
            try {
                projectIdStr = projectIdJson.getString("id");
            } catch(Exception e) {
                projectIdStr = "";
            }
        }
        return projectIdStr;
    }

    public static TranslationType fetchTranslationType(Manifest manifest) {
        String translationTypeStr = "";
        JSONObject typeJson = manifest.getJSONObject(FIELD_TRANSLATION_TYPE);
        if(typeJson != null) {
            try {
                translationTypeStr = typeJson.getString("id");
            } catch(Exception e) {
                translationTypeStr = "";
            }
        }
        return TranslationType.get(translationTypeStr);
    }

    /**
     * Opens an existing target translation.
     * @param targetTranslationDir
     * @return null if the directory does not exist or the manifest is invalid
     */
    public static TargetTranslation open(File targetTranslationDir) {
        if(targetTranslationDir != null) {
            File manifestFile = new File(targetTranslationDir, "manifest.json");
            if (manifestFile.exists()) {
                try {
                    JSONObject manifest = new JSONObject(FileUtils.readFileToString(manifestFile));
                    int version = manifest.getInt(FIELD_PACKAGE_VERSION);
                    if (version == PACKAGE_VERSION) {
                        return new TargetTranslation(targetTranslationDir);
                    } else {
                        Logger.w(TargetTranslation.class.getName(), "Unsupported target translation version " + version + " in" + targetTranslationDir.getName());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Logger.w(TargetTranslation.class.getName(), "Missing manifest file in target translation " + targetTranslationDir.getName());
            }
        }
        return null;
    }

    /**
     * Creates a new target translation
     * @param translator
     * @param translationFormat
     * @param targetLanguage
     * @param projectId
     * @param translationType
     * @param resourceSlug
     * @param packageInfo
     * @param targetTranslationDir
     * @return
     * @throws Exception
     */
    public static TargetTranslation create(Context context, NativeSpeaker translator, TranslationFormat translationFormat, TargetLanguage targetLanguage, String projectId, TranslationType translationType, String resourceSlug, PackageInfo packageInfo, File targetTranslationDir) throws Exception {
        targetTranslationDir.mkdirs();
        Manifest manifest = Manifest.generate(targetTranslationDir);

        // build new manifest
        JSONObject projectJson = new JSONObject();
        projectJson.put("id", projectId);
        projectJson.put("name", "");
        manifest.put(FIELD_PROJECT, projectJson);
        JSONObject typeJson = new JSONObject();
        typeJson.put("id", translationType);
        typeJson.put("name", translationType.getName());
        manifest.put(FIELD_TRANSLATION_TYPE, typeJson);
        JSONObject generatorJson = new JSONObject();
        generatorJson.put("name", "ts-android");
        generatorJson.put("build", packageInfo.versionCode);
        manifest.put(FIELD_GENERATOR, generatorJson);
        manifest.put(FIELD_PACKAGE_VERSION, PACKAGE_VERSION);
        manifest.put(FIELD_TARGET_LANGUAGE, targetLanguage.toJson());
        manifest.put(FIELD_FORMAT, translationFormat);
        JSONObject resourceJson = new JSONObject();
        resourceJson.put("id", resourceSlug);
        manifest.put(FIELD_RESOURCE, resourceJson);

        File licenseFile = new File(targetTranslationDir, LICENSE_FILE);
        InputStream is = context.getAssets().open(LICENSE_FILE);
        if(is != null) {
            FileUtils.copyInputStreamToFile(is, licenseFile);
        } else {
            throw new FileNotFoundException("The template LICENSE.md file could not be found in the assets");
        }

        // return the new target translation
        TargetTranslation targetTranslation = new TargetTranslation(targetTranslationDir);
        targetTranslation.addContributor(translator);
        return targetTranslation;
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
        targetTranslation.manifest.put(FIELD_GENERATOR, generatorJson);
    }

    /**
     * Returns the id of the project of the target translation
     * @param targetTranslationId the target translation id
     * @return
     */
    public static String getProjectSlugFromId(String targetTranslationId) throws StringIndexOutOfBoundsException {
        String[] complexId = targetTranslationId.split("_");
        if(complexId.length >= 3) {
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
    public static String getTargetLanguageSlugFromId(String targetTranslationId) throws StringIndexOutOfBoundsException {
        String[] complexId = targetTranslationId.split("_");
        if(complexId.length >= 3) {
            return complexId[0];
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
        JSONArray sourceTranslationsJson = manifest.getJSONArray(FIELD_SOURCE_TRANSLATIONS);
        // check for duplicate
        boolean foundDuplicate = false;
        for(int i = 0; i < sourceTranslationsJson.length(); i ++) {
            JSONObject obj = sourceTranslationsJson.getJSONObject(i);
            if(obj.getString("language_id").equals(sourceTranslation.sourceLanguageSlug) && obj.getString("resource_id").equals(sourceTranslation.resourceSlug)) {
                foundDuplicate = true;
                break;
            }
        }
        if(!foundDuplicate) {
            JSONObject translationJson = new JSONObject();
            translationJson.put("language_id", sourceTranslation.sourceLanguageSlug);
            translationJson.put("resource_id", sourceTranslation.resourceSlug);
            translationJson.put("checking_level", sourceTranslation.getCheckingLevel());
            translationJson.put("date_modified", sourceTranslation.getDateModified());
            translationJson.put("version", sourceTranslation.getVersion());
            sourceTranslationsJson.put(translationJson);
            manifest.put(FIELD_SOURCE_TRANSLATIONS, sourceTranslationsJson);
        }
    }

    /**
     * get list of source translation slugs used
     */
    public String[] getSourceTranslations() {

        try {
            List<String> sources = new ArrayList<>();

            JSONArray sourceTranslationsJson = manifest.getJSONArray(FIELD_SOURCE_TRANSLATIONS);

            for (int i = 0; i < sourceTranslationsJson.length(); i++) {
                JSONObject obj = sourceTranslationsJson.getJSONObject(i);

                String sourceLanguageSlug = obj.getString("language_id");
                String resourceSlug = obj.getString("resource_id");

                SourceTranslation sourceTranslation =  SourceTranslation.simple(this.projectId, sourceLanguageSlug, resourceSlug);
                sources.add(sourceTranslation.getId());
            }

            return sources.toArray(new String[sources.size()]);
        } catch(Exception e) {
            Logger.e(TAG, "Error reading sources", e);
        }
        return new String[0]; // return empty array on error
    }

    /**
     * Adds a native speaker as a translator
     * This will replace contributors with the same name
     * @param speaker
     */
    public void addContributor(NativeSpeaker speaker) {
        if(speaker != null) {
            removeContributor(speaker);
            JSONArray translatorsJson = manifest.getJSONArray(FIELD_TRANSLATORS);
            translatorsJson.put(speaker.name);
            manifest.put(FIELD_TRANSLATORS, translatorsJson);
        }
    }

    /**
     * Removes a native speaker from the list of translators
     * This will remove all contributors with the same name as the given speaker
     * @param speaker
     */
    public void removeContributor(NativeSpeaker speaker) {
        if(speaker != null) {
            JSONArray translatorsJson = manifest.getJSONArray(FIELD_TRANSLATORS);
            manifest.put(FIELD_TRANSLATORS, Manifest.removeValue(translatorsJson, speaker.name));
        }
    }

    /**
     * Returns the contributor that has the given name
     * @param name
     * @return null if no contributor was found
     */
    public NativeSpeaker getContributor(String name) {
        manifest.load();
        ArrayList<NativeSpeaker> translators = getContributors();
        for (NativeSpeaker speaker:translators) {
            if (speaker.name.equals(name)) {
                return speaker;
            }
        }
        return null;
    }

    /**
     * Returns an array of native speakers who have worked on this translation.
     * This will look into the "translators" field in the manifest and check in the commit history.
     * @return
     */
    public ArrayList<NativeSpeaker> getContributors() {
        manifest.load();
        JSONArray translatorsJson = manifest.getJSONArray(FIELD_TRANSLATORS);
        ArrayList<NativeSpeaker> translators = new ArrayList<>();

        for(int i = 0; i < translatorsJson.length(); i ++) {
            try {
                String name = translatorsJson.getString(i);
                if(!name.isEmpty()) {
                    translators.add(new NativeSpeaker(name));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return translators;
    }

    /**
     * This will add the default translator if no other translator has been recorded
     * @param speaker
     */
    public void setDefaultContributor(NativeSpeaker speaker) {
        if(speaker != null) {
            if (getContributors().isEmpty()) {
                addContributor(speaker);
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
        return new ChapterTranslation(title, reference, chapterSlug, isChapterTitleFinished(chapterSlug), isChapterReferenceFinished(chapterSlug), getFormat());
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
        return new File(targetTranslationDir, chapterId + "/" + frameId + ".txt");
    }

    /**
     * Returns the chapter reference file
     * @param chapterId
     * @return
     */
    public File getChapterReferenceFile(String chapterId) {
        return new File(targetTranslationDir, chapterId + "/reference.txt");
    }

    /**
     * Returns the chapter title file
     * @param chapterId
     * @return
     */
    public File getChapterTitleFile(String chapterId) {
        return new File(targetTranslationDir, chapterId + "/title.txt");
    }

    /**
     * Returns the project title file
     * @return
     */
    public File getProjectTitleFile() {
        return new File(targetTranslationDir, "00/title.txt");
    }

    /**
     * Marks the project title as finished
     * @return
     */
    public boolean closeProjectTitle() {
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
    public boolean openProjectTitle() {
        return openProjectComponent("title");
    }

    /**
     * Checks if the translation of a component of a project has been marked as done
     * @param component
     * @return
     */
    private boolean isProjectComponentFinished(String component) {
        return isChunkClosed("00-" + component);
    }

    /**
     * Reopens a project component
     * @param component
     * @return
     */
    private boolean openProjectComponent(String component) {
        return openChunk("00-" + component);
    }

    /**
     * Marks a component of the project as finished
     * @param component
     * @return
     */
    private boolean finishProjectComponent(String component) {
        return closeChunk("00-" + component);
    }

    /**
     * Marks the translation of a chapter title as complete
     * @param chapter
     * @return returns true if the translation actually exists and the update was successful
     */
    public boolean finishChapterTitle(Chapter chapter) {
        File file = getChapterTitleFile(chapter.getId());
        if(file.exists()) {
            return closeChunk(chapter.getId() + "-title");
        }
        return false;
    }

    /**
     * Marks the translation of a chapter title as not complete
     * @param chapter
     * @return
     */
    public boolean reopenChapterTitle(Chapter chapter) {
        return openChunk(chapter.getId() + "-title");
    }

    /**
     * Checks if the translation of a chapter title has been marked as done
     * @param chapterSlug
     * @return
     */
    private boolean isChapterTitleFinished(String chapterSlug) {
        return isChunkClosed(chapterSlug + "-title");
    }

    /**
     * Marks the translation of a chapter title as complete
     * @param chapter
     * @return returns true if the translation actually exists and the update was successful
     */
    public boolean finishChapterReference(Chapter chapter) {
        File file = getChapterReferenceFile(chapter.getId());
        if(file.exists()) {
            return closeChunk(chapter.getId() + "-reference");
        }
        return false;
    }

    /**
     * Marks the translation of a chapter title as not complete
     * @param chapter
     * @return
     */
    public boolean reopenChapterReference(Chapter chapter) {
        return openChunk(chapter.getId() + "-reference");
    }

    /**
     * Checks if the translation of a chapter reference has been marked as done
     * @param chapterSlug
     * @return
     */
    private boolean isChapterReferenceFinished(String chapterSlug) {
        return isChunkClosed(chapterSlug + "-reference");
    }

    /**
     * Marks the translation of a frame as complete
     * @param frame
     * @return returns true if the translation actually exists and the update was successful
     */
    public boolean finishFrame(Frame frame) {
        File file = getFrameFile(frame.getChapterId(), frame.getId());
        if(file.exists()) {
            return closeChunk(frame.getComplexId());
        }
        return false;
    }

    /**
     * Marks the translation of a frame as not complete
     * @param frame
     * @return
     */
    public boolean reopenFrame(Frame frame) {
        return openChunk(frame.getComplexId());
    }

    /**
     * Checks if the translation of a frame has been marked as done
     * @param frameComplexId
     * @return
     */
    private boolean isFrameFinished(String frameComplexId) {
        return isChunkClosed(frameComplexId);
    }

    /**
     * Closes a chunk from editing. e.g. marks as finished
     * @param complexId the chapter + chunk id e.g. `01-05`, or `01-title`
     * @return
     */
    private boolean closeChunk(String complexId) {
        if(!isChunkClosed(complexId)) {
            JSONArray finishedChunks = manifest.getJSONArray(FIELD_FINISHED_CHUNKS);
            finishedChunks.put(complexId);
            manifest.put(FIELD_FINISHED_CHUNKS, finishedChunks);
        }
        return true;
    }

    /**
     * Opens a chunk for editing. e.g. marks as not finished
     * @param complexId the chapter + chunk id e.g. `01-05`, or `01-title`
     * @return
     */
    private boolean openChunk(String complexId) {
        JSONArray finishedChunks = manifest.getJSONArray(FIELD_FINISHED_CHUNKS);
        JSONArray updatedChunks = new JSONArray();
        try {
            for (int i = 0; i < finishedChunks.length(); i++) {
                String currId = finishedChunks.getString(i);
                if(!currId.equals(complexId)) {
                    updatedChunks.put(finishedChunks.getString(i));
                }
            }
            manifest.put(FIELD_FINISHED_CHUNKS, updatedChunks);
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Checks if a chunk has been closed. e.g. has been marked as finished
     * @param complexId the chapter + chunk id e.g. `01-05`, or `01-title`
     * @return
     */
    private boolean isChunkClosed(String complexId) {
        JSONArray finishedChunks = manifest.getJSONArray(FIELD_FINISHED_CHUNKS);
        try {
            for (int i = 0; i < finishedChunks.length(); i++) {
                if(finishedChunks.getString(i).equals(complexId)) {
                    return true;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean commitSync() throws Exception {
        return commitSync(".");
    }

    /**
     * Checks if there are any non-committed changes in the repo
     * @return
     * @throws Exception
     */
    public boolean isClean() {
        try {
            Git git = getRepo().getGit();
            return git.status().call().isClean();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Sets the author to be used when making commits
     * @param name
     * @param email
     */
    public void setAuthor(String name, String email) {
        this.author = new PersonIdent(name, email);
    }

    public boolean commitSync(String filePattern) throws Exception {
        Git git = getRepo().getGit();

        // check if dirty
        if(isClean()) {
            return true;
        }

        // stage changes
        AddCommand add = git.add();
        add.addFilepattern(filePattern).call();

        // commit changes
        final CommitCommand commit = git.commit();
        commit.setAll(true);
        if(author != null) {
            commit.setAuthor(author);
        }
        commit.setMessage("auto save");

        try {
            commit.call();
        } catch (Exception e) {
            Logger.e(TargetTranslation.class.getName(), "Failed to commit changes", e);
            return false;
        }
        return true;
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
     * @param listener the listener that will be called when finished
     */
    private void commit(final String filePattern, final OnCommitListener listener) throws Exception {

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    boolean result = commitSync(filePattern);
                    if(listener != null) {
                        listener.onCommit(result);
                    }
                } catch (Exception e) {
                    if(listener != null) {
                        listener.onCommit(false);
                    }
                }
            }
        };
        thread.start();
    }

    /**
     * Marks the current HEAD of the translation repo as published
     * @return true if successful
     */
    public void setPublished(final OnPublishedListener listener)  {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    commitSync();

                    Git git = getRepo().getGit();
                    final TagCommand tag = git.tag();
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd/HH.mm.ss", Locale.US);
                    format.setTimeZone(TimeZone.getTimeZone("UTC"));
                    String name = "R2P/" + format.format(new Date());
                    tag.setName(name);
                    if(author != null) {
                        tag.setTagger(author);
                    }

                    // tag if not already
                    if(getPublishedStatus() != PublishStatus.IS_CURRENT) {
                        tag.call();
                    }
                    if (listener != null) {
                        listener.onSuccess();
                    }
                } catch (Exception e) {
                    if(listener != null) {
                        listener.onFailed(e);
                    }
                }
            }
        };
        thread.start();

    }

    /**
     * Returns the most recent published tag
     * @return
     */
    public RevCommit getLastPublishedTag() throws Exception {
        Git git = getRepo().getGit();
        Repository repository = git.getRepository();
        ListTagCommand tags = git.tagList();
        List<Ref> refs = tags.call();
        for (int i=refs.size()-1; i >= 0; i--) {
            Ref ref = refs.get(i);

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
                return rev;
            }
        }
        return null;
    }

    /**
     * Returns the status of the this target translation's published state
     * @return
     */
    public PublishStatus getPublishedStatus()  {
        try {
            RevCommit lastTag = getLastPublishedTag();
            if(null == lastTag) {
                return PublishStatus.NOT_PUBLISHED;
            }

            RevCommit head = getGitHead(getRepo());
            if(null == head) {
                return PublishStatus.ERROR;
            }

            if(head.getCommitTime() > lastTag.getCommitTime()) {
                return PublishStatus.NOT_CURRENT;
            }

            return PublishStatus.IS_CURRENT;

        } catch (Exception e) {
            Logger.w(this.getClass().toString(), "Error checking published status", e);
        }

        return PublishStatus.ERROR;
    }

    /**
     * Sets the draft that is a parent of this target translation
     * @param draftTranslation
     */
    public void setParentDraft(SourceTranslation draftTranslation) {
        JSONObject draftStatus = new JSONObject();
        try {
            draftStatus.put("resource_id", draftTranslation.resourceSlug);
            draftStatus.put("checking_level", draftTranslation.getCheckingLevel());
            draftStatus.put("version", draftTranslation.getVersion());
            // TODO: 3/2/2016 need to update resource object to collect all info from api so we can include more detail here
            manifest.put(FIELD_PARENT_DRAFT, draftStatus);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the draft translation that is a parent of this target translation.
     */
    public SourceTranslation getParentDraft () {
        try {
            JSONObject parentDraftStatus = manifest.getJSONObject(FIELD_PARENT_DRAFT);
            if(Manifest.valueExists(parentDraftStatus, "resource_id")) {
                // TODO: it would be handy to include the version of the actual parent draft so we can see if the one pulled has updates
                return SourceTranslation.simple(getProjectId(), getTargetLanguageId(), parentDraftStatus.getString("resource_id"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Merges a local repository into this one
     * @param newDir
     * @return boolean false if there were merge conflicts
     * @throws Exception
     */
    public boolean merge(File newDir) throws Exception {
        Manifest importedManifest = Manifest.generate(newDir);
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

        // merge manifests
        manifest.join(importedManifest.getJSONArray(FIELD_TRANSLATORS), FIELD_TRANSLATORS);
        manifest.join(importedManifest.getJSONArray(FIELD_FINISHED_CHUNKS), FIELD_FINISHED_CHUNKS);
        manifest.join(importedManifest.getJSONObject(FIELD_SOURCE_TRANSLATIONS), FIELD_SOURCE_TRANSLATIONS);

        // add missing parent draft status
        if((!manifest.has(FIELD_PARENT_DRAFT) || !Manifest.valueExists(manifest.getJSONObject(FIELD_PARENT_DRAFT), "resource_id"))
            && importedManifest.has(FIELD_PARENT_DRAFT)) {
            manifest.put(FIELD_PARENT_DRAFT, importedManifest.getJSONObject(FIELD_PARENT_DRAFT));
        }

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
        ERROR
    }

    /**
     * Returns the repository for this target translation
     * @return
     */
    public Repo getRepo() {
        return new Repo(targetTranslationDir.getAbsolutePath());
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
        File[] chapterDirs = targetTranslationDir.listFiles(new FileFilter() {
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
        if(manifest.has(FIELD_FINISHED_CHUNKS)) {
            JSONArray finishedFrames = manifest.getJSONArray(FIELD_FINISHED_CHUNKS);
            return finishedFrames.length();
        } else {
            return 0;
        }
    }

    /**
     * Returns an array of chapter translations
     * @return
     */
    public ChapterTranslation[] getChapterTranslations() {
        String[] chapterSlugs = targetTranslationDir.list(new FilenameFilter() {
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
        String[] frameFileNames = new File(targetTranslationDir, chapterSlug).list(new FilenameFilter() {
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

    public interface OnPublishedListener {
        void onSuccess();
        void onFailed(Exception e);
    }
}
