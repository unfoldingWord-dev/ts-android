package com.door43.translationstudio.footnotes;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ClickableSpan;
import android.view.View;

/**
 * Created by joel on 10/28/2014.
 */
public class Footnote {
    private final String mText;

    public Footnote(String text) {
        mText = text;
    }

    @Override
    public String toString() {
        return mText;
    }

    /**
     * Converts the footnote to a spannable char sequence
     * @return
     */
    public CharSequence toCharSequence() {
        SpannableString spannable = new SpannableString(mText);
        int length = spannable.length();
        if(length > 0) {
            spannable.setSpan(new FootnoteSpan(this), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannable;
    }
}
