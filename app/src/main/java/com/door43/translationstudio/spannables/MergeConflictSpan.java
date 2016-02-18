package com.door43.translationstudio.spannables;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;

import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;

/**
 * Created by joel on 2/16/2016.
 */
public class MergeConflictSpan extends Span {
    public static final String PATTERN = "<<<<<<< HEAD (.*) ======= (.*) >>>>>>> refs\\/heads\\/new";
    private final String mHeadChanges;
    private final String mNewChanges;
    private SpannableStringBuilder mSpannable;

    public MergeConflictSpan(String machine, String headChanges, String newChanges) {
        super("", machine);

        mHeadChanges = headChanges;
        mNewChanges = newChanges;
    }

    @Override
    public SpannableStringBuilder render() {
        if(mSpannable == null) {
            mSpannable = super.render();

            SpannableStringBuilder headChanges = new SpannableStringBuilder(mHeadChanges);
            headChanges.setSpan(new BackgroundColorSpan(AppContext.context().getResources().getColor(R.color.green)), 0, headChanges.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            SpannableStringBuilder newChanges = new SpannableStringBuilder(mNewChanges);
            newChanges.setSpan(new BackgroundColorSpan(AppContext.context().getResources().getColor(R.color.red)), 0, newChanges.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            mSpannable.append(headChanges);
            mSpannable.append("\n");
            mSpannable.append(newChanges);
        }
        return mSpannable;
    }
}
