package com.door43.translationstudio.projects;

import android.content.SharedPreferences;
import android.util.Log;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.data.DataStore;
import com.door43.translationstudio.util.Logger;
import com.door43.translationstudio.util.MainContext;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The project manager handles all of the projects within the app.
 * Created by joel on 8/29/2014.
 */
public class ProjectManager {
    private static DataStore mDataStore;

    // so we can look up by index
    private static List<Project> mProjects = new ArrayList<Project>();
    // so we can look up by id
    private static Map<String, Project> mProjectMap = new HashMap<String, Project>();

    // meta projects
    private static Map<String, PseudoProject> mMetaProjectMap = new HashMap<String, PseudoProject>();

    private static List<Model> mListableProjects = new ArrayList<Model>();
    private static Map<String, Model> mListableProjectMap = new HashMap<String, Model>();

    // so we can look up by index
    private static List<Language> mLanguages = new ArrayList<Language>();
    // so we can look up by id
    private static Map<String, Language> mLanguagesMap = new HashMap<String, Language>();
    // so we can look up by name
    private static Map<String, Language> mLanguagesNameMap = new HashMap<String, Language>();

    // so we can look up by index
    private static List<SourceLanguage> mSourceLanguages = new ArrayList<SourceLanguage>();
    // so we can look up by id
    private static Map<String, SourceLanguage> mSourceLanguagesMap = new HashMap<String, SourceLanguage>();
    // so we can look up by name
    private static Map<String, SourceLanguage> mSourceLanguagesNameMap = new HashMap<String, SourceLanguage>();

    // these constants are used to bind the progress bar to within certain ranges for the data.
    private final double PERCENT_TARGET_LANGUAGES = 70.0;
    private final double PERCENT_PROJECTS = 10.0;
    private final double PERCENT_PROJECT_SOURCE = 20.0;
    private double mProgress = 0;

    private static String mSelectedProjectId;
    private static MainApplication mContext;
    private static final String TAG = "ProjectManager";
    private OnProgressCallback mCallback;
    private static boolean mHasLoaded = false;

    public ProjectManager(MainApplication context) {
        mContext = context;
        mDataStore = new DataStore(mContext);
    }

    /**
     * Allows the project manager to be initialized again
     */
    public void reset() {
        mHasLoaded = false;
    }

    /**
     * Checks if the project manager has been loaded
     * @return
     */
    public boolean isLoaded() {
        return mHasLoaded;
    }

    /**
     * loads the source projects
     */
    public void init(OnProgressCallback callback) {
        // make sure we only call this once.
        if(!mHasLoaded) {
            mHasLoaded = true;
            mCallback = callback;
            // begin loading target languages
            String targetLanguageCatalog = mDataStore.fetchTargetLanguageCatalog();
            loadTargetLanguagesCatalog(targetLanguageCatalog);
            // begin loading projects
            initProjects();
        }
        if(mCallback != null) {
            mCallback.onSuccess();
        }
    }

    /**
     * Loads the projects
     */
    private void initProjects() {
        String projectsCatalog = mDataStore.fetchProjectCatalog(false);
        loadProjectsCatalog(projectsCatalog, false);
    }

    /**
     * Downloads any new projects from the server
     */
    public void downloadNewProjects() {
        Logger.i(this.getClass().getName(), "downloading new projects");
        String catalog = mDataStore.fetchProjectCatalog(true);
        loadProjectsCatalog(catalog, true);
    }

    /**
     * Downloads the latest version of the project resources from the server
     * @param p
     */
    public void downloadProjectUpdates(Project p) {
        // download the source language catalog
        String languageCatalog = mDataStore.fetchSourceLanguageCatalog(p.getId(), true);
        List<SourceLanguage> languages = loadSourceLanguageCatalog(p, languageCatalog, true);
        for(SourceLanguage l:languages) {
            // reload the source if this is the currently selected project
            if(p.getId().equals(mSelectedProjectId) && getSelectedProject().getSelectedSourceLanguage().equals(l)) {
                fetchProjectSource(p, false);
            }
        }
    }

    /**
     * Loads the source for a single project.
     * A loading notice will be displayed to the user
     * This should be called from within a thread
     * @param p
     */
    public void fetchProjectSource(Project p) {
        fetchProjectSource(p, true);
    }

    /**
     * Loads the source for a single project.
     * This should be called from within a thread
     * @param p the project that will be loaded
     * @param displayNotice you dispaly a loading notice to the user
     */
    public void fetchProjectSource(Project p, Boolean displayNotice) {
        if(displayNotice) {
            mContext.showProgressDialog(R.string.loading_project_chapters);
        }
        if(p == null || p.getSelectedSourceLanguage() == null) return;

        String source = mDataStore.fetchSource(p.getId(), p.getSelectedSourceLanguage().getId(), p.getSelectedSourceLanguage().getSelectedResource().getId(), false);
        p.flush();
        if(!displayNotice) {
            mProgress += PERCENT_PROJECT_SOURCE/3;
            if(mCallback != null) {
                mCallback.onProgress(mProgress, mContext.getResources().getString(R.string.opening_project));
            }
        }
        loadProject(source, p);
        String terms = mDataStore.fetchTerms(p.getId(), p.getSelectedSourceLanguage().getId(), p.getSelectedSourceLanguage().getSelectedResource().getId(), false);
        if(!displayNotice) {
            mProgress += PERCENT_PROJECT_SOURCE/3;
            if(mCallback != null) {
                mCallback.onProgress(mProgress, mContext.getResources().getString(R.string.loading_key_terms));
            }
        }
        loadTerms(terms, p);
        String notes = mDataStore.fetchNotes(p.getId(), p.getSelectedSourceLanguage().getId(), p.getSelectedSourceLanguage().getSelectedResource().getId(), false);
        if(!displayNotice) {
            mProgress += PERCENT_PROJECT_SOURCE/3;
            if(mCallback != null) {
                mCallback.onProgress(mProgress, mContext.getResources().getString(R.string.loading_translation_notes));
            }
        }
        loadNotes(notes, p);
        if(displayNotice) {
            mContext.closeProgressDialog(true);
        }
    }

    /**
     * Adds a project to the manager
     * @param p the project to add
     */
    private boolean addProject(Project p) {
        if(!mProjectMap.containsKey(p.getId())) {
            mProjectMap.put(p.getId(), p);
            mProjects.add(p);
            return true;
        } else {
            getProject(p.getId()).setDateModified(p.getDateModified());
            return false;
        }
    }

    /**
     * Removes a project from the manager
     * @param p the project to be removed
     */
    private void deleteProject(Project p) {
        if(mProjectMap.containsKey(p.getId())) {
            mProjectMap.remove(p.getId());
            mProjects.remove(p);
        }
    }

    /**
     * Adds a meta project to the manager
     * @param p
     * @return
     */
    private boolean addMetaProject(PseudoProject p) {
        if(!mMetaProjectMap.containsKey(p.getId())) {
            mMetaProjectMap.put(p.getId(), p);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sorts the listable projects
     */
    public void sortListableProjects() {
        Collections.sort(mListableProjects, new Comparator<Model>() {
            @Override
            public int compare(Model model, Model model2) {
                try {
                    // sort children
                    if(model.getClass().getName().equals(PseudoProject.class.getName())) {
                        ((PseudoProject)model).sortChildren();
                    }
                    if(model2.getClass().getName().equals(PseudoProject.class.getName())) {
                        ((PseudoProject)model2).sortChildren();
                    }
                    // sort models
                    int i = Integer.parseInt(model.getSortKey());
                    int i2 = Integer.parseInt(model2.getSortKey());
                    return i - i2;
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "unable to sort models", e);
                    return 0;
                }
            }
        });
    }

    /**
     * Adds a project to the list of projects visible in the projects list.
     * @param p
     */
    private void addListableProject(Project p) {
        if(!mListableProjectMap.containsKey(p.getId())) {
            mListableProjectMap.put(p.getId(), p);
            mListableProjects.add(p);
        }
    }

    /**
     * Adds a pseudo project to the list of projects visible in the projects list.
     * When clicked on users will navigate through the meta projects until they
     * select a real project at which point normal application flow will continue.
     * @param p
     */
    private void addListableProject(PseudoProject p) {
        if(!mListableProjectMap.containsKey("m-"+p.getId())) {
            mListableProjectMap.put("m-"+p.getId(), p);
            mListableProjects.add(p);
        }
    }

    /**
     * Removes a project from the list of projects visible in the projects list
     * @param p
     */
    private void deleteListableProject(Project p) {
        if(mListableProjectMap.containsKey(p.getId())) {
            mListableProjectMap.remove(p.getId());
            mListableProjects.remove(p);
        }
    }

    /**
     * Removes a pseudo project from the list of projects visible in the projects list
     * @param p
     */
    private void deleteListableProject(PseudoProject p) {
        if(mListableProjectMap.containsKey("m-"+p.getId())) {
            mListableProjectMap.remove("m-" + p.getId());
            mListableProjects.remove(p);
        }
    }

    /**
     * Returns the project or meta-project by id.
     * @param id
     * @return
     */
    public Model getListableProject(String id) {
        if(mListableProjectMap.containsKey(id)) {
            return mListableProjectMap.get(id);
        } else if(mListableProjectMap.containsKey("m-"+id)) {
            return mListableProjectMap.get("m-"+id);
        } else {
            return null;
        }
    }

    /**
     * Returns the project or meta-project by index
     * @param index
     * @return
     */
    public Model getListableProject(int index) {
        if(index < mListableProjects.size() && index >= 0) {
            return mListableProjects.get(index);
        } else {
            return null;
        }
    }

    /**
     * Returns an array of listable projects
     * This may be a mix of projects and sudo projects.
     * @return
     */
    public Model[] getListableProjects() {
        return mListableProjects.toArray(new Model[mListableProjects.size()]);
    }

    /**
     * Returns the number of projects that are to be displayed in the project list.
     * @return
     */
    public int numListableProjects() {
        return mListableProjectMap.size();
    }

    /**
     * Get a meta project by id
     * @param id
     * @return
     */
    public PseudoProject getMetaProject(String id) {
        if(mMetaProjectMap.containsKey(id)) {
            return mMetaProjectMap.get(id);
        } else {
            return null;
        }
    }

    /**
     * Adds a source lanuage to the manager
     * @param l the language to add
     */
    private boolean addLanguage(Language l) {
        if(!mLanguagesMap.containsKey(l.getId())) {
            mLanguagesMap.put(l.getId(), l);
            mLanguagesNameMap.put(l.getName(), l);
            mLanguages.add(l);
            return true;
//        } else if(getLanguage(l.getId()).getDateModified() == 0) {
//            // replace plain target languages with source languages because they contain more information
//            // remove
//            mLanguagesMap.remove(l.getId());
//            mLanguagesNameMap.remove(l.getName());
//            mLanguages.remove(l);
//            // add
//            mLanguagesMap.put(l.getId(), l);
//            mLanguagesNameMap.put(l.getName(), l);
//            mLanguages.add(l);
//            return true;
        } else {
            return false;
        }
    }

    /**
     * Adds a source lanuage to the manager
     * @param l the language to add
     */
    private boolean addSourceLanguage(SourceLanguage l) {
        if(!mSourceLanguagesMap.containsKey(l.getId())) {
            mSourceLanguagesMap.put(l.getId(), l);
            mSourceLanguagesNameMap.put(l.getName(), l);
            mSourceLanguages.add(l);
            return true;
//        } else if(getLanguage(l.getId()).getDateModified() == 0) {
//            // replace plain target languages with source languages because they contain more information
//            // remove
//            mLanguagesMap.remove(l.getId());
//            mLanguagesNameMap.remove(l.getName());
//            mLanguages.remove(l);
//            // add
//            mLanguagesMap.put(l.getId(), l);
//            mLanguagesNameMap.put(l.getName(), l);
//            mLanguages.add(l);
//            return true;
        } else {
            // updated the date modified so we can keep track of updates from the server
            SourceLanguage cachedLanguage = getSourceLanguage(l.getId());
            if(l.getDateModified() > cachedLanguage.getDateModified()) {
                cachedLanguage.setDateModified(l.getDateModified());
            }
            return false;
        }
    }

    /**
     * Returns an array of projects
     * @return
     */
    public Project[] getProjects() {
        return mProjects.toArray(new Project[mProjects.size()]);
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
     * Returns a source language by id
     * @param id the langyage id a.k.a language code
     * @return null if the language does not exist
     */
    public SourceLanguage getSourceLanguage(String id) {
        if(mSourceLanguagesMap.containsKey(id)) {
            return mSourceLanguagesMap.get(id);
        } else {
            return null;
        }
    }

    /**
     * Finds a language by the human readable name
     * @param name the name of the language
     * @return null if the language does not exist
     */
    public Language getLanguageByName(String name) {
        if(mLanguagesNameMap.containsKey(name)) {
            return mLanguagesNameMap.get(name);
        } else {
            return null;
        }
    }

    /**
     * Finds a language by the human readable name
     * @param name the name of the language
     * @return null if the language does not exist
     */
    private Language getSourceLanguageByName(String name) {
        if(mSourceLanguagesNameMap.containsKey(name)) {
            return mSourceLanguagesNameMap.get(name);
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
     * Returns a source lanuage
     * @param index the language index
     * @return null if the language does not exist
     */
    public SourceLanguage getSourceLanguage(int index) {
        if(index < mSourceLanguages.size() && index >= 0) {
            return mSourceLanguages.get(index);
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
            storeSelectedProject(p.getId());
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
            storeSelectedProject(p.getId());
        }
        return p != null;
    }

    /**
     * stores the selected frame in the preferences so we can load it the next time the app starts
     * @param id
     */
    private void storeSelectedProject(String id) {
        SharedPreferences settings = MainContext.getContext().getSharedPreferences(MainContext.getContext().PREFERENCES_TAG, MainContext.getContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("selected_project", id);
        editor.apply();
    }

    /**
     * Returns the currently selected project in the app
     * @return
     */
    public Project getSelectedProject() {
        if(MainContext.getContext().rememberLastPosition()) {
            SharedPreferences settings = MainContext.getContext().getSharedPreferences(MainContext.getContext().PREFERENCES_TAG, MainContext.getContext().MODE_PRIVATE);
            mSelectedProjectId = settings.getString("selected_project", null);
        }

        Project selectedProject = getProject(mSelectedProjectId);;
        if(selectedProject == null) {
            // auto select the first project if no other project has been selected
//            int defaultProjectIndex = 0;
//            setSelectedProject(defaultProjectIndex);
//            return getProject(defaultProjectIndex);
            return null;
        } else {
            return selectedProject;
        }
    }

    /**
     * Returns the number of projects in the app.
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
            Logger.e(this.getClass().getName(), "malformed project source", e);
            return;
        }

        // load the data
        int numLanguages = json.length();
        for(int i=0; i<numLanguages; i++) {
            try {
                JSONObject jsonLanguage = json.getJSONObject(i);
                if(jsonLanguage.has("lc") && jsonLanguage.has("ln")) {
                    mProgress += PERCENT_TARGET_LANGUAGES / numLanguages;
                    // publish updates every 100 languages to ease up on the ui
                    if(i % 100 == 0) {
                        if(mCallback != null) {
                            mCallback.onProgress(mProgress, String.format(mContext.getResources().getString(R.string.loading_target_language), jsonLanguage.get("lc").toString()));
                        }
                    }
                    // TODO: it would be best to include the language direction in the target language list
                    Language l = new Language(jsonLanguage.get("lc").toString(), jsonLanguage.get("ln").toString(), Language.Direction.RightToLeft);
                    addLanguage(l);
                } else {
                    Logger.w(this.getClass().getName(),"missing required parameters in the target language catalog. "+jsonLanguage.toString());
                }
            } catch (JSONException e) {
                Logger.e(this.getClass().getName(), "failed to load target language catalog", e);
                continue;
            }
        }
    }

    /**
     * Loads the projects catalog
     * @param projectsCatalog
     * @param checkServer indicates the latest languages should be downloaded from the server
     */
    private List<Project> loadProjectsCatalog(String projectsCatalog, boolean checkServer) {
        List<Project> importedProjects = new ArrayList<Project>();
        // load projects
        JSONArray json;
        try {
            json = new JSONArray(projectsCatalog);
        } catch (JSONException e) {
            Logger.e(this.getClass().getName(), "malformed projects catalog", e);
            return new ArrayList<>();
        }

        // load the data
        int numProjects = json.length();
        for(int i=0; i<numProjects; i++) {
            try {
                JSONObject jsonProject = json.getJSONObject(i);
                if(jsonProject.has("slug") && jsonProject.has("date_modified")) {
                    mProgress += PERCENT_PROJECTS / numProjects;
                    if(mCallback != null) {
                        mCallback.onProgress(mProgress, String.format(mContext.getResources().getString(R.string.loading_project), jsonProject.get("slug").toString()));
                    }
                    Project p = new Project(jsonProject.get("slug").toString(), Integer.parseInt(jsonProject.get("date_modified").toString()));
                    if(jsonProject.has("sort")) {
                        p.setSortKey(jsonProject.getString("sort"));
                    }

                    // load meta
                    PseudoProject rootPseudoProject = null;
                    if(jsonProject.has("meta")) {
                        JSONArray jsonMeta = jsonProject.getJSONArray("meta");
                        if(jsonMeta.length() > 0) {
                            // TRICKY: if the meta has changed it will display twice in the project list so we always remove old meta
                            Project originalProject = getProject(p.getId());
                            if(originalProject != null && originalProject.getPseudoProjects().length > 0) {
                                PseudoProject originalRoot = originalProject.getPseudoProjects()[0];
                                deleteListableProject(originalRoot);
                            }

                            // get the root meta
                            String metaSlug = jsonMeta.get(0).toString();
                            rootPseudoProject = getMetaProject(metaSlug);
                            if(rootPseudoProject == null) {
                                rootPseudoProject = new PseudoProject(metaSlug);
                                addMetaProject(rootPseudoProject);
                            }
                            p.addSudoProject(rootPseudoProject);
                            // load children meta
                            PseudoProject currentPseudoProject = rootPseudoProject;
                            for (int j = 1; j < jsonMeta.length(); j++) {
                                PseudoProject sp = new PseudoProject(jsonMeta.get(j).toString());
                                if(currentPseudoProject.getMetaChild(sp.getId()) != null) {
                                    // load already created meta
                                    currentPseudoProject = currentPseudoProject.getMetaChild(sp.getId());
                                } else {
                                    // create new meta
                                    currentPseudoProject.addChild(sp);
                                    currentPseudoProject = sp;
                                }
                                p.addSudoProject(sp);
                            }
                            // close with the project
                            currentPseudoProject.addChild(p);
                        }
                    }

                    // add project or meta to the project list
                    if(rootPseudoProject == null) {
                        addListableProject(p);
                    } else {
                        addListableProject(rootPseudoProject);
                    }

                    // determine if the language catalog should be re-downloaded
                    boolean downloadLanguages = false;
                    if(checkServer) {
                        Project cachedProject = getProject(p.getId());
                        if(cachedProject == null) {
                            downloadLanguages = true;
                        } else if(p.getDateModified() > cachedProject.getDateModified()) {
                            downloadLanguages = true;
                        }
                    }

                    // add project to the internal list and continue loading
                    if(addProject(p)) {
                        importedProjects.add(p);
                    }

                    // load source languages
                    String sourceLanguageCatalog = mDataStore.fetchSourceLanguageCatalog(p.getId(), downloadLanguages);
                    // TRICKY: pull the project from the cache so we have a history of cached languages and resources when checking if a download is needed
                    List<SourceLanguage> languages = loadSourceLanguageCatalog(getProject(p.getId()), sourceLanguageCatalog, downloadLanguages);
                    // validate project has languages
                    if(languages.size() == 0) {
                        Logger.e(this.getClass().getName(), "the source languages could not be loaded for the project "+p.getId());
                        importedProjects.remove(p);
                        deleteProject(p);
                        if(rootPseudoProject == null) {
                            deleteListableProject(p);
                        } else {
                            deleteListableProject(rootPseudoProject);
                        }
                    }
                } else {
                    Logger.w(this.getClass().getName(), "missing required parameters in the project catalog");
                }
            } catch (JSONException e) {
                Logger.e(this.getClass().getName(), "failed to load projects catalog", e);
                continue;
            }
        }

        // sort the listable projects
        sortListableProjects();

        return importedProjects;
    }

    /**
     * Loads the source languages into the given project
     * @param p the project into which the source languages will be
     * @param sourceLanguageCatalog the catalog of source languages
     * @param checkServer indicates that the latest resources should be downloaded from the server
     */
    private List<SourceLanguage> loadSourceLanguageCatalog(Project p, String sourceLanguageCatalog, boolean checkServer) {
        List<SourceLanguage> importedLanguages = new ArrayList<>();
        if(sourceLanguageCatalog == null) {
            return importedLanguages;
        }
        // parse source languages
        JSONArray json;
        try {
            json = new JSONArray(sourceLanguageCatalog);
        } catch (Exception e) {
            Logger.e(this.getClass().getName(), "malformed source language catalog", e);
            return new ArrayList<>();
        }

        // load the data
        for(int i=0; i<json.length(); i++) {
            try {
                JSONObject jsonLanguage = json.getJSONObject(i);
                if(jsonLanguage.has("language") && jsonLanguage.has("project")) {
                    JSONObject jsonLangInfo = jsonLanguage.getJSONObject("language");
                    JSONObject jsonProjInfo = jsonLanguage.getJSONObject("project");

                    // load language
                    Language.Direction langDir = jsonLangInfo.get("direction").toString().equals("ltr") ? Language.Direction.LeftToRight : Language.Direction.RightToLeft;
                    SourceLanguage l = new SourceLanguage(jsonLangInfo.get("slug").toString(), jsonLangInfo.get("name").toString(), langDir, Integer.parseInt(jsonLangInfo.get("date_modified").toString()));

                    // load the rest of the project info
                    // TRICKY: we need to specify a default title and description for the project
                    if(i == 0) {
                        p.setDefaultTitle(jsonProjInfo.getString("name"));
                        p.setDefaultDescription(jsonProjInfo.getString("desc"));
                    }

                    // load title and description translations.
                    p.setTitle(jsonProjInfo.getString("name"), l);
                    p.setDescription(jsonProjInfo.getString("desc"), l);

                    // load sudo project names
                    if(jsonProjInfo.has("meta") && p.numSudoProjects() > 0) {
                        JSONArray jsonMeta = jsonProjInfo.getJSONArray("meta");
                        if(jsonMeta.length() > 0) {
                            for (int j = 0; j < jsonMeta.length(); j++) {
                                PseudoProject sp = p.getSudoProject(j);
                                if(sp != null) {
                                    sp.addTranslation(new Translation(l, jsonMeta.get(j).toString()));
                                } else {
                                    Logger.w(this.getClass().getName(), "missing meta category in project "+p.getId());
                                    break;
                                }
                            }
                        } else {
                            Logger.w(this.getClass().getName(), "missing meta translations in project "+p.getId());
                        }
                    } else if(p.numSudoProjects() > 0) {
                        Logger.w(this.getClass().getName(), "missing meta translations in project "+p.getId());
                    }

                    // determine if resources should be re-downloaded
                    boolean downloadResources = false;
                    if(checkServer) {
                        Project cachedProject = getProject(p.getId());
                        SourceLanguage cachedLanguage = cachedProject.getSourceLanguage(l.getId());
                        if(cachedLanguage == null) {
                            downloadResources = true;
                        } else if(l.getDateModified() > cachedLanguage.getDateModified()) {
                            downloadResources = true;
                        }
                    }

                    if(p.getId().equals("luk") && l.getId().equals("ar")) {
                        Log.i("test", "test");
                    }

                    // load translation versions
                    String resourcesCatalog = mDataStore.fetchResourceCatalog(p.getId(), l.getId(), downloadResources);
                    List<Resource> importedResources = loadResourcesCatalog(p, l, resourcesCatalog, downloadResources);

                    // only resources with the minium checking level will get imported, so it's possible we'll need to skip a language
                    if(importedResources.size() > 0) {
                        // TODO: we need to put all the new information (resources etc.) from the new language into the existing source language. (not sure if we actually need to do this)
                        // For the most part source and target languages can be used interchangably, however there are some cases were we need some extra information in source languages.
                        addSourceLanguage(l);
                        importedLanguages.add(l);

                        if (p != null) {
                            p.addSourceLanguage(l);
                        } else {
                            Logger.w(this.getClass().getName(), "could not find project while loading source languages");
                        }
                    }
                } else {
                    Logger.w(this.getClass().getName(), "missing required parameters in the source language catalog");
                }
            } catch (JSONException e) {
                Logger.w(this.getClass().getName(), "failed to load source language", e);
                continue;
            }
        }

        // attempt to select a more accurate default language for the project title and description
        String deviceLocale = Locale.getDefault().getLanguage();
        if(p.getSourceLanguage(deviceLocale) != null) {
            p.setSelectedSourceLanguage(deviceLocale);
        } else if(p.getSourceLanguage("en") != null) {
            p.setSelectedSourceLanguage("en");
        }

        return importedLanguages;
    }

    /**
     * Loads the resources into the given source language.
     * The soruce terms and notes may be downloaded, but will not be loaded at this time.
     * @param l the source language
     * @param resourcesCatalog the json resources
     * @param checkServer indicates the latest source terms and notes should be downloaded.
     */
    private List<Resource> loadResourcesCatalog(Project p, SourceLanguage l, String resourcesCatalog, boolean checkServer) {
        List<Resource> importedResources = new ArrayList<>();
        if(resourcesCatalog == null) {
            return importedResources;
        }
        // parse resources
        JSONArray json;
        try {
            json = new JSONArray(resourcesCatalog);
        } catch (Exception e) {
            Logger.e(this.getClass().getName(), "malformed resource catalog", e);
            return new ArrayList<>();
        }

        // load the data
        for(int i=0; i<json.length(); i++) {
            try {
                JSONObject jsonResource = json.getJSONObject(i);
                if(jsonResource.has("slug") && jsonResource.has("name") && jsonResource.has("date_modified") && jsonResource.has("status")) {
                    // verify the checking level
                    JSONObject jsonStatus = jsonResource.getJSONObject("status");
                    if(jsonStatus.has("checking_level")) {
                        if (Integer.parseInt(jsonStatus.get("checking_level").toString()) >= mContext.getResources().getInteger(R.integer.min_source_lang_checking_level)) {
                            // load resource
                            Resource r = new Resource(jsonResource.getString("slug"), jsonResource.getString("name"), jsonResource.getInt("date_modified"));

                            // @deprecated see the note below.
                            // determine if we need to download the source, notes, and terms
//                            boolean downloadResourceItems = false;
//                            if(checkServer) {
//                                // TRICKY: in order to correctly identify cached resources we must use the cached language
//                                Project cachedProject = getProject(p.getId());
//                                SourceLanguage cachedLanguage = cachedProject.getSourceLanguage(l.getId());
//                                if(cachedLanguage != null) {
//                                    Resource cachedResource = cachedLanguage.getResource(r.getId());
//                                    if (cachedResource == null) {
//                                        downloadResourceItems = true;
//                                    } else if (r.getDateModified() > cachedResource.getDateModified()) {
//                                        downloadResourceItems = true;
//                                    }
//                                } else {
//                                    downloadResourceItems = true;
//                                }
//                            }

                            // NOTE: the data store will automatically determine if a download is nessesary if the url includes a date_modified parameter.
                            if(checkServer) {
                                // we will attempt to use the provided urls before using the default download path
                                if(jsonResource.has("notes")) {
                                    mDataStore.fetchNotes(p.getId(), l.getId(), r.getId(), jsonResource.getString("notes"));
                                } else {
                                    mDataStore.fetchNotes(p.getId(), l.getId(), r.getId(), true);
                                }
                                if(jsonResource.has("terms")) {
                                    mDataStore.fetchTerms(p.getId(), l.getId(), r.getId(), jsonResource.getString("terms"));
                                } else {
                                    mDataStore.fetchTerms(p.getId(), l.getId(), r.getId(), true);
                                }
                                if(jsonResource.has("source")) {
                                    mDataStore.fetchSource(p.getId(), l.getId(), r.getId(), jsonResource.getString("source"));
                                } else {
                                    mDataStore.fetchSource(p.getId(), l.getId(), r.getId(), true);
                                }
                            }

                            l.addResource(r);
                            importedResources.add(r);
                        }
                    }
                } else {
                    Logger.w(this.getClass().getName(), "missing required parameters in the resoruces catalog for the language " + l.getId());
                }
            } catch (Exception e) {
                Logger.e(this.getClass().getName(), "failed to load the resources catalog", e);
                continue;
            }
        }
        return importedResources;
    }

    /**
     * Loads the translation notes for the project
     * @param jsonString
     * @param p
     */
    private void loadNotes(String jsonString, Project p) {
        // TODO: cache the notes by frame and add accessors to the frame object to retreive the notes. Then we can just load one set of notes at a time instead of loading everything into memory
        if(p == null) return;
        int numNotes = 0;

        // load source
        JSONArray jsonNotes;
        if(jsonString == null) {
            return;
        }
        try {
            jsonNotes = new JSONArray(jsonString);
        } catch (JSONException e) {
            Logger.e(this.getClass().getName(), "malformed notes", e);
            return;
        }

        // load the data
        for(int i=0; i<jsonNotes.length(); i++) {
            try {
                JSONObject jsonNote = jsonNotes.getJSONObject(i);
                if(jsonNote.has("date_modified")) continue; // skip the timestamp
                if(jsonNote.has("id") && jsonNote.has("tn")) {

                    // load id
                    String[] chapterFrameId = jsonNote.getString("id").split("-");
                    String frameId = chapterFrameId[1];
                    String chapterId = chapterFrameId[0];

                    // load notes
                    List<TranslationNote.Note> notes = new ArrayList<TranslationNote.Note>();
                    JSONArray jsonNoteItems = jsonNote.getJSONArray("tn");
                    for (int j = 0; j < jsonNoteItems.length(); j++) {
                        JSONObject jsonNoteItem = jsonNoteItems.getJSONObject(j);
                        notes.add(new TranslationNote.Note(jsonNoteItem.getString("ref").toString(), jsonNoteItem.getString("text").toString()));
                    }

                    // add translation notes to the frame
                    if(p.getChapter(chapterId) != null && p.getChapter(chapterId).getFrame(frameId) != null) {
                        p.getChapter(chapterId).getFrame(frameId).setTranslationNotes(new TranslationNote(notes));
                        numNotes ++;
                    } else {
                        Logger.w(this.getClass().getName(), "no chapter or frame exists for that note "+p.getId()+":"+chapterId+":"+frameId);
                    }
                } else {
                    Logger.w(this.getClass().getName(), "missing required parameters in the notes");
                }
            } catch (JSONException e) {
                Logger.e(this.getClass().getName(), "failed to load notes", e);
                continue;
            }
        }

        // set a flag that the project has notes
        if(numNotes > 0) {
            p.setHasNotes(true);
        }
    }

    /**
     * Loads the key terms for the project
     * @param jsonString
     * @param p
     */
    private void loadTerms(String jsonString, Project p) {
        if(p == null) return;

        // load source
        JSONArray jsonTerms;
        if(jsonString == null) {
            return;
        }
        try {
            jsonTerms = new JSONArray(jsonString);
        } catch (JSONException e) {
            Logger.e(this.getClass().getName(), "malformed terms", e);
            return;
        }

        // load the data
        for(int i=0; i<jsonTerms.length(); i++) {
            try {
                JSONObject jsonTerm = jsonTerms.getJSONObject(i);
                if(jsonTerm.has("date_modified")) continue; // skip the timestamp
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
                                Logger.w(this.getClass().getName(), "invalid key term reference "+jsonExample.getString("ref").toString());
                            }
                        }
                    }

                    // load term
                    Term t = new Term(jsonTerm.get("term").toString(), jsonTerm.get("sub").toString(), jsonTerm.get("def").toString(), jsonTerm.get("def_title").toString(), relatedTerms, examples);

                    // add term to the project
                    p.addTerm(t);
                } else {
                    Logger.w(this.getClass().getName(), "missing required parameters in the terms");
                }
            } catch (JSONException e) {
                Logger.e(this.getClass().getName(), "failed to load terms", e);
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
            Logger.e(this.getClass().getName(), "The project source was not found for "+p.getId());
            return;
        }
        try {
            JSONObject json = new JSONObject(jsonString);
            jsonChapters = json.getJSONArray("chapters");
        } catch (JSONException e) {
            Logger.e(this.getClass().getName(), "malformed project source", e);
            return;
        }

        // TODO: extract the images if nessesary
        // "sourceTranslations/"+p.getId()+"/en/images.tar.gz";
        // should go to
        // Note: for now all images are english.
        // cache/assets/p.getid()/en/images

        // load the data
        for(int i=0; i<jsonChapters.length(); i++) {
            try {
                JSONObject jsonChapter = jsonChapters.getJSONObject(i);
                if(jsonChapter.has("frames")) {
                    // load chapter
                    String chapterNumber = jsonChapter.get("number").toString();
                    String title = "";
                    String reference = "";
                    if(jsonChapter.has("title") && jsonChapter.has("ref")) {
                        title = jsonChapter.get("title").toString();
                        reference = jsonChapter.get("ref").toString();
                    }
                    Chapter c = new Chapter(chapterNumber, title, reference);

                    // add chapter to the project
                    p.addChapter(c);

                    // load frames
                    JSONArray jsonFrames = jsonChapter.getJSONArray("frames");
                    for(int j=0; j<jsonFrames.length(); j++) {
                        JSONObject jsonFrame = jsonFrames.getJSONObject(j);
                        if(jsonFrame.has("id") && jsonFrame.has("text")) {
                            String format = "";
                            if(jsonFrame.has("format")) {
                                format = jsonFrame.getString("format");
                            }
                            String img = "";
                            if(jsonFrame.has("img")) {
                                img = jsonFrame.get("img").toString();
                            }
                            c.addFrame(new Frame(jsonFrame.get("id").toString(), img, jsonFrame.get("text").toString(), format));
                        } else {
                            Logger.w(this.getClass().getName(), "missing required parameters in source frame at index "+i+":"+j);
                        }
                    }
                } else {
                    Logger.w(this.getClass().getName(), "missing required parameters in source chapter at index " + i);
                }
            } catch (JSONException e) {
                Logger.e(this.getClass().getName(), "Failed to load project source", e);
                continue;
            }
        }
    }

    /**
     * Imports the source from the given directory
     * @param dir
     */
    public void importSource(File dir) {
        Logger.i(this.getClass().getName(), "importing source files from "+ dir.getName());
        if(dir.exists() && dir.isDirectory()) {
            File projectsCatalogFile = new File(dir, "projects_catalog.json");
            if(projectsCatalogFile.exists()) {
                try {
                    // load projects
                    String projCat = FileUtils.readFileToString(projectsCatalogFile);
                    JSONArray projCatJson = new JSONArray(projCat);
                    for(int i = 0; i < projCatJson.length(); i ++) {
                        try {
                            JSONObject projJson = projCatJson.getJSONObject(i);
                            String projSlug = projJson.getString("slug");
                            Project existingProject = getProject(projSlug);
                            int projDateModified = projJson.getInt("date_modified");
                            if (existingProject == null || existingProject.getDateModified() < projDateModified) {
                                // import/replace the project
                                mDataStore.importProject(projJson.toString());
                                // load languages
                                File projDir = new File(dir, projSlug);
                                if(projDir.exists()) {
                                    File langCatFile = new File(projDir, "languages_catalog.json");
                                    if(langCatFile.exists()) {
                                        String langCat = FileUtils.readFileToString(langCatFile);
                                        JSONArray langCatJson = new JSONArray(langCat);
                                        for(int j = 0; j < langCatJson.length(); j ++) {
                                            try {
                                                JSONObject langJson = langCatJson.getJSONObject(j);
                                                JSONObject langInfoJson = langJson.getJSONObject("language");
                                                String langSlug = langInfoJson.getString("slug");
                                                int langDateModified = langInfoJson.getInt("date_modified");
                                                SourceLanguage existingLanguage = null;
                                                if(existingProject != null) {
                                                    existingLanguage = existingProject.getSourceLanguage(langSlug);
                                                }
                                                if(existingLanguage == null || existingLanguage.getDateModified() < langDateModified) {
                                                    // import/replace the source language
                                                    mDataStore.importSourceLanguage(projSlug, langJson.toString());
                                                    // load resources
                                                    File langDir = new File(projDir, langSlug);
                                                    if(langDir.exists()) {
                                                        File resCatFile = new File(langDir, "resources_catalog.json");
                                                        if(resCatFile.exists()) {
                                                            String resCat = FileUtils.readFileToString(resCatFile);
                                                            JSONArray resCatJson = new JSONArray(resCat);
                                                            for(int k = 0; k < resCatJson.length(); k ++) {
                                                                try {
                                                                    JSONObject resJson = resCatJson.getJSONObject(k);
                                                                    String resSlug = resJson.getString("slug");
                                                                    int resDateModified = resJson.getInt("date_modified");
                                                                    Resource existingResource = null;
                                                                    if(existingLanguage != null) {
                                                                        existingResource = existingLanguage.getResource(resSlug);
                                                                    }
                                                                    if(existingResource == null || existingResource.getDateModified() < resDateModified) {
                                                                        // import/replace the resource catalog
                                                                        mDataStore.importResource(projSlug, langSlug, resJson.toString());
                                                                        // load the individual resource files
                                                                        File resDir = new File(langDir, resSlug);
                                                                        if(resDir.exists()) {
                                                                            File notesFile = new File(resDir, "notes.json");
                                                                            File sourceFile = new File(resDir, "source.json");
                                                                            File termsFile = new File(resDir, "terms.json");

                                                                            String notes = FileUtils.readFileToString(notesFile);
                                                                            String source = FileUtils.readFileToString(sourceFile);
                                                                            String terms = FileUtils.readFileToString(termsFile);

                                                                            mDataStore.importNotes(projSlug, langSlug, resSlug, notes);
                                                                            mDataStore.importSource(projSlug, langSlug, resSlug, source);
                                                                            mDataStore.importTerms(projSlug, langSlug, resSlug, terms);
                                                                        }
                                                                    }
                                                                } catch (Exception e) {
                                                                    Logger.e(this.getClass().getName(), "failed to import the resource", e);
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            } catch (Exception e) {
                                                Logger.e(this.getClass().getName(), "failed to import the source language", e);
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Logger.e(this.getClass().getName(), "failed to import the source project", e);
                        }
                    }
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "failed to import the source files", e);
                }
                // reload the projects
                initProjects();
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

    /**
     * Returns a list of languages
     * @return
     */
    public List<SourceLanguage> getSourceLanguages() {
        return mSourceLanguages;
    }

    /**
     * Returns the data store. This should not be used unless you know what you are doing.
     * @return
     */
    public DataStore getDataStore() {
        return mDataStore;
    }

    public interface OnProgressCallback {
        void onProgress(double progress, String message);
        void onSuccess();
    }
}
