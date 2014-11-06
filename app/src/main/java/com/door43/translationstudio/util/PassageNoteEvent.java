package com.door43.translationstudio.util;

import android.app.DialogFragment;

import com.door43.translationstudio.spannables.NoteSpan;

/**
 * passes the passage note dialog event to the main activity
 */
public class PassageNoteEvent {
    private DialogFragment mDialog;
    private Status mStatus;
    private String mPassage;
    private String mNote;
    private NoteSpan.NoteType mNoteType;
    private String mSpanId;
    public static enum Status {
        OK, CANCEL, DELETE
    };


    public PassageNoteEvent(DialogFragment dialog, Status status, String passage, String note, String spanId, NoteSpan.NoteType noteType) {
        mDialog = dialog;
        mStatus = status;
        mPassage = passage;
        mNote = note;
        mSpanId = spanId;
        mNoteType = noteType;
    }

    public DialogFragment getDialog() {
        return mDialog;
    }

    public Status getStatus() {
        return mStatus;
    }

    public String getPassage() {
        return mPassage;
    }

    public String getNote() {
        return mNote;
    }

    public NoteSpan.NoteType getNoteType() {
        return mNoteType;
    }

    public String getSpanId() {
        return mSpanId;
    }
}
