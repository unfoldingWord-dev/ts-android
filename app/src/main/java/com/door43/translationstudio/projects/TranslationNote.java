package com.door43.translationstudio.projects;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A set of translation notes for a frame
 *
 */
public class TranslationNote {
    private List<Note> mNotes = new ArrayList<Note>();

    public TranslationNote(List<Note> notes) {
        mNotes = notes;
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
     * stores an individual note
     */
    public static class Note {
        private String mRef;
        private String mText;

        public Note(String ref, String text) {
            mRef = ref;
            mText = text;
        }

        public String getRef() {
            return mRef;
        }

        public String getText() {
            return mText;
        }
    }
}
