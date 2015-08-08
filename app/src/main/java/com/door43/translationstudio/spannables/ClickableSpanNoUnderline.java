package com.door43.translationstudio.spannables;

import android.text.TextPaint;
import android.text.style.ClickableSpan;

/**
 * This class is the same as URLSpan except it does not underline the text
 */
public abstract class ClickableSpanNoUnderline extends ClickableSpan {

    @Override
    public void updateDrawState(TextPaint ds) {
        ds.setUnderlineText(false);
    }
}
