package com.door43.translationstudio.library.temp;

import com.door43.translationstudio.projects.Project;

/**
 * This class handles the temporary list of available projects
 */
public class LibraryTempData {
    private static Project[] mProjects = new Project[]{};

    public static void setAvailableProjects(Project[] projects) {
        mProjects = projects;
    }

    /**
     * Returns all of the projects
     * @return
     */
    public static Project[] getProjects() {
        return mProjects;
    }

    /**
     * Returns a cached project
     * @param i
     * @return
     */
    public static Project getProject(int i) {
        return mProjects[i];
    }
}
