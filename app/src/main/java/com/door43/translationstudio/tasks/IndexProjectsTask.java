package com.door43.translationstudio.tasks;

import com.door43.translationstudio.projects.Project;
import com.door43.util.threads.ManagedTask;

import java.util.List;

/**
 * This task indexes a project so we don't have to load the entire thing into memory.
 */
public class IndexProjectsTask extends ManagedTask {

    private final Project[] mProjects;

    public IndexProjectsTask(Project[] projects) {
        mProjects = projects;
    }

    @Override
    public void start() {
        for(Project p:mProjects) {
            // TODO: implement this
            // We will compile projects down into segmented directories in the cache
            // pid/ -> lid/ -> pinfo, rid/ -> terms, notes/ -> (just like source), source/ -> cid/ -> fid, cinfo
            //               
        }
    }
}
