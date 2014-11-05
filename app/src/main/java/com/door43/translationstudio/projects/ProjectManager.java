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
 * TODO: need to provide progress information so we can display appropriate information while the user is waiting for the app to load. e.g. a loading screen while projects are parsed.
 * TODO: parsing tasks need to be ran asyncronously
 * Created by joel on 8/29/2014.
 */
public class ProjectManager implements DelegateListener {
    private static DataStore mDataStore;

    // so we can look up by index
    private static List<Project> mProjects = new ArrayList<Project>();
    // so we can look up by id
    private static Map<String, Project> mProjectMap = new HashMap<String, Project>();

    // so we can look up by index
    private static List<Language> mLanguages = new ArrayList<Language>();
    // so we can look up by id
    private static Map<String, Language> mLanguagesMap = new HashMap<String, Language>();

    // these constants are used to bind the progress bar to within certain ranges for the data.
    private final double PERCENT_TARGET_LANGUAGES = 70.0;
    private final double PERCENT_PROJECTS = 10.0;
    private final double PERCENT_PROJECT_SOURCE = 20.0;
    private double mProgress = 0;

//    // so we can look up by index
//    private static List<Language> mTargetLanguages = new ArrayList<Language>();
//    // so we can look up by id
//    private static Map<String, Language> mTargetLanguageMap = new HashMap<String, Language>();

    private static String mSelectedProjectId;
    private static MainApplication mContext;
    private static final String TAG = "ProjectManager";

    public ProjectManager(MainApplication context) {
        mContext = context;
    }
    private OnProgressCallback mCallback;

    /**
     * loads the source projects
     */
    public void init(OnProgressCallback callback) {
        mCallback = callback;
        mDataStore = new DataStore(mContext);
        // register to receive async messages from the datastore
        mDataStore.registerDelegateListener(this);
        // begin loading target languages
        String targetLanguageCatalog = mDataStore.fetchTargetLanguageCatalog();
        loadTargetLanguagesCatalog(targetLanguageCatalog);
        mCallback.finished();
    }

    /**
     * Loads the source for a single project.
     * A loading notice will be displayed to the user
     * @param p
     */
    public void fetchProjectSource(Project p) {
        fetchProjectSource(p, true);
    }

    /**
     * Loads the source for a single project
     * @param p the project that will be loaded
     * @param displayNotice you dispaly a loading notice to the user
     */
    public void fetchProjectSource(Project p, Boolean displayNotice) {
        if(displayNotice) {
            mContext.showProgressDialog(R.string.loading_project);
        }
        String source = mDataStore.fetchSourceText(p.getId(), p.getSelectedSourceLanguage().getId());
        p.flush();
        if(!displayNotice) {
            mProgress += PERCENT_PROJECT_SOURCE/3;
            mCallback.onProgress(mProgress, "opening project");
        }
        loadProject(source, p);
        String terms = mDataStore.fetchTermsText(p.getId(), p.getSelectedSourceLanguage().getId());
        if(!displayNotice) {
            mProgress += PERCENT_PROJECT_SOURCE/3;
            mCallback.onProgress(mProgress, "loading key terms");
        }
        loadTerms(terms, p);
        String notes = mDataStore.fetchTranslationNotes(p.getId(), p.getSelectedSourceLanguage().getId());
        if(!displayNotice) {
            mProgress += PERCENT_PROJECT_SOURCE/3;
            mCallback.onProgress(mProgress, "loading translation notes");
        }
        loadNotes(notes, p);
        mContext.closeProgressDialog();
    }

    /**
     * Adds a project to the manager
     * @param p the project to add
     */
    private void addProject(Project p) {
        if(!mProjectMap.containsKey(p.getId())) {
            mProjectMap.put(p.getId(), p);
            mProjects.add(p);
        }
    }

    /**
     * Adds a source lanuage to the manager
     * @param l the language to add
     */
    private void addLanguage(Language l) {
        if(!mLanguagesMap.containsKey(l.getId())) {
            mLanguagesMap.put(l.getId(), l);
            mLanguages.add(l);
        }
    }

    /**
     * Returns a project by id
     * @param id the project id a.k.a slug
     * @return null if the project does not exist
     */
    public Project getProject(String id) {
        if(mProjectMap.containsKey(id)) {
            return mProjectMap.get(id);
        } else {
            return null;
        }
    }

    /**
     * Returns a project by index
     * @param index the project index
     * @return null if the project does not exist
     */
    public Project getProject(int index) {
        if(index < mProjects.size() && index >= 0) {
            return mProjects.get(index);
        } else {
            return null;
        }
    }

    /**
     * Returns a source language by id
     * @param id the langyage id a.k.a language code
     * @return null if the language does not exist
     */
    public Language getLanguage(String id) {
        if(mLanguagesMap.containsKey(id)) {
            return mLanguagesMap.get(id);
        } else {
            return null;
        }
    }

    /**
     * Returns a source lanuage
     * @param index the language index
     * @return null if the language does not exist
     */
    public Language getLanguage(int index) {
        if(index < mLanguages.size() && index >= 0) {
            return mLanguages.get(index);
        } else {
            return null;
        }
    }

    /**
     * Sets the selected project in the app by id
     * @param id the project id
     * @return true if the project exists
     */
    public boolean setSelectedProject(String id) {
        Project p = getProject(id);
        if(p != null) {
            mSelectedProjectId = p.getId();
        }
        return p != null;
    }

    /**
     * Sets the selected project in the app by index
     * @param index the project index
     * @return true if the project exists
     */
    public boolean setSelectedProject(int index) {
        Project p = getProject(index);
        if(p != null) {
            mSelectedProjectId = p.getId();
        }
        return p != null;
    }

    /**
     * Returns the currently selected project in the app
     * @return
     */
    public Project getSelectedProject() {
        Project selectedProject = getProject(mSelectedProjectId);;
        if(selectedProject == null) {
            // auto select the first project if no other project has been selected
            int defaultProjectIndex = 0;
            setSelectedProject(defaultProjectIndex);
            return getProject(defaultProjectIndex);
        } else {
            return selectedProject;
        }
    }

    /**
     * Returns the number of projects in the app
     * @return
     */
    public int numProjects() {
        return mProjectMap.size();
    }

    /**
     * Loads the target languages catalog
     * @param targetLanguages
     */
    private void loadTargetLanguagesCatalog(String targetLanguages) {
        // parse target languages
        JSONArray json;
        try {
            json = new JSONArray(targetLanguages);
        } catch (JSONException e) {
            Log.w(TAG, e.getMessage());
            return;
        }

        // load the data
        int numLanguages = json.length();
        for(int i=0; i<numLanguages; i++) {
            try {
                JSONObject jsonLanguage = json.getJSONObject(i);
                if(jsonLanguage.has("lc") && jsonLanguage.has("ln")) {
                    mProgress += PERCENT_TARGET_LANGUAGES / numLanguages;
                    mCallback.onProgress(mProgress, "loading target language: " + jsonLanguage.get("lc").toString());
                    // TODO: it would be best to include the language direction in the target language list
                    Language l = new Language(jsonLanguage.get("lc").toString(), jsonLanguage.get("ln").toString(), Language.Direction.RightToLeft);
                    addLanguage(l);
                } else {
                    Log.w(TAG, "missing required parameters in the target language catalog");
                }
            } catch (JSONException e) {
                Log.w(TAG, e.getMessage());
                continue;
            }
        }
        // begin loading projects
        String projectsCatalog = mDataStore.fetchProjectCatalog();
        loadProjectsCatalog(projectsCatalog);
    }

    /**
     * Loads the projects catalog
     * @param projectsCatalog
     */
    private void loadProjectsCatalog(String projectsCatalog) {
        // load projects
        JSONArray json;
        try {
            json = new JSONArray(projectsCatalog);
        } catch (JSONException e) {
            Log.w(TAG, e.getMessage());
            return;
        }

        // load the data
        int numProjects = json.length();
        for(int i=0; i<numProjects; i++) {
            try {
                JSONObject jsonProject = json.getJSONObject(i);
                if(jsonProject.has("title") && jsonProject.has("slug") && jsonProject.has("desc")) {
                    mProgress += PERCENT_PROJECTS / numProjects;
                    mCallback.onProgress(mProgress, "loading project: " + jsonProject.get("slug").toString());
                    Project p = new Project(jsonProject.get("title").toString(), jsonProject.get("slug").toString(), jsonProject.get("desc").toString());
                    addProject(p);
                    String sourceLanguageCatalog = mDataStore.fetchSourceLanguageCatalog(p.getId());
                    loadSourceLanguageCatalog(p, sourceLanguageCatalog);
                } else {
                    Log.w(TAG, "missing required parameters in the project catalog");
                }
            } catch (JSONException e) {
                Log.w(TAG, e.getMessage());
                continue;
            }
        }
    }

    private void loadSourceLanguageCatalog(Project p, String sourceLangaugeCatalog) {
        // parse source languages
        JSONArray json;
        try {
            json = new JSONArray(sourceLangaugeCatalog);
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
                            addLanguage(l);

                            if(p != null) {
                                p.addSourceLanguage(l);

                                // fetch source text
//                                    mDataStore.fetchSourceText(p.getId(), l.getId());
                            } else {
                                Log.w(TAG, "project not found");
                            }
                        }
                    } else {
                        Log.w(TAG, "missing required parameters in the source language catalog");
                    }
                } else {
                    Log.w(TAG, "missing required parameters in the source language catalog");
                }
            } catch (JSONException e) {
                Log.w(TAG, e.getMessage());
                continue;
            }
        }
    }

    /**
     * We were loading everything through callbacks, however since introducing the splash page we've been loading
     * everything in a thread so the callbacks are not nessesary anymore.
     * @param id the id specified by the listener when it registered itself with the listener
     * @param response the delegate response sent by the delegate sender. You can determine which
     *                 message has been sent by comparing it's class with a known delegat response class
     */
    @Override
    public void onDelegateResponse(String id, DelegateResponse response) {
        DataStoreDelegateResponse message = (DataStoreDelegateResponse)response;
        if(message.getType() == DataStoreDelegateResponse.MessageType.IMAGES) {
            // TODO: handle loading image assets for frames. Care should be taken to avoid memory leaks or slow load times. We may want to do this on demand instead of up front (except for locally stored assets).
        } else if(message.getType() == DataStoreDelegateResponse.MessageType.AUDIO) {
            // TODO: handle loading audio assets
        } else {
            // Unknown message type
            Log.w("ProjectManager", "Unknown delegate message type "+message.getType());
        }
    }

    /**
     * Loads the translation notes for the project
     * @param jsonString
     * @param p
     */
    private void loadNotes(String jsonString, Project p) {
        // TODO: cache the notes by frame and add accessors to the frame object to retreive the notes. Then we can just load one set of notes at a time instead of loading everything into memory
        if(p == null) return;

        // load source
        JSONArray jsonNotes;
        if(jsonString == null) {
            Log.w(TAG, "The source was not found");
            return;
        }
        try {
            jsonNotes = new JSONArray(jsonString);
        } catch (JSONException e) {
            Log.w(TAG, e.getMessage());
            return;
        }

        // load the data
        for(int i=0; i<jsonNotes.length(); i++) {
            try {
                JSONObject jsonNote = jsonNotes.getJSONObject(i);
                if(jsonNote.has("id") && jsonNote.has("it") && jsonNote.has("tn")) {

                    // load id
                    String[] chapterFrameId = jsonNote.getString("id").split("-");
                    String frameId = chapterFrameId[1];
                    String chapterId = chapterFrameId[0];

                    // load important terms
                    List<String> importantTerms = new ArrayList<String>();
                    JSONArray jsonImportantTerms = jsonNote.getJSONArray("it");
                    for (int j = 0; j < jsonImportantTerms.length(); j++) {
                        importantTerms.add(jsonImportantTerms.getString(j));
                    }

                    // load notes
                    List<TranslationNote.Note> notes = new ArrayList<TranslationNote.Note>();
                    JSONArray jsonNoteItems = jsonNote.getJSONArray("tn");
                    for (int j = 0; j < jsonNoteItems.length(); j++) {
                        JSONObject jsonNoteItem = jsonNoteItems.getJSONObject(j);
                        notes.add(new TranslationNote.Note(jsonNoteItem.getString("ref").toString(), jsonNoteItem.getString("text").toString()));
                    }

                    // add translation notes to the frame
                    p.getChapter(chapterId).getFrame(frameId).setTranslationNotes(new TranslationNote(importantTerms, notes));
                } else {
                    Log.w(TAG, "missing required parameters in the source notes");
                }
            } catch (JSONException e) {
                Log.w(TAG, e.getMessage());
                continue;
            }
        }
    }

    /**
     * Loads the key terms for the project
     * @param jsonString
     * @param p
     */
    private void loadTerms(String jsonString, Project p) {
        // TODO: cache the terms by frame and add accessors to the frame object to retreive the terms. Then we can just load one set of terms at a time instead of loading everything into memory
        if(p == null) return;

        // load source
        JSONArray jsonTerms;
        if(jsonString == null) {
            Log.w(TAG, "The source was not found");
            return;
        }
        try {
            jsonTerms = new JSONArray(jsonString);
        } catch (JSONException e) {
            Log.w(TAG, e.getMessage());
            return;
        }

        // load the data
        for(int i=0; i<jsonTerms.length(); i++) {
            try {
                JSONObject jsonTerm = jsonTerms.getJSONObject(i);
                if(jsonTerm.has("def") && jsonTerm.has("def_title") && jsonTerm.has("term")) {

                    // load related terms
                    List<String> relatedTerms = new ArrayList<String>();
                    if(jsonTerm.has("cf")) {
                        JSONArray jsonRelated = jsonTerm.getJSONArray("cf");
                        for (int j = 0; j < jsonRelated.length(); j++) {
                            relatedTerms.add(jsonRelated.getString(j));
                        }
                    }

                    // load examples
                    List<Term.Example> examples = new ArrayList<Term.Example>();
                    if(jsonTerm.has("ex")) {
                        JSONArray jsonExamples = jsonTerm.getJSONArray("ex");
                        for (int j = 0; j < jsonExamples.length(); j++) {
                            JSONObject jsonExample = jsonExamples.getJSONObject(j);
                            String[] ref = jsonExample.getString("ref").toString().split("-");
                            if (ref.length == 2) {
                                examples.add(new Term.Example(ref[0], ref[1], jsonExample.getString("text").toString()));
                            } else {
                                Log.w(TAG, "invalid term example reference");
                            }
                        }
                    }

                    // load term
                    Term t = new Term(jsonTerm.get("term").toString(), jsonTerm.get("sub").toString(), jsonTerm.get("def").toString(), jsonTerm.get("def_title").toString(), relatedTerms, examples);

                    // add term to the project
                    p.addTerm(t);
                } else {
                    Log.w(TAG, "missing required parameters in the source terms");
                }
            } catch (JSONException e) {
                Log.w(TAG, e.getMessage());
                continue;
            }
        }
    }

    /**
     * Loads the source translation into a project
     * @param jsonString
     * @param p
     */
    private void loadProject(String jsonString, Project p) {
        if(p == null) return;

        // load source
        JSONArray jsonChapters;
        if(jsonString == null) {
            Log.w(TAG, "The source was not found");
            return;
        }
        try {
            JSONObject json = new JSONObject(jsonString);
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
                    String chapterNumber = jsonChapter.get("number").toString();
                    Chapter c = new Chapter(chapterNumber, jsonChapter.get("title").toString(), jsonChapter.get("ref").toString());

                    // add chapter to the project
                    p.addChapter(c);

                    // load frames
                    JSONArray jsonFrames = jsonChapter.getJSONArray("frames");
                    for(int j=0; j<jsonFrames.length(); j++) {
                        JSONObject jsonFrame = jsonFrames.getJSONObject(j);
                        if(jsonFrame.has("id") && jsonFrame.has("text")) {
                            c.addFrame(new Frame(jsonFrame.get("id").toString(), jsonFrame.get("img").toString(), jsonFrame.get("text").toString()));
                        } else {
                            Log.w(TAG, "missing required parameters in the source frames");
                        }
                    }
                } else {
                    Log.w(TAG, "missing required parameters in the source chapters");
                }
            } catch (JSONException e) {
                Log.w(TAG, e.getMessage());
                continue;
            }
        }
    }

    /**
     * Returns a list of languages
     * @return
     */
    public List<Language> getLanguages() {
        return mLanguages;
    }

    public interface OnProgressCallback {
        void onProgress(double progress, String message);
        void finished();
    }
}
