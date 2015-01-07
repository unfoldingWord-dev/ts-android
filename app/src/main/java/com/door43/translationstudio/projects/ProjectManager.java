package com.door43.translationstudio.projects;

import android.content.SharedPreferences;
import android.util.Log;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.data.DataStore;
import com.door43.translationstudio.util.MainContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static Map<String, SudoProject> mMetaProjectMap = new HashMap<String, SudoProject>();

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
     * loads the source projects
     */
    public void init(OnProgressCallback callback) {
        // make sure we only call this once.
        if(!mHasLoaded) {
            mCallback = callback;
            // begin loading target languages
            String targetLanguageCatalog = mDataStore.fetchTargetLanguageCatalog();
            loadTargetLanguagesCatalog(targetLanguageCatalog);
        }
        mCallback.onSuccess();
    }

    /**
     * Downloads any new projects from the server
     */
    public void downloadNewProjects() {
        String catalog = mDataStore.fetchProjectCatalog(true);
        List<Project> projects = loadProjectsCatalog(catalog);
        for(Project p:projects) {
            downloadProjectUpdates(p);
        }
    }

    /**
     * Downloads the latest version of the project resources from the server
     * TODO: this method is in major need of repair.
     * @param p
     */
    public void downloadProjectUpdates(Project p) {
        // download the source language catalog
        String languageCatalog = mDataStore.fetchSourceLanguageCatalog(p.getId(), true);
        List<SourceLanguage> languages = loadSourceLanguageCatalog(p, null, languageCatalog);
        for(SourceLanguage l:languages) {
            // only download changed languages or languages that don't have any source
            // TODO: this date check will not work because the loadSourceLanguageCatalog overwrites it
            boolean hasNewVersion = getSourceLanguage(l.getId()).getDateModified() < l.getDateModified();
            boolean neededUpdate = false;
            if(hasNewVersion) {
                String resourceCatalog = mDataStore.fetchResourceCatalog(p.getId(), l.getId(), true);
                List<Resource> resources = loadResourcesCatalog(l, resourceCatalog);
                for(Resource r:resources) {
                    if(hasNewVersion || mDataStore.fetchSourceText(p.getId(), l.getId(), r.getId(),  false) == null) {
                        mDataStore.fetchSourceText(p.getId(), l.getId(), r.getId(), true);
                        neededUpdate = true;
                    }

                    if(hasNewVersion || mDataStore.fetchTermsText(p.getId(), l.getId(), r.getId(), false) == null) {
                        mDataStore.fetchTermsText(p.getId(), l.getId(), r.getId(), true);
                        neededUpdate = true;
                    }
                    if(hasNewVersion || mDataStore.fetchTranslationNotes(p.getId(), l.getId(), r.getId(), false) == null) {
                        mDataStore.fetchTranslationNotes(p.getId(), l.getId(), r.getId(), true);
                        neededUpdate = true;
                    }
                }
            } else {
                // TODO: download missing resources as well
            }



            // reload the source if this is the currently selected project
            if(neededUpdate && p.getId().equals(mSelectedProjectId) && getSelectedProject().getSelectedSourceLanguage().equals(l)) {
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
        if(p == null) return;

        String source = mDataStore.fetchSourceText(p.getId(), p.getSelectedSourceLanguage().getId(), p.getSelectedSourceLanguage().getSelectedResource().getId(), false);
        p.flush();
        if(!displayNotice) {
            mProgress += PERCENT_PROJECT_SOURCE/3;
            mCallback.onProgress(mProgress, mContext.getResources().getString(R.string.opening_project));
        }
        loadProject(source, p);
        String terms = mDataStore.fetchTermsText(p.getId(), p.getSelectedSourceLanguage().getId(), p.getSelectedSourceLanguage().getSelectedResource().getId(), false);
        if(!displayNotice) {
            mProgress += PERCENT_PROJECT_SOURCE/3;
            mCallback.onProgress(mProgress, mContext.getResources().getString(R.string.loading_key_terms));
        }
        loadTerms(terms, p);
        String notes = mDataStore.fetchTranslationNotes(p.getId(), p.getSelectedSourceLanguage().getId(), p.getSelectedSourceLanguage().getSelectedResource().getId(), false);
        if(!displayNotice) {
            mProgress += PERCENT_PROJECT_SOURCE/3;
            mCallback.onProgress(mProgress, mContext.getResources().getString(R.string.loading_translation_notes));
        }
        loadNotes(notes, p);
        if(displayNotice) {
            mContext.closeProgressDialog();
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
            return false;
        }
    }

    /**
     * Adds a meta project to the manager
     * @param p
     * @return
     */
    private boolean addMetaProject(SudoProject p) {
        if(!mMetaProjectMap.containsKey(p.getId())) {
            mMetaProjectMap.put(p.getId(), p);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Adds a project to the list of projects visible in the projects list.
     * @param p
     */
    public void addListableProject(Project p) {
        if(!mListableProjectMap.containsKey(p.getId())) {
            mListableProjectMap.put(p.getId(), p);
            mListableProjects.add(p);
        }
    }

    /**
     * Adds a meta project to the list of projects visible in the projects list.
     * When clicked on users will navigate through the meta projects until they
     * select a real project at which point normal application flow will continue.
     * @param p
     */
    public void addListableProject(SudoProject p) {
        if(!mListableProjectMap.containsKey("m-"+p.getId())) {
            mListableProjectMap.put("m-"+p.getId(), p);
            mListableProjects.add(p);
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
    public SudoProject getMetaProject(String id) {
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
    private Language getLanguageByName(String name) {
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
     * Imports a 1.x translation (Doku Wiki) into a project.
     * TODO: this needs to support 2.x translations as well! 2.x archives chapter files. 1.x combines them into one file. This will work for 2.x, but you'd have to do one file at a time.
     * @param file the doku wiki file
     * @return
     */
    public boolean importTranslation(File file) {
        if(file.exists() && file.isFile()) {
            StringBuilder frameBuffer = new StringBuilder();
            String line, chapterId = "", frameId = "", chapterTitle = "";
            Pattern pattern = Pattern.compile("-(\\d\\d)-(\\d\\d)\\.jpg");
            Language targetLanguage = null;
            Project project = null;

            try {
                BufferedReader br = new BufferedReader(new FileReader(file));

                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if(line.length() > 4 && line.substring(0, 2).equals("//")) {
                        line = line.substring(2, line.length() - 2).trim();
                        if(targetLanguage == null) {
                            // retrieve the translation language
                            targetLanguage = getLanguageByName(line);
                            if(targetLanguage == null) return false;
                        } else if(project == null) {
                            // retrieve project
                            project = getProject(line);
                            if(project == null) return false;
                            // place this translation into the correct language
                            project.setSelectedTargetLanguage(targetLanguage.getId());
                        } else if (!chapterId.isEmpty() && !frameId.isEmpty()) {
                            // retrieve chapter reference (end of chapter)
                            Chapter c =  project.getChapter(chapterId);
                            c.setReferenceTranslation(line);
                            if(!chapterTitle.isEmpty()) {
                                c.setTitleTranslation(chapterTitle);
                            }
                            c.save();

                            // save the last frame of the chapter
                            if(frameBuffer.length() > 0) {
                                Frame f = c.getFrame(frameId);
                                f.setTranslation(frameBuffer.toString().trim());
                                f.save();
                            }
                            chapterId = "";
                            frameId = "";
                            frameBuffer.setLength(0);
                        } else {
                            // unexpected input
                            return false;
                        }
                    } else if(line.length() > 12 && line.substring(0, 6).equals("======")) {
                        // start of a new chapter
                        chapterTitle = line.substring(6, line.length() - 6).trim(); // this is saved at the end of the chapter
                    } else if(line.length() > 4 && line.substring(0, 2).equals("{{")) {
                        // save the previous frame
                        if(project != null && !chapterId.isEmpty() && !frameId.isEmpty() && frameBuffer.length() > 0) {
                            Frame f = project.getChapter(chapterId).getFrame(frameId);
                            f.setTranslation(frameBuffer.toString().trim());
                            f.save();
                        }

                        // image tag. We use this to get the frame number for the following text.
                        Matcher matcher = pattern.matcher(line);
                        while(matcher.find()) {
                            chapterId = matcher.group(1);
                            frameId = matcher.group(2);
                        }
                        // clear the frame buffer
                        frameBuffer.setLength(0);
                    } else {
                        // frame translation
                        frameBuffer.append(line);
                        frameBuffer.append('\n');
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        } else {
            return false;
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
                    // publish updates every 100 languages to ease up on the ui
                    if(i % 100 == 0) {
                        mCallback.onProgress(mProgress, String.format(mContext.getResources().getString(R.string.loading_target_language), jsonLanguage.get("lc").toString()));
                    }
                    // TODO: it would be best to include the language direction in the target language list
                    Language l = new Language(jsonLanguage.get("lc").toString(), jsonLanguage.get("ln").toString(), Language.Direction.RightToLeft);
                    addLanguage(l);
                } else {
//                    Log.w(TAG, "missing required parameters in the target language catalog");
                }
            } catch (JSONException e) {
                Log.w(TAG, e.getMessage());
                continue;
            }
        }
        // begin loading projects
        String projectsCatalog = mDataStore.fetchProjectCatalog(false);
        loadProjectsCatalog(projectsCatalog);
    }

    /**
     * Loads the projects catalog
     * @param projectsCatalog
     */
    private List<Project> loadProjectsCatalog(String projectsCatalog) {
        List<Project> importedProjects = new ArrayList<Project>();
        // load projects
        JSONArray json;
        try {
            json = new JSONArray(projectsCatalog);
        } catch (JSONException e) {
            Log.w(TAG, e.getMessage());
            return new ArrayList<Project>();
        }

        // load the data
        int numProjects = json.length();
        for(int i=0; i<numProjects; i++) {
            try {
                JSONObject jsonProject = json.getJSONObject(i);
                if(jsonProject.has("slug") && jsonProject.has("date_modified")) {
                    mProgress += PERCENT_PROJECTS / numProjects;
                    mCallback.onProgress(mProgress, String.format(mContext.getResources().getString(R.string.loading_project), jsonProject.get("slug").toString()));
                    Project p = new Project(jsonProject.get("slug").toString(), Integer.parseInt(jsonProject.get("date_modified").toString()));

                    // load meta
                    SudoProject rootMeta = null;
                    if(jsonProject.has("meta")) {
                        JSONArray jsonMeta = jsonProject.getJSONArray("meta");
                        if(jsonMeta.length() > 0) {
                            // get the root meta
                            String metaSlug = jsonMeta.get(0).toString();
                            rootMeta = getMetaProject(metaSlug);
                            if(rootMeta == null) {
                                rootMeta = new SudoProject(metaSlug);
                                addMetaProject(rootMeta);
                            }
                            p.addMetaCategory(rootMeta.getId());
                            // load children meta
                            SudoProject currentMeta = rootMeta;
                            for (int j = 1; j < jsonMeta.length(); j++) {
                                SudoProject meta = new SudoProject(jsonMeta.get(j).toString());
                                if(currentMeta.getMetaChild(meta.getId()) != null) {
                                    // load already created meta
                                    currentMeta = currentMeta.getMetaChild(meta.getId());
                                } else {
                                    // create new meta
                                    currentMeta.addChild(meta);
                                    currentMeta = meta;
                                }
                                p.addMetaCategory(meta.getId());
                            }
                            // close with the project
                            currentMeta.addChild(p);
                        }
                    }

                    // add project or meta to the project list
                    if(rootMeta == null) {
                        addListableProject(p);
                    } else {
                        addListableProject(rootMeta);
                    }

                    // add project to the internal list and continue loading
                    if(addProject(p)) {
                        importedProjects.add(p);
                    }
                    String sourceLanguageCatalog = mDataStore.fetchSourceLanguageCatalog(p.getId(), false);
                    loadSourceLanguageCatalog(p, rootMeta, sourceLanguageCatalog);
                } else {
//                    Log.w(TAG, "missing required parameters in the project catalog");
                }
            } catch (JSONException e) {
                Log.w(TAG, e.getMessage());
                continue;
            }
        }
        return importedProjects;
    }

    /**
     * Loads the source languages into the given project
     * @param p the project into which the source languages will be loaded
     * @param sourceLangaugeCatalog the catalog of source languages
     */
    private List<SourceLanguage> loadSourceLanguageCatalog(Project p, SudoProject rootMeta, String sourceLangaugeCatalog) {
        List<SourceLanguage> importedLanguages = new ArrayList<SourceLanguage>();
        if(sourceLangaugeCatalog == null) {
            return importedLanguages;
        }
        // parse source languages
        JSONArray json;
        try {
            json = new JSONArray(sourceLangaugeCatalog);
        } catch (Exception e) {
            Log.w(TAG, e.getMessage());
            return new ArrayList<SourceLanguage>();
        }

        // load the data
        for(int i=0; i<json.length(); i++) {
            try {
                JSONObject jsonLanguage = json.getJSONObject(i);
                if(jsonLanguage.has("language") && jsonLanguage.has("project")) {
                    JSONObject jsonLangInfo = jsonLanguage.getJSONObject("language");
                    JSONObject jsonProjInfo = jsonLanguage.getJSONObject("project");

                    // load the rest of the project info
                    // TRICKY: we need to load at least one version of the project title to begin with.
                    // Then whenever the selected language changes the appropriate title and description will be loaded
                    if(p.getTitle() == null) {
                        p.setTitle(jsonProjInfo.getString("name"));
                        p.setDescription(jsonProjInfo.getString("desc"));
                    }

                    // load language
                    Language.Direction langDir = jsonLangInfo.get("direction").toString().equals("ltr") ? Language.Direction.LeftToRight : Language.Direction.RightToLeft;
                    SourceLanguage l = new SourceLanguage(jsonLangInfo.get("slug").toString(), jsonLangInfo.get("name").toString(), langDir, Integer.parseInt(jsonLangInfo.get("date_modified").toString()));

                    // load sudo project names
                    if(jsonProjInfo.has("meta") && rootMeta != null) {
                        JSONArray jsonMeta = jsonProjInfo.getJSONArray("meta");
                        if(jsonMeta.length() > 0) {
                            SudoProject currentMeta = rootMeta;
                            for (int j = 0; j < jsonMeta.length(); j++) {
                                currentMeta.addTranslation(new Translation(l, jsonMeta.get(j).toString()));
                                if(p.getMeta(j) != null) {
                                    currentMeta = currentMeta.getMetaChild(p.getMeta(j+1));
                                } else {
                                    Log.d(TAG, "missing meta category in project");
                                    break;
                                }
                            }
                        } else {
                            Log.d(TAG, "missing project meta translations");
                        }
                    } else if(rootMeta != null) {
                        Log.d(TAG, "missing project meta translations");
                    }

                    // load translation versions
                    String resourcesCatalog = mDataStore.fetchResourceCatalog(p.getId(), l.getId(), false);
                    List<Resource> importedResources = loadResourcesCatalog(l, resourcesCatalog);

                    // only resources with the minium checking level will get imported, so it's possible we'll need to skip a language
                    if(importedResources.size() > 0) {
                        // For the most part source and target languages can be used interchangably, however there are some cases were we need some extra information in source languages.
                        addSourceLanguage(l);
                        importedLanguages.add(l);

                        if (p != null) {
                            p.addSourceLanguage(l);
                        } else {
//                          Log.w(TAG, "project not found");
                        }
                    }
                } else {
//                    Log.w(TAG, "missing required parameters in the source language catalog");
                }
            } catch (JSONException e) {
//                Log.w(TAG, e.getMessage());
                continue;
            }
        }
        return importedLanguages;
    }

    /**
     * Loads the resources into the given source language
     * @param l the source language
     * @param resourcesCatalog the json resources
     */
    private List<Resource> loadResourcesCatalog(SourceLanguage l, String resourcesCatalog) {
        List<Resource> importedResources = new ArrayList<Resource>();
        if(resourcesCatalog == null) {
            return importedResources;
        }
        // parse resources
        JSONArray json;
        try {
            json = new JSONArray(resourcesCatalog);
        } catch (Exception e) {
            Log.w(TAG, e.getMessage());
            return new ArrayList<Resource>();
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
                            l.addResource(r);
                            importedResources.add(r);
                        }
                    }
                } else {
                    // missing required parameters
                }
            } catch (Exception e) {
                e.printStackTrace();
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

        // load source
        JSONArray jsonNotes;
        if(jsonString == null) {
//            Log.w(TAG, "The source was not found");
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
                    } else {
                        // no chapter or frame exists for that note
                    }
                } else {
//                    Log.w(TAG, "missing required parameters in the source notes");
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
        if(p == null) return;

        // load source
        JSONArray jsonTerms;
        if(jsonString == null) {
//            Log.w(TAG, "The source was not found");
            return;
        }
        try {
            jsonTerms = new JSONArray(jsonString);
        } catch (JSONException e) {
//            Log.w(TAG, e.getMessage());
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
//                                Log.w(TAG, "invalid term example reference");
                            }
                        }
                    }

                    // load term
                    Term t = new Term(jsonTerm.get("term").toString(), jsonTerm.get("sub").toString(), jsonTerm.get("def").toString(), jsonTerm.get("def_title").toString(), relatedTerms, examples);

                    // add term to the project
                    p.addTerm(t);
                } else {
//                    Log.w(TAG, "missing required parameters in the source terms");
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
//            Log.w(TAG, "The source was not found");
            return;
        }
        try {
            JSONObject json = new JSONObject(jsonString);
            jsonChapters = json.getJSONArray("chapters");
        } catch (JSONException e) {
            Log.w(TAG, e.getMessage());
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
//                            Log.w(TAG, "missing required parameters in the source frames");
                        }
                    }
                } else {
//                    Log.w(TAG, "missing required parameters in the source chapters");
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

    /**
     * Returns a list of languages
     * @return
     */
    public List<SourceLanguage> getSourceLanguages() {
        return mSourceLanguages;
    }

    public interface OnProgressCallback {
        void onProgress(double progress, String message);
        void onSuccess();
    }
}
