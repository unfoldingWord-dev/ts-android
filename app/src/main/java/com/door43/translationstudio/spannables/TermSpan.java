package com.door43.translationstudio.spannables;

import com.door43.translationstudio.R;
import com.door43.translationstudio.util.MainContext;

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
        return generateSpan(R.drawable.span_blue_bubble, R.color.white, R.dimen.h5);
    }
}
