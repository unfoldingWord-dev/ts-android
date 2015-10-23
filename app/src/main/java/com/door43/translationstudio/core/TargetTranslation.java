package com.door43.translationstudio.core;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.git.Repo;
import com.door43.translationstudio.git.tasks.StopTaskException;
import com.door43.translationstudio.git.tasks.repo.CommitTask;
import com.door43.util.Manifest;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Timer;

/**
 * Created by joel on 8/29/2015.
 */
public class TargetTranslation {
    private static final long COMMIT_DELAY = 5000;
    private static final int PACKAGE_VERSION = 2; // the version of the translation format
    private final String mTargetLanguageId;
    private final String mProjectId;
    private static final String GLOBAL_PROJECT_ID = "uw";
    private final File mTargetTranslationDirectory;
    private final Manifest mManifest;
    private final String mTargetTranslationName;
    private final LanguageDirection mDirection;
    private Timer mApplyFrameTimer;

    public TargetTranslation(String targetLanguageId, String projectId, File rootDir) {
        mTargetLanguageId = targetLanguageId;
        mProjectId = projectId;
        mTargetTranslationDirectory = generateTargetTranslationDir(generateTargetTranslationId(targetLanguageId, projectId), rootDir);;
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
     * @param mRootDir the parent directory in which the target translation directory will be created
     * @return
     */
    public static TargetTranslation generate(Context context, TargetLanguage targetLanguage, String projectId, File mRootDir) throws Exception {
        // generate new target translation if it does not exist
        File translationDir = generateTargetTranslationDir(generateTargetTranslationId(targetLanguage.getId(), projectId), mRootDir);
        if(!translationDir.exists()) {
            // build new manifest
            Manifest manifest = Manifest.generate(translationDir);
            manifest.put("slug", projectId);
            JSONObject generatorJson = new JSONObject();
            generatorJson.put("name", "ts-android");
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            generatorJson.put("build", pInfo.versionCode);
            manifest.put("generator", generatorJson);
            manifest.put("package_version", PACKAGE_VERSION);
            JSONObject targetLangaugeJson = new JSONObject();
            targetLangaugeJson.put("direction", targetLanguage.direction.toString());
            targetLangaugeJson.put("slug", targetLanguage.code);
            targetLangaugeJson.put("name", targetLanguage.name);
            // TODO: we should restructure this output to match what we see in the api. if we do we'll need to migrate all the old manifest files.
            // also the target language should have a toJson method that will do all of this.
            manifest.put("target_language", targetLangaugeJson);
        }
        // load the target translation (new or otherwise)
        return new TargetTranslation(targetLanguage.getId(), projectId, mRootDir);
    }

    /**
     * Returns a properly formatted target language id
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
    public void useSourceTranslation(SourceTranslation sourceTranslation) throws JSONException {
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
        File referenceFile = getChapterReferenceFile(chapter.getId());
        File titleFile = getChapterTitleFile(chapter.getId());
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
            saveFrameTranslation(frameTranslation, translatedText);
        } catch (IOException e) {
            e.printStackTrace();
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
    private File getFrameFile(String chapterId, String frameId) {
        return new File(mTargetTranslationDirectory, chapterId + "/" + frameId + ".txt");
    }

    /**
     * Returns the reference file
     * @param chapterId
     * @return
     */
    private File getChapterReferenceFile(String chapterId) {
        return new File(mTargetTranslationDirectory, chapterId + "/reference.txt");
    }

    /**
     * Returns the title file
     * @param chapterId
     * @return
     */
    private File getChapterTitleFile(String chapterId) {
        return new File(mTargetTranslationDirectory, chapterId + "/title.txt");
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
        return isFrameFinished(frame.getComplexId());
    }

    /**
     * Checks if the translation of a frame is complete
     * @param frameComplexId
     * @return
     */
    private boolean isFrameFinished(String frameComplexId) {
        JSONObject framesJson = mManifest.getJSONObject("frames");
        if(framesJson.has(frameComplexId)) {
            try {
                JSONObject frameJson = framesJson.getJSONObject(frameComplexId);
                if(frameJson.has("finished")) {
                    return frameJson.getBoolean("finished");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Stages and commits changes to the repository
     * @throws Exception
     */
    public void commit() throws Exception {
        commit(".");
    }

    /**
     * Stages and commits changes to the repository
     * @param filePattern the file pattern that will be used to match files for staging
     */
    public void commit(String filePattern) throws Exception {
        Git git = getRepo().getGit();

        // check if dirty
        try {
            if(git.status().call().isClean()) {
                return;
            }
        } catch (GitAPIException e) {
            e.printStackTrace();
        }

        // stage changes
        AddCommand add = git.add();
        add.addFilepattern(filePattern).call();

        // commit changes
        CommitCommand commit = git.commit();
        commit.setAll(true);
        commit.setMessage("auto save");
        commit.call();
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
    public String commitHash() throws Exception {
        Repo repo = getRepo();
        String tag = null;
        Iterable<RevCommit> commits = repo.getGit().log().setMaxCount(1).call();
        RevCommit commit = null;
        for(RevCommit c : commits) {
            commit = c;
        }
        if(commit != null) {
            String[] pieces = commit.toString().split(" ");
            tag = pieces[1];
        } else {
            tag = null;
        }

        return tag;
    }

    /**
     * Sets whether or not this target translation is publishable
     * @param publishable
     */
    public void setPublishable(boolean publishable) throws Exception {
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
        commit();
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
                return pathname.isDirectory();
            }
        });
        for(File dir:chapterDirs) {
            numFiles += dir.list().length;
        }
        return numFiles;
    }
}
