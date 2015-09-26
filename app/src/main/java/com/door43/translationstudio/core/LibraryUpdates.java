package com.door43.translationstudio.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a set of updates available for the library
 */
public class LibraryUpdates implements Serializable {
    private static final long serialVersionUID = 0L;
    private Map<String, Map<String, List<String>>> mUpdates;

    public LibraryUpdates() {
        mUpdates = new HashMap<>();
    }

    public void addUpdate(SourceTranslation translation) {
        if(!mUpdates.containsKey(translation.projectId)) {
            mUpdates.put(translation.projectId, new HashMap<String, List<String>>());
        }
        if(!mUpdates.get(translation.projectId).containsKey(translation.sourceLanguageId)) {
            mUpdates.get(translation.projectId).put(translation.sourceLanguageId, new ArrayList<String>());
        }
        if(!mUpdates.get(translation.projectId).get(translation.sourceLanguageId).contains(translation.resourceId)) {
            mUpdates.get(translation.projectId).get(translation.sourceLanguageId).add(translation.resourceId);
        }
    }

    /**
     * Returns an array of project ids that have updates.
     * @return
     */
    public String[] getUpdatedProjects() {
        return mUpdates.keySet().toArray(new String[0]);
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
            return mUpdates.get(projectId).keySet().toArray(new String[0]);
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
            return mUpdates.get(projectId).get(sourceLanguageId).toArray(new String[0]);
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
            mUpdates.get(projectId).remove(sourceLanguageId);
            if(mUpdates.get(projectId).size() == 0) {
                mUpdates.remove(projectId);
            }
        }
    }

    /**
     * Removes a project update from the list of available updates
     * @param projectId
     */
    public void removeProjectUpdate(String projectId) {
        mUpdates.remove(projectId);
    }
}
