package com.door43.translationstudio.projects;

import android.content.SharedPreferences;

import com.door43.translationstudio.util.MainContext;

/**
 * Created by joel on 11/25/2014.
 * @deprecated
 */
public abstract class BaseModel {
    private final String mModelType;
    private final String mPreferencesTag;

    /**
     * Creates a new model
     * @param modelType the machine readable model type e.g. project, chapter, frame.
     */
    public BaseModel(String modelType) {
        mModelType = modelType.trim().toLowerCase().replace(" ", "_");
        mPreferencesTag = "com.door43.translationstudio." + mModelType;
    }

    /**
     * Stores a setting for this model
     * @param key
     * @param value
     */
    protected void setSetting(String key, int value) {
        SharedPreferences settings = MainContext.getContext().getSharedPreferences(mPreferencesTag, MainContext.getContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(mModelType + "_" + getId() + "_" + key, value);
        editor.apply();
    }

    protected void setSetting(String key, Boolean value) {
        SharedPreferences settings = MainContext.getContext().getSharedPreferences(mPreferencesTag, MainContext.getContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(mModelType + "_" + getId() + "_" + key, value);
        editor.apply();
    }

    /**
     * Retrieves a setting for this model
     * @param key
     * @param defaultValue
     * @return
     */
    protected int getSetting(String key, int defaultValue) {
        SharedPreferences settings = MainContext.getContext().getSharedPreferences(mPreferencesTag, MainContext.getContext().MODE_PRIVATE);
        return settings.getInt(mModelType + "_" + getId() + "_" + key, defaultValue);
    }

    protected boolean getSetting(String key, Boolean defaultValue) {
        SharedPreferences settings = MainContext.getContext().getSharedPreferences(mPreferencesTag, MainContext.getContext().MODE_PRIVATE);
        return settings.getBoolean(mModelType + "_" + getId() + "_" + key, defaultValue);
    }

    /**
     * Returns the id of this model
     * @return
     */
    protected abstract String getId();

}
