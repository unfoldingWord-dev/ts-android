package com.door43.translationstudio.library.temp;

import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.ProjectManager;
import com.door43.translationstudio.projects.SourceLanguage;

import java.util.ArrayList;
import java.util.List;

/**
 * This class handles the temporary list of available projects
 */
public class LibraryTempData {
    private static List<Project> mProjects = new ArrayList<>();

    public static void setAvailableProjects(List<Project> projects) {
        mProjects = projects;
        ProjectManager.sortModelList(mProjects);
    }

    /**
     * Returns all of the projects
     * @return
     */
    public static Project[] getProjects() {
        return mProjects.toArray(new Project[mProjects.size()]);
    }

    /**
     * Returns a cached project
     * @param i
     * @return
     */
    public static Project getProject(int i) {
        return mProjects.get(i);
    }
}
