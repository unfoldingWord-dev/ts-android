package com.door43.translationstudio;

import java.util.ArrayList;

/**
 * Created by joel on 8/29/2014.
 */
public class ProjectManager {
    private static ArrayList<Project> mProjects = new ArrayList<Project>();
    private static int mSelectedIndex;

    /**
     * Adds a project to the manager
     * @param p the new project to be added
     * @return The project now managed by the manager. The return value should be used instead of the input value to ensure you are using the proper reference.
     */
    public Project add(Project p) {
        if(!this.mProjects.contains(p)) {
            this.mProjects.add(p);
            return p;
        } else {
            // TODO: is this nessesary? need to double check that the object signatures are different. If they are the same we should just always return the input project.
            return get(this.mProjects.indexOf(p));
        }
    }

    /**
     * Returns a project
     * @param i the index of the project
     * @return the existing project or null
     */
    public Project get(int i) {
        if(this.mProjects.size() > i && i >= 0) {
            return this.mProjects.get(i);
        } else {
            // out of bounds
            return null;
        }
    }

    /**
     * Sets the selected project within the application
     * @param i the index of the selected project
     * @return boolean returns true of the index is valid
     */
    public boolean setSelectedProject(int i) {
        if (mProjects.size() > i && i >= 0) {
            mSelectedIndex = i;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns the currently selected project
     * @return
     */
    public Project getSelectedProject() {
        return get(mSelectedIndex);
    }

    /**
     * Returns the number of mProjects
     * @return
     */
    public int size() {
        return mProjects.size();
    }
}
