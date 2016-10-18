package com.door43.translationstudio.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a set of updates available for the library
 */
@Deprecated
public class LibraryUpdates implements Serializable {
    private static final long serialVersionUID = 0L;
    private Map<String, Map<String, List<String>>> mUpdates;
    private int numSourceTranslationUpdates = 0;

    public LibraryUpdates() {
        mUpdates = new HashMap<>();
    }

    public void addUpdate(SourceTranslation translation) {
        if(!mUpdates.containsKey(translation.project.slug)) {
            mUpdates.put(translation.project.slug, new HashMap<String, List<String>>());
        }
        if(!mUpdates.get(translation.project.slug).containsKey(translation.language.slug)) {
            mUpdates.get(translation.project.slug).put(translation.language.slug, new ArrayList<String>());
        }
        if(!mUpdates.get(translation.project.slug).get(translation.language.slug).contains(translation.resource.slug)) {
            mUpdates.get(translation.project.slug).get(translation.language.slug).add(translation.resource.slug);
            numSourceTranslationUpdates ++;
        }
    }

    /**
     * Returns an array of project ids that have updates.
     * @return
     */
    public String[] getUpdatedProjects() {
        return mUpdates.keySet().toArray(new String[mUpdates.keySet().size()]);
    }

    /**
     * Checks if an update is available for a project
     * @param projectId
     * @return
     */
    public boolean hasProjectUpdate(String projectId) {
        return mUpdates.containsKey(projectId);
    }

    /**
     * Returns an array of source language ids that have updates.
     * @return
     */
    public String[] getUpdatedSourceLanguages(String projectId) {
        if(mUpdates.get(projectId) != null) {
            Set<String> sourceLanguages = mUpdates.get(projectId).keySet();
            return sourceLanguages.toArray(new String[sourceLanguages.size()]);
        } else {
            return new String[0];
        }
    }

    /**
     * Returns an array of resource ids that have updates
     * @param projectId
     * @param sourceLanguageId
     * @return
     */
    public String[] getUpdatedResources(String projectId, String sourceLanguageId) {
        if(mUpdates.get(projectId) != null && mUpdates.get(projectId).get(sourceLanguageId) != null) {
            List<String> resources = mUpdates.get(projectId).get(sourceLanguageId);
            return resources.toArray(new String[resources.size()]);
        } else {
            return new String[0];
        }
    }

    /**
     * Checks if an update is available for a source language
     * @param projectId
     * @param sourceLanguageId
     * @return
     */
    public boolean hasSourceLanguageUpdate(String projectId, String sourceLanguageId) {
        if(mUpdates.containsKey(projectId)) {
            return mUpdates.get(projectId).containsKey(sourceLanguageId);
        }
        return false;
    }

    /**
     * Removes a source language update from the list of available updates
     * If all the source languages in a project is removed it also will be removed
     * @param projectId
     * @param sourceLanguageId
     */
    public void removeSourceLanguageUpdate(String projectId, String sourceLanguageId) {
        if(mUpdates.containsKey(projectId)) {
            if(mUpdates.get(projectId) != null && mUpdates.get(projectId).get(sourceLanguageId) != null) {
                numSourceTranslationUpdates -= mUpdates.get(projectId).get(sourceLanguageId).size();
                mUpdates.get(projectId).remove(sourceLanguageId);
                if (mUpdates.get(projectId).size() == 0) {
                    mUpdates.remove(projectId);
                }
            }
        }
    }

    /**
     * Returns the number of source translations available for update
     * @return
     */
    public int numSourceTranslationUpdates() {
        return numSourceTranslationUpdates;
    }
}
