package com.door43.translationstudio.projects;

import android.content.SharedPreferences;
import android.net.Uri;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.data.DataStore;
import com.door43.util.Logger;

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
    private static List<Project> mProjects = new ArrayList<>();
    // so we can look up by id
    private static Map<String, Project> mProjectMap = new HashMap<>();

    // meta projects
    private static Map<String, PseudoProject> mPseudoProjectMap = new HashMap<>();

    private static List<Model> mListableProjects = new ArrayList<>();
    private static Map<String, Model> mListableProjectMap = new HashMap<>();

    // so we can look up by index
    private static List<Language> mLanguages = new ArrayList<>();
    // so we can look up by id
    private static Map<String, Language> mLanguagesMap = new HashMap<>();
    // so we can look up by name
    private static Map<String, Language> mLanguagesNameMap = new HashMap<>();

    // these constants are used to bind the progress bar to within certain ranges for the data.
    private final double PERCENT_TARGET_LANGUAGES = 70.0;
    private final double PERCENT_PROJECTS = 10.0;
    private final double PERCENT_PROJECT_SOURCE = 20.0;
    private double mProgress = 0;

    private static String mSelectedProjectId;
    private static MainApplication mContext;
    private static final String TAG = "ProjectManager";
    private OnProgressCallback mInitProgressCallback;
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
            mInitProgressCallback = callback;
            // begin loading target languages
            initTargetLanguages();
            // begin loading projects
            initProjects();
        }
        if(mInitProgressCallback != null) {
            mInitProgressCallback.onSuccess();
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
        if(p == null || p.getSelectedSourceLanguage() == null) return;

        String source = mDataStore.pullSource(p.getId(), p.getSelectedSourceLanguage().getId(), p.getSelectedSourceLanguage().getSelectedResource().getId(), false, false);
        p.flush();
        if(!displayNotice) {
            mProgress += PERCENT_PROJECT_SOURCE/3;
            if(mInitProgressCallback != null) {
                mInitProgressCallback.onProgress(mProgress, mContext.getResources().getString(R.string.opening_project));
            }
        }
        loadProjectSource(source, p);
        String terms = mDataStore.pullTerms(p.getId(), p.getSelectedSourceLanguage().getId(), p.getSelectedSourceLanguage().getSelectedResource().getId(), false, false);
        if(!displayNotice) {
            mProgress += PERCENT_PROJECT_SOURCE/3;
            if(mInitProgressCallback != null) {
                mInitProgressCallback.onProgress(mProgress, mContext.getResources().getString(R.string.loading_key_terms));
            }
        }
        loadTerms(terms, p);
        String notes = mDataStore.pullNotes(p.getId(), p.getSelectedSourceLanguage().getId(), p.getSelectedSourceLanguage().getSelectedResource().getId(), false, false);
        if(!displayNotice) {
            mProgress += PERCENT_PROJECT_SOURCE/3;
            if(mInitProgressCallback != null) {
                mInitProgressCallback.onProgress(mProgress, mContext.getResources().getString(R.string.loading_translation_notes));
            }
        }
        loadNotes(notes, p);
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
        if(!mPseudoProjectMap.containsKey(p.getId())) {
            mPseudoProjectMap.put(p.getId(), p);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sorts the listable projects
     */
    public void sortListableProjects() {
        sortModelList(mListableProjects);
    }

    /**
     * Sorts a list of models
     * @param models
     */
    public void sortModelList(List<? extends Model> models) {
        Collections.sort(models, new Comparator<Model>() {
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
    public PseudoProject getPseudoProject(String id) {
        if(mPseudoProjectMap.containsKey(id)) {
            return mPseudoProjectMap.get(id);
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
        } else {
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
        SharedPreferences settings = mContext.getSharedPreferences(mContext.PREFERENCES_TAG, mContext.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("selected_project", id);
        editor.apply();
    }

    /**
     * Returns the currently selected project in the app
     * @return
     */
    public Project getSelectedProject() {
        // This part should be moved into the Navigator class
        if(mContext.rememberLastPosition()) {
            SharedPreferences settings = mContext.getSharedPreferences(mContext.PREFERENCES_TAG, mContext.MODE_PRIVATE);
            mSelectedProjectId = settings.getString("selected_project", null);
        }

        Project selectedProject = getProject(mSelectedProjectId);;
        if(selectedProject == null) {
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
     * Loads a list of target languages from the disk
     */
    private void initTargetLanguages() {
        String catalog = mDataStore.pullTargetLanguageCatalog();
        // parse target languages
        JSONArray json;
        try {
            json = new JSONArray(catalog);
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
                        if(mInitProgressCallback != null) {
                            mInitProgressCallback.onProgress(mProgress, String.format(mContext.getResources().getString(R.string.loading_target_language), jsonLanguage.get("lc").toString()));
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
     * Returns a list of projects that are available on the server
     * These are just the plain projects without any translation information.
     * The downloaded data is stored temporarily and does not affect the currently loaded projects
     * @return
     */
    public List<Project> downloadProjectList(boolean ignoreCache) {
        String catalog = mDataStore.fetchProjectCatalog(ignoreCache);
        List<Project> projects = new ArrayList<>();

        // load projects
        JSONArray json;
        try {
            json = new JSONArray(catalog);
        } catch (JSONException e) {
            Logger.e(this.getClass().getName(), "malformed projects catalog", e);
            return new ArrayList<>();
        }

        // load the data
        for(int i=0; i<json.length(); i ++) {
            if(Thread.currentThread().isInterrupted()) break;
            try {
                JSONObject jsonProj = json.getJSONObject(i);
                Project p = Project.generate(jsonProj);
                if(p != null) {
                    projects.add(p);
                }
            } catch (JSONException e) {
                Logger.e(this.getClass().getName(), "Failed to read the project entry", e);
                continue;
            }
        }

        sortModelList(projects);
        return projects;
    }

    /**
     * Downloads the project image from the server and returns the local path.
     * The image is stored in a temporary location and does not affect the currently loaded image
     * @param p the project who's image will be downloaded
     * @param ignoreCache
     * @return
     */
    public String downloadProjectImage(Project p, boolean ignoreCache) {
        // TODO: we are waiting for the project image url to be made avaiable in the api.
//        return mDataStore.fetchTempAsset(p.getImageUrl(), ignoreCache);
        return null;
    }

    /**
     * Checks if a language along with it's resources has been downloaded
     * @param projectId
     * @param languageId
     * @return
     */
    public boolean isSourceLanguageDownloaded(String projectId, String languageId) {
        Project p = getProject(projectId);
        if(p != null) {
           SourceLanguage l = p.getSourceLanguage(languageId);
            if(l != null) {
                return l.getResources().length > 0;
            }
        }
        return false;
    }

    /**
     * Checks if there are updates available for the source language
     * @param projectId the id of the project to which the source language belongs
     * @param latestLanguage the source language that contains the latest date modified info
     * @return
     */
    public boolean updateAvailable(String projectId, SourceLanguage latestLanguage) {
        Project p = getProject(projectId);
        if(p != null) {
            SourceLanguage currentLanguage = p.getSourceLanguage(latestLanguage.getId());
            if(currentLanguage != null && (latestLanguage.getDateModified() > currentLanguage.getDateModified() || latestLanguage.getResourcesDateModified() > currentLanguage.getResourcesDateModified())) {
                // there is an update to the language definition or the resources
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if there are updates available for the project
     * @param latestProject the project that contains the latest date modified info
     * @return
     */
    public boolean updateAvailable(Project latestProject) {
        if(latestProject != null) {
            Project currentProject = getProject(latestProject.getId());
            if(currentProject != null && (latestProject.getDateModified() > currentProject.getDateModified() || latestProject.getSourceLanguagesDateModified() > currentProject.getSourceLanguagesDateModified())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a list of project source languages that are available on the server
     * These are just the plain languages without any resources.
     * The downloaded data is stored temporarily and does not affect the currently loaded source languages
     * The languages are also loaded into the project
     * @param p
     * @param ignoreCache
     * @return
     */
    public List<SourceLanguage> downloadSourceLanguageList(Project p, boolean ignoreCache) {
        String catalog;
        List<SourceLanguage> languages = new ArrayList<>();
        if(p.getLanguageCatalog() != null) {
            catalog = mDataStore.fetchTempAsset(p.getLanguageCatalog(), ignoreCache);
        } else {
            catalog = mDataStore.fetchTempAsset(mDataStore.sourceLanguageCatalogUrl(p.getId()), ignoreCache);
        }

        // parse source languages
        JSONArray json;
        try {
            json = new JSONArray(catalog);
        } catch (Exception e) {
            Logger.e(this.getClass().getName(), "malformed source language catalog", e);
            return languages;
        }

        // load the data
        for(int i=0; i<json.length(); i ++) {
            if(Thread.currentThread().isInterrupted()) break;
            try {
                JSONObject jsonLang = json.getJSONObject(i);
                JSONObject jsonProj = jsonLang.getJSONObject("project");
                SourceLanguage l = SourceLanguage.generate(jsonLang);
                if(l != null) {
                    // default title and description
                    if(i == 0) {
                        p.setDefaultTitle(jsonProj.getString("name"));
                        p.setDefaultDescription(jsonProj.getString("desc"));
                    }

                    // title and description translations
                    p.setTitle(jsonProj.getString("name"), l);
                    p.setDescription(jsonProj.getString("desc"), l);

                    // meta translations (Pseudo projects)
                    if(jsonProj.has("meta") && p.numSudoProjects() > 0) {
                        JSONArray jsonMeta = jsonProj.getJSONArray("meta");
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

                    // load into project
                    p.addSourceLanguage(l);

                    languages.add(l);
                }
            } catch (JSONException e) {
                Logger.e(this.getClass().getName(), "Failed to read the source language entry", e);
                continue;
            }
        }

        // Attempt to select a more accurate default language for the project title and description
        String deviceLocale = Locale.getDefault().getLanguage();
        if (p.getSourceLanguage(deviceLocale) != null) {
            p.setSelectedSourceLanguage(deviceLocale);
        } else if (p.getSourceLanguage("en") != null) {
            p.setSelectedSourceLanguage("en");
        }

        return languages;
    }

    /**
     * Returns a list of source language resources that are available on the server
     * These are just the plain resources without any data e.g. notes, terms, source.
     * The downloaded data is stored temporarily and does not affect the currently loaded resources
     * The resoruces are also loaded into the language
     * @param p
     * @param l
     * @return
     */
    public List<Resource> downloadResourceList(Project p, SourceLanguage l, boolean ignoreCache) {
        String catalog;
        List<Resource> resources = new ArrayList<>();
        if(p.getLanguageCatalog() != null) {
            catalog = mDataStore.fetchTempAsset(p.getSourceLanguage(l.getId()).getResourceCatalog(), ignoreCache);
        } else {
            catalog = mDataStore.fetchTempAsset(mDataStore.resourceCatalogUrl(p.getId(), l.getId()), ignoreCache);
        }

        // parse resources
        JSONArray json;
        try {
            json = new JSONArray(catalog);
        } catch (Exception e) {
            Logger.e(this.getClass().getName(), "malformed resource catalog", e);
            return resources;
        }

        // load the data
        for(int i=0; i<json.length(); i ++) {
            if(Thread.currentThread().isInterrupted()) break;
            try {
                JSONObject jsonRes = json.getJSONObject(i);
                Resource r = Resource.generate(jsonRes);
                if(r != null) {
                    resources.add(r);
                }
            } catch (JSONException e) {
                Logger.e(this.getClass().getName(), "Failed to read the resource entry", e);
                continue;
            }
        }

        return resources;
    }

    /**
     * Downloads the notes from the server.
     * The downloaded data is stored temporarily and does not affect the currently loaded notes
     * @param p the project
     * @param l the source language
     * @param r the resource
     * @param ignoreCache
     */
    public void downloadNotes(Project p, SourceLanguage l, Resource r, boolean ignoreCache) {
        if(r.getNotesCatalog() != null) {
            mDataStore.fetchTempAsset(r.getNotesCatalog(), ignoreCache);
        } else {
            mDataStore.fetchTempAsset(mDataStore.notesUrl(p.getId(), l.getId(), r.getId()), ignoreCache);
        }
    }

    /**
     * Downloads the terms from the server.
     * The downloaded data is stored temporarily and does not affect the currently loaded terms
     * @param p the project
     * @param l the source language
     * @param r the resource
     * @param ignoreCache
     */
    public void downloadTerms(Project p, SourceLanguage l, Resource r, boolean ignoreCache) {
        if(r.getNotesCatalog() != null) {
            mDataStore.fetchTempAsset(r.getTermsCatalog(), ignoreCache);
        } else {
            mDataStore.fetchTempAsset(mDataStore.termsUrl(p.getId(), l.getId(), r.getId()), ignoreCache);
        }
    }

    /**
     * Downloads the source from the server.
     * The downloaded data is stored temporarily and does not affect the currently loaded source
     * @param p the project
     * @param l the source language
     * @param r the resource
     * @param ignoreCache
     */
    public void downloadSource(Project p, SourceLanguage l, Resource r, boolean ignoreCache) {
        if(r.getNotesCatalog() != null) {
            mDataStore.fetchTempAsset(r.getSourceCatalog(), ignoreCache);
        } else {
            mDataStore.fetchTempAsset(mDataStore.sourceUrl(p.getId(), l.getId(), r.getId()), ignoreCache);
        }
    }

    /**
     * Loads a list of projects from the disk
     */
    private List<Project> initProjects() {
        String catalog = mDataStore.pullProjectCatalog(false, false);

        List<Project> importedProjects = new ArrayList<>();
        // load projects
        JSONArray json;
        try {
            json = new JSONArray(catalog);
        } catch (JSONException e) {
            Logger.e(this.getClass().getName(), "malformed projects catalog", e);
            return new ArrayList<>();
        }

        // load the data
        int numProjects = json.length();
        for(int i=0; i<numProjects; i++) {
            try {
                JSONObject jsonProj = json.getJSONObject(i);
                // update the progress
                mProgress += PERCENT_PROJECTS / numProjects;
                if(mInitProgressCallback != null) {
                    mInitProgressCallback.onProgress(mProgress, String.format(mContext.getResources().getString(R.string.loading_project), jsonProj.get("slug").toString()));
                }

                // generate project
                Project p = Project.generate(jsonProj);

                if(p != null) {
                    // build pseudo category's in the library
                    PseudoProject rootPseudoProject = null;
                    if(p.getPseudoProjects().length > 0) {
                        // load or set the root pseudo category
                        rootPseudoProject = getPseudoProject(p.getPseudoProjects()[0].getId());
                        if (rootPseudoProject == null) {
                            rootPseudoProject = p.getPseudoProjects()[0];
                            addMetaProject(rootPseudoProject);
                        }

                        // populate pseudo sub-categories
                        PseudoProject currentPseudoProject = rootPseudoProject;
                        for (int j = 1; j < p.getPseudoProjects().length; j++) {
                            PseudoProject sp = p.getPseudoProjects()[j];
                            if (currentPseudoProject.getMetaChild(sp.getId()) != null) {
                                // follow existing pseudo sub-categories
                                currentPseudoProject = currentPseudoProject.getMetaChild(sp.getId());
                            } else {
                                // insert new pseudo sub-category
                                currentPseudoProject.addChild(sp);
                                currentPseudoProject = sp;
                            }
                        }
                        // close with the project
                        currentPseudoProject.addChild(p);
                    }

                    // add project or pseudo category to the library list
                    if(rootPseudoProject == null) {
                        addListableProject(p);
                    } else {
                        addListableProject(rootPseudoProject);
                    }

                    // add project to internal list of projects
                    if(addProject(p)) {
                        importedProjects.add(p);
                    }

                    // loads the source languages into the project
                    List<SourceLanguage> languages = loadSourceLanguageCatalog(p);
                    // validate project has languages
                    if(languages.size() == 0) {
                        Logger.e(this.getClass().getName(), "the source languages could not be loaded for the project "+p.getId());
                        importedProjects.remove(p);
                        deleteProject(p);
                        if(rootPseudoProject == null) {
                            deleteListableProject(p);
                        } else {
                            // TODO: this might delete all the projects from the list
                            deleteListableProject(rootPseudoProject);
                        }
                    }
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
     * Loads source languages from the disk into the project
     * @param p
     */
    private List<SourceLanguage> loadSourceLanguageCatalog(Project p) {
        String catalog = mDataStore.pullSourceLanguageCatalog(p.getId(), false, false);

        List<SourceLanguage> importedLanguages = new ArrayList<>();
        if(catalog == null) {
            return importedLanguages;
        }
        // parse source languages
        JSONArray json;
        try {
            json = new JSONArray(catalog);
        } catch (Exception e) {
            Logger.e(this.getClass().getName(), "malformed source language catalog", e);
            return new ArrayList<>();
        }

        // load the data
        for(int i=0; i<json.length(); i++) {
            if(Thread.currentThread().isInterrupted()) break;
            try {
                JSONObject jsonLang = json.getJSONObject(i);
                JSONObject jsonProj = jsonLang.getJSONObject("project");
                SourceLanguage l = SourceLanguage.generate(jsonLang);

                if(l != null) {
                    // default title and description
                    if(i == 0) {
                        p.setDefaultTitle(jsonProj.getString("name"));
                        p.setDefaultDescription(jsonProj.getString("desc"));
                    }

                    // load title and description translations.
                    p.setTitle(jsonProj.getString("name"), l);
                    p.setDescription(jsonProj.getString("desc"), l);

                    // meta (pseudo project/category) translations
                    if(jsonProj.has("meta") && p.numSudoProjects() > 0) {
                        JSONArray jsonMeta = jsonProj.getJSONArray("meta");
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

                    List<Resource> importedResources = loadResourcesCatalog(p, l);

                    // only resources with the minium checking level are valid for source translations
                    if(importedResources.size() > 0) {
                        // TRICKY: we add source languages to the language list as a precautionary measure
                        // to make sure we have as much info as possible
                        addLanguage(l);
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

        // Attempt to select a more accurate default language for the project title and description
        String deviceLocale = Locale.getDefault().getLanguage();
        // don't change the source language if already selecteed
        if(!p.hasSelectedSourceLanguage()) {
            if (p.getSourceLanguage(deviceLocale) != null) {
                p.setSelectedSourceLanguage(deviceLocale);
            } else if (p.getSourceLanguage("en") != null) {
                p.setSelectedSourceLanguage("en");
            }
        }

        // load the correct title and description
        p.setDefaultTitle(p.getTitle(p.getSelectedSourceLanguage()));
        p.setDefaultDescription(p.getDescription(p.getSelectedSourceLanguage()));
        return importedLanguages;
    }

    /**
     * Loads the resource definitions for the given language.
     * This does not load any of the source
     * @param p  the project
     * @param l the source language
     */
    private List<Resource> loadResourcesCatalog(Project p, SourceLanguage l) {
        String catalog = mDataStore.pullResourceCatalog(p.getId(), l.getId(), false, false);

        List<Resource> importedResources = new ArrayList<>();
        if(catalog == null) {
            return importedResources;
        }
        // parse resources
        JSONArray json;
        try {
            json = new JSONArray(catalog);
        } catch (Exception e) {
            Logger.e(this.getClass().getName(), "malformed resource catalog", e);
            return new ArrayList<>();
        }

        // load the data
        for(int i=0; i<json.length(); i++) {
            if(Thread.currentThread().isInterrupted()) break;
            try {
                JSONObject jsonResource = json.getJSONObject(i);
                if(jsonResource.has("slug") && jsonResource.has("name") && jsonResource.has("date_modified") && jsonResource.has("status")) {
                    // verify the checking level
                    JSONObject jsonStatus = jsonResource.getJSONObject("status");
                    if(jsonStatus.has("checking_level")) {
                        if (jsonStatus.getInt("checking_level") >= mContext.getResources().getInteger(R.integer.min_source_lang_checking_level)) {
                            // load resource
                            Resource r = new Resource(jsonResource.getString("slug"), jsonResource.getString("name"), jsonStatus.getInt("checking_level"), jsonResource.getInt("date_modified"));
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
            Logger.e(this.getClass().getName(), "malformed notes for project "+p.getId(), e);
            return;
        }

        // load the data
        for(int i=0; i<jsonNotes.length(); i++) {
            if(Thread.currentThread().isInterrupted()) break;
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
            Logger.e(this.getClass().getName(), "malformed terms for project "+p.getId(), e);
            return;
        }

        // load the data
        for(int i=0; i<jsonTerms.length(); i++) {
            if(Thread.currentThread().isInterrupted()) break;
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

                    // load aliases
                    List<String> aliases = new ArrayList<>();
                    if(jsonTerm.has("aliases")) {
                        JSONArray jsonAliases = jsonTerm.getJSONArray("aliases");
                        for(int j = 0; j < jsonAliases.length(); j ++) {
                            String alias = jsonAliases.getString(j);
                            aliases.add(alias);
                        }
                    }

                    // load term
                    // TODO: the Bible terms sometimes contain multiple terms in the term field delimited by a comma. there should only be one that is supplimented with aliases. This should get fixed on the api some time.
                    Term t = new Term(jsonTerm.get("term").toString(), jsonTerm.get("sub").toString(), jsonTerm.get("def").toString(), jsonTerm.get("def_title").toString(), relatedTerms, examples, aliases);

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
    private void loadProjectSource(String jsonString, Project p) {
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
     * Returns the data store. This should not be used unless you know what you are doing.
     * @return
     */
    public DataStore getDataStore() {
        return mDataStore;
    }

    /**
     * Merges a project catalog entry from the temp assets into the assets
     * @param projectId
     */
    public void mergeProject(String projectId) {
        File catalogFile = mDataStore.getTempAsset(mDataStore.getKey(Uri.parse(mDataStore.projectCatalogUrl())));
        if(catalogFile.exists()) {
            try {
                String catalog = FileUtils.readFileToString(catalogFile);
                JSONArray json = new JSONArray(catalog);
                for(int i=0; i<json.length(); i++) {
                    JSONObject jsonProj = json.getJSONObject(i);
                    if(jsonProj.getString("slug").equals(projectId)) {
                        mDataStore.importProject(jsonProj.toString());
                        break;
                    }
                }
            } catch (Exception e) {
                Logger.e(this.getClass().getName(), "Failed to merge the project "+projectId, e);
            }
        }
    }

    /**
     * Merges a source language catalog entry from the temp assets into the assets
     * @param projectId
     * @param languageId
     */
    public void mergeSourceLanguage(String projectId, String languageId) {
        File catalogFile = mDataStore.getTempAsset(mDataStore.getKey(Uri.parse(mDataStore.sourceLanguageCatalogUrl(projectId))));
        if(catalogFile.exists()) {
            try {
                String catalog = FileUtils.readFileToString(catalogFile);
                JSONArray json = new JSONArray(catalog);
                for(int i=0; i<json.length(); i++) {
                    JSONObject jsonLang = json.getJSONObject(i).getJSONObject("language");
                    if(jsonLang.getString("slug").equals(languageId)) {
                        mDataStore.importSourceLanguage(projectId, json.getJSONObject(i).toString());
                        break;
                    }
                }
            } catch (Exception e) {
                Logger.e(this.getClass().getName(), "Failed to merge the source language "+languageId, e);
            }
        }
    }

    /**
     * Merges a resource catalog entry from the temp assets into the assets
     * @param projectId
     * @param languageId
     * @param resourceId
     */
    public void mergeResource(String projectId, String languageId, String resourceId) {
        File catalogFile = mDataStore.getTempAsset(mDataStore.getKey(Uri.parse(mDataStore.resourceCatalogUrl(projectId, languageId))));
        if(catalogFile.exists()) {
            try {
                String catalog = FileUtils.readFileToString(catalogFile);
                JSONArray json = new JSONArray(catalog);
                for(int i=0; i<json.length(); i++) {
                    JSONObject jsonRes = json.getJSONObject(i);
                    if(jsonRes.getString("slug").equals(resourceId)) {
                        mDataStore.importResource(projectId, languageId, jsonRes.toString());
                        break;
                    }
                }
            } catch (Exception e) {
                Logger.e(this.getClass().getName(), "Failed to merge the resource "+resourceId, e);
            }
        }
    }

    /**
     * Merges notes from the temp assets into the assets
     * @param projectId
     * @param languageId
     * @param resourceId
     */
    public void mergeNotes(String projectId, String languageId, String resourceId) {
        String key = mDataStore.getKey(Uri.parse(mDataStore.notesUrl(projectId, languageId, resourceId)));
        File newNotesFile = mDataStore.getTempAsset(key);

        if(newNotesFile.exists()) {
            try {
                mDataStore.importNotes(projectId, languageId, resourceId, FileUtils.readFileToString(newNotesFile));
            } catch (Exception e) {
                Logger.e(this.getClass().getName(), "Failed to merge the notes "+resourceId, e);
            }
        }
    }

    /**
     * Merges important terms from the temp assets into the assets
     * @param projectId
     * @param languageId
     * @param resourceId
     */
    public void mergeTerms(String projectId, String languageId, String resourceId) {
        String key = mDataStore.getKey(Uri.parse(mDataStore.termsUrl(projectId, languageId, resourceId)));
        File newTermsFile = mDataStore.getTempAsset(key);

        if(newTermsFile.exists()) {
            try {
                mDataStore.importTerms(projectId, languageId, resourceId, FileUtils.readFileToString(newTermsFile));
            } catch (Exception e) {
                Logger.e(this.getClass().getName(), "Failed to merge the terms "+resourceId, e);
            }
        }
    }

    /**
     * Merges source from the temp assets into the assets
     * @param projectId
     * @param languageId
     * @param resourceId
     */
    public void mergeSource(String projectId, String languageId, String resourceId) {
        String key = mDataStore.getKey(Uri.parse(mDataStore.sourceUrl(projectId, languageId, resourceId)));
        File newSourceFile = mDataStore.getTempAsset(key);

        if(newSourceFile.exists()) {
            try {
                mDataStore.importSource(projectId, languageId, resourceId, FileUtils.readFileToString(newSourceFile));
            } catch (Exception e) {
                Logger.e(this.getClass().getName(), "Failed to merge the source "+resourceId, e);
            }
        }
    }

    /**
     * Reloads the project from the disk
     * @param projectId
     */
    public void reloadProject(String projectId) {
        String catalog = mDataStore.pullProjectCatalog(false, false);

        // load projects
        JSONArray json;
        try {
            json = new JSONArray(catalog);
        } catch (JSONException e) {
            Logger.e(this.getClass().getName(), "malformed projects catalog", e);
            return;
        }

        // load the data
        for(int i=0; i<json.length(); i++) {
            try {
                JSONObject jsonProj = json.getJSONObject(i);
                if(jsonProj.getString("slug") .equals(projectId)) {
                    // reload the project
                    Project p = new Project(jsonProj.get("slug").toString(), Integer.parseInt(jsonProj.get("date_modified").toString()));
                    if(jsonProj.has("sort")) {
                        p.setSortKey(jsonProj.getString("sort"));
                    }

                    // remove the old project
                    Project originalProject = getProject(p.getId());
                    if(originalProject != null) {
                        deleteProject(originalProject);
                        if(originalProject.getPseudoProjects().length > 0) {
                            PseudoProject originalRoot = originalProject.getPseudoProjects()[0];
                            deleteListableProject(originalRoot);
                        }
                    }

                    // load meta
                    PseudoProject rootPseudoProject = null;
                    if(jsonProj.has("meta")) {
                        JSONArray jsonMeta = jsonProj.getJSONArray("meta");
                        if(jsonMeta.length() > 0) {
                            // get the root meta
                            String metaSlug = jsonMeta.get(0).toString();
                            rootPseudoProject = getPseudoProject(metaSlug);
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

                    // add project to the internal list and continue loading
                    addProject(p);

                    // load source languages
                    List<SourceLanguage> languages = loadSourceLanguageCatalog(p);
                    // validate project has languages
                    if(languages.size() == 0) {
                        Logger.e(this.getClass().getName(), "the source languages could not be loaded for the project "+p.getId());
                        deleteProject(p);
                        if(rootPseudoProject == null) {
                            deleteListableProject(p);
                        } else {
                            // TODO: this might delete all the projects from the list
                            deleteListableProject(rootPseudoProject);
                        }
                    }

                    // reload the selected project source
                    if(getSelectedProject() != null && getSelectedProject().getId().equals(p.getId())) {
                        fetchProjectSource(p, false);
                    }
                    break;
                }
            } catch (JSONException e) {
                Logger.e(this.getClass().getName(), "failed to load projects catalog", e);
                continue;
            }
        }
    }

    public interface OnProgressCallback {
        void onProgress(double progress, String message);
        void onSuccess();
    }
}
