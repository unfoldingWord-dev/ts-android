package com.door43.translationstudio.projects;

import android.content.SharedPreferences;

import com.door43.translationstudio.R;
import com.door43.translationstudio.events.ChapterTranslationStatusChangedEvent;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.FileUtilities;
import com.door43.util.Logger;
import com.door43.util.ListMap;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

/**
 * Chapters encapsulate a specific set of translation Frames regardless of language. Chapters mostly act to organize the translation effort into sections for better navigation
 */
public class Chapter implements Model {
    private static final String REFERENCE_FILE = "reference.txt";
    private static final String TITLE_FILE = "title.txt";
    private ListMap<Frame> mFrames = new ListMap<>();
    private String mId;
    private String mTitle;
    private String mReference;
    private String mSelectedFrameId = null;
    private Project mProject;
    private Translation mTitleTranslation;
    private Translation mReferenceTranslation;

    /**
     * Create a new chapter
     * @param id the chapter id. This is effectively the chapter number.
     * @param title the human readable title of the chapter
     * @param reference a short description of the chapter
     */
    public Chapter(String id, String title, String reference) {
        mId = id;
        mTitle = title;
        mReference = reference;
    }

    /**
     * Specifies the project this chapter belongs to.
     * This can only be set once.
     * @param project
     */
    public void setProject(Project project){
        if(mProject == null) mProject = project;
    }

    /**
     * Returns the project this chapter belongs to
     * @return
     */
    public Project getProject() {
        return mProject;
    }

    /**
     * Returns the chapter id
     * @return
     */
    public String getId() {
        return mId;
    }

    /**
     * Checks if this chapter has settings (ref and title) that need to be translated as well
     * @return
     */
    public boolean hasChapterSettings() {
        return mTitle != null && !mTitle.isEmpty() && mReference != null && !mReference.isEmpty();
    }

    /**
     * Returns the chapter title
     * @return
     */
    public String getTitle() {
        if(mTitle != null && !mTitle.isEmpty()) {
            return mTitle;
        } else {
            return String.format(AppContext.context().getResources().getString(R.string.label_chapter_title_detailed), Integer.parseInt(getId()));
        }
    }

    /**
     * Returns the chapter reference
     * @return
     */
    public String getReference() {
        if(mReference != null && !mReference.isEmpty()) {
            return mReference;
        } else {
            // TODO: return the range of verses that are included in this chapter
            return "";
        }
    }

    /**
     * Stores the translated chapter title
     * @param translation
     */
    public void setTitleTranslation(String translation) {
        setTitleTranslation(translation, mProject.getSelectedTargetLanguage());
    }

    /**
     * Stores the translated title text.
     * Important! you should almost always use setTitleTranslation(translation) instead.
     * Only use this if you know what you are doing.
     * @param translation the text to store as a translation
     * @param targetLanguage the target language the translation was made in.
     */
    public void setTitleTranslation(String translation, Language targetLanguage) {
        if(mTitleTranslation != null && !mTitleTranslation.isLanguage(targetLanguage) && !mTitleTranslation.isSaved()) {
            // save pending changes first
            save();
        }
        mTitleTranslation = new Translation(targetLanguage, translation);
    }

    /**
     * Returns this chapter's title translation
     * @return
     */
    public Translation getTitleTranslation() {
        if(mTitleTranslation == null || !mTitleTranslation.isLanguage(mProject.getSelectedTargetLanguage())) {
            if(mTitleTranslation != null) {
                save();
            }
            // load translation from disk
            String path = getTitlePath(mProject.getId(), mProject.getSelectedTargetLanguage().getId(), getId());
            try {
                String text = FileUtilities.getStringFromFile(path);
                mTitleTranslation = new Translation(mProject.getSelectedTargetLanguage(), text);
                mTitleTranslation.isSaved(true);
            } catch (Exception e) {
                Logger.e(this.getClass().getName(), "failed to load translation from disk", e);
            }
        }
        return mTitleTranslation;
    }

    /**
     * Stores the translated chapter reference
     * @param translation
     */
    public void setReferenceTranslation(String translation) {
        setReferenceTranslation(translation, mProject.getSelectedTargetLanguage());
    }

    /**
     * Stores the translated chapter reference
     * Important! you should almost always use setReferenceTranslation(translation) instead.
     * Only use this method if you know what you are doing.
     * @param translation the text to store as the translation
     * @param targetLanguage the language the translation was made in.
     */
    public void setReferenceTranslation(String translation, Language targetLanguage) {
        if(mReferenceTranslation != null && !mReferenceTranslation.isLanguage(targetLanguage) && !mReferenceTranslation.isSaved()) {
            // save pending changes first
            save();
        }
        mReferenceTranslation = new Translation(targetLanguage, translation);
    }

    /**
     * Returns this chapter's reference translation
     * @return
     */
    public Translation getReferenceTranslation() {
        if(mReferenceTranslation == null || !mReferenceTranslation.isLanguage(mProject.getSelectedTargetLanguage())) {
            if(mReferenceTranslation != null) {
                save();
            }
            // load translation from disk
            String path = getReferencePath(mProject.getId(), mProject.getSelectedTargetLanguage().getId(), getId());
            try {
                String text = FileUtilities.getStringFromFile(path);
                mReferenceTranslation = new Translation(mProject.getSelectedTargetLanguage(), text);
                mReferenceTranslation.isSaved(true);
            } catch (Exception e) {
                Logger.e(this.getClass().getName(), "failed to load the translation from disk", e);
            }
        }
        return mReferenceTranslation;
    }

    /**
     * Returns a description of the chapter
     * @return
     */
    public String getDescription() {
        return mReference;
    }

    @Override
    public SourceLanguage getSelectedSourceLanguage() {
        return mProject.getSelectedSourceLanguage();
    }

    @Override
    public String getSortKey() {
        return null;
    }

    /**
     * Returns the number of frames in this chapter
     * @return
     */
    public int numFrames() {
        return mFrames.size();
    }

    /**
     * Returns a frame by id
     * @param id the frame id
     * @return null if the frame does not exist
     */
    public Frame getFrame(String id) {
        return mFrames.get(id);
    }

    /**
     * Returns a frame by index
     * @param index the frame index
     * @return null if the frame does not exist
     */
    public Frame getFrame(int index) {
        return mFrames.get(index);
    }

    /**
     * Returns the index of the given frame
     * @param f
     * @return
     */
    public int getFrameIndex(Frame f) {
        return mFrames.indexOf(f);
    }

    /**
     * Sets the currently selected frame in the chapter by id
     * @param id the frame id
     * @return true if the frame exists
     */
    public boolean setSelectedFrame(String id) {
        Frame f = getFrame(id);
        if(f != null) {
            mSelectedFrameId = f.getId();
            storeSelectedFrame(f.getId());
        }
        return f != null;
    }

    /**
     * Sets the currently selected frame in the chapter by index
     * @param index the frame index
     * @return true if the frame exists
     */
    public boolean setSelectedFrame(int index) {
        Frame f = getFrame(index);
        if(f != null) {
            mSelectedFrameId = f.getId();
            storeSelectedFrame(f.getId());
        }
        return f != null;
    }

    /**
     * stores the selected frame in the preferences so we can load it the next time the app starts
     * @param id
     */
    private void storeSelectedFrame(String id) {
        SharedPreferences settings = AppContext.context().getSharedPreferences(Project.PREFERENCES_TAG, AppContext.context().MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("selected_frame_"+getProject().getId(), id);
        editor.apply();
    }

    /**
     * Returns the currently selected frame in the chapter
     * @return
     */
    public Frame getSelectedFrame() {
        if(mSelectedFrameId == null && AppContext.context().rememberLastPosition()) {
            SharedPreferences settings = AppContext.context().getSharedPreferences(Project.PREFERENCES_TAG, AppContext.context().MODE_PRIVATE);
            mSelectedFrameId = settings.getString("selected_frame_" + getProject().getId(), null);
        }

        Frame selectedFrame = getFrame(mSelectedFrameId);
        return selectedFrame;
    }

    /**
     * Add a frame to the chapter
     * @param f the frame to add
     */
    public void addFrame(Frame f) {
        if(mFrames.get(f.getId()) == null) {
            f.setChapter(this);
            mFrames.add(f.getId(), f);
        }
    }

    /**
     * Returns the path to the image for the chapter
     * Uses the image from the first frame if available
     * @return
     */
    public String getImagePath() {
        return "sourceTranslations/"+getProject().getId()+"/"+getProject().getSelectedSourceLanguage().getId()+"/"+getProject().getSelectedSourceLanguage().getSelectedResource().getId()+"/images/"+getId()+"-00.jpg";
    }

    public String getDefaultImagePath() {
        return "sourceTranslations/"+getProject().getId()+"/en/"+getProject().getSelectedSourceLanguage().getSelectedResource().getId()+"/images/"+getId()+"-00.jpg";
    }

    /**
     * Returns the next ordered frame
     * @return
     */
    public Frame getNextFrame() {
        int index = mFrames.indexOf(getSelectedFrame()) + 1;
        return getFrame(index);
    }

    public Frame getPreviousFrame() {
        int index = mFrames.indexOf(getSelectedFrame()) - 1;
        return getFrame(index);
    }

    /**
     * Returns the path to the reference file
     * @param projectId
     * @param languageId
     * @param chapterId
     * @return
     */
    public static String getTitlePath(String projectId, String languageId, String chapterId) {
        return Project.getRepositoryPath(projectId, languageId) + chapterId + "/" + REFERENCE_FILE;
    }

    /**
     * Returns the path to the title file
     * @param projectId
     * @param languageId
     * @param chapterId
     * @return
     */
    public static String getReferencePath(String projectId, String languageId, String chapterId) {
        return Project.getRepositoryPath(projectId, languageId) + chapterId + "/" + TITLE_FILE;
    }

    /**
     * Saves the reference and title to the disk
     */
    public void save() {
        Boolean didSave = false;

        if(mReferenceTranslation != null && mTitleTranslation != null) {
            String referencePath = getReferencePath(mProject.getId(), mReferenceTranslation.getLanguage().getId(), getId());
            String titlePath = getTitlePath(mProject.getId(), mTitleTranslation.getLanguage().getId(), getId());

            // save reference
            if(!mReferenceTranslation.isSaved()) {
                didSave = true;
                mReferenceTranslation.isSaved(true);
                File refFile = new File(referencePath);
                if(mReferenceTranslation.getText().isEmpty()) {
                    // delete empty file
                    refFile.delete();
                    cleanDir(mReferenceTranslation.getLanguage());
                } else {
                    // write translation
                    if(!refFile.exists()) {
                        refFile.getParentFile().mkdirs();
                    }
                    try {
                        refFile.createNewFile();
                        PrintStream ps = new PrintStream(refFile);
                        ps.print(mReferenceTranslation.getText());
                        ps.close();
                    } catch (IOException e) {
                        Logger.e(this.getClass().getName(), "failed to write the translation to disk", e);
                    }
                }
            }

            // save title
            if(!mTitleTranslation.isSaved()) {
                didSave = true;
                mTitleTranslation.isSaved(true);
                File titleFile = new File(titlePath);
                if(mTitleTranslation.getText().isEmpty()) {
                    // delete empty file
                    titleFile.delete();
                    cleanDir(mTitleTranslation.getLanguage());
                } else {
                    // write translation
                    if(!titleFile.exists()) {
                        titleFile.getParentFile().mkdirs();
                    }
                    try {
                        titleFile.createNewFile();
                        PrintStream ps = new PrintStream(titleFile);
                        ps.print(mTitleTranslation.getText());
                        ps.close();
                    } catch (IOException e) {
                        Logger.e(this.getClass().getName(), "failed to write the translation to disk", e);
                    }
                }
            }
            // let the project know it has been saved
            if(didSave) {
                mProject.onChapterSaved(this);
            }
        }
    }

    /**
     * Removes empty chapter directory
     * @param translationLanguage the language of the chapter in question
     */
    public void cleanDir(Language translationLanguage) {
        File dir = new File(Project.getRepositoryPath(mProject.getId(), translationLanguage.getId()) + getId());
        String[] files = dir.list();
        if(files != null && files.length == 0) {
            FileUtilities.deleteRecursive(dir);
            AppContext.getEventBus().post(new ChapterTranslationStatusChangedEvent());
        } else if(dir.exists()) {
            // the chapter has translations.
            AppContext.getEventBus().post(new ChapterTranslationStatusChangedEvent());
        }
    }

    /**
     * Checks if the chapter has started being translated
     * @return
     */
    public boolean translationInProgress() {
        return !getReferenceTranslation().getText().isEmpty() || !getTitleTranslation().getText().isEmpty();
    }

    /**
     * Checks if the chapter is being translated
     * @param projectId the project that contains the chapter
     * @param languageId the language of the translation
     * @param chapterId the chapter to check
     * @return
     */
    public static boolean isTranslating(String projectId, String languageId, String chapterId) {
        File dir = new File(Project.getRepositoryPath(projectId, languageId), chapterId);
        String[] files = dir.list();
        return files != null && files.length > 0;
    }

    /**
     * Checks if the chapter translation notes are being translated
     * @param projectId
     * @param languageId
     * @param chapterId
     * @return
     */
    public static boolean isTranslatingNotes(String projectId, String languageId, String chapterId) {
        File dir = new File(TranslationNote.getRepositoryPath(projectId, languageId), chapterId);
        String[] files = dir.list();
        return files != null && files.length > 0;
    }

    /**
     * Check if the chapter is currently being translated
     * @return
     */
    @Override
    public boolean isTranslating() {
        return isTranslating(mProject.getId(), mProject.getSelectedTargetLanguage().getId(), getId());
    }

    /**
     * Checks if the notes in this chapter are being translated
     * @return
     */
    @Override
    public boolean isTranslatingNotes() {
        return isTranslatingNotes(mProject.getId(), mProject.getSelectedTargetLanguage().getId(), getId());
    }

    @Override
    public boolean isTranslatingGlobal() {
        return false;
    }

    @Override
    public boolean isTranslatingNotesGlobal() {
        return false;
    }

    @Override
    public String getType() {
        return "chapter";
    }

    /**
     * Checks if this is the currently selected chapter
     * @return
     */
    @Override
    public boolean isSelected() {
        Project p = AppContext.projectManager().getSelectedProject();
        if(p == null) return false;
        Chapter c = p.getSelectedChapter();
        if(c == null) return false;
        return p.getId().equals(getProject().getId()) && c.getId().equals(getId());
    }

    @Override
    public JSONObject serialize() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("number", mId);
        json.put("ref", mReference);
        json.put("title", mTitle);
        return json;
    }

    /**
     * Returns the frames in this chapter
     * @return
     */
    public Model[] getFrames() {
        return mFrames.getAll().toArray(new Model[mFrames.size()]);
    }

    /**
     * Called when a frame in this chapter has been saved
     * @param frame
     */
    public void onFrameSaved(Frame frame) {
        // let the project know the translation has been saved
        mProject.onChapterSaved(this);
    }

    /**
     * Generates a new chapter instance from json
     * @param json
     * @return
     */
    public static Chapter generate(JSONObject json) {
        try {
            String ref = "";
            if(json.has("ref")) {
                ref = json.getString("ref");
            }
            String title = "";
            if(json.has("title")) {
                title = json.getString("title");
            }
            return new Chapter(json.getString("number"), title, ref);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
