package com.door43.translationstudio.spannables;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;

import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;

/**
 * Created by joel on 10/1/2015.
 */
public class VerseBubbleSpan extends VerseSpan {

    private SpannableStringBuilder mSpannable;

    public VerseBubbleSpan(String verse) {
        super(verse);
    }

    public VerseBubbleSpan(int verse) {
        super(verse);
    }

    public SpannableStringBuilder render() {
        if(mSpannable == null) {
            mSpannable = super.render();
            // apply custom styles
            mSpannable.setSpan(new RelativeSizeSpan(0.8f), 0, mSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            mSpannable.setSpan(new ForegroundColorSpan(AppContext.context().getResources().getColor(R.color.white)), 0, mSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            mSpannable.setSpan(new BackgroundColorSpan(AppContext.context().getResources().getColor(R.color.accent)), 0, mSpannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return mSpannable;
    }
}
