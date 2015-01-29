package com.door43.translationstudio.spannables;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;

import com.door43.translationstudio.R;
import com.door43.translationstudio.util.MainContext;

/**
 * Created by joel on 1/27/2015.
 */
public class VerseSpan extends Span {

    /**
     * Creates a new verse span
     *
     */
    public VerseSpan(String verse) {
        super(" "+verse.trim()+" ", "<verse number=\""+verse.trim()+"\" style=\"v\" />");
    }

    @Override
    public SpannableStringBuilder generateSpan() {
        SpannableStringBuilder span = super.generateSpan();
        // apply custom styles
        span.setSpan(new RelativeSizeSpan(0.8f), 0, span.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        span.setSpan(new ForegroundColorSpan(MainContext.getContext().getResources().getColor(R.color.gray)), 0, span.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return span;
    }
}
