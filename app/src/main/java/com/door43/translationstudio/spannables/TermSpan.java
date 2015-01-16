package com.door43.translationstudio.spannables;

/**
 * Created by joel on 10/31/2014.
 */
public class TermSpan extends FancySpan{
    public TermSpan(String id, String text, OnClickListener clickListener) {
        super(id, text, clickListener);
    }

    /**
     * Converts the footnote to a spannable char sequence
     * @return
     */
    public CharSequence toCharSequence() {
        return generateSpan();
    }
}
