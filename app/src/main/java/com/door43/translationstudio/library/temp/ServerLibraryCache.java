package com.door43.translationstudio.library.temp;

import com.door43.translationstudio.core.LibraryUpdates;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.ProjectManager;
import com.door43.translationstudio.util.AppContext;

import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class handles the temporary list of available projects
 */
public class ServerLibraryCache {
    private static final int CACHE_TTL = 5 * 60 * 1000; // the cache will expire every 5 minutes.
    private static Map<String, Integer> mProjectIds = new HashMap<>();
    private static List<Project> mProjects = new ArrayList<>();
    private static List<Project> mNewProjects = new ArrayList<>();
    private static List<Project> mUpdatedProjects = new ArrayList<>();
    private static Boolean mShowNewProjects = false;
    private static Boolean mShowProjectUpdates = false;
    private static boolean mEnableEditing = false;
    private static LibraryUpdates mAvailableLibraryUpdates;
    private static int mCacheTimestamp = 0;

    @Deprecated
    public static void setAvailableProjects(List<Project> projects) {
        setProjects(projects);
        ProjectManager.sortModelList(mProjects);
        organizeProjects();
    }

    @Deprecated
    private static void setProjects(List<Project> projects) {
        mProjects = projects;
        // map id's to index
        for(int i = 0; i < projects.size(); i ++) {
            mProjectIds.put(projects.get(i).getId(), i);
        }
    }

    /**
     * Filters projects into new projects and/or updated projects
     */
    @Deprecated
    public static void organizeProjects() {
        mNewProjects = new ArrayList<>();
        mUpdatedProjects = new ArrayList<>();
        for(Project p:mProjects) {
            if(AppContext.projectManager().isNewSourceLanguageAvailable(p)) {
                mNewProjects.add(p);
            }
            if(AppContext.projectManager().isProjectUpdateAvailable(p)) {
                mUpdatedProjects.add(p);
            } else if(AppContext.projectManager().isProjectDownloaded(p.getId())) {
                mUpdatedProjects.add(p);
            }
        }
    }

    /**
     * Returns all of the projects
     * @return
     */
    @Deprecated
    public static List<Project> getProjects() {
        return mProjects;
    }

    /**
     * Returns a cached project
     * @param id
     * @return
     */
    @Deprecated
    public static Project getProject(String id) {
        if(mProjectIds.containsKey(id)) {
            return mProjects.get(mProjectIds.get(id));
        } else {
            return null;
        }
    }

    /**
     * Returns a list of projects that are new or have new languages
     * @return
     */
    @Deprecated
    public static List<Project> getNewProjects() {
        return mNewProjects;
    }

    /**
     * Returns a list of project updates
     * @return
     */
    @Deprecated
    public static List<Project> getUpdatedProjects() {
        return mUpdatedProjects;
    }

    public static void setShowNewProjects(Boolean showNewProjects) {
        mShowNewProjects = showNewProjects;
    }

    public static void setShowProjectUpdates(Boolean showProjectUpdates) {
        mShowProjectUpdates = showProjectUpdates;
    }

    /**
     * Checks if new projects should be shwon
     * @return
     */
    public static boolean getShowNewProjects() {
        return mShowNewProjects;
    }

    /**
     *
     * Checks if project updates should be shown
     * @return
     */
    public static boolean getShowProjectUpdates() {
        return mShowProjectUpdates;
    }

    /**
     * Enables the user to edit downloaded projects
     * e.g. delete them.
     * @param enableEditing
     */
    public static void setEnableEditing(boolean enableEditing) {
        mEnableEditing = enableEditing;
    }

    /**
     * Checks if the user can edit downloaded projects.
     * @return
     */
    public static boolean getEnableEditing() {
        return mEnableEditing;
    }

    /**
     * Sets the available updates
     * @param availableLibraryUpdates
     */
    public static void setAvailableUpdates(LibraryUpdates availableLibraryUpdates) {
        mAvailableLibraryUpdates = availableLibraryUpdates;
        mCacheTimestamp = (int)System.currentTimeMillis();
    }

    /**
     * Returns the available updates
     * @return
     */
    public static LibraryUpdates getAvailableUpdates() {
        return mAvailableLibraryUpdates;
    }

    /**
     * Checks if the library cache has expired
     * @return
     */
    public static boolean isExpired() {
        return (mAvailableLibraryUpdates == null || Math.abs(mCacheTimestamp - (int)System.currentTimeMillis()) > CACHE_TTL);
    }
}
