package com.door43.translationstudio.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.text.Editable;
import android.text.SpannedString;

import com.door43.translationstudio.AppContext;
import com.door43.util.Manifest;
import com.door43.util.Zip;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 8/29/2015.
 */
public class Translator {
    private static final int TSTUDIO_PACKAGE_VERSION = 2;
    private static final String GENERATOR_NAME = "ts-android";
    public static final String ARCHIVE_EXTENSION = "tstudio";
    private final File mRootDir;
    private final Context mContext;

    public Translator(Context context, File rootDir) {
        mContext = context;
        mRootDir = rootDir;
    }

    /**
     * Returns the root directory to the target translations
     * @return
     */
    public File getPath() {
        return mRootDir;
    }

    /**
     * Returns an array of all active translations
     * @return
     */
    public TargetTranslation[] getTargetTranslations() {
        final List<TargetTranslation> translations = new ArrayList<>();
        mRootDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                TargetTranslation translation = getTargetTranslation(filename);
                if(translation != null) {
                    translations.add(translation);
                }
                return false;
            }
        });

        return translations.toArray(new TargetTranslation[translations.size()]);
    }

    /**
     * Returns the local translations cache directory.
     * This is where import and export operations can expand files.
     * @return
     */
    private File getLocalCacheDir() {
        return new File(mRootDir, "cache");
    }

    /**
     * Initializes a new target translation
     * @param targetLanguage the target language the project will be translated into
     * @param projectId the id of the project that will be translated
     * @return
     */
    public TargetTranslation createTargetTranslation(TargetLanguage targetLanguage, String projectId) {
        try {
            return TargetTranslation.generate(mContext, targetLanguage, projectId, mRootDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns a target translation if it exists
     * @param targetLanguageId
     * @param projectId
     * @return
     */
    public TargetTranslation getTargetTranslation(String targetLanguageId, String projectId) {
        File dir = TargetTranslation.generateTargetTranslationDir(targetLanguageId, projectId, mRootDir);
        if(dir.exists()) {
            return new TargetTranslation(targetLanguageId, projectId, mRootDir);
        } else {
            return null;
        }
    }

    /**
     * Returns a target translation if it exists
     * @param targetTranslationId
     * @return
     */
    public TargetTranslation getTargetTranslation(String targetTranslationId) {
        if(targetTranslationId != null) {
            try {
                String projectId = TargetTranslation.getProjectIdFromId(targetTranslationId);
                String targetLanguageId = TargetTranslation.getTargetLanguageIdFromId(targetTranslationId);
                return getTargetTranslation(targetLanguageId, projectId);
            } catch (StringIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Deletes a target translation from the device
     * @param targetTranslationId
     */
    public void deleteTargetTranslation(String targetTranslationId) {
        if(targetTranslationId != null) {
            try {
                String projectId = TargetTranslation.getProjectIdFromId(targetTranslationId);
                String targetLanguageId = TargetTranslation.getTargetLanguageIdFromId(targetTranslationId);
                File dir = TargetTranslation.generateTargetTranslationDir(targetLanguageId, projectId, mRootDir);
                FileUtils.deleteQuietly(dir);
            } catch (StringIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Compiles all the spans within the text into human readable strings
     * @param text
     * @return
     */
    public static String compileTranslation(Editable text) {
        StringBuilder compiledString = new StringBuilder();
        int next;
        int lastIndex = 0;
        for (int i = 0; i < text.length(); i = next) {
            next = text.nextSpanTransition(i, text.length(), SpannedString.class);
            SpannedString[] verses = text.getSpans(i, next, SpannedString.class);
            for (SpannedString s : verses) {
                int sStart = text.getSpanStart(s);
                int sEnd = text.getSpanEnd(s);
                // attach preceeding text
                if (lastIndex >= text.length() | sStart >= text.length()) {
                    // out of bounds
                }
                compiledString.append(text.toString().substring(lastIndex, sStart));
                // explode span
                compiledString.append(s.toString());
                lastIndex = sEnd;
            }
        }
        // grab the last bit of text
        compiledString.append(text.toString().substring(lastIndex, text.length()));
        return compiledString.toString().trim();
    }

    /**
     * Exports a single target translation in .tstudio format
     * @param t
     * @param outputFile
     */
    public void export(TargetTranslation t, File outputFile) throws Exception {
        if(!FilenameUtils.getExtension(outputFile.getName()).toLowerCase().equals(ARCHIVE_EXTENSION)) {
            throw new Exception("Not a translationStudio archive");
        }

        // build manifest
        JSONObject manifestJson = new JSONObject();
        JSONObject generatorJson = new JSONObject();
        generatorJson.put("name", GENERATOR_NAME);
        PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
        generatorJson.put("build", pInfo.versionCode);
        manifestJson.put("generator", generatorJson);
        manifestJson.put("package_version", TSTUDIO_PACKAGE_VERSION);
        manifestJson.put("timestamp", System.currentTimeMillis() / 1000L);
        JSONArray translationsJson = new JSONArray();
        JSONObject translationJson = new JSONObject();
        translationJson.put("path", t.getId());
        translationJson.put("id", t.getId());
        translationJson.put("commit_hash", t.commitHash());
        translationJson.put("direction", t.getTargetLanguageDirection());
        translationJson.put("target_language_name", t.getTargetLanguageName());
        translationsJson.put(translationJson);
        manifestJson.put("target_translations", translationsJson);

        File tempCache = new File(getLocalCacheDir(), System.currentTimeMillis()+"");
        try {
            tempCache.mkdirs();
            File manifestFile = new File(tempCache, "manifest.json");
            manifestFile.createNewFile();
            FileUtils.write(manifestFile, manifestJson.toString());
            Zip.zip(new File[]{manifestFile, t.getPath()}, outputFile);
        } catch (Exception e) {
            FileUtils.deleteQuietly(tempCache);
            FileUtils.deleteQuietly(outputFile);
            throw e;
        }

        // clean
        FileUtils.deleteQuietly(tempCache);
    }

    /**
     * Imports target translations from an archive
     * todo: we should have another method that will inspect the archive and return the details to the user so they can decide if they want to import it
     * @param file
     */
    public void importArchive(File file) throws Exception {
        if(!FilenameUtils.getExtension(file.getName()).toLowerCase().equals(ARCHIVE_EXTENSION)) {
            throw new Exception("Not a translationStudio archive");
        }

        File tempCache = new File(getLocalCacheDir(), System.currentTimeMillis()+"");
        try {
            tempCache.mkdirs();
            Zip.unzip(file, tempCache);
            File[] targetTranslationDirs = Importer.importArchive(tempCache);
            for(File dir:targetTranslationDirs) {
                File newDir = new File(mRootDir, dir.getName());
                // delete existing translation
                FileUtils.deleteQuietly(newDir);
                // import new translation
                FileUtils.moveDirectory(dir, newDir);
            }
        } catch (Exception e) {
            FileUtils.deleteQuietly(tempCache);
            throw e;
        }

        // clean
        FileUtils.deleteQuietly(tempCache);
    }
}
