package com.door43.translationstudio.spannables;

/**
 * Created by blm on 3/7/16.
 */
public class VerseSpan extends Span {

    VerseSpan(CharSequence humanReadable, CharSequence machineReadable) {
        super(humanReadable, machineReadable);
    }

    /**
     * Returns the start verse number
     * @return
     */
    public int getStartVerseNumber() {
        return -1;
    }

    /**
     * Returns the end verse number
     * @return
     */
    public int getEndVerseNumber() {
        return -1;
    }


}
