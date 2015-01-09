package com.door43.translationstudio.events;

import android.app.DialogFragment;

import com.door43.translationstudio.network.Peer;
import com.door43.translationstudio.projects.Project;

/**
 * Created by joel on 1/8/2015.
 */
public class ChooseProjectToImportEvent {
    private final Project mProject;
    private final DialogFragment mDialog;

    public ChooseProjectToImportEvent(Peer peer, Project p, DialogFragment f) {
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
