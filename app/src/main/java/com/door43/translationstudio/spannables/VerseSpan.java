package com.door43.translationstudio.spannables;

/**
 * Created by joel on 1/27/2015.
 */
public class VerseSpan extends FancySpan {
    public VerseSpan(String spanId, String spanText, OnClickListener clickListener) {
        super(spanId, spanText, clickListener);
    }

    /**
     * Converts the verse to a spannable char sequence
     * @return
     */
    public CharSequence toCharSequence() {
        return generateSpan();
    }
}
