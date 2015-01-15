package com.door43.translationstudio.projects;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.git.Repo;
import com.door43.translationstudio.git.tasks.StopTaskException;
import com.door43.translationstudio.git.tasks.repo.AddTask;
import com.door43.translationstudio.spannables.NoteSpan;
import com.door43.translationstudio.util.FileUtilities;
import com.door43.translationstudio.util.ListMap;
import com.door43.translationstudio.util.Logger;
import com.door43.translationstudio.util.MainContext;
import com.door43.translationstudio.util.Security;
import com.door43.translationstudio.util.Zip;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Projects encapsulate the source text for a specific translation effort regardless of language.
 * This source text is subdivided into Chapters and Frames.
 */
public class Project implements Model {
    // so we can look up by index
    private List<Chapter> mChapters = new ArrayList<Chapter>();
    // so we can look up by id
    private Map<String,Chapter> mChapterMap = new HashMap<String, Chapter>();
    private List<SourceLanguage> mSourceLanguages = new ArrayList<SourceLanguage>();
    private Map<String,SourceLanguage> mSourceLanguageMap = new HashMap<String, SourceLanguage>();
    private List<Language> mTargetLanguages = new ArrayList<Language>();
    private Map<String,Language> mTargetLanguageMap = new HashMap<String, Language>();
    private List<Term> mTerms = new ArrayList<Term>();
    private Map<String, Term> mTermMap = new HashMap<String, Term>();
    private List<PseudoProject> mPseudoProjects = new ArrayList<PseudoProject>();
    private Map<String, PseudoProject> mSudoProjectMap = new HashMap<String, PseudoProject>();
//    private Map<String, Translation> mTitleTranslationMap = new HashMap<String, Translation>();
//    private List<Translation> mTitleTranslations = new ArrayList<Translation>();
    private ListMap<Translation> mTitleTranslations = new ListMap<Translation>();
    private ListMap<Translation> mDescriptionTranslations = new ListMap<Translation>();

    // TODO: the extension should be placed in the app settings
    public static final String PROJECT_EXTENSION = "tstudio";

    private String mDefaultTitle;
    private final String mSlug;
    private int mDateModified;
    private String mDefaultDescription;
    private String mSelectedChapterId = null;
    private String mSelectedSourceLanguageId;
    private String mSelectedTargetLanguageId;
    public static final String GLOBAL_PROJECT_SLUG = "uw";
    private static final String TAG = "project";
    public static final String PREFERENCES_TAG = "com.door43.translationstudio.projects";
    private static final String TRANSLATION_READY_TAG = "READY";

    /**
     * Creates a new project
     * @param slug the project slug
     * @param dateModified the date the project was last modified (usualy when pulled from the server)
     */
    public Project(String slug, int dateModified) {
        mSlug = slug;
        mDateModified = dateModified;
        init();
    }

    /**
     * Creates a new project
     * @param slug the project slug
     */
    public Project(String slug) {
        mSlug = slug;
        init();
    }

    /**
     * Initializes default settings in the project
     */
    private void init() {
        // load the selected language
        SharedPreferences settings = MainContext.getContext().getSharedPreferences(PREFERENCES_TAG, MainContext.getContext().MODE_PRIVATE);
        mSelectedSourceLanguageId = settings.getString("selected_source_language_"+mSlug, null);
        mSelectedTargetLanguageId = settings.getString("selected_target_language_"+mSlug, null);
    }

    /**
     * Create a new project
     * @deprecated
     * @param title The human readable title of the project.
     * @param slug The machine readable slug identifying the project.
     * @param description A short description of the project.
     */
    public Project(String title, String slug, String description) {
        mDefaultTitle = title;
        mSlug = slug;
        mDefaultDescription = description;
        // load the selected language
        SharedPreferences settings = MainContext.getContext().getSharedPreferences(PREFERENCES_TAG, MainContext.getContext().MODE_PRIVATE);
        mSelectedSourceLanguageId = settings.getString("selected_source_language_"+mSlug, null);
        mSelectedTargetLanguageId = settings.getString("selected_target_language_"+mSlug, null);
    }

    /**
     * Sets the title of the project.
     * @param title
     */
    public void setDefaultTitle(String title) {
        mDefaultTitle = title;
    }

    /**
     * Sets the description of the project. This can only be set once
     * @param description
     */
    public void setDefaultDescription(String description) {
        mDefaultDescription = description;
    }

    /**
     * Adds a translation of the project title
     * @param title the title of the project
     * @param l the language the title is in
     */
    public void setTitle(String title, SourceLanguage l) {
        mTitleTranslations.replace(l.getId(), new Translation(l, title));
    }

    /**
     * Adds a translation of the project description
     * @param description the description of the project
     * @param l the language the description is in
     */
    public void setDescription(String description, SourceLanguage l) {
        mDescriptionTranslations.replace(l.getId(), new Translation(l, description));
    }

    /**
     * Sets whether the translation in the currently selected target langauge is ready for submission
     * @param isReady
     */
    public void setTranslationIsReady(boolean isReady){
        File file = new File(getRepositoryPath(), TRANSLATION_READY_TAG);
        if(isReady == true) {
            // place a file in the repo so the server knows it is ready
            try {
                file.createNewFile();
            } catch (IOException e) {
                return;
            }
        } else {
            // remove the ready tag
            file.delete();
        }
    }

    /**
     * Checks if the translation in the currently selected target is ready for submission
     * @return
     */
    public boolean getTranslationIsReady() {
        File file = new File(getRepositoryPath(), TRANSLATION_READY_TAG);
        return file.exists();
    }

    /**
     * Dumps all the frames and chapters from the project
     */
    public void flush() {
        mChapters = new ArrayList<Chapter>();
        mChapterMap = new HashMap<String, Chapter>();
        mSelectedChapterId = null;
    }

    /**
     * Returns the project id a.k.a the project slug.
     * @return
     */
    @Override
    public String getId() {
        return mSlug;
    }

    /**
     * Returns the title of the project for the specified language
     * @param l
     * @return
     */
    public String getTitle(SourceLanguage l) {
        Translation t = mTitleTranslations.get(l.getId());
        if(t != null) {
            return t.getText();
        } else {
            return mDefaultTitle;
        }
    }

    /**
     * Returns the project title
     * @return
     */
    public String getTitle() {
        Translation t = mTitleTranslations.get(mSelectedSourceLanguageId);
        if(t != null) {
            return t.getText();
        } else {
            return mDefaultTitle;
        }
    }

    /**
     * Returns the description of the project for the specified language
     * @param l
     * @return
     */
    public String getDescription(SourceLanguage l) {
        Translation t = mDescriptionTranslations.get(l.getId());
        if(t != null) {
            return t.getText();
        } else {
            return mDefaultDescription;
        }
    }

    /**
     * Returns a description of the project
     * @return
     */
    public String getDescription() {
        Translation t = mDescriptionTranslations.get(mSelectedSourceLanguageId);
        if(t != null) {
            return t.getText();
        } else {
            return mDefaultDescription;
        }
    }

    /**
     * Returns the number of chapters in this project
     * @return
     */
    public int numChapters() {
        return mChapterMap.size();
    }

    /**
     * Returns the number of languages in this project
     * @return
     */
    public int numSourceLanguages() {
        return mSourceLanguageMap.size();
    }

    /**
     * Returns a chapter by id
     * @param id the chapter id
     * @return null if the chapter does not exist
     */
    public Chapter getChapter(String id) {
        if(mChapterMap.containsKey(id)) {
            return mChapterMap.get(id);
        } else {
            return null;
        }
    }

    /**
     * Returns a chapter by index
     * @param index the chapter index
     * @return null if the chapter does not exist
     */
    public Chapter getChapter(int index){
        if(index < mChapters.size() && index >= 0) {
            return mChapters.get(index);
        } else {
            return null;
        }
    }

    /**
     * Sets the currently selected chapter in the project by id
     * @param id the chapter id
     * @return true if the chapter exists
     */
    public boolean setSelectedChapter(String id) {
        Chapter c = getChapter(id);
        if(c != null) {
            mSelectedChapterId = c.getId();
            storeSelectedChapter(c.getId());
        }
        return c != null;
    }

    /**
     * Sets the currently selected chapter in the project by index
     * @param index the chapter index
     * @return true if the chapter exists
     */
    public boolean setSelectedChapter(int index) {
        Chapter c = getChapter(index);
        if(c != null) {
            mSelectedChapterId = c.getId();
            storeSelectedChapter(c.getId());
        }
        return c != null;
    }

    /**
     * stores the selected chapter in the preferences so we can load it the next time the app starts
     * @param id
     */
    private void storeSelectedChapter(String id) {
        SharedPreferences settings = MainContext.getContext().getSharedPreferences(PREFERENCES_TAG, MainContext.getContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("selected_chapter_"+mSlug, id);
        editor.remove("selected_frame_"+mSlug); // the frame needs to reset each time so it doesn't propogate between chapters
        editor.apply();
    }

    /**
     * Returns the currently selected chapter in the project
     * @return
     */
    public Chapter getSelectedChapter() {
        if(MainContext.getContext().rememberLastPosition()) {
            SharedPreferences settings = MainContext.getContext().getSharedPreferences(PREFERENCES_TAG, MainContext.getContext().MODE_PRIVATE);
            mSelectedChapterId = settings.getString("selected_chapter_" + mSlug, null);
        }

        Chapter selectedChapter = getChapter(mSelectedChapterId);
        if(selectedChapter == null) {
            // auto select the first chapter if no other chapter has been selected
            int defaultChapterIndex = 0;
            setSelectedChapter(defaultChapterIndex);
            return getChapter(defaultChapterIndex);
        } else {
            return selectedChapter;
        }
    }

    /**
     * Adds a sudo project. These are used to help categorize projects
     * @param p
     */
    public void addSudoProject(PseudoProject p) {
        if(!mSudoProjectMap.containsKey(p.getId())) {
            mSudoProjectMap.put(p.getId(), p);
            mPseudoProjects.add(p);
        }
    }

    /**
     * Returns the number of sudo projects in this project
     * @return
     */
    public int numSudoProjects() {
        return mPseudoProjects.size();
    }

    /**
     * Returns an array of meta categories for this project
     * @return
     */
    public PseudoProject[] getSudoProjects() {
        return mPseudoProjects.toArray(new PseudoProject[mPseudoProjects.size()]);
    }


    /**
    * Returns the id of a meta category b
    * @param index
    * @return
    */
    public PseudoProject getSudoProject(int index) {
        PseudoProject[] pseudoProjects = getSudoProjects();
        if(index < pseudoProjects.length && index >= 0) {
            return pseudoProjects[index];
        } else {
            return null;
        }
    }

    /**
     * Adds a chapter to the project
     * @param c the chapter to add
     */
    public void addChapter(Chapter c) {
        if(!mChapterMap.containsKey(c.getId())) {
            c.setProject(this);
            mChapterMap.put(c.getId(), c);
            mChapters.add(c);
        }
    }

    /**
     * Adds a source language to the project
     * @param l the source language to add
     */
    public boolean addSourceLanguage(SourceLanguage l) {
        if(!mSourceLanguageMap.containsKey(l.getId())) {
            l.setProject(this);
            mSourceLanguageMap.put(l.getId(), l);
            mSourceLanguages.add(l);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Adds a target language to the project.
     * This is just used when importing translations from device to device.
     * @param l the target language to add
     * @return
     */
    public boolean addTargetLanguage(Language l) {
        if(!mTargetLanguageMap.containsKey(l.getId())) {
            mTargetLanguageMap.put(l.getId(), l);
            mTargetLanguages.add(l);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns an array of target languages.
     * This is just used when importing translations from device to device.
     * @return
     */
    public Language[] getTargetLanguages() {
        return mTargetLanguages.toArray(new Language[mTargetLanguages.size()]);
    }

    /**
     * Adds a term to the project
     * @param term
     */
    public void addTerm(Term term) {
        if(!mTermMap.containsKey(term.getName())) {
            mTermMap.put(term.getName(), term);
            mTerms.add(term);
        }
    }

    /**
     * Returns the number of terms there are in the project
     * @return
     */
    public int numTerms() {
        return mTerms.size();
    }

    /**
     * Returns a term by index
     * @param index
     * @return
     */
    public Term getTerm(int index) {
        if(index < mTerms.size() && index >= 0) {
            return mTerms.get(index);
        } else {
            return null;
        }
    }

    /**
     * Looks up a key term by name.
     * @param name the case sensitive name of the term
     * @return
     */
    public Term getTerm(String name) {
        if(mTermMap.containsKey(name)) {
            return mTermMap.get(name);
        } else {
            return null;
        }
    }

    /**
     * Returns a list of terms in the project
     * @return
     */
    public List<Term> getTerms() {
        return mTerms;
    }

    /**
     * Checks to see if this project is currently being translated in the selected target language
     * @return
     */
    public boolean isTranslating() {
        File dir = new File(Project.getRepositoryPath(getId(), getSelectedTargetLanguage().getId()));
        String[] files = dir.list(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return !s.equals(".git");
            }
        });
        return files != null && files.length > 0;
    }

    @Override
    public String getType() {
        return "project";
        }

    /**
     * Checks if this project is the currently selected one
     * @return
     */
    @Override
    public boolean isSelected() {
        Project p = MainContext.getContext().getSharedProjectManager().getSelectedProject();
        if(p == null) return false;
        return p.getId().equals(getId());
    }

    /**
     * Checks to see if this project is currently being translated in any language
     * @return
     */
    public boolean isTranslatingGlobal() {
        File dir = new File(Project.getProjectsPath());
        String[] files = dir.list(new FilenameFilter() {
            @Override
            public boolean accept(File file, String name) {
                String[] pieces = name.split("-");
                if(pieces.length == 3) {
                    // make sure the dir is not empty
                    String[] contents = new File(file, name).list(new FilenameFilter() {
                        @Override
                        public boolean accept(File file, String s) {
                            return !s.equals(".git");
                        }
                    });
                    if(contents != null && contents.length > 0) {
                        return pieces[0].equals(GLOBAL_PROJECT_SLUG) && pieces[1].equals(getId());
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        });
        return files != null && files.length > 0;
    }

    /**
     * Returns the currently selected source language
     * TODO: in some cases it would be nice not to have the language automatically selected. However, this would require some work to migrate all the code to support this.
     */
    public SourceLanguage getSelectedSourceLanguage() {
        SourceLanguage selectedLanguage = getSourceLanguage(mSelectedSourceLanguageId);
        if(selectedLanguage == null) {
            // auto select the first chapter if no other chapter has been selected
            int defaultLanguageIndex = 0;
            setSelectedSourceLanguage(defaultLanguageIndex);
            return getSourceLanguage(defaultLanguageIndex);
        } else {
            return selectedLanguage;
        }
    }

    /**
     * Sets the currently selected target language in the project by id
     * @param id the language id
     * @return
     */
    public boolean setSelectedTargetLanguage(String id) {
        Language l = MainContext.getContext().getSharedProjectManager().getLanguage(id);
        if(l != null) {
            mSelectedTargetLanguageId = l.getId();
            storeSelectedTargetLanguage(mSelectedTargetLanguageId);
        }
        return l != null;
    }

    /**
     * Sets the currently selected target language in the project by index
     * @param index the language index
     * @return true if the language exists
     */
    public boolean setSelectedTargetLanguage(int index) {
        Language l = MainContext.getContext().getSharedProjectManager().getLanguage(index);
        if(l != null) {
            mSelectedTargetLanguageId = l.getId();
            storeSelectedTargetLanguage(mSelectedTargetLanguageId);
        }
        return l != null;
    }

    /**
     * stores the selected target language in the preferences so we can load it the next time the app starts
     * @param slug
     */
    private void storeSelectedTargetLanguage(String slug) {
        SharedPreferences settings = MainContext.getContext().getSharedPreferences(PREFERENCES_TAG, MainContext.getContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("selected_target_language_"+mSlug, slug);
        editor.apply();
    }

    /**
     * Returns the currently selected target language
     * If no target language has been chosen it will return the default language
     * @return
     */
    public Language getSelectedTargetLanguage() {
        Language selectedLanguage = MainContext.getContext().getSharedProjectManager().getLanguage(mSelectedTargetLanguageId);
        if(selectedLanguage == null) {
            // auto select the first language
            int defaultLanguageIndex = 0;
            return MainContext.getContext().getSharedProjectManager().getLanguage(defaultLanguageIndex);
        } else {
            return selectedLanguage;
        }
    }

    /**
     * Checks if the user has chosen a target language for this project yet.
     * @return
     */
    public boolean hasChosenTargetLanguage() {
        return mSelectedTargetLanguageId != null;
    }

    /**
     * Sets the currently selected source language in the project by id
     * @param id the language id
     * @return true if the language exists
     */
    public boolean setSelectedSourceLanguage(String id) {
        Language l = getSourceLanguage(id);
        if(l != null) {
            mSelectedSourceLanguageId = l.getId();
            storeSelectedSourceLanguage(mSelectedSourceLanguageId);
        }
        return l != null;
    }

    /**
     * Sets the currently selected source language in the project by index
     * @param index the language index
     * @return true if the language exists
     */
    public boolean setSelectedSourceLanguage(int index) {
        Language l = getSourceLanguage(index);
        if(l != null) {
            mSelectedSourceLanguageId = l.getId();
            storeSelectedSourceLanguage(mSelectedSourceLanguageId);
        }
        return l != null;
    }

    /**
     * stores the selected target language in the preferences so we can load it the next time the app starts
     * @param slug
     */
    private void storeSelectedSourceLanguage(String slug) {
        SharedPreferences settings = MainContext.getContext().getSharedPreferences(PREFERENCES_TAG, MainContext.getContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("selected_source_language_"+mSlug, slug);
        editor.apply();
    }

    /**
     * Returns the path to the image for this project
     * @return
     */
    public String getImagePath() {
        return "sourceTranslations/" + getId() + "/icon.jpg";
    }

    /**
     * Finds a language by the language code
     * @param id the language code
     * @return null if the language does not exist
     */
    public SourceLanguage getSourceLanguage(String id) {
        if(mSourceLanguageMap.containsKey(id)) {
            return mSourceLanguageMap.get(id);
        } else {
            return null;
        }
    }

    /**
     * Finds a language by index
     * @param index the language index
     * @return null if the language does not exist
     */
    public SourceLanguage getSourceLanguage(int index){
        if(index < mSourceLanguages.size() && index >= 0) {
            return mSourceLanguages.get(index);
        } else {
            return null;
        }
    }

    /**
     * Returns the absolute repository path for the given language
     * @param projectId
     * @param languageId
     * @return
     */
    public static String getRepositoryPath(String projectId, String languageId) {
        return MainContext.getContext().getFilesDir() + "/" + MainContext.getContext().getResources().getString(R.string.git_repository_dir) + "/" + GLOBAL_PROJECT_SLUG + "-" + projectId + "-" + languageId + "/";
    }

    /**
     * Returns the absolute path to the directory of projects. e.g. the git directory.
     * @return
     */
    public static String getProjectsPath() {
        return MainContext.getContext().getFilesDir() + "/" + MainContext.getContext().getResources().getString(R.string.git_repository_dir);
    }

    /**
     * Returns the absoute repository path for the currently selected language
     * @return
     */
    public String getRepositoryPath() {
        return getRepositoryPath(getId(), getSelectedTargetLanguage().getId());
    }


    /**
     * Returns a list of source languages for this project
     * @return
     */
    public List<SourceLanguage> getSourceLanguages() {
        return mSourceLanguages;
    }

    /**
     * Exports the project with the currently selected target language as a translationStudio project
     * This is process heavy and should not be ran on the main thread.
     * @return the path to the export archive
     * @throws IOException
     */
    public String exportProject() throws IOException {
        return exportProject(new Language[]{getSelectedTargetLanguage()});
    }

    /**
     * Exports the project in multiple languages as a translationStudio project.
     * This is process heavy and should not be ran on the main thread.
     * @param languages an array of target languages that will be exported
     * @return the path to the export archive
     */
    public String exportProject(Language[] languages) throws IOException {
        Context context = MainContext.getContext();
        File exportDir = new File(MainContext.getContext().getCacheDir() + "/" + MainContext.getContext().getResources().getString(R.string.exported_projects_dir));
        File stagingDir = new File(exportDir, System.currentTimeMillis() + "");
        ArrayList<File> zipList = new ArrayList<File>();
        File manifestFile = new File(stagingDir, "manifest.json");
        JSONObject manifestJson = new JSONObject();
        JSONArray projectsJson = new JSONArray();
        stagingDir.mkdirs();
        Boolean stagingSucceeded = true;
        String gitCommit = "";
        String archivePath = "";

        // prepare manifest
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            manifestJson.put("generator", "translationStudio");
            manifestJson.put("version", pInfo.versionCode);
            manifestJson.put("timestamp", System.currentTimeMillis());
        } catch (JSONException e) {
            Logger.e(this.getClass().getName(), "failed to add to json object", e);
            return archivePath;
        } catch (PackageManager.NameNotFoundException e) {
            Logger.e(this.getClass().getName(), "failed to get the package name", e);
            return archivePath;
        }

        // stage all the translations
        for(Language l:languages) {
            String projectComplexName = GLOBAL_PROJECT_SLUG + "-" + getId() + "-" + l.getId();
            String repoPath = getRepositoryPath(getId(), l.getId());
            // commit changes to repo
            Repo repo = new Repo(repoPath);
            try {
                // only commit if the repo is dirty
                if(!repo.getGit().status().call().isClean()) {
                    // add
                    AddCommand add = repo.getGit().add();
                    add.addFilepattern(".").call();

                    // commit
                    CommitCommand commit = repo.getGit().commit();
                    commit.setAll(true);
                    commit.setMessage("auto save");
                    commit.call();
                }
            } catch (Exception e) {
                stagingSucceeded = false;
                continue;
            }

            // TRICKY: this has to be read after we commit changes to the repo
            gitCommit += getLocalTranslationVersion(l);

            // update manifest
            JSONObject translationJson = new JSONObject();
            try {
                translationJson.put("global_identifier", GLOBAL_PROJECT_SLUG);
                translationJson.put("project", getId());
                translationJson.put("title", getTitle());
                translationJson.put("target_language", l.getId());
                translationJson.put("source_language", getSelectedSourceLanguage().getId());
                translationJson.put("git_commit", gitCommit);
                translationJson.put("path", projectComplexName);
            } catch (JSONException e) {
                Logger.e(this.getClass().getName(), "failed to add to json object", e);
                return archivePath;
            }
            projectsJson.put(translationJson);

            zipList.add(new File(repoPath));
        }
        String signature = Security.md5(gitCommit);
        String tag = signature.substring(0, 10);

        // close manifest
        try {
            manifestJson.put("projects", projectsJson);
            manifestJson.put("signature", signature);
        } catch (JSONException e) {
            Logger.e(this.getClass().getName(), "failed to add to json object", e);
            return archivePath;
        }
        FileUtils.write(manifestFile, manifestJson.toString());
        zipList.add(manifestFile);

        // zip
        if(stagingSucceeded) {
            File outputZipFile = new File(exportDir, GLOBAL_PROJECT_SLUG + "-" + getId() + "_" + tag + "." + PROJECT_EXTENSION);

            // create the archive if it does not already exist
            if(!outputZipFile.exists()) {
                Zip.zip(zipList.toArray(new File[zipList.size()]), outputZipFile);
            }

            archivePath = outputZipFile.getAbsolutePath();
        }

        // clean up old exports. Android should do this automatically, but we'll make sure
        File[] cachedExports = exportDir.listFiles();
        if(cachedExports != null) {
            for(File f:cachedExports) {
                // trash cached files that are more than 12 hours old.
                if(System.currentTimeMillis() - f.lastModified() > 1000 * 60 * 60 * 12) {
                    if(f.isFile()) {
                        f.delete();
                    } else {
                        FileUtilities.deleteRecursive(f);
                    }
                }
            }
        }

        // clean up staging area
        FileUtilities.deleteRecursive(stagingDir);

        return archivePath;
    }

    /**
     * Exports the project with the currently selected target language in DokuWiki format
     * This is a process heavy method and should not be ran on the main thread
     * TODO: we need to update this so we don't include the root directory. We already support the new method (no root dir) as well as provide legacy suport for importing this format.
     * @return the path to the export archive
     */
    public String exportDW() throws IOException {
        String projectComplexName = GLOBAL_PROJECT_SLUG + "-" + getId() + "-" + getSelectedTargetLanguage().getId();
        File exportDir = new File(MainContext.getContext().getCacheDir() + "/" + MainContext.getContext().getResources().getString(R.string.exported_projects_dir));
        Boolean commitSucceeded = true;

        Pattern pattern = Pattern.compile(NoteSpan.REGEX_OPEN_TAG + "((?!" + NoteSpan.REGEX_CLOSE_TAG + ").)*" + NoteSpan.REGEX_CLOSE_TAG);
        Pattern defPattern = Pattern.compile("def=\"(((?!\").)*)\"");
        exportDir.mkdirs();

        // commit changes to repo
        Repo repo = new Repo(getRepositoryPath());
        try {
            // only commit if the repo is dirty
            if(!repo.getGit().status().call().isClean()) {
                // add
                AddCommand add = repo.getGit().add();
                add.addFilepattern(".").call();

                // commit
                CommitCommand commit = repo.getGit().commit();
                commit.setAll(true);
                commit.setMessage("auto save");
                commit.call();
            }
        } catch (Exception e) {
            commitSucceeded = false;
        }

        // TRICKY: this has to be read after we commit changes to the repo
        String translationVersion = getLocalTranslationVersion();
        File outputZipFile = new File(exportDir, projectComplexName + "_" + translationVersion + ".zip");
        File outputDir = new File(exportDir, projectComplexName + "_" + translationVersion);

        // clean up old exports
        String[] cachedExports = exportDir.list();
        for(int i=0; i < cachedExports.length; i ++) {
            String[] pieces = cachedExports[i].split("_");
            if(pieces[0].equals(projectComplexName) && !pieces[1].equals(translationVersion)) {
                File oldDir = new File(exportDir, cachedExports[i]);
                FileUtilities.deleteRecursive(oldDir);
            }
        }

        // return the already exported project
        // TRICKY: we can only rely on this when all changes are commited to the repo
        if(outputZipFile.isFile() && commitSucceeded) {
            return outputZipFile.getAbsolutePath();
        }

        // export the project
        outputDir.mkdirs();
        for(int i = 0; i < mChapters.size(); i ++) {
            Chapter c = getChapter(i);
            if(c != null) {
                // check if any frames have been translated
                File chapterDir = new File(getRepositoryPath(), c.getId());
                if(!chapterDir.exists()) continue;
                String[] translatedFrames = chapterDir.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File file, String s) {
                        return !s.equals("title") && !s.equals("reference");
                    }
                });
                if(translatedFrames.length == 0 && c.getTitleTranslation().getText().trim().isEmpty() && c.getReferenceTranslation().getText().trim().isEmpty()) continue;

                // compile translation
                File chapterFile = new File(outputDir, c.getId() + ".txt");
                chapterFile.createNewFile();
                PrintStream ps = new PrintStream(chapterFile);

                // language
                ps.print("//");
                ps.print(getSelectedTargetLanguage().getName());
                ps.println("//");
                ps.println();

                // project
                ps.print("//");
                ps.print(getId());
                ps.println("//");
                ps.println();

                // chapter title
//                if(!c.getTitleTranslation().getText().trim().isEmpty()) {
                ps.print("======");
                ps.print(c.getTitleTranslation().getText().trim());
                ps.println("======");
                ps.println();
//                }

                // frames
                for(int j = 0; j < c.numFrames(); j ++) {
                    Frame f = c.getFrame(j);
                    if(f != null && !f.getTranslation().getText().isEmpty()) {
                        // image
                        ps.print("{{");
                        // TODO: the api version and image dimensions should be placed in the user preferences
                        String apiVersion = "1";
                        // TODO: for now all images use the english versions
                        String languageCode = "en"; // eventually we should use: getSelectedTargetLanguage().getId()
                        ps.print(MainContext.getContext().getUserPreferences().getString(SettingsActivity.KEY_PREF_MEDIA_SERVER, MainContext.getContext().getResources().getString(R.string.pref_default_media_server))+"/"+getId()+"/jpg/"+apiVersion+"/"+languageCode+"/360px/"+getId()+"-"+languageCode+"-"+c.getId()+"-"+f.getId()+".jpg");
                        ps.println("}}");
                        ps.println();

                        // convert tags
                        String text = f.getTranslation().getText().trim();
                        Matcher matcher = pattern.matcher(text);
                        String convertedText = "";
                        int lastEnd = 0;
                        while(matcher.find()) {
                            if(matcher.start() > lastEnd) {
                                // add the last piece
                                convertedText += text.substring(lastEnd, matcher.start());
                            }
                            lastEnd = matcher.end();

                            // extract note
                            NoteSpan note = NoteSpan.getInstanceFromXML(matcher.group());
                            if(note.getNoteType() == NoteSpan.NoteType.Footnote) {
                                // iclude footnotes
                                convertedText += note.generateDokuWikiTag();
                            } else if(note.getNoteType() == NoteSpan.NoteType.UserNote) {
                                // skip user notes
                                convertedText += note.getSpanText();
                            }
                        }
                        if(lastEnd < text.length()) {
                            convertedText += text.substring(lastEnd, text.length());
                        }

                        // text
                        ps.println(convertedText);
                        ps.println();
                    }
                }

                // chapter reference
//                if(!c.getReferenceTranslation().getText().trim().isEmpty()) {
                ps.print("//");
                ps.print(c.getReferenceTranslation().getText().trim());
                ps.println("//");
//                }

                ps.close();
            }
        }

        // zip
        MainContext.getContext().zip(outputDir.getAbsolutePath(), outputZipFile.getAbsolutePath());
        // cleanup
        FileUtilities.deleteRecursive(outputDir);
        return outputZipFile.getAbsolutePath();
    }

    /**
     * Returns the latest git commit id for the project repo with the given target language
     * @param l the target language for the repo to check
     * @return
     */
    public String getLocalTranslationVersion(Language l) {
        Repo repo = new Repo(getRepositoryPath(getId(), l.getId()));
        try {
            Iterable<RevCommit> commits = repo.getGit().log().setMaxCount(1).call();
            RevCommit commit = null;
            for(RevCommit c : commits) {
                commit = c;
            }
            if(commit != null) {
                String[] pieces = commit.toString().split(" ");
                return pieces[1];
            } else {
                return null;
            }
        } catch (GitAPIException e) {
            Logger.e(this.getClass().getName(), "failed to fetch the git commit", e);
        } catch (StopTaskException e) {
            Logger.e(this.getClass().getName(), "the task was stopped", e);
        }
        return null;
    }

    /**
     * Returns the latest git commit id for the project repo with the selected target language
     * @return
     */
    public String getLocalTranslationVersion() {
        return getLocalTranslationVersion(getSelectedTargetLanguage());
    }

    /**
     * Adds and commits the changes to the repository
     */
    public void commit(final OnCommitComplete callback) {
        final Repo repo = new Repo(getRepositoryPath());

        AddTask add = new AddTask(repo, ".", new AddTask.OnAddComplete() {
            @Override
            public void success() {
                if(callback != null) {
                    callback.success();
                }
            }

            @Override
            public void error() {
                if(callback != null) {
                    callback.error();
                }
            }
        });
        add.executeTask();
    }

    /**
     * Generates the remote path for a local repo
     * @param lang
     * @return
     */
    public String getRemotePath(Language lang) {
        String server = MainContext.getContext().getUserPreferences().getString(SettingsActivity.KEY_PREF_GIT_SERVER, MainContext.getContext().getResources().getString(R.string.pref_default_git_server));
        return server + ":tS/" + MainContext.getContext().getUDID() + "/" + GLOBAL_PROJECT_SLUG + "-" + getId() + "-" + lang.getId();
    }

    /**
     * Generates the remote path for a local repo from the currently selected target language
     * @return
     */
    public String getRemotePath() {
        return getRemotePath(getSelectedTargetLanguage());
    }

    /**
     * Returns the chapters in this project
     * @return
     */
    public Model[] getChapters() {
        return mChapters.toArray(new Model[mChapters.size()]);
    }

    /**
     * Returns an array of target languages thare are currently being translated
     * @return
     */
    public Language[] getActiveTargetLanguages() {
        File dir = new File(Project.getProjectsPath());
        // find active project dirs
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                String[] pieces = s.split("-");
                if (pieces.length == 3) {
                    // make sure dir is not empty
                    File[] contents = file.listFiles();
                    if (contents != null && contents.length > 0) {
                        // find project dirs
                        return pieces[0].equals(GLOBAL_PROJECT_SLUG) && pieces[1].equals(getId());
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        });
        // collect active languages
        List<Language> languages = new ArrayList<Language>();
        for(File f:files) {
            String[] pieces = f.getName().split("-");
            if(pieces.length == 3) {
                Language l = MainContext.getContext().getSharedProjectManager().getLanguage(pieces[2]);
                if(l != null) {
                    languages.add(l);
                }
            }
        }
        return languages.toArray(new Language[languages.size()]);
    }

    /**
     * Returns the date the project was last modified
     * @return
     */
    public int getDateModified() {
        return mDateModified;
    }

    /**
     * Set the date the project was last modified
     * @param dateModified
     */
    public void setDateModified(int dateModified) {
        mDateModified = dateModified;
    }

    public interface OnCommitComplete {
        public void success();
        public void error();
    }

    /**
     * This performs house cleaning operations after a project has been imported.
     * You should still run this even if you just prepared the import but didn't actually import
     * because some files get extracted durring the process.
     * @param request the import status that will be cleaned
     */
    public static void cleanImport(ImportRequest request) {
        if(request.sourceDir.exists()) {
            FileUtilities.deleteRecursive(request.sourceDir);
        }
    }

    /**
     * Performs the actual import of the project
     * @param request
     * @return
     */
    public static boolean importProject(ImportRequest request) {
        // locate existing project
        Project p = MainContext.getContext().getSharedProjectManager().getProject(request.projectId);
        if(p != null) {
            File repoDir = new File(Project.getRepositoryPath(request.projectId, request.languageId));
            if(repoDir.exists()) {
                // merge into existing project
                File[] files = request.sourceDir.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File file, String s) {
                        return !s.equals(".git");
                    }
                });
                if(files != null) {
                    for (File f : files) {
                        try {
                            File destDir = new File(repoDir, f.getName());
                            if (destDir.exists()) {
                                FileUtilities.deleteRecursive(destDir);
                            }
                            FileUtils.moveDirectory(f, destDir);
                        } catch (IOException e) {
                            Logger.e(Project.class.getName(), "failed to import the chapter directory ", e);
                            return false;
                            // TODO: record list of files that cannot be coppied and display to the user
                        }
                    }
                    Language l = MainContext.getContext().getSharedProjectManager().getLanguage(request.languageId);
                    l.touch();
                    // TODO: perform a git diff to see if there are any changes
                    return true;
                } else {
                    Logger.w(Project.class.getName(), "the project import directory does not exist");
                    return false;
                }
            } else {
                // import as new project
                try {
                    FileUtils.moveDirectory(request.sourceDir, repoDir);
                    return true;
                } catch (IOException e) {
                    Logger.e(Project.class.getName(), "failed to import the project directory", e);
                    return false;
                }
            }
        } else {
            // TODO: create a new project and add it to the project manager. This will require the existance of the project source in the archive.
        }
        return false;
    }

    /**
     * Performs some checks on a project to make sure it can be imported.
     * @param projectDir the project directory that will be imported
     * @return
     */
    private static ImportRequest prepareImport(String projectId, String languageId, File projectDir) {
        ImportRequest request = new ImportRequest(projectId, languageId, projectDir);

        // locate existing project
        Project p = MainContext.getContext().getSharedProjectManager().getProject(projectId);
        if(p != null) {
            File repoDir = new File(Project.getRepositoryPath(projectId, languageId));
            if(repoDir.exists()) {
                // the project already exists
                request.addWarning("Project translation already exists");
                return request;
            } else {
                // new project translation
                return request;
            }
        } else {
            // new project source and translation
            // TODO: eventually we should check if the import includes the source text as well. Then this should just be a warning. Letting the user know that the source will be imported as well.
            request.addError("Missing project source");
        }
        return request;
    }

    /**
     * Prepares a translationStudio project archive for import.
     * This leaves files around so be sure to run the importcleanup when finished.
     * @param archive the archive that will be imported
     * @return true if the import was successful
     */
    public static List<ImportRequest> prepareProjectArchiveImport(File archive) {
        List<ImportRequest> importRequests = new ArrayList<ImportRequest>();

        // validate extension
        String[] name = archive.getName().split("\\.");
        Boolean success = false;
        if(name[name.length - 1].equals(PROJECT_EXTENSION)) {
            long timestamp = System.currentTimeMillis();
            File extractedDir = new File(MainContext.getContext().getCacheDir() + "/" + MainContext.getContext().getResources().getString(R.string.imported_projects_dir) + "/" + timestamp);

            try {
                Zip.unzip(archive, extractedDir);
            } catch (IOException e) {
                FileUtilities.deleteRecursive(extractedDir);
                Logger.e(Project.class.getName(), "failed to extract the project archive", e);
                return importRequests;
            }

            File manifest = new File(extractedDir, "manifest.json");
            if(manifest.exists() && manifest.isFile()) {
                try {
                    JSONObject manifestJson = new JSONObject(FileUtils.readFileToString(manifest));
                    // NOTE: the manifest contains extra information that we are not using right now
                    if(manifestJson.has("projects")) {
                        JSONArray projectsJson = manifestJson.getJSONArray("projects");
                        for(int i=0; i<projectsJson.length(); i++) {
                            JSONObject projJson = projectsJson.getJSONObject(i);
                            if(projJson.has("path") && projJson.has("project") && projJson.has("target_language")) {
                                // import the project
                                File projectDir = new File(extractedDir, projJson.getString("path"));
                                ImportRequest request = prepareImport(projJson.getString("project"), projJson.getString("target_language"), projectDir);
                                importRequests.add(request);
                            }
                        }
                    }
                } catch (JSONException e) {
                    Logger.e(Project.class.getName(), "failed to parse the manifest", e);
                } catch (IOException e) {
                    Logger.e(Project.class.getName(), "failed to read the manifest file", e);
                }
            }
        }
        return importRequests;
    }

    /**
     * Imports a Translation Studio Project from a directory
     * This is a legacy import method for archives exported by 2.0.2 versions of the app.
     * @param archiveFile the directory that will be imported
     * @return
     */
    public static boolean prepareLegacyProjectArchiveImport(File archiveFile) {
        String[] name = archiveFile.getName().split("\\.");
        if(name[name.length - 1].equals("zip")) {
            // extract archive
            long timestamp = System.currentTimeMillis();
            File extractedDirectory = new File(MainContext.getContext().getCacheDir() + "/" + MainContext.getContext().getResources().getString(R.string.imported_projects_dir) + "/" + timestamp);
            File importDirectory;
            Boolean success = false;
            try {
                // extract into a timestamped directory so we don't accidently throw files all over the place
                Zip.unzip(archiveFile, extractedDirectory);
                File[] files = extractedDirectory.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File file, String s) {
                        return Project.validateProjectArchiveName(s);
                    }
                });
                if(files.length == 1) {
                    importDirectory = files[0];
                } else {
                    // malformed archive
                    FileUtilities.deleteRecursive(extractedDirectory);
                    return false;
                }
            } catch (IOException e) {
                FileUtilities.deleteRecursive(extractedDirectory);
                Logger.e(Project.class.getName(), "failed to extract the legacy project archive", e);
                return false;
            }

            // read project info
            TranslationArchiveInfo translationInfo = getTranslationArchiveInfo(importDirectory.getName());
            if(translationInfo != null) {
                ImportRequest request = prepareImport(translationInfo.projectId, translationInfo.languageId, importDirectory);
                // TODO: for now we are just blindly importing legacy projects (dangerous). We'll need to update this method as well as the DokuWiki import method in order to properly handle these legacy projects
                importProject(request);
                cleanImport(request);
            }
            FileUtilities.deleteRecursive(extractedDirectory);
            return success;
        } else {
            return false;
        }
    }

    /**
     * Returns information about the translation archive
     * @param archiveName
     * @return
     */
    public static TranslationArchiveInfo getTranslationArchiveInfo(String archiveName) {
        String[] parts = archiveName.split("_");
        String name = parts[0];
        // TRICKY: older version of the app mistakenly included the leading directory separator
        while(name.startsWith("/")) {
            name = name.substring(name.indexOf("/"));
        }
        if(validateProjectArchiveName(name)) {
            String[] fields = name.toLowerCase().split("-");
            return new TranslationArchiveInfo(fields[0], fields[1], fields[2]);
        }
        return null;
    }

    /**
     * Checks if the project archive is named properly
     * @param name
     * @return
     */
    public static boolean validateProjectArchiveName(String name) {
        String[] fields = name.toLowerCase().split("-");
        return fields.length == 3 && fields[0].equals(GLOBAL_PROJECT_SLUG);
    }

    /**
     * Stores information about a translation archive
     * @deprecated this is legacy code for the old import methods
     */
    public static class TranslationArchiveInfo {
        public final String globalProjectId;
        public final String projectId;
        public final String languageId;

        public TranslationArchiveInfo(String globalProjectId, String projectId, String languageId) {
            this.globalProjectId = globalProjectId;
            this.projectId = projectId;
            this.languageId = languageId;
        }

        public Project getProject() {
            return MainContext.getContext().getSharedProjectManager().getProject(projectId);
        }

        public Language getLanguage() {
            return MainContext.getContext().getSharedProjectManager().getLanguage(languageId);
        }
    }

    /**
     * Represents the status of the project import. This is generated before the import actually begins
     * so that we can get user feedback on warnings before actually performing the import.
     */
    public static class ImportRequest {
        public final File sourceDir;
        public final String projectId;
        public final String languageId;
        private String mError;
        private String mWarning;
        private boolean mApproved = true;

        public ImportRequest(String projectId, String languageId, File sourceDir) {
            this.projectId = projectId;
            this.languageId = languageId;
            this.sourceDir = sourceDir;
        }

        public void addWarning(String s) {
            mWarning = s;
            mApproved = false;
        }

        public void addError(String s) {
            mError = s;
            mApproved = false;
        }

        public String getWarning() {
            return mWarning;
        }

        public String getError() {
            return mError;
        }

        public boolean isApproved() {
            return mApproved;
        }

        public void setIsApproved(boolean approved) {
            mApproved = approved;
        }
    }
}
