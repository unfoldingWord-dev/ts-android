package com.door43.translationstudio.core;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.support.annotation.Nullable;

import org.eclipse.jgit.api.ResetCommand;
import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.resourcecontainer.ContainerTools;
import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.door43client.models.TargetLanguage;
import org.unfoldingword.tools.logger.Logger;
import com.door43.translationstudio.git.Repo;
import com.door43.util.NumericStringComparator;
import com.door43.util.FileUtilities;
import com.door43.util.Manifest;

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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Created by joel on 8/29/2015.
 */
public class TargetTranslation {
    public static final String TAG = TargetTranslation.class.getSimpleName();
    public static final int PACKAGE_VERSION = 7; // the version of the target translation implementation
    public static final String LICENSE_FILE = "LICENSE.md";
    public static final String OBS_LICENSE_FILE = "OBS_LICENSE.md";

    private static final String FIELD_PARENT_DRAFT = "parent_draft";
    private static final String FIELD_FINISHED_CHUNKS = "finished_chunks";
    private static final String FIELD_TRANSLATORS = "translators";

    public static final String FIELD_MANIFEST_TARGET_LANGUAGE = "target_language";
    public static final String FIELD_MANIFEST_FORMAT = "format";
    public static final String FIELD_MANIFEST_RESOURCE = "resource";
    public static final String FIELD_SOURCE_TRANSLATIONS = "source_translations";
    public static final String FIELD_MANIFEST_PACKAGE_VERSION = "package_version";
    public static final String FIELD_MANIFEST_PROJECT = "project";
    public static final String FIELD_MANIFEST_GENERATOR = "generator";
    public static final String FIELD_MANIFEST_TRANSLATION_TYPE = "type";
    public static final String FIELD_TRANSLATION_FORMAT = "format";
    public static final String FIELD_MANIFEST_ID = "id";
    public static final String FIELD_MANIFEST_NAME = "name";
    public static final String FIELD_MANIFEST_BUILD = "build";
    public static final String APPLICATION_NAME = "ts-android";
    public static final String OBS_PROJECT_TYPE = "obs";

    private final File targetTranslationDir;
    private final Manifest manifest;
    private String targetLanguageId;
    private String targetLanguageName;
    private String targetLanguageDirection;
    private final String projectId;
    private final String projectName;
    private final ResourceType resourceType;
    private final String translationTypeName;

    private String resourceSlug = null;
    private String resourceName = null;

    private TranslationFormat mTranslationFormat;
    private PersonIdent author = null;
    private String targetLanguageRegion = "unknown";

    /**
     * Creates a new instance of the target translation
     * @param targetTranslationDir
     */
    private TargetTranslation(File targetTranslationDir) throws Exception {
        this.targetTranslationDir = targetTranslationDir;
        this.manifest = Manifest.generate(targetTranslationDir);

        // target language
        JSONObject targetLanguageJson = this.manifest.getJSONObject(FIELD_MANIFEST_TARGET_LANGUAGE);
        this.targetLanguageId = targetLanguageJson.getString(FIELD_MANIFEST_ID);
        this.targetLanguageName = Manifest.valueExists(targetLanguageJson, FIELD_MANIFEST_NAME) ? targetLanguageJson.getString(FIELD_MANIFEST_NAME) : this.targetLanguageId.toUpperCase();
        this.targetLanguageDirection = targetLanguageJson.getString("direction");
        if(targetLanguageJson.has("region")) {
            this.targetLanguageRegion = targetLanguageJson.getString("region");
        }

        // project
        JSONObject projectJson = this.manifest.getJSONObject(FIELD_MANIFEST_PROJECT);
        this.projectId = projectJson.getString(FIELD_MANIFEST_ID);
        this.projectName = Manifest.valueExists(projectJson, FIELD_MANIFEST_NAME) ? projectJson.getString(FIELD_MANIFEST_NAME) : this.projectId.toUpperCase();

        // translation type
        JSONObject typeJson = this.manifest.getJSONObject(FIELD_MANIFEST_TRANSLATION_TYPE);
        this.resourceType = ResourceType.get(typeJson.getString(FIELD_MANIFEST_ID));
        this.translationTypeName = Manifest.valueExists(typeJson, FIELD_MANIFEST_NAME) ? typeJson.getString(FIELD_MANIFEST_NAME) : this.resourceType.toString().toUpperCase();

        if(this.resourceType == ResourceType.TEXT) {
            // resource
            JSONObject resourceJson = this.manifest.getJSONObject(FIELD_MANIFEST_RESOURCE);
            this.resourceSlug = resourceJson.getString(FIELD_MANIFEST_ID);
            this.resourceName = Manifest.valueExists(resourceJson, FIELD_MANIFEST_NAME) ? resourceJson.getString(FIELD_MANIFEST_NAME) : this.resourceSlug.toUpperCase();
        }

        mTranslationFormat = readTranslationFormat();
    }

    /**
     * Returns the id of the target translation
     * @return
     */
    public String getId() {
        return generateTargetTranslationId(this.targetLanguageId, this.projectId, this.resourceType, this.resourceSlug);
    }

    /**
     * Returns the translation type of the target translation
     * @return
     */
    public ResourceType getTranslationType() {
        return resourceType;
    }

    /**
     * Returns a properly formatted target translation id
     * @param targetLanguageSlug
     * @param projectSlug
     * @param resourceType
     * @param resourceSlug
     * @return
     */
    public static String generateTargetTranslationId(String targetLanguageSlug, String projectSlug, ResourceType resourceType, String resourceSlug) {
        String id = targetLanguageSlug + "_" + projectSlug + "_" + resourceType;
        if(resourceType == ResourceType.TEXT && resourceSlug != null) {
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
    public String getTargetLanguageDirection() {
        return targetLanguageDirection;
    }

    /**
     * Returns the name of the target language
     * @return
     */
    public String getTargetLanguageName() {
        return targetLanguageName;
    }

    public String getTargetLanguageRegion() {
        return targetLanguageRegion;
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
     * determine if project type is OBS
     * @return
     */
    public boolean isObsProject() {
        return isObsProject(getProjectId());
    }

    /**
     * read the format of the translation
     * @return
     */
    private TranslationFormat readTranslationFormat() {
        TranslationFormat format = fetchTranslationFormat(manifest);
        if(null == format) {
            ResourceType resourceType = fetchTranslationType(manifest);
            if(resourceType != ResourceType.TEXT) {
                return TranslationFormat.MARKDOWN;
            } else {
                String projectIdStr = fetchProjectID(manifest);
                if(isObsProject(projectIdStr)) {
                    return TranslationFormat.MARKDOWN;
                }
                return TranslationFormat.USFM;
            }
        }
        return format;
    }

    private static boolean isObsProject(String projectId) {
        return OBS_PROJECT_TYPE.equalsIgnoreCase(projectId);
    }

    private static TranslationFormat fetchTranslationFormat(Manifest manifest) {
        String formatStr = manifest.getString(FIELD_TRANSLATION_FORMAT);
        return TranslationFormat.get(formatStr);
    }

    public static String fetchProjectID(Manifest manifest) {
        String projectIdStr = "";
        JSONObject projectIdJson = manifest.getJSONObject(FIELD_MANIFEST_PROJECT);
        if(projectIdJson != null) {
            try {
                projectIdStr = projectIdJson.getString(FIELD_MANIFEST_ID);
            } catch(Exception e) {
                projectIdStr = "";
            }
        }
        return projectIdStr;
    }

    public static ResourceType fetchTranslationType(Manifest manifest) {
        String translationTypeStr = "";
        JSONObject typeJson = manifest.getJSONObject(FIELD_MANIFEST_TRANSLATION_TYPE);
        if(typeJson != null) {
            try {
                translationTypeStr = typeJson.getString(FIELD_MANIFEST_ID);
            } catch(Exception e) {
                translationTypeStr = "";
            }
        }
        return ResourceType.get(translationTypeStr);
    }

    /**
     * Opens an existing target translation.
     * @param targetTranslationDir
     * @return null if the directory does not exist or the manifest is invalid
     */
    @Nullable
    public static TargetTranslation open(File targetTranslationDir) {
        if(targetTranslationDir != null) {
            File manifestFile = new File(targetTranslationDir, "manifest.json");
            if (manifestFile.exists()) {
                try {
                    JSONObject manifest = new JSONObject(FileUtilities.readFileToString(manifestFile));
                    int version = manifest.getInt(FIELD_MANIFEST_PACKAGE_VERSION);
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
     * @param resourceType
     * @param resourceSlug
     * @param packageInfo
     * @param targetTranslationDir
     * @return
     * @throws Exception
     */
    public static TargetTranslation create(Context context, NativeSpeaker translator, TranslationFormat translationFormat, TargetLanguage targetLanguage, String projectId, ResourceType resourceType, String resourceSlug, PackageInfo packageInfo, File targetTranslationDir) throws Exception {
        targetTranslationDir.mkdirs();
        Manifest manifest = Manifest.generate(targetTranslationDir);

        // build new manifest
        JSONObject projectJson = new JSONObject();
        projectJson.put(FIELD_MANIFEST_ID, projectId);
        projectJson.put(FIELD_MANIFEST_NAME, "");
        manifest.put(FIELD_MANIFEST_PROJECT, projectJson);
        JSONObject typeJson = new JSONObject();
        typeJson.put(FIELD_MANIFEST_ID, resourceType);
        typeJson.put(FIELD_MANIFEST_NAME, resourceType.getName());
        manifest.put(FIELD_MANIFEST_TRANSLATION_TYPE, typeJson);
        JSONObject generatorJson = new JSONObject();
        generatorJson.put(FIELD_MANIFEST_NAME, APPLICATION_NAME);
        generatorJson.put(FIELD_MANIFEST_BUILD, packageInfo.versionCode);
        manifest.put(FIELD_MANIFEST_GENERATOR, generatorJson);
        manifest.put(FIELD_MANIFEST_PACKAGE_VERSION, PACKAGE_VERSION);
        JSONObject targetLanguageJson = targetLanguage.toJSON();
        targetLanguageJson.put("id", targetLanguage.slug);
        targetLanguageJson.remove("slug");
        manifest.put(FIELD_MANIFEST_TARGET_LANGUAGE, targetLanguageJson);
        manifest.put(FIELD_MANIFEST_FORMAT, translationFormat);
        JSONObject resourceJson = new JSONObject();
        resourceJson.put(FIELD_MANIFEST_ID, resourceSlug);
        manifest.put(FIELD_MANIFEST_RESOURCE, resourceJson);

        File licenseFile = new File(targetTranslationDir, LICENSE_FILE);
        InputStream is;
        if(projectId.toLowerCase().equals("obs")) {
            is = context.getAssets().open(OBS_LICENSE_FILE);
        } else {
            is = context.getAssets().open(LICENSE_FILE);
        }
        if(is != null) {
            FileUtilities.copyInputStreamToFile(is, licenseFile);
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
        generatorJson.put(FIELD_MANIFEST_NAME, "ts-android");
        PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        generatorJson.put(FIELD_MANIFEST_BUILD, pInfo.versionCode);
        targetTranslation.manifest.put(FIELD_MANIFEST_GENERATOR, generatorJson);
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
     * @param translation
     * @throws JSONException
     */
    public void addSourceTranslation(Translation translation, int modifiedAt) throws JSONException {
        JSONArray sourceTranslationsJson = manifest.getJSONArray(FIELD_SOURCE_TRANSLATIONS);
        sourceTranslationsJson = cleanupDuplicateSources(sourceTranslationsJson);

        // check for duplicate
        boolean foundDuplicate = false;
        for(int i = 0; i < sourceTranslationsJson.length(); i ++) {
            JSONObject obj = sourceTranslationsJson.getJSONObject(i);
            if(obj.getString("language_id").equals(translation.language.slug) && obj.getString("resource_id").equals(translation.resource.slug)) {
                foundDuplicate = true;
                break;
            }
        }
        if(!foundDuplicate) {
            JSONObject translationJson = new JSONObject();
            translationJson.put("language_id", translation.language.slug);
            translationJson.put("resource_id", translation.resource.slug);
            translationJson.put("checking_level", translation.resource.checkingLevel);
            translationJson.put("date_modified", modifiedAt);
            translationJson.put("version", translation.resource.version);
            sourceTranslationsJson.put(translationJson);
            manifest.put(FIELD_SOURCE_TRANSLATIONS, sourceTranslationsJson);
        }
    }

    /**
     * cleanup in case we have duplicates in the list
     * @param sourceTranslationsJson
     * @return
     * @throws JSONException
     */
    private JSONArray cleanupDuplicateSources(JSONArray sourceTranslationsJson) throws JSONException {
        boolean doCleanup = false;
        Map<String, JSONObject> sources = new HashMap<>();

        int length = sourceTranslationsJson.length();
        for (int i = 0; i < length; i++) {
            Object object = sourceTranslationsJson.get(i);
            if(!(object instanceof JSONObject)) {
                doCleanup = true; // invalid type, skip
                continue;
            }
            JSONObject obj = (JSONObject) object;

            String sourceLanguageSlug = obj.getString("language_id");
            String resourceSlug = obj.getString("resource_id");

            String containerSlug = ContainerTools.makeSlug(sourceLanguageSlug, this.projectId, resourceSlug);
            if(sources.containsKey(containerSlug)) {
                doCleanup = true; // duplicate
            }
            sources.put(containerSlug, obj); // save most recent
        }

        if(doCleanup) {
            JSONArray newSourceTranslationsJson = new JSONArray();
            for (Map.Entry<String, JSONObject> source : sources.entrySet()) {
                newSourceTranslationsJson.put(source.getValue());
            }
            sourceTranslationsJson = newSourceTranslationsJson;
            Logger.i(TAG, "Cleaning up from " + length + " items to " + newSourceTranslationsJson.length());
            manifest.put(FIELD_SOURCE_TRANSLATIONS, sourceTranslationsJson);
        }
        return sourceTranslationsJson;
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

                String containerSlug = ContainerTools.makeSlug(sourceLanguageSlug, this.projectId, resourceSlug);
                sources.add(containerSlug);
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
     * @param chapterId
     * @param frameId
     * @param format
     * @return
     */
    public FrameTranslation getFrameTranslation(String chapterId, String frameId, TranslationFormat format) {
        File frameFile = getFrameFile(chapterId, frameId);
        if(frameFile.exists()) {
            try {
                String body = FileUtilities.readFileToString(frameFile);
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
                reference = FileUtilities.readFileToString(referenceFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(titleFile.exists()) {
            try {
                title = FileUtilities.readFileToString(titleFile);
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
                title = FileUtilities.readFileToString(titleFile);
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
            FileUtilities.writeStringToFile(titleFile, translatedText);
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
            FileUtilities.writeStringToFile(frameFile, translatedText);
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
            FileUtilities.writeStringToFile(chapterReferenceFile, translatedText);
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
            FileUtilities.writeStringToFile(chapterTitleFile, translatedText);
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
        return new File(targetTranslationDir, "front/title.txt");
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
        return isChunkClosed("front-" + component);
    }

    /**
     * Reopens a project component
     * @param component
     * @return
     */
    private boolean openProjectComponent(String component) {
        return openChunk("front-" + component);
    }

    /**
     * Marks a component of the project as finished
     * @param component
     * @return
     */
    private boolean finishProjectComponent(String component) {
        return closeChunk("front-" + component);
    }

    /**
     * Marks the translation of a chapter title as complete
     * @param chapterSlug
     * @return returns true if the translation actually exists and the update was successful
     */
    public boolean finishChapterTitle(String chapterSlug) {
        File file = getChapterTitleFile(chapterSlug);
        if(file.exists()) {
            return closeChunk(chapterSlug + "-title");
        }
        return false;
    }

    /**
     * Marks the translation of a chapter title as not complete
     * @param chapterSlug
     * @return
     */
    public boolean reopenChapterTitle(String chapterSlug) {
        return openChunk(chapterSlug + "-title");
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
     * @param chapterSlug
     * @return returns true if the translation actually exists and the update was successful
     */
    public boolean finishChapterReference(String chapterSlug) {
        File file = getChapterReferenceFile(chapterSlug);
        if(file.exists()) {
            return closeChunk(chapterSlug + "-reference");
        }
        return false;
    }

    /**
     * Marks the translation of a chapter title as not complete
     * @param chapterSlug
     * @return
     */
    public boolean reopenChapterReference(String chapterSlug) {
        return openChunk(chapterSlug + "-reference");
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
     * @param chapterSlug
     * @param chunkSlug
     * @return returns true if the translation actually exists and the update was successful
     */
    public boolean finishFrame(String chapterSlug, String chunkSlug) {
        File file = getFrameFile(chapterSlug, chunkSlug);
        if(file.exists()) {
            return closeChunk(chapterSlug + "-" + chunkSlug);
        }
        return false;
    }

    /**
     * Marks the translation of a frame as not complete
     * @param chapterSlug
     * @param chunkSlug
     * @return
     */
    public boolean reopenFrame(String chapterSlug, String chunkSlug) {
        return openChunk(chapterSlug + "-" + chunkSlug);
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
        return commitSync(filePattern, true);
    }

    public boolean commitSync(String filePattern, boolean forced) throws Exception {
        Git git = getRepo().getGit();

        // check if dirty
        if(isClean()) {
            return true;
        }

        // stage changes
        AddCommand add = git.add();
        add.addFilepattern(filePattern);

        if(forced) {
            try {
                add.call();
            } catch (Exception e) {
                Logger.e(TAG, "Failed to stage changes for " + getId(), e);
            }
        } else {
            add.call();
        }

        // commit changes
        final CommitCommand commit = git.commit();
        commit.setAll(true);
        if(author != null) {
            commit.setAuthor(author);
        }
        commit.setMessage("auto save");

        if(forced) {
            try {
                commit.call();
            } catch (Exception e) {
                Logger.e(TargetTranslation.class.getName(), "Failed to commit changes for " + getId(), e);
                return false;
            }
        } else {
            commit.call();
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
     * Sets the draft that is a parent of this target translation
     * @param draftTranslation
     */
    public void setParentDraft(ResourceContainer draftTranslation) {
        JSONObject draftStatus = new JSONObject();
        try {
            draftStatus.put("resource_id", draftTranslation.resource.slug);
            draftStatus.put("checking_level", draftTranslation.resource.checkingLevel);
            draftStatus.put("version", draftTranslation.resource.version);
            // TODO: 3/2/2016 need to update resource object to collect all info from api so we can include more detail here
            manifest.put(FIELD_PARENT_DRAFT, draftStatus);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * undo last merge
     * @return
     */
    public boolean resetToMasterBackup() {
        try { // restore state before the pull
            Git git = getRepo().getGit();
            ResetCommand resetCommand = git.reset();
            resetCommand.setMode(ResetCommand.ResetType.HARD)
                    .setRef("backup-master")
                    .call();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Merges a local repository into this one
     * @param newDir
     * @return boolean false if there were merge conflicts
     * @throws Exception
     */
    public boolean merge(File newDir) throws Exception {
        // commit everything
        TargetTranslation importedTargetTranslation = TargetTranslation.open(newDir);
        if(importedTargetTranslation != null) {
            importedTargetTranslation.commitSync();
        }
        commitSync();

        Manifest importedManifest = Manifest.generate(newDir);
        Repo repo = getRepo();

        // create a backup branch
        Git git  = repo.getGit();
        DeleteBranchCommand deleteBranchCommand = git.branchDelete();
        deleteBranchCommand.setBranchNames("backup-master")
                .setForce(true)
                .call();
        CreateBranchCommand createBranchCommand = git.branchCreate();
        createBranchCommand.setName("backup-master")
                .setForce(true)
                .call();

        // attach remote
        repo.deleteRemote("new");
        repo.setRemote("new", newDir.getAbsolutePath());
        FetchCommand fetch = repo.getGit().fetch();
        fetch.setRemote("new");
        fetch.call();

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
        mergeManifests(manifest, importedManifest);

        if (result.getMergeStatus().equals(MergeResult.MergeStatus.CONFLICTING)) {
            System.out.println(result.getConflicts().toString());
            return false;
        }
        return true;
    }

    /**
     * Merges two manifest files together
     * @param original
     * @param imported
     * @return
     */
    public static Manifest mergeManifests(Manifest original, Manifest imported) {
        // merge manifests
        // TODO: 5/25/16 merge notes
        original.join(imported.getJSONArray(FIELD_TRANSLATORS), FIELD_TRANSLATORS);
        original.join(imported.getJSONArray(FIELD_FINISHED_CHUNKS), FIELD_FINISHED_CHUNKS);
        original.join(imported.getJSONArray(FIELD_SOURCE_TRANSLATIONS), FIELD_SOURCE_TRANSLATIONS);

        // add missing parent draft status
        if((!original.has(FIELD_PARENT_DRAFT) || !Manifest.valueExists(original.getJSONObject(FIELD_PARENT_DRAFT), "resource_id"))
                && imported.has(FIELD_PARENT_DRAFT)) {
            original.put(FIELD_PARENT_DRAFT, imported.getJSONObject(FIELD_PARENT_DRAFT));
        }
        return original;
    }

    /**
     * Sets the new language request that represents the temporary language code being used by this target translation
     * @param request
     * @throws IOException
     */
    public void setNewLanguageRequest(NewLanguageRequest request) throws IOException {
        File requestFile = new File(getPath(), "new_language.json");
        if(request != null) {
            FileUtilities.writeStringToFile(requestFile, request.toJson());
        } else if(requestFile.exists()) {
            FileUtilities.safeDelete(requestFile);
        }
    }

    /**
     * Returns the new language request if one exists
     * @return
     * @throws IOException
     */
    @Nullable
    public NewLanguageRequest getNewLanguageRequest() {
        File requestFile = new File(getPath(), "new_language.json");
        if(requestFile.exists()) {
            try {
                String data = FileUtilities.readFileToString(requestFile);
                return NewLanguageRequest.generate(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Changes the target language for this target translation
     * This does not change the name of the directory where the target translation is stored.
     * @param targetLanguage
     */
    public void changeTargetLanguage(TargetLanguage targetLanguage) {
        JSONObject languageJson = this.manifest.getJSONObject("target_language");
        try {
            languageJson.put("name", targetLanguage.name);
            languageJson.put("direction", targetLanguage.direction);
            languageJson.put("id", targetLanguage.slug);
            this.manifest.put("target_language", languageJson);
            this.targetLanguageDirection = targetLanguage.direction;
            this.targetLanguageId = targetLanguage.slug;
            this.targetLanguageName = targetLanguage.name;
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public TargetLanguage getTargetLanguage() {
        return new TargetLanguage(targetLanguageId, targetLanguageName, "", targetLanguageDirection, targetLanguageRegion, false);
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
        Arrays.sort(chapterSlugs, new NumericStringComparator());
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
        Arrays.sort(frameFileNames, new NumericStringComparator());
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
}
