package com.door43.translationstudio.projects;

import android.content.SharedPreferences;
import android.net.Uri;

import com.door43.translationstudio.util.AppContext;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by joel on 12/15/2014.
 */
public class SourceLanguage extends Language {
//    private String mVariant;
    private int mDateModified = 0;
    private Map<String, Resource> mResourceMap = new HashMap<String, Resource>();
    private List<Resource> mResources = new ArrayList<Resource>();
    private String mSelectedResourceId = null;
    private Project mProject;
    private Uri mResourceCatalogUri;
    private int mResourcesDateModified = 0;
    private int mCheckingLevel = -1;

    public SourceLanguage(String code, String name, Direction direction, int dateModified) {
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
        } else {
            getResource(r.getId()).setDateModified(r.getDateModified());
        }
        mCheckingLevel = -1;
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
        SharedPreferences settings = AppContext.context().getSharedPreferences(Project.PREFERENCES_TAG, AppContext.context().MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("selected_language_resource_"+mProject.getId(), id);
        editor.apply();
    }

    /**
     * Returns the currently selected resource in the language
     * @return
     */
    public Resource getSelectedResource() {
        if(AppContext.context().rememberLastPosition()) {
            SharedPreferences settings = AppContext.context().getSharedPreferences(Project.PREFERENCES_TAG, AppContext.context().MODE_PRIVATE);
            mSelectedResourceId = settings.getString("selected_language_resource_" + mProject.getId(), null);
        }

        Resource selectedResource = getResource(mSelectedResourceId);
        if(selectedResource == null) {
            // try to use ulb resources as default
            if(getResource("ulb") != null) {
                setSelectedResource("ulb");
                return getResource("ulb");
            } else {
                // auto select the first resource if no other resource has been selected.
                int defaultResourceIndex = 0;
                setSelectedResource(defaultResourceIndex);
                return getResource(defaultResourceIndex);
            }
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

//    /**
//     * Creates a source language from a generic language.
//     * @param l
//     * @return
//     */
//    public static SourceLanguage fromLanguage(Language l) {
//        return new SourceLanguage(l.getId(), l.getName(), l.getDirection(), 0);
//    }

    /**
     * Sets the date the language was last modified.
     * @param dateModified
     */
    public void setDateModified(int dateModified) {
        mDateModified = dateModified;
    }

    /**
     * Generates a source language instance from json
     *
     * @param json
     * @return the language or null
     */
    public static SourceLanguage generate(JSONObject json) {
        try {
            JSONObject jsonLang = json.getJSONObject("language");
            Language.Direction langDirection = jsonLang.get("direction").toString().equals("ltr") ? Language.Direction.LeftToRight : Language.Direction.RightToLeft;
            SourceLanguage l = new SourceLanguage(jsonLang.get("slug").toString(), jsonLang.get("name").toString(), langDirection, Integer.parseInt(jsonLang.get("date_modified").toString()));
            if (json.has("res_catalog")) {
                l.setResourceCatalog(json.getString("res_catalog"));
            }
            return l;
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * Specifies the resource catalog url for this source language (as it pertains this this project)
     * This is where the resources will be downloaded from
     * @param resourceCatalogUrl
     */
    public void setResourceCatalog(String resourceCatalogUrl) {
        if(resourceCatalogUrl != null) {
            mResourceCatalogUri = Uri.parse(resourceCatalogUrl);

            // set the resources date modified
            String dateModified = mResourceCatalogUri.getQueryParameter("date_modified");
            if(dateModified != null) {
                mResourcesDateModified = Integer.parseInt(dateModified);
            }
        } else {
            mResourceCatalogUri = null;
        }
    }



    /**
     * Returns the url to the resource catalog
     * @return the url string or null
     */
    public Uri getResourceCatalog() {
        return mResourceCatalogUri;
    }

    /**
     * Returns the date the resources catalog was last modified
     * TRICKY: this is not always reliable because when we check for updates the new languages catalog
     * is imported giving the wrong date modified.
     * @return
     */
    public int getResourcesDateModified() {
        return mResourcesDateModified;
    }

    /**
     * Returns the maximum checking level found within this language
     *
     * @return
     */
    public int checkingLevel() {
        // TODO: it would be faster to update the checking level when we add a resource.
        if(mCheckingLevel == -1) {
            int maxCheckingLevel = -1;
            for(Resource r:mResources) {
                if(r.getCheckingLevel() > maxCheckingLevel) {
                    maxCheckingLevel = r.getCheckingLevel();
                }
            }
            mCheckingLevel = maxCheckingLevel;
        }
        return mCheckingLevel;
    }

    /**
     * Serializes the language
     * @return
     */
    public JSONObject serialize() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("res_catalog",getResourceCatalog().toString());
        JSONObject lJson = new JSONObject();
        lJson.put("slug", getId());
        lJson.put("date_modified", mDateModified);
        lJson.put("direction", getDirectionName());
        lJson.put("name", getName());
        json.put("language", lJson);
        // NOTE: the project info can be injected later by the project
        return json;
    }
}
