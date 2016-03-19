package com.door43.translationstudio.spannables;

import android.text.SpannableStringBuilder;

/**
 * abstract base class for NoteSpans
 */
public abstract class NoteSpan extends Span {

    /**
     * returns the caller
     * @return
     */
    public abstract String getCaller();

    /**
     * Returns the type of note this is
     * @return
     */
    public abstract String getStyle();

    /**
     * Returns the notes regarding the passage
     * @return
     */
    public abstract CharSequence getNotes();

    /**
     * Returns the text upon which the notes are made
     * @return
     */
    public abstract CharSequence getPassage();


}
