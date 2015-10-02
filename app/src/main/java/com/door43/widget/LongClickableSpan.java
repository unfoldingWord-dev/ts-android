package com.door43.widget;

import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

/**
 * This class is the same as URLSpan except it does not underline the text
 */
public abstract class LongClickableSpan extends android.text.style.ClickableSpan {

    @Override
    public void updateDrawState(TextPaint ds) {
        ds.setUnderlineText(false);
    }

    public abstract void onLongClick(View view);

    public abstract void onClick(View view);
}
