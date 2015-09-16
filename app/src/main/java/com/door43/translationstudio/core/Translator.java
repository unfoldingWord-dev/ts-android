package com.door43.translationstudio.core;

import android.content.Context;
import android.content.SharedPreferences;

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
    private static final String OPEN_SOURCE_TRANSLATIONS = "open_source_translations";
    private static final String SELECTED_SOURCE_TRANSLATION = "selected_source_translation_id";
    private static final String TRANSLATION_VIEW_MODE = "tranlsation_view_mode";
    private final File mRootDir;
    private final Context mContext;
    private static final String PREFERENCES_NAME = "translator";

    public Translator(Context context, File rootDir) {
        mContext = context;
        mRootDir = rootDir;
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

                clearTargetTranslationSettings(targetTranslationId);
            } catch (StringIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Adds a source translation to a target translation
     * This effectively adds a tab to the source translation card. It also updates the target
     * translation manifest to include it under the source translations used.
     *
     * @param targetTranslationId
     * @param sourceTranslation
     */
    public void addSourceTranslation(String targetTranslationId, SourceTranslation sourceTranslation) {
        TargetTranslation targetTranslation = getTargetTranslation(targetTranslationId);
        if(targetTranslation != null && sourceTranslation != null) {
            try {
                targetTranslation.addSourceTranslation(sourceTranslation);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Manifest m = Manifest.generate(mRootDir);
            JSONObject json = m.getJSONObject(targetTranslationId);
            try {
                if(!json.has(OPEN_SOURCE_TRANSLATIONS)) {
                    json.put(OPEN_SOURCE_TRANSLATIONS, new JSONArray());
                }
                JSONArray tabsJson = json.getJSONArray(OPEN_SOURCE_TRANSLATIONS);
                boolean exists = false;
                for(int i = 0; i < tabsJson.length(); i ++) {
                    String id = tabsJson.getString(i);
                    if(id.equals(sourceTranslation.getId())) {
                        exists = true;
                        break;
                    }
                }
                if(!exists) {
                    tabsJson.put(sourceTranslation.getId());
                    m.put(targetTranslationId, json);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Removes a source translation from the open source translations for the target translation.
     * This does not remove it from the list of source languages used but effectively removes it as a tab.
     * @param targetTranslationId
     * @param sourceTranslationId
     */
    public void removeSourceTranslation(String targetTranslationId, String sourceTranslationId) {
        TargetTranslation targetTranslation = getTargetTranslation(targetTranslationId);
        if(targetTranslation != null) {
            String selectedSourceTranslation = getSelectedSourceTranslationId(targetTranslationId);
            if(selectedSourceTranslation.equals(sourceTranslationId)) {
                setSelectedSourceTranslation(targetTranslationId, null);
            }

            // remove tab
            Manifest m = Manifest.generate(mRootDir);
            JSONObject json = m.getJSONObject(targetTranslationId);
            if(json.has(OPEN_SOURCE_TRANSLATIONS)) {
                try {
                    JSONArray tabsJson = json.getJSONArray(OPEN_SOURCE_TRANSLATIONS);
                    JSONArray newTabsJson = new JSONArray();
                    for (int i = 0; i < tabsJson.length(); i++) {
                        String id = tabsJson.getString(i);
                        if (!id.equals(sourceTranslationId)) {
                            newTabsJson.put(id);
                        }
                    }
                    json.put(OPEN_SOURCE_TRANSLATIONS, newTabsJson);
                    m.put(targetTranslationId, json);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            // change the selected source translation if we are removing it
            if(selectedSourceTranslation.equals(sourceTranslationId)) {
                String[] sourceTranslationIds = getSourceTranslations(targetTranslationId);
                if(sourceTranslationIds.length > 0) {
                    setSelectedSourceTranslation(targetTranslationId, sourceTranslationIds[0]);
                } else {
                    setSelectedSourceTranslation(targetTranslationId, null);
                }
            }
        }
    }

    /**
     * Returns an array of source translation id's that are open in the target translation
     * @param targetTranslationId
     * @return
     */
    public String[] getSourceTranslations(String targetTranslationId) {
        Manifest m = Manifest.generate(mRootDir);
        JSONObject json = m.getJSONObject(targetTranslationId);
        try {
            JSONArray tabsJson = json.getJSONArray(OPEN_SOURCE_TRANSLATIONS);
            List<String> sourceTranslationIds = new ArrayList<>();
            for(int i = 0; i < tabsJson.length(); i ++) {
                sourceTranslationIds.add(tabsJson.getString(i));
            }
            return sourceTranslationIds.toArray(new String[sourceTranslationIds.size()]);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new String[0];
    }

    /**
     * Resets all the user settings and configurations for a target translation
     * @param targetTranslationId
     */
    private void clearTargetTranslationSettings(String targetTranslationId) {
        Manifest m = Manifest.generate(mRootDir);
        m.remove(targetTranslationId);

        SharedPreferences prefs = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(SELECTED_SOURCE_TRANSLATION + "-" + targetTranslationId);
        editor.apply();
    }

    /**
     * Sets the source translation that is current selected for a target translation.
     * This is effectivelly the currently selected tab
     *
     * @param targetTranslationId
     * @param sourceTranslationId if null then the setting will be removed
     */
    public void setSelectedSourceTranslation(String targetTranslationId, String sourceTranslationId) {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if(sourceTranslationId != null) {
            editor.putString(SELECTED_SOURCE_TRANSLATION + "-" + targetTranslationId, sourceTranslationId);
        } else {
            editor.remove(SELECTED_SOURCE_TRANSLATION + "-" + targetTranslationId);
        }
        editor.apply();
    }

    /**
     * Returns the id of the selected source translation for the target translation
     *
     * If no source translation has been selected it will choose the first available
     * source translation
     *
     * @param targetTranslationId
     * @return
     */
    public String getSelectedSourceTranslationId(String targetTranslationId) {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        String selectedSourceTranslationId = prefs.getString(SELECTED_SOURCE_TRANSLATION + "-" + targetTranslationId, null);
        if(selectedSourceTranslationId == null) {
            String[] openSourceTranslationIds = getSourceTranslations(targetTranslationId);
            if(openSourceTranslationIds.length > 0) {
                selectedSourceTranslationId = openSourceTranslationIds[0];
                setSelectedSourceTranslation(targetTranslationId, selectedSourceTranslationId);
            }
        }
        return selectedSourceTranslationId;
    }

    /**
     * Returns the current view mode for the target translation
     * @param targetTranslationId
     */
    public TranslationViewMode getViewMode(String targetTranslationId) {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        TranslationViewMode viewMode = TranslationViewMode.get(prefs.getString(TRANSLATION_VIEW_MODE + "-" + targetTranslationId, null));
        if(viewMode == null) {
            return TranslationViewMode.READ;
        }
        return viewMode;
    }

    /**
     * Sets the current view mode for the target translation
     * @param targetTranslationId
     * @param viewMode
     */
    public void setViewMode(String targetTranslationId, TranslationViewMode viewMode) {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(TRANSLATION_VIEW_MODE + "-" + targetTranslationId, viewMode.toString());
        editor.apply();
    }
}
