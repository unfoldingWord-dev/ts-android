package com.door43.translationstudio.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.SpannedString;

import com.door43.util.Manifest;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 8/29/2015.
 */
public class Translator {
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
     * Initializes a new target translation
     * @param targetLanguage the target language the project will be translated into
     * @param projectId the id of the project that will be translated
     * @return
     */
    public TargetTranslation createTargetTranslation(TargetLanguage targetLanguage, String projectId) {
        try {
            return TargetTranslation.generate(targetLanguage, projectId, mRootDir);
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
     * Exports a target translation in .tstudio
     * @param t
     * @param outputFile
     */
    public void export(TargetTranslation t, File outputFile) {
        // TODO: perform export
        // manifest
        // translation
    }
}
