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
}
