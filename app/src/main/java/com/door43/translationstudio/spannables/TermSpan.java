package com.door43.translationstudio.spannables;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import com.door43.translationstudio.R;
import com.door43.translationstudio.util.AppContext;

/**
 * Created by joel on 10/31/2014.
 */
public class TermSpan extends Span {
    private SpannableStringBuilder mSpannable;
    private final String mTermId;
    public static final String PATTERN = "<keyterm>(((?!</keyterm>).)*)</keyterm>";

    public TermSpan(String id, String text) {
        super(text, text);
        mTermId = id;
    }

    @Override
    public SpannableStringBuilder render() {
        if(mSpannable == null) {
            mSpannable = super.render();
            // apply custom styles
            mSpannable.setSpan(new ForegroundColorSpan(AppContext.context().getResources().getColor(R.color.accent)), 0, mSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
