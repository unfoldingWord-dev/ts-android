package com.door43.translationstudio.projects;

import com.door43.translationstudio.R;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.FileUtilities;
import com.door43.util.reporting.Logger;
import com.door43.util.Security;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * A set of translation notes for a frame
 *
 */
public class TranslationNote {
    private List<Note> mNotes = new ArrayList<Note>();
    private Frame mFrame;

    protected TranslationNote(List<Note> notes) {
        mNotes = notes;
        for(Note n:notes) {
            n.setNote(this);
        }
    }

    /**
     * Returns the path to the notes repository
     * @param language
     * @return
     */
    public String getRepositoryPath(Language language) {
        return getRepositoryPath(mFrame.getChapter().getProject().getId(), language.getId());
    }

    /**
     * Returns the path to the notes repository
     * @param projectId
     * @param languageId
     * @return
     */
    public static String getRepositoryPath(String projectId, String languageId) {
        return AppContext.context().getFilesDir() + "/" + AppContext.context().getResources().getString(R.string.git_repository_dir) + "/" + Project.GLOBAL_PROJECT_SLUG + "-" + projectId + "-" + languageId + "-notes/";
    }

    /**
     * Returns the path to this translation note
     * @return
     */
    public String getTranslationNotePath() {
        return getRepositoryPath(mFrame.getChapter().getProject().getSelectedTargetLanguage()) + mFrame.getChapter().getId() + "/" + mFrame.getId() + "/";
    }

    /**
     * Returns a list of notes
     * @return
     */
    public List<Note> getNotes() {
        return mNotes;
    }

    /**
     * Serializes the notes
     * @return
     */
    public JSONObject serialize() throws JSONException {
        JSONObject json = new JSONObject();
        JSONArray notesJson = new JSONArray();
        for(Note n:mNotes) {
            JSONObject note = new JSONObject();
            note.put("ref", n.getRef());
            note.put("text", n.getText());
            notesJson.put(note);
        }
        json.put("tn", notesJson);
        // NOTE: the id of the note must be inserted by the frame
        return json;
    }

    /**
     * Generates a new translation note instance
     * @param json
     * @return
     */
    public static TranslationNote Generate(JSONObject json) {
        try {
            JSONArray notesJson = json.getJSONArray("tn");
            List<Note> notes = new ArrayList<>();
            for(int i = 0; i < notesJson.length(); i ++) {
                JSONObject noteJson = notesJson.getJSONObject(i);
                Note n = new Note(noteJson.getString("ref"), noteJson.getString("text"));
                notes.add(n);
            }
            return new TranslationNote(notes);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns the frame to which the translation note belongs
     * @return
     */
    private Frame getFrame() {
        return mFrame;
    }

    /**
     * Attaches the translation note to a frame
     * @param frame
     */
    public void setFrame(Frame frame) {
        mFrame = frame;
    }

    /**
     * stores an individual note
     */
    public static class Note {
        private String mRef;
        private String mText;
        private TranslationNote mTranslationNote;
        private Translation mDefinitionTranslation;
        private Translation mReferenceTranslation;
        private String mId = null;

        public Note(String ref, String text) {
            mRef = ref;
            mText = text;
        }

        /**
         * Attaches the note to a translation note
         * @param n
         */
        public void setNote(TranslationNote n) {
            mTranslationNote = n;
        }

        public String getRef() {
            return mRef;
        }

        public String getText() {
            return mText;
        }

        /**
         * Sets the translation of the definition
         * @param translation
         */
        public void setDefinitionTranslation(String translation) {
            if(mTranslationNote.getFrame() != null) {
                mDefinitionTranslation = new Translation(mTranslationNote.getFrame().getChapter().getProject().getSelectedTargetLanguage(), translation);
            }
        }

        /**
         * Sets the translation of the reference
         * @param translation
         */
        public void setReferenceTranslation(String translation) {
            if(mTranslationNote.getFrame() != null) {
                mReferenceTranslation = new Translation(mTranslationNote.getFrame().getChapter().getProject().getSelectedTargetLanguage(), translation);
            }
        }

        /**
         * Returns the translation of the referernce
         * @return
         */
        public Translation getRefTranslation() {
            if(mReferenceTranslation == null || !mReferenceTranslation.isLanguage(mTranslationNote.getFrame().getChapter().getProject().getSelectedTargetLanguage())) {
                if(mReferenceTranslation != null) {
                    save();
                }
                // load from disk
                try {
                    File refFile = new File(getReferencePath());
                    if(refFile.exists()) {
                        String text = FileUtils.readFileToString(refFile);
                        setReferenceTranslation(text);
                        mReferenceTranslation.isSaved(true);
                    } else {
                        setReferenceTranslation("");
                        mReferenceTranslation.isSaved(true);
                    }
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "failed to load the note reference translation from disk", e);
                }
            }
            return mReferenceTranslation;
        }

        /**
         * Returns the translation of the definition
         * @return
         */
        public Translation getTextTranslation() {
            if(mDefinitionTranslation == null || !mDefinitionTranslation.isLanguage(mTranslationNote.getFrame().getChapter().getProject().getSelectedTargetLanguage())) {
                if(mDefinitionTranslation != null) {
                    save();
                }
                // load from disk
                try {
                    File definitionFile = new File(getDefinitionPath());
                    if(definitionFile.exists()) {
                        String text = FileUtils.readFileToString(definitionFile);
                        setDefinitionTranslation(text);
                        mDefinitionTranslation.isSaved(true);
                    } else {
                        setDefinitionTranslation("");
                        mDefinitionTranslation.isSaved(true);
                    }
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "failed to load the note definition translation from disk", e);
                }
            }
            return mDefinitionTranslation;
        }

        public String getReferencePath() {
            return mTranslationNote.getTranslationNotePath() + getId() + "/ref.txt";
        }

        public String getDefinitionPath() {
            return mTranslationNote.getTranslationNotePath() + getId() + "/def.txt";
        }

        /**
         * Saves the translation note
         */
        public void save() {
            synchronized (this) {
                if (mReferenceTranslation != null && !mReferenceTranslation.isSaved()) {
                    mReferenceTranslation.isSaved(true);
                    File file = new File(getReferencePath());
                    if (mReferenceTranslation.getText().isEmpty()) {
                        cleanDiretory(file);
                    } else {
                        file.getParentFile().mkdirs();
                        try {
                            FileUtils.write(file, mReferenceTranslation.getText());
                        } catch (Exception e) {
                            Logger.e(this.getClass().getName(), "Failed to save the translation note reference", e);
                        }
                    }
                }
                if (mDefinitionTranslation != null) {
                    mDefinitionTranslation.isSaved(true);
                    File file = new File(getDefinitionPath());
                    if (mDefinitionTranslation.getText().isEmpty()) {
                        cleanDiretory(file);
                    } else {
                        file.getParentFile().mkdirs();
                        try {
                            FileUtils.write(file, mDefinitionTranslation.getText());
                        } catch (Exception e) {
                            Logger.e(this.getClass().getName(), "Failed to save the translation note definition", e);
                        }
                    }
                }
            }
        }

        /**
         * Deletes a note file and removes the directory if there are no more files
         * @param file
         */
        private void cleanDiretory(File file) {
            file.delete();

            // remove note directory
            deleteEmptyDir(file.getParentFile());
            // remove frame directory
            deleteEmptyDir(file.getParentFile().getParentFile());
            // remove chapter directory
            deleteEmptyDir(file.getParentFile().getParentFile().getParentFile());
            // remove project directory
            deleteEmptyDir(file.getParentFile().getParentFile().getParentFile().getParentFile());
        }

        /**
         * Deletes a directory if it is empty
         * @param dir
         */
        private void deleteEmptyDir(File dir) {
            String[] fileNames = dir.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return !filename.equals(".") && !filename.equals("..");
                }
            });
            // remove frame directory
            if(fileNames == null || fileNames.length == 0) {
                FileUtilities.deleteRecursive(dir);
            }
        }

        /**
         * Returns the generated id of the note
         * @return
         */
        public String getId() {
            if(mId == null) {
                mId = Security.md5(mTranslationNote.getFrame().getChapterFrameId() + mRef);
            }
            return mId;
        }
    }
}
