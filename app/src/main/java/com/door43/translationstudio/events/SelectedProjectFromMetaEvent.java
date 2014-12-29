package com.door43.translationstudio.events;

import com.door43.translationstudio.projects.Project;

/**
  * This events communicates the chosen project from the MetaProjectDialog to the ProjectsTabFragment.
  */
 public class SelectedProjectFromMetaEvent {
    private final Project mProject;

    public SelectedProjectFromMetaEvent(Project p) {
        mProject = p;
    }

    /**
     * Returns the chosen project
     * @return
     */
    public Project getProject() {
        return mProject;
    }
 }
