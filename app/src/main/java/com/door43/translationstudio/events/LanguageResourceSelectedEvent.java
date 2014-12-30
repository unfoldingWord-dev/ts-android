package com.door43.translationstudio.events;

import com.door43.translationstudio.projects.Resource;

/**
 * This is fired when a language resource is chosen from the LanguageResourceDialog.
 */
public class LanguageResourceSelectedEvent {
    private final Resource mResource;
    public LanguageResourceSelectedEvent(Resource resource) {
        mResource = resource;
    }

    /**
     * Returns the resource for this event
     * @return
     */
    public Resource getResource() {
        return mResource;
    }
}
