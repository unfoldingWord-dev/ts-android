package com.door43.translationstudio.util;

import android.app.DialogFragment;

/**
 * passes the passage note dialog event to the main activity
 */
public class PassageNoteEvent {
    private DialogFragment mDialog;
    private Status mStatus;
    private String mPassage;
    private String mNote;
    private Boolean mIsFootnote;
    private String mSpanId;
    public static enum Status {
        OK, CANCEL, DELETE
    };


    public PassageNoteEvent(DialogFragment dialog, Status status, String passage, String note, String spanId, Boolean isFootnote) {
        mDialog = dialog;
        mStatus = status;
        mPassage = passage;
        mNote = note;
        mSpanId = spanId;
        mIsFootnote = isFootnote;
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

    public Boolean getIsFootnote() {
        return mIsFootnote;
    }

    public String getSpanId() {
        return mSpanId;
    }
}
