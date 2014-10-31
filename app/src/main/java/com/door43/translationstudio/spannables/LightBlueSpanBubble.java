package com.door43.translationstudio.spannables;

import com.door43.translationstudio.R;

/**
 * Created by joel on 10/28/2014.
 */
public class LightBlueSpanBubble extends FancySpan {
    public LightBlueSpanBubble(String id, String text, OnClickListener clickListener) {
        super(id, text, clickListener);
    }

    /**
     * Converts the footnote to a spannable char sequence
     * @return
     */
    public CharSequence toCharSequence() {
        return generateSpan(R.drawable.light_blue_bubble, R.color.blue, R.dimen.h5);
    }
}
