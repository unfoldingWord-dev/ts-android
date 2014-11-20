package com.door43.translationstudio.spannables;

import com.door43.translationstudio.R;

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
        // TODO: we have to use a plain span (without any drawing) to preserve word wrapping.
        // perhaps we could provide an option in the user prefs to use bubble spans instead.
        return generateSpan();
    }
}
