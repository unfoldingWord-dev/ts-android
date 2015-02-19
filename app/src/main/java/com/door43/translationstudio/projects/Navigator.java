package com.door43.translationstudio.projects;

import android.content.Context;

import com.door43.translationstudio.events.OpenedChapterEvent;
import com.door43.translationstudio.events.OpenedFrameEvent;
import com.door43.translationstudio.events.OpenedProjectEvent;
import com.squareup.otto.Bus;

/**
 * This class handles the project navigation within the app.
 * e.g. switching between projects, chapter, frames etc.
 * Other features include history, undo, redo and more.
 */
public class Navigator {
    private final ProjectManager mProjectManager;
    private final Context mContext;
    private final Bus mEventBus;

    // TODO: impliment this class and migrate all navigation operations into it
    // This class should also handle all the unsaved data so we don't to depend on what's in the translation edit text.
    // that way we can easily move around and reload things without needing to be on the main activity.
    // TODO: we need to be sure to save all changes before opening anything

    /**
     * Creates a new instance of the navigator.
     * @param context the application context
     * @param projectManager the project manager
     * @param eventBus
     */
    public Navigator(Context context, ProjectManager projectManager, Bus eventBus) {
        mContext = context;
        mProjectManager = projectManager;
        mEventBus = eventBus;
    }

    /**
     * Opens the project
     * The default chapter and frame will also be loaded
     * @param p
     */
    public boolean open(Project p) {
        if(p != null) {
            Project prev = mProjectManager.getSelectedProject();
            if(prev == null || !prev.getId().equals(p.getId())) {
                mProjectManager.setSelectedProject(p.getId());
                // TODO: record project change
                mEventBus.post(new OpenedProjectEvent());
            }
            return true;
        }
        return false;
    }

    /**
     * Opens the chapter if it exists within the current project.
     * @param c
     */
    public boolean open(Chapter c) {
        if(c != null) {
            Project p = c.getProject();
            if(open(p)) {
                Chapter prev = p.getSelectedChapter();
                if (prev == null || !prev.getId().equals(c.getId())) {
                    p.setSelectedChapter(c.getId());
                    // TODO: record chapter open
                    mEventBus.post(new OpenedChapterEvent());
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Opens the frame if it exists within the current chapter
     * @param f
     */
    public boolean open(Frame f) {
        if(f != null) {
            Project p = f.getChapter().getProject();
            if(open(p)) {
                Chapter c = p.getSelectedChapter();
                if(open(c)) {
                    Frame prev = c.getSelectedFrame();
                    if(prev == null || !prev.getId().equals(f.getId())) {
                        c.setSelectedFrame(f.getId());
                        // TODO: record frame open
                        mEventBus.post(new OpenedFrameEvent());
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Opens the resource if it exists within the current source language
     * @param r
     */
    public boolean open(Resource r) {
        if(r != null) {
            // TODO: load resource
        }
        return false;
    }

    /**
     * Opens the source language if it exists within the current project
     * The default resource for this language will also be loaded
     * @param l
     */
    public boolean open(SourceLanguage l) {
        if(l != null) {
            // TODO: load source language
        }
        return false;
    }
}

