package com.door43.translationstudio;

import android.util.Log;

import com.door43.delegate.DelegateListener;
import com.door43.delegate.DelegateResponse;
import com.door43.translationstudio.datastore.DataStore;
import com.door43.translationstudio.datastore.DataStoreDelegateResponse;

import java.util.ArrayList;

/**
 * Created by joel on 8/29/2014.
 */
public class ProjectManager implements DelegateListener {
    private DataStore mDataStore = new DataStore();
    private static ArrayList<Project> mProjects = new ArrayList<Project>();
    private static int mSelectedIndex;

    public ProjectManager() {
        // register to receive async messages from the datastore
        mDataStore.registerDelegateListener(this);
        // begin loading projects
        mDataStore.fetchProjectCatalog();
    }

    /**
     * Adds a project to the manager
     * @param p the new project to be added
     * @return The project now managed by the manager. The return value should be used instead of the input value to ensure you are using the proper reference.
     */
    public Project add(Project p) {
        if(!this.mProjects.contains(p)) {
            this.mProjects.add(p);
            return p;
        } else {
            // TODO: is this nessesary? need to double check that the object signatures are different. If they are the same we should just always return the input project.
            return get(this.mProjects.indexOf(p));
        }
    }

    /**
     * Returns a project
     * @param i the index of the project
     * @return the existing project or null
     */
    public Project get(int i) {
        if(this.mProjects.size() > i && i >= 0) {
            return this.mProjects.get(i);
        } else {
            // out of bounds
            return null;
        }
    }

    /**
     * Sets the selected project within the application
     * @param i the index of the selected project
     * @return boolean returns true of the index is valid
     */
    public boolean setSelectedProject(int i) {
        if (mProjects.size() > i && i >= 0) {
            mSelectedIndex = i;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns the currently selected project
     * @return
     */
    public Project getSelectedProject() {
        return get(mSelectedIndex);
    }

    /**
     * Returns the number of mProjects
     * @return
     */
    public int size() {
        return mProjects.size();
    }

    @Override
    public void onDelegateResponse(String id, DelegateResponse response) {
        DataStoreDelegateResponse message = (DataStoreDelegateResponse)response;
        if(message.getType() == DataStoreDelegateResponse.MessageType.PROJECT) {
            // TODO: load the projects from the json and load the languages
//            mDataStore.fetchLanguageCatalog();
        } else if(message.getType() == DataStoreDelegateResponse.MessageType.LANGUAGE) {
            // TODO: load the languages from the json and load the source text
//            mDataStore.fetchSourceText();
        } else if(message.getType() == DataStoreDelegateResponse.MessageType.SOURCE) {
            // TODO: load source from the json and load the images and audio
        } else if(message.getType() == DataStoreDelegateResponse.MessageType.IMAGES) {

        } else if(message.getType() == DataStoreDelegateResponse.MessageType.AUDIO) {

        } else {
            // Unknown message type
            Log.w("ProjectManager", "Unknown delegate message type "+message.getType());
        }
    }
}
