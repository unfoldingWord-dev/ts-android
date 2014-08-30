package com.door43.translationstudio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by joel on 8/29/2014.
 */
public class ProjectManager {
    private static ArrayList<Project> projects = new ArrayList<Project>();
//    private static Map<Integer, Project> projects = new HashMap<Integer, Project>();

    /**
     * Adds a project to the manager
     * @param p the new project to be added
     * @return The project now managed by the manager. The return value should be used instead of the input value to ensure you are using the proper reference.
     */
    public Project add(Project p) {
        if(!this.projects.contains(p)) {
            this.projects.add(p);
            return p;
        } else {
            // TODO: is this nessesary? need to double check that the object signatures are different. If they are the same we should just always return the input project.
            return get(this.projects.indexOf(p));
        }
    }

    /**
     * Returns a project
     * @param i the index of the project
     * @return the existing project or null
     */
    public Project get(int i) {
        if(this.projects.size() > i) {
            return this.projects.get(i);
        } else {
            // out of bounds
            return null;
        }
    }

    /**
     * Returns the number of projects
     * @return
     */
    public int size() {
        return projects.size();
    }
}
