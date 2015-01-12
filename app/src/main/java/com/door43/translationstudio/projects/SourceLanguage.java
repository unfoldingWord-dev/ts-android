package com.door43.translationstudio.projects;

import android.content.SharedPreferences;

import com.door43.translationstudio.util.MainContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by joel on 12/15/2014.
 */
public class SourceLanguage extends Language {
//    private String mVariant;
    private final int mDateModified;
    private Map<String, Resource> mResourceMap = new HashMap<String, Resource>();
    private List<Resource> mResources = new ArrayList<Resource>();
    private String mSelectedResourceId = null;
    private Project mProject;

    public SourceLanguage(String code, String name, Direction direction,  int dateModified) {
        super(code, name, direction);
        mDateModified = dateModified;
    }

    /**
     * Adds a resource slug to the language
     * @param r
     */
    public void addResource(Resource r) {
        if(!mResourceMap.containsKey(r.getId())) {
            mResourceMap.put(r.getId(), r);
            mResources.add(r);
        }
    }

    /**
     * Returns an array of all the resources in this language
     * @return
     */
    public Resource[] getResources() {
        return mResourceMap.values().toArray(new Resource[]{});
    }

    /**
     * Returns the timestamp when the language was last modified
     * @return
     */
    public int getDateModified() {
        return mDateModified;
    }

    /**
     * Returns a resource by id
     * @param id the resoruce id
     * @return null if the resource does not exist.
     */
    public Resource getResource(String id) {
        if(mResourceMap.containsKey(id)) {
            return mResourceMap.get(id);
        } else {
            return null;
        }
    }

    /**
     * Returns a resource by id
     * @param index the resource index
     * @return null if the resource does not exist
     */
    public Resource getResource(int index) {
        if(index < mResources.size() && index >= 0) {
            return mResources.get(index);
        } else {
            return null;
        }
    }

    /**
     * Sets the currently selected resource in the language by id
     * @param id the resource id
     * @return true if the resource exists
     */
    public boolean setSelectedResource(String id) {
        Resource r = getResource(id);
        if(r != null) {
            mSelectedResourceId = r.getId();
            storeSelectedResource(r.getId());
        }
        return r != null;
    }

    /**
     * Sets the currently selected resource in the language by index
     * @param index
     * @return
     */
    public boolean setSelectedResource(int index) {
        Resource r = getResource(index);
        if(r != null) {
            mSelectedResourceId = r.getId();
            storeSelectedResource(r.getId());
        }
        return r != null;
    }

    /**
     * stores the selected resource in the preferences so we can load it the next time the app starts
     * @param id
     */
    private void storeSelectedResource(String id) {
        SharedPreferences settings = MainContext.getContext().getSharedPreferences(Project.PREFERENCES_TAG, MainContext.getContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("selected_language_resource_"+mProject.getId(), id);
        editor.apply();
    }

    /**
     * Returns the currently selected resource in the language
     * @return
     */
    public Resource getSelectedResource() {
        if(MainContext.getContext().rememberLastPosition()) {
            SharedPreferences settings = MainContext.getContext().getSharedPreferences(Project.PREFERENCES_TAG, MainContext.getContext().MODE_PRIVATE);
            mSelectedResourceId = settings.getString("selected_language_resource_" + mProject.getId(), null);
        }

        Resource selectedResource = getResource(mSelectedResourceId);
        if(selectedResource == null) {
            // auto select the first resource if no other resource has been selected.
            int defaultResourceIndex = 0;
            setSelectedResource(defaultResourceIndex);
            return getResource(defaultResourceIndex);
        } else {
            return selectedResource;
        }
    }

    /**
     * Sets the project this source language belongs to
     * @param project
     */
    public void setProject(Project project) {
        mProject = project;
    }

    /**
     * Creates a source language from a generic language.
     * @param l
     * @return
     */
    public static SourceLanguage fromLanguage(Language l) {
        return new SourceLanguage(l.getId(), l.getName(), l.getDirection(), 0);
    }
}
