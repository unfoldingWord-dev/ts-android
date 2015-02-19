package com.door43.translationstudio.projects;

import android.content.Context;

/**
 * This class handles the project navigation within the app.
 * e.g. switching between projects, chapter, frames etc.
 * Other features include history, undo, redo and more.
 */
public class Navigator {
    private final ProjectManager mProjectManager;
    private final Context mContext;

    // TODO: impliment this class and migrate all navigation operations into it
    // This class should also handle all the unsaved data so we don't to depend on what's in the translation edit text.
    // that way we can easily move around and reload things without needing to be on the main activity.

    /**
     * Creates a new instance of the navigator.
     * @param context the application context
     * @param projectManager the project manager
     */
    public Navigator(Context context, ProjectManager projectManager) {
        mContext = context;
        mProjectManager = projectManager;
    }

    /**
     * Opens the project
     * The default chapter and frame will also be loaded
     * @param p
     */
    public void open(Project p) {

    }

    /**
     * Opens the chapter if it exists within the current project.
     * The default frame for this chapter will also be loaded
     * @param c
     */
    public void open(Chapter c) {

    }

    /**
     * Opens the frame if it exists within the current chapter
     * @param f
     */
    public void open(Frame f) {

    }

    /**
     * Opens the resource if it exists within the current source language
     * @param r
     */
    public void open(Resource r) {

    }

    /**
     * Opens the source language if it exists within the current project
     * The default resource for this language will also be loaded
     * @param l
     */
    public void open(SourceLanguage l) {

    }
}

