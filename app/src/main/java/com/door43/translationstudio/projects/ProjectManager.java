package com.door43.translationstudio.projects;

import android.util.Log;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.data.DataStore;

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
 * TODO: need to provide progress information so we can display appropriate information while the user is waiting for the app to load. e.g. a loading screen while projects are parsed.
 * TODO: parsing tasks need to be ran asyncronously
 * Created by joel on 8/29/2014.
 */
public class ProjectManager {
    private static DataStore mDataStore;

    // so we can look up by index
    private static List<Project> mProjects = new ArrayList<Project>();
    // so we can look up by id
    private static Map<String, Project> mProjectMap = new HashMap<String, Project>();

    // so we can look up by index
    private static List<Language> mLanguages = new ArrayList<Language>();
    // so we can look up by id
    private static Map<String, Language> mLanguagesMap = new HashMap<String, Language>();
    // so we can look up by name
    private static Map<String, Language> mLanguagesNameMap = new HashMap<String, Language>();

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
    private OnProgressCallback mCallback;

    public ProjectManager(MainApplication context) {
        mContext = context;
    }

    /**
     * loads the source projects
     */
    public void init(OnProgressCallback callback) {
        // make sure we only call this once.
        if(mDataStore == null) {
            mCallback = callback;
            mDataStore = new DataStore(mContext);
            // begin loading target languages
            String targetLanguageCatalog = mDataStore.fetchTargetLanguageCatalog();
            loadTargetLanguagesCatalog(targetLanguageCatalog);
        }
        mCallback.onSuccess();
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

        String source = mDataStore.fetchSourceText(p.getId(), p.getSelectedSourceLanguage().getId());
        p.flush();
        if(!displayNotice) {
            mProgress += PERCENT_PROJECT_SOURCE/3;
            mCallback.onProgress(mProgress, mContext.getResources().getString(R.string.opening_project));
        }
        loadProject(source, p);
        String terms = mDataStore.fetchTermsText(p.getId(), p.getSelectedSourceLanguage().getId());
        if(!displayNotice) {
            mProgress += PERCENT_PROJECT_SOURCE/3;
            mCallback.onProgress(mProgress, mContext.getResources().getString(R.string.loading_key_terms));
        }
        loadTerms(terms, p);
        String notes = mDataStore.fetchTranslationNotes(p.getId(), p.getSelectedSourceLanguage().getId());
        if(!displayNotice) {
            mProgress += PERCENT_PROJECT_SOURCE/3;
            mCallback.onProgress(mProgress, mContext.getResources().getString(R.string.loading_translation_notes));
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
            mLanguagesNameMap.put(l.getName(), l);
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
                    mCallback.onProgress(mProgress, String.format(mContext.getResources().getString(R.string.loading_target_language), jsonLanguage.get("lc").toString()));
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
                    mCallback.onProgress(mProgress, String.format(mContext.getResources().getString(R.string.loading_project), jsonProject.get("slug").toString()));
                    Project p = new Project(jsonProject.get("title").toString(), jsonProject.get("slug").toString(), jsonProject.get("desc").toString());
                    addProject(p);
                    String sourceLanguageCatalog = mDataStore.fetchSourceLanguageCatalog(p.getId());
                    loadSourceLanguageCatalog(p, sourceLanguageCatalog);
                } else {
//                    Log.w(TAG, "missing required parameters in the project catalog");
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
                            } else {
//                                Log.w(TAG, "project not found");
                            }
                        }
                    } else {
//                        Log.w(TAG, "missing required parameters in the source language catalog");
                    }
                } else {
//                    Log.w(TAG, "missing required parameters in the source language catalog");
                }
            } catch (JSONException e) {
//                Log.w(TAG, e.getMessage());
                continue;
            }
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
                    p.getChapter(chapterId).getFrame(frameId).setTranslationNotes(new TranslationNote(notes));
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

    public interface OnProgressCallback {
        void onProgress(double progress, String message);
        void onSuccess();
    }
}
