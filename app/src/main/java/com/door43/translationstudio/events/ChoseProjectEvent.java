package com.door43.translationstudio.events;

import android.app.DialogFragment;

import com.door43.translationstudio.projects.Project;

/**
  * This events communicates the chosen project from the MetaProjectDialog to the ProjectsTabFragment.
  */
 public class ChoseProjectEvent {
    private final Project mProject;
    private final DialogFragment mDialog;

    public ChoseProjectEvent(Project p, DialogFragment f) {
        mProject = p;
        mDialog = f;
    }

    /**
     * Returns the chosen project
     * @return
     */
    public Project getProject() {
        return mProject;
    }

    public DialogFragment getDialog() {
        return mDialog;
    }
 }
