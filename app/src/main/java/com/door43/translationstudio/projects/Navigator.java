package com.door43.translationstudio.projects;

import android.content.Context;

import com.door43.translationstudio.events.OpenedChapterEvent;
import com.door43.translationstudio.events.OpenedFrameEvent;
import com.door43.translationstudio.events.OpenedProjectEvent;
import com.door43.translationstudio.util.AppContext;
import com.door43.translationstudio.util.ThreadableUI;
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
     * @param p
     * @param listener
     */
    public void open(Project p, final OnSuccessListener listener) {
        if(p != null) {
            Project currProj = mProjectManager.getSelectedProject();
            if(currProj == null || !currProj.getId().equals(p.getId())) {
                mProjectManager.setSelectedProject(p.getId());
                // TODO: record project change
                new ThreadableUI(mContext) {
                    @Override
                    public void onStop() {
                        listener.onFailed();
                    }
                    @Override
                    public void run() {
                        AppContext.projectManager().fetchProjectSource(AppContext.projectManager().getSelectedProject());
                    }
                    @Override
                    public void onPostExecute() {
                        mEventBus.post(new OpenedProjectEvent());
                        listener.onSuccess();
                    }
                }.start();
            } else {
                listener.onSuccess();
            }
        } else {
            listener.onFailed();
        }
    }

    /**
     * Opens the chapter if it exists within the current project.
     * @param c
     */
    public void open(Chapter c) {
        if(c != null) {
            Project currProj = mProjectManager.getSelectedProject();
            if(currProj != null) {
                Project p = c.getProject();
                if(p.getId().equals(currProj.getId())) {
                    currProj.setSelectedChapter(c.getId());
                    // TODO: record chapter change
                    mEventBus.post(new OpenedChapterEvent());
                }
            }
        }
    }

    /**
     * Opens the frame if it exists within the current chapter
     * @param f
     */
    public void open(final Frame f) {
        if(f != null) {
            Project currProj = mProjectManager.getSelectedProject();
            if(currProj != null) {
                Chapter currChapt = currProj.getSelectedChapter();
                if(currChapt != null) {
                    Chapter c = f.getChapter();
                    Project p = c.getProject();
                    if(c.getId().equals(currChapt.getId()) && p.getId().equals(currProj.getId())) {
                        currChapt.setSelectedFrame(f.getId());
                        // TODO: record frame change
                        mEventBus.post(new OpenedFrameEvent());
                    }
                }
            }
        }
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

    public static interface OnSuccessListener {
        public void onSuccess();
        public void onFailed();
    }
}

