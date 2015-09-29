package com.door43.translationstudio.projects;

import android.content.Context;

import com.door43.translationstudio.events.OpenedChapterEvent;
import com.door43.translationstudio.events.OpenedFrameEvent;
import com.door43.translationstudio.events.OpenedProjectEvent;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.tasks.ThreadableUI;
import com.squareup.otto.Bus;

/**
 * This class handles the project navigation within the app.
 * e.g. switching between projects, chapter, frames etc.
 * Other features include history, undo, redo and more.
 */
@Deprecated
public class Navigator {
    private final ProjectManager mProjectManager;
    private final Context mContext;
    private final Bus mEventBus;
    private OnOpenListener mOpenProjectListener;
    private OnOpenListener mOpenChapterListener;
    private OnOpenListener mOpenFrameListener;
    private OnOpenListener mOpenSourceLanguageListener;
    private OnOpenListener mOpenResourceListener;

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
     * Sets the listener to be called when a project is opened
     * @param listener
     */
    public void setOnOpenProjectListener(OnOpenListener listener) {
        mOpenProjectListener = listener;
        onProjectOpen();
    }

    /**
     * Sets the listener to be called when a chapter is opened
     * @param listener
     */
    public void setOnOpenChapterListener(OnOpenListener listener) {
        mOpenChapterListener = listener;
        onChapterOpen();
    }

    /**
     * Sets the listener to be called when a frame is opened
     * @param listener
     */
    public void setOnOpenFrameListener(OnOpenListener listener) {
        mOpenFrameListener = listener;
        onFrameOpen();
    }

    /**
     * Sets the listener to be called when a source language is opened
     * @param listener
     */
    public void setOnOpenSourceLanguageListener(OnOpenListener listener) {
        mOpenSourceLanguageListener = listener;
        onSourceLanguageOpen();
    }

    /**
     * Sets the listener to be called when a resource is opened
     * @param listener
     */
    public void setOnOpenResourceListener(OnOpenListener listener) {
        mOpenResourceListener = listener;
        onResourceOpen();
    }

    /**
     * Fires off the project open event
     */
    private void onProjectOpen() {
        if(mOpenProjectListener != null) {
            try {
                mOpenProjectListener.onOpen();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Fires off the chapter open event
     */
    private void onChapterOpen() {
        if(mOpenChapterListener != null) {
            try {
                mOpenChapterListener.onOpen();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Fires off the frame open event
     */
    private void onFrameOpen() {
        if(mOpenFrameListener != null) {
            try {
                mOpenFrameListener.onOpen();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Fires off the source language open event
     */
    private void onSourceLanguageOpen() {
        if(mOpenSourceLanguageListener != null) {
            try {
                mOpenSourceLanguageListener.onOpen();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Fires off the resource open event
     */
    private void onResourceOpen() {
        if(mOpenResourceListener != null) {
            try {
                mOpenResourceListener.onOpen();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Opens the project
     * TODO: outdated. this does not work with indexes
     * @param p
     * @param listener
     */
    public void open(Project p, final OnSuccessListener listener) {
        if(p != null) {
            Project currProj = mProjectManager.getSelectedProject();
            if(currProj == null || !currProj.getId().equals(p.getId())) {
                mProjectManager.setSelectedProject(p.getId());
                // TODO: record project change for history
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
     * TODO: outdated. this does not work with indexes
     * @param c
     */
    public void open(Chapter c) {
        if(c != null) {
            Project currProj = mProjectManager.getSelectedProject();
            if(currProj != null) {
                Project p = c.getProject();
                if(p.getId().equals(currProj.getId())) {
                    currProj.setSelectedChapter(c.getId());
                    // TODO: record chapter change for history
                    mEventBus.post(new OpenedChapterEvent());
                }
            }
        }
    }

    /**
     * Opens the frame if it exists within the current chapter
     * TODO: outdated. this does not work with indexes
     * @param f
     */
    public void open(final Frame f) {
        if(f != null) {
            Project currProj = mProjectManager.getSelectedProject();
            // fire event that frame is being opened
            if(currProj != null) {
                Chapter currChapt = currProj.getSelectedChapter();
                if(currChapt != null) {
                    Chapter c = f.getChapter();
                    Project p = c.getProject();
                    if(c.getId().equals(currChapt.getId()) && p.getId().equals(currProj.getId())) {
                        currChapt.setSelectedFrame(f.getId());
                        // TODO: record frame change for history
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

    public static interface OnOpenListener {
        public void onOpen();
    }
}

