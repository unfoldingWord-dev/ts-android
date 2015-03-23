package com.door43.translationstudio.library.temp;

import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.ProjectManager;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.util.AppContext;

import java.util.ArrayList;
import java.util.List;

/**
 * This class handles the temporary list of available projects
 */
public class LibraryTempData {
    private static List<Project> mProjects = new ArrayList<>();
    private static List<Project> mNewProjects = new ArrayList<>();
    private static List<Project> mUpdatedProjects = new ArrayList<>();

    public static void setAvailableProjects(List<Project> projects) {
        mProjects = projects;
        ProjectManager.sortModelList(mProjects);
        filter();
    }

    /**
     * Filters projects into new projects and/or updated projects
     */
    private static void filter() {
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
    public static Project[] getProjects() {
        return mProjects.toArray(new Project[mProjects.size()]);
    }

    /**
     * Returns a cached project
     * @deprecated
     * @param i
     * @return
     */
    public static Project getProject(int i) {
        return mProjects.get(i);
    }

    /**
     * Returns a cached updated project
     * @param i
     * @return
     */
    public static Project getUpdatedProject(int i) {
        return mUpdatedProjects.get(i);
    }

    /**
     * Returns a cached new project
     * @param i
     * @return
     */
    public static Project getNewProject(int i) {
        return mNewProjects.get(i);
    }

    /**
     * Returns a list of projects that are new or have new languages
     * @return
     */
    public static Project[] getNewProjects() {
        return mNewProjects.toArray(new Project[mNewProjects.size()]);
    }

    /**
     * Returns a list of project updates
     * @return
     */
    public static Project[] getUpdatedProjects() {
        return mUpdatedProjects.toArray(new Project[mUpdatedProjects.size()]);
    }
}
