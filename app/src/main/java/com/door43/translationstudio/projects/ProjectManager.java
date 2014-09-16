package com.door43.translationstudio.projects;

import android.util.Log;

import com.door43.delegate.DelegateListener;
import com.door43.delegate.DelegateResponse;
import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.data.DataStore;
import com.door43.translationstudio.projects.data.DataStoreDelegateResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The project manager handles all of the projects within the app.
 * TODO: need to provide progress information so we can display appropriate information while the user is waiting for the app to load. e.g. a loading screen.
 * Created by joel on 8/29/2014.
 */
public class ProjectManager implements DelegateListener {
    private static DataStore mDataStore;
    private static Map<String, Project> mProjects = new HashMap<String, Project>();
    private static ArrayList<Language> mLanguages = new ArrayList<Language>();
    private static String mSelectedProjectSlug;
    private static MainApplication mContext;
    private static final String TAG = "ProjectManager";

    public ProjectManager(MainApplication context) {
        mContext = context;
        mDataStore = new DataStore(context);
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
    private Project addProject(Project p) {
        if(!this.mProjects.containsKey(p.getSlug())) {
            this.mProjects.put(p.getSlug(), p);
            return p;
        } else {
            // TODO: is this nessesary? need to double check that the object signatures are different. If they are the same we should just always return the input project.
            return getProject(p.getSlug());
        }
    }

    /**
     * Adds a lanuage to the manager
     * @param l
     * @return
     */
    private Language addLanguage(Language l) {
        if(!this.mLanguages.contains(l)) {
            this.mLanguages.add(l);
            return l;
        } else {
            // TODO: is this nessesary? need to double check that the object signatures are different. If they are the same we should just always return the input project.
            return getLanguage(this.mLanguages.indexOf(l));
        }
    }

    /**
     * Returns a project
     * @param slug the slug of the project
     * @return the existing project or null
     */
    public Project getProject(String slug) {
        if(this.mProjects.containsKey(slug)) {
            return this.mProjects.get(slug);
        } else {
            return null;
        }
    }

    /**
     * Retusn a lanuage
     * @param i
     * @return
     */
    public Language getLanguage(int i) {
        if(this.mLanguages.size() > i && i >= 0) {
            return this.mLanguages.get(i);
        } else {
            // out of bounds
            return null;
        }
    }

    /**
     * Sets the selected project within the application
     * @param slug the slug of the selected project
     * @return boolean returns true of the index is valid
     */
    public boolean setSelectedProject(String slug) {
        if (mProjects.containsKey(slug)) {
            mSelectedProjectSlug = slug;
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
        Project selectedProject = getProject(mSelectedProjectSlug);;
        if(selectedProject == null) {
            // atuo select the first project if no other project has been selected yet.
            String key = (String) getProjectsKeySet().get(0);
            setSelectedProject(key);
            return getProject(key);
        } else {
            return selectedProject;
        }
    }

    /**
     * Returns the number of mProjects
     * @return
     */
    public int numProjects() {
        return mProjects.size();
    }

    @Override
    public void onDelegateResponse(String id, DelegateResponse response) {
        DataStoreDelegateResponse message = (DataStoreDelegateResponse)response;
        if(message.getType() == DataStoreDelegateResponse.MessageType.PROJECT) {
            // parse the message
            JSONArray json;
            try {
                json = new JSONArray(message.getJSON());
            } catch (JSONException e) {
                Log.w(TAG, e.getMessage());
                return;
            }

            // load the data
            for(int i=0; i<json.length(); i++) {
                try {
                    JSONObject jsonProject = json.getJSONObject(i);
                    if(jsonProject.has("title") && jsonProject.has("slug") && jsonProject.has("desc")) {
                        Project p = new Project(jsonProject.get("title").toString(), jsonProject.get("slug").toString(), jsonProject.get("desc").toString());
                        addProject(p);
                        mDataStore.fetchLanguageCatalog(p.getSlug());
                    } else {
                        Log.w(TAG, "missing required parameters in the project catalog");
                    }
                } catch (JSONException e) {
                    Log.w(TAG, e.getMessage());
                    continue;
                }
            }
        } else if(message.getType() == DataStoreDelegateResponse.MessageType.LANGUAGE) {
            // parse the message
            JSONArray json;
            try {
                json = new JSONArray(message.getJSON());
            } catch (JSONException e) {
                Log.w(TAG, e.getMessage());
                return;
            }

            // load the data
            for(int i=0; i<json.length(); i++) {
                try {
                    JSONObject jsonLanguage = json.getJSONObject(i);
                    if(jsonLanguage.has("language") && jsonLanguage.has("status") && jsonLanguage.has("string") && jsonLanguage.has("direction")) {
                        JSONObject jsonStatus = jsonLanguage.getJSONObject("status");
                        if(jsonStatus.has("checking_level")) {
                            // require minimum language checking level
                            if(Integer.parseInt(jsonStatus.get("checking_level").toString()) >= mContext.getResources().getInteger(R.integer.min_source_lang_checking_level)) {
                                // add the language
                                Language.Direction langDir = jsonLanguage.get("direction").toString() == "ltr" ? Language.Direction.LeftToRight : Language.Direction.RightToLeft;
                                Language l = new Language(jsonLanguage.get("language").toString(), jsonLanguage.get("string").toString(), langDir);
                                l = addLanguage(l);

                                // fetch source text
                                Project p = getProject(message.getProjectSlug());
                                if(p != null) {
                                    mDataStore.fetchSourceText(p.getSlug(), l.getCode(), mLanguages.indexOf(l));
                                } else {
                                    Log.w(TAG, "project not found");
                                }
                            }
                        } else {
                            Log.w(TAG, "missing required parameters in the project catalog");
                        }
                    } else {
                        Log.w(TAG, "missing required parameters in the project catalog");
                    }
                } catch (JSONException e) {
                    Log.w(TAG, e.getMessage());
                    continue;
                }
            }
        } else if(message.getType() == DataStoreDelegateResponse.MessageType.SOURCE) {
            // TODO: this will break once we have multiple source languages. It will just load all the source into a single project mixing up all the languages. We need a way to manager different languges for each project.

            // parse the message
            JSONArray jsonChapters;
            try {
                JSONObject json = new JSONObject(message.getJSON());
                jsonChapters = json.getJSONArray("chapters");
            } catch (JSONException e) {
                Log.w(TAG, e.getMessage());
                return;
            }

            // load the data
            for(int i=0; i<jsonChapters.length(); i++) {
                try {
                    JSONObject jsonChapter = jsonChapters.getJSONObject(i);
                    if(jsonChapter.has("ref") && jsonChapter.has("frames") && jsonChapter.has("title") && jsonChapter.has("number")) {
                        // load chapter
                        String num = jsonChapter.get("number").toString();
                        int chapterNumber = Integer.parseInt(num.substring(0, num.indexOf(".")));
                        Chapter c = new Chapter(chapterNumber, jsonChapter.get("title").toString(), jsonChapter.get("ref").toString());

                        // add chapter to the project
                        Project p = getProject(message.getProjectSlug());
                        if(p != null) {
                            p.addChapter(c);

                            // load frames
                            JSONArray jsonFrames = jsonChapter.getJSONArray("frames");
                            for(int j=0; j<jsonFrames.length(); j++) {
                                JSONObject jsonFrame = jsonFrames.getJSONObject(j);
                                if(jsonFrame.has("id") && jsonFrame.has("text")) {
                                    c.addFrame(new Frame(jsonFrame.get("id").toString(), jsonFrame.get("text").toString()));
                                    // TODO: load image assets for the frame
                                } else {
                                    Log.w(TAG, "missing required parameters in the source frames");
                                }
                            }
                        } else {
                            Log.w(TAG, "could not locate project");
                        }
                    } else {
                        Log.w(TAG, "missing required parameters in the source chapters");
                    }
                } catch (JSONException e) {
                    Log.w(TAG, e.getMessage());
                    continue;
                }
            }

        } else if(message.getType() == DataStoreDelegateResponse.MessageType.IMAGES) {
            // TODO: handle loading image assets for frames. Care should be taken to avoid memory leaks or slow load times. We may want to do this on demand instead of up front (except for locally stored assets).
        } else if(message.getType() == DataStoreDelegateResponse.MessageType.AUDIO) {
            // TODO: handle loading audio assets
        } else {
            // Unknown message type
            Log.w("ProjectManager", "Unknown delegate message type "+message.getType());
        }
    }

    /**
     * Returns a keyset of project keys so list adapters can use indexes to identify projects.
     * @return
     */
    public List getProjectsKeySet() {
        return new ArrayList(mProjects.keySet());
    }
}
