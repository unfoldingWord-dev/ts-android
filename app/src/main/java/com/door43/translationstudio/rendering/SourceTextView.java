package com.door43.translationstudio.rendering;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by joel on 1/26/2015.
 */
@Deprecated
public class SourceTextView extends TextView {
    public SourceTextView(Context context) {
        super(context);
    }

    public SourceTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public SourceTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Performs rendering operations before setting the text on this text view
     * @param text
     */
    public void renderText(CharSequence text) {
        // TODO: render
        setText(text);
    }
}
