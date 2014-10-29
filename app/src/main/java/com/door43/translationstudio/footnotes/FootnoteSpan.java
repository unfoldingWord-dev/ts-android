package com.door43.translationstudio.footnotes;

import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import com.door43.translationstudio.util.MainContext;

/**
 * Created by joel on 10/28/2014.
 */
public class FootnoteSpan extends ClickableSpan {
    private final Footnote mFootnote;

    public FootnoteSpan(Footnote footnote) {
        mFootnote = footnote;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        ds.setUnderlineText(true);
    }

    @Override
    public void onClick(View view) {
        MainContext.getContext().showToastMessage("hello world!!");
    }
}
