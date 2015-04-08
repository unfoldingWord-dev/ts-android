package com.door43.translationstudio.events;

import com.door43.translationstudio.projects.Language;

/**
 * Created by joel on 10/7/2014.
 * @deprecated
 */
public class LanguageModalDismissedEvent {
    private Language mLanguage;
    private Boolean mDidCancel;

    public LanguageModalDismissedEvent(Language language, Boolean didCancel) {
        mLanguage = language;
        mDidCancel = didCancel;
    }

    /**
     * Returns the language that was selected or null
     * @return
     */
    public Language getLanguage() {
        return mLanguage;
    }

    /**
     * Checks if the dialog was canceled.
     * @return
     */
    public Boolean didCancel() {
        return mDidCancel;
    }
}
