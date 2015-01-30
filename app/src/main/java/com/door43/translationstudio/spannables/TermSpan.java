package com.door43.translationstudio.spannables;

import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;

import com.door43.translationstudio.R;
import com.door43.translationstudio.util.MainContext;

/**
 * Created by joel on 10/31/2014.
 */
public class TermSpan extends Span {
    private SpannableStringBuilder mSpannable;
    private final String mTermId;

    public TermSpan(String id, String text) {
        super(text, text);
        mTermId = id;
    }

    public SpannableStringBuilder generateSpan() {
        if(mSpannable == null) {
            mSpannable = super.generateSpan();
            // apply custom styles
            mSpannable.setSpan(new ForegroundColorSpan(MainContext.getContext().getResources().getColor(R.color.blue)), 0, mSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//            mSpannable.setSpan(new LeadingMarginSpan.Standard(10), 0, mSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return mSpannable;
    }

    /**
     * Returns the id of the term
     * @return
     */
    public String getTermId() {
        return mTermId;
    }
}
