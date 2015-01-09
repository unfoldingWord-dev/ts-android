package com.door43.translationstudio.events;

import android.app.DialogFragment;

import com.door43.translationstudio.network.Peer;
import com.door43.translationstudio.projects.Project;

/**
 * Created by joel on 1/8/2015.
 */
public class ChoseProjectToImportEvent {
    private final Project mProject;
    private final DialogFragment mDialog;
    private final Peer mPeer;

    public ChoseProjectToImportEvent(Peer peer, Project p, DialogFragment f) {
        mProject = p;
        mDialog = f;
        mPeer = peer;
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

    /**
     * Returns the peer that the client is accessing.
     * @return
     */
    public Peer getPeer() {
        return mPeer;
    }
}
