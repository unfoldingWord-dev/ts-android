package com.door43.translationstudio.projects;

import com.door43.translationstudio.R;
import com.door43.translationstudio.util.MainContext;

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

    private final String mTitle;
    private final String mSlug;
    private final String mDescription;
    private String mSelectedChapterId;
    private String mSelectedSourceLanguageId;
    private String mSelectedTargetLanguageId;
    private static final String GLOBAL_PROJECT_SLUG = "uw";

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
        Language l = MainContext.getContext().getSharedProjectManager().getTargetLanguage(id);
        if(l != null) {
            mSelectedTargetLanguageId = l.getId();
        }
        return l != null;
    }

    /**
     * Sets the currently selected target language in the project by index
     * @param index the language index
     * @return true if the language exists
     */
    public boolean setSelectedTargetLanguage(int index) {
        Language l = MainContext.getContext().getSharedProjectManager().getTargetLanguage(index);
        if(l != null) {
            mSelectedTargetLanguageId = l.getId();
        }
        return l != null;
    }

    /**
     * Returns the currently selected target language
     * @return
     */
    public Language getSelectedTargetLanguage() {
        Language selectedLanguage = MainContext.getContext().getSharedProjectManager().getTargetLanguage(mSelectedTargetLanguageId);
        if(selectedLanguage == null) {
            // auto select the first language
            int defaultLanguageIndex = 0;
            setSelectedTargetLanguage(defaultLanguageIndex);
            return MainContext.getContext().getSharedProjectManager().getTargetLanguage(defaultLanguageIndex);
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
        }
        return l != null;
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
     * @param language
     * @return
     */
    public String getRepositoryPath(Language language) {
        return MainContext.getContext().getFilesDir() + "/" + MainContext.getContext().getResources().getString(R.string.git_repository_dir) + "/" + GLOBAL_PROJECT_SLUG + "-" + getId() + "-" + language.getId() + "/";
    }

    /**
     * Returns the absoute repository path for the currently selected language
     * @return
     */
    public String getRepositoryPath() {
        return getRepositoryPath(getSelectedTargetLanguage());
    }

    /**
     * Returns a list of source languages for this project
     * @return
     */
    public List<Language> getSourceLanguages() {
        return mSourceLanguages;
    }
}
