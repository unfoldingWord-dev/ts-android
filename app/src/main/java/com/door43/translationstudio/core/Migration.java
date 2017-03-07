package com.door43.translationstudio.core;

import android.content.SharedPreferences;
import com.door43.translationstudio.App;

/**
 * Provides some tools for migrating stuff from older versions of the app
 */
public class Migration {

    /**
     * Converts an old slug to the new format.
     * The conversion will occur if the old slug does not contain any underscores (the delimiter for new slugs)
     *
     * Feb 27, 2017
     * Note: this update is based on the resource container format v0.1
     *
     * old: project-lang-resource
     * new: lang-project-resource
     *
     * @return the migrated slug
     */
    public static String migrateSourceTranslationSlug(String slug) {
        if(slug == null) return null;
        if(slug.contains("_")) return slug;

        String[] pieces = slug.split("-");
        if(pieces.length < 3) return slug; // cannot process
        String project = pieces[0];
        String resource = pieces[pieces.length - 1];
        String language = "";
        for(int i=1; i<pieces.length - 1; i ++) {
            if(!language.equals("")) language += "-";
            language += pieces[i];
        }

        return language + "_" + project + "_" + resource;
    }

    /**
     * moves all settings for a target translation to new target translation (such as when target language changed)
     * @param targetTranslationId
     */
    public static void moveTargetTranslationAppSettings(String targetTranslationId, String newTargetTranslationId) {
        String[] sources = App.getOpenSourceTranslations(targetTranslationId);
        for (String source : sources) {
            App.addOpenSourceTranslation(newTargetTranslationId, source);
        }

        String source = App.getSelectedSourceTranslationId(targetTranslationId);
        App.setSelectedSourceTranslation(newTargetTranslationId, source);

        String lastFocusChapterId = App.getLastFocusChapterId(targetTranslationId);
        String lastFocusFrameId = App.getLastFocusFrameId(targetTranslationId);
        App.setLastFocus(newTargetTranslationId, lastFocusChapterId, lastFocusFrameId);

        TranslationViewMode lastViewMode = App.getLastViewMode(targetTranslationId);
        App.setLastViewMode(newTargetTranslationId, lastViewMode);

        //remove old settings
        App.clearTargetTranslationSettings(targetTranslationId);
    }

    /**
     * move a setting from one translation ID to new one and remove from the original
     * @param key
     * @param targetTranslationId
     * @param newTargetTranslationId
     * @param prefs
     * @param editor
     */
    protected static void movePrefsString(String key,  String targetTranslationId, String newTargetTranslationId, SharedPreferences prefs, SharedPreferences.Editor editor) {
        String data = prefs.getString(key + targetTranslationId, ""); // get original
        editor.putString(key + newTargetTranslationId, data); // move to new location
        editor.remove(key + targetTranslationId); // remove original
    }


}
