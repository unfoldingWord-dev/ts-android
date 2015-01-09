package com.door43.translationstudio.events;

import com.door43.translationstudio.network.Peer;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Project;

/**
 * This event is fired when the user choses the languages of a project they wish to import.
 */
public class ChoseProjectLanguagesToImportEvent {
    private final Project mProject;
    private final Language[] mLanguages;
    private final Peer mPeer;

    public ChoseProjectLanguagesToImportEvent(Peer peer, Project p, Language[] languages) {
        mPeer = peer;
        mProject = p;
        mLanguages = languages;
    }

    /**
     * Returns the project that has been selected to be imported
     * @return
     */
    public Project getProject() {
        return mProject;
    }

    /**
     * Returns the languages (of the related project) that have been selected to be imported
     * @return
     */
    public Language[] getLanguages() {
        return mLanguages;
    }

    /**
     * Returns the peer the client is accessing.
     * @return
     */
    public Peer getPeer() {
        return mPeer;
    }
}
