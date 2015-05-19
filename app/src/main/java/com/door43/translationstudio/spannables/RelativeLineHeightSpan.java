package com.door43.translationstudio.spannables;

import android.graphics.Paint;
import android.text.style.LineHeightSpan;

@Deprecated
public class RelativeLineHeightSpan implements LineHeightSpan {
    private final float mProportion;

    public RelativeLineHeightSpan(float proportion) {
        mProportion = proportion;
    }

    @Override
    public void chooseHeight(CharSequence text, int start, int end, int spanstartv, int v, Paint.FontMetricsInt fm) {
        fm.bottom *= mProportion;
        fm.descent *= mProportion;
    }
}