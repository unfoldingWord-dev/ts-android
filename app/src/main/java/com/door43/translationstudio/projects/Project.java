package com.door43.translationstudio.projects;

import android.content.SharedPreferences;
import android.util.Log;

import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.git.Repo;
import com.door43.translationstudio.git.tasks.StopTaskException;
import com.door43.translationstudio.git.tasks.repo.AddTask;
import com.door43.translationstudio.util.FileUtilities;
import com.door43.translationstudio.util.MainContext;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Projects encapsulate the source text for a specific translation effort regardless of language.
 * This source text is subdivided into Chapters and Frames.
 */
public class Project {
    // so we can look up by index
    private List<Chapter> mChapters = new ArrayList<Chapter>();
    // so we can look up by id
    private Map<String,Chapter> mChapterMap = new HashMap<String, Chapter>();
    private List<Language> mSourceLanguages = new ArrayList<Language>();
    private Map<String,Language> mSourceLanguageMap = new HashMap<String, Language>();
    private List<Term> mTerms = new ArrayList<Term>();
    private Map<String, Term> mTermMap = new HashMap<String, Term>();

    private final String mTitle;
    private final String mSlug;
    private final String mDescription;
    private String mSelectedChapterId;
    private String mSelectedSourceLanguageId;
    private String mSelectedTargetLanguageId;
    public static final String GLOBAL_PROJECT_SLUG = "uw";
    private static final String TAG = "project";
    private static final String PREFERENCES_TAG = "com.door43.translationstudio.projects";

    /**
     * Create a new project
     * @param title The human readable title of the project.
     * @param slug The machine readable slug identifying the project.
     * @param description A short description of the project.
     */
    public Project(String title, String slug, String description) {
        mTitle = title;
        mSlug = slug;
        mDescription = description;
        // load the selected language
        SharedPreferences settings = MainContext.getContext().getSharedPreferences(PREFERENCES_TAG, MainContext.getContext().MODE_PRIVATE);
        mSelectedSourceLanguageId = settings.getString("selected_source_language_"+mSlug, null);
        mSelectedTargetLanguageId = settings.getString("selected_target_language_"+mSlug, null);
    }

    /**
     * Returns the global project id
     * @return
     */
    public String getGlobalProjectId() {
        return GLOBAL_PROJECT_SLUG;
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
    public String getId() {
        return mSlug;
    }

    /**
     * Returns the project title
     * @return
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Returns a description of the project
     * @return
     */
    public String getDescription() {
        return mDescription;
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
        }
        return c != null;
    }

    /**
     * Returns the currently selected chapter in the project
     * @return
     */
    public Chapter getSelectedChapter() {
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
     * Adds a language to the project
     * @param l the language to add
     */
    public void addSourceLanguage(Language l) {
        if(!mSourceLanguageMap.containsKey(l.getId())) {
            mSourceLanguageMap.put(l.getId(), l);
            mSourceLanguages.add(l);
        }
    }

    /**
     * Adds a term to the project
     * @param term
     */
    public void addTerm(Term term) {
        if(!mTermMap.containsKey(term.getName().toLowerCase())) {
            mTermMap.put(term.getName().toLowerCase(), term);
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

    public Term getTerm(String name) {
        if(mTermMap.containsKey(name.toLowerCase())) {
            return mTermMap.get(name.toLowerCase());
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
     * Returns the currently selected source language
     */
    public Language getSelectedSourceLanguage() {
        Language selectedLanguage = getSourceLanguage(mSelectedSourceLanguageId);
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
        editor.commit();
    }

    /**
     * Returns the currently selected target language
     * @return
     */
    public Language getSelectedTargetLanguage() {
        Language selectedLanguage = MainContext.getContext().getSharedProjectManager().getLanguage(mSelectedTargetLanguageId);
        if(selectedLanguage == null) {
            // auto select the first language
            int defaultLanguageIndex = 0;
            setSelectedTargetLanguage(defaultLanguageIndex);
            return MainContext.getContext().getSharedProjectManager().getLanguage(defaultLanguageIndex);
        } else {
            return selectedLanguage;
        }
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
        editor.commit();
    }

    /**
     * Returns the path to the image for this project
     * @return
     */
    public String getImagePath() {
        return "sourceTranslations/" + getId() + "/icon.jpg";
    }

    /**
     * Returns a language by id
     * @param id the language id
     * @return null if the language does not exist
     */
    public Language getSourceLanguage(String id) {
        if(mSourceLanguageMap.containsKey(id)) {
            return mSourceLanguageMap.get(id);
        } else {
            return null;
        }
    }

    /**
     * Returns a language by index
     * @param index the language index
     * @return null if the language does not exist
     */
    public Language getSourceLanguage(int index){
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
    public List<Language> getSourceLanguages() {
        return mSourceLanguages;
    }

    /**
     * Exports the project with the currently selected target language in Doku Wiki format
     * This is a process heavy method and should not be ran on the main thread
     * @return the path to the export directory
     */
    public String export() throws IOException {
        String translationVersion = getLocalTranslationVersion();
        String projectComplexName = GLOBAL_PROJECT_SLUG + "-" + getId() + "-" + getSelectedTargetLanguage().getId();
        File exportDir = new File(MainContext.getContext().getCacheDir() + "/" + MainContext.getContext().getResources().getString(R.string.dokuwiki_export_dir) + "/");
        File outputDir = new File(exportDir, projectComplexName + "_" + translationVersion + "/");
        Boolean commitSucceeded = true;
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
        if(outputDir.isDirectory() && commitSucceeded) {
            return outputDir.getAbsolutePath();
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



                // chapter title
                if(!c.getTitleTranslation().getText().trim().isEmpty()) {
                    ps.print("======");
                    ps.print(c.getTitleTranslation().getText().trim());
                    ps.println("======");
                    ps.println();
                }

                // frames
                for(int j = 0; j < c.numFrames(); j ++) {
                    Frame f = c.getFrame(j);
                    if(f != null && !f.getTranslation().getText().isEmpty()) {
                        // image
                        ps.print("{{");
                        // TODO: the api version and image dimensions should be placed in the user preferences
                        String apiVersion = "1";
                        ps.print(MainContext.getContext().getUserPreferences().getString(SettingsActivity.KEY_PREF_MEDIA_SERVER, MainContext.getContext().getResources().getString(R.string.pref_default_media_server))+"/"+getId()+"/jpg/"+apiVersion+"/"+getSelectedTargetLanguage().getId()+"/360px/"+getId()+"-"+getSelectedTargetLanguage().getId()+"-"+c.getId()+"-"+f.getId()+".jpg");
                        ps.println("}}");
                        ps.println();

                        // text
                        ps.println(f.getTranslation().getText().trim());
                        ps.println();
                    }
                }

                // chapter reference
                if(!c.getReferenceTranslation().getText().trim().isEmpty()) {
                    ps.print("//");
                    ps.print(c.getReferenceTranslation().getText().trim());
                    ps.println("//");
                }

                ps.close();
            }
        }
        return outputDir.getAbsolutePath();
    }

    /**
     * Returns the latest git commit id for the project repo with the selected target language
     * @return
     */
    public String getLocalTranslationVersion() {
        Repo repo = new Repo(getRepositoryPath());
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
            e.printStackTrace();
        } catch (StopTaskException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Adds and commits the changes to the repository
     */
    public void commit(final OnCommitComplete callback) {
        final Repo repo = new Repo(getRepositoryPath());

        AddTask add = new AddTask(repo, ".", new AddTask.OnAddComplete() {
            @Override
            public void success() {
                callback.success();
            }

            @Override
            public void error() {
                callback.error();
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

    public interface OnCommitComplete {
        public void success();
        public void error();
    }

    /**
     * Imports a project from a file and returns an instance of the project
     * @param archiveDir the directory that will be imported
     * @return
     */
    public static boolean importTranslationFromFile(File archiveDir) {
        TranslationArchiveInfo translationInfo = getTranslationArchiveInfo(archiveDir.getName());
        if(translationInfo != null) {
            // locate existing project
            Project p = MainContext.getContext().getSharedProjectManager().getProject(translationInfo.projectId);
            if(p != null) {
                File repoDir = new File(Project.getRepositoryPath(p.getId(), translationInfo.languageId));
                if(repoDir.exists()) {
                    // merge repos
                    File archiveGitDir = new File(archiveDir, ".git");
                    FileUtilities.deleteRecursive(archiveGitDir);
                    File[] files = archiveDir.listFiles();
                    for(File f:files) {
                        if(!FileUtilities.moveOrCopy(f, new File(repoDir, f.getName()))) {
                            // TODO: record list of files that cannot be coppied and display to the user
                            Log.w(TAG, "failed to import translation file "+f.getName());
                        }
                    }
                    translationInfo.getLanguage().touch();
                    // TODO: perform a git diff to see if there are any changes
                    return true;
                } else {
                    // add as new repo
                    return FileUtilities.moveOrCopy(archiveDir, repoDir);
                }
            } else {
                // TODO: create a new project and add it to the project manager.
            }
        }
        return false;
    }

    /**
     * Returns information about the translation archive
     * @param archiveName
     * @return
     */
    public static TranslationArchiveInfo getTranslationArchiveInfo(String archiveName) {
        if(validateProjectArchiveName(archiveName)) {
            String[] fields = archiveName.toLowerCase().split("-");
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
     * Sets whether or not the project is being translated
     * @param hasTranslation
     */
    public void setIsTranslating(Boolean hasTranslation) {
        SharedPreferences settings = MainContext.getContext().getSharedPreferences(PREFERENCES_TAG, MainContext.getContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("is_translating_"+mSlug, hasTranslation);
        editor.commit();
    }

    /**
     * Checks if the project is being translated
     * @return
     */
    public Boolean getIsTranslating() {
        SharedPreferences settings = MainContext.getContext().getSharedPreferences(PREFERENCES_TAG, MainContext.getContext().MODE_PRIVATE);
        return settings.getBoolean("is_translating_"+mSlug, false);
    }
}
