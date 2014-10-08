package com.door43.translationstudio.events;

import com.door43.translationstudio.util.ModalDialog;

/**
 * Created by joel on 10/7/2014.
 */
public class ModalDismissedEvent {
    private ModalDialog mDialog;
    private String mId;
    private Boolean mDidCancel;

    public ModalDismissedEvent(ModalDialog dialog, String id, Boolean didCancel) {
        mDialog = dialog;
        mId = id;
        mDidCancel = didCancel;
    }

    /**
     * Returns the modal dialog that was dismissed.
     * @return
     */
    public ModalDialog getDialog() {
        return mDialog;
    }

    /**
     * Returns the id of the dialog that fired the event
     * @return
     */
    public String getId() {
        return mId;
    }

    /**
     * Checks if the dialog was canceled.
     * @return
     */
    public Boolean didCancel() {
        return mDidCancel;
    }
}
