package com.door43.translationstudio.rendering;

import android.text.TextUtils;

import com.door43.translationstudio.spannables.PassageLinkSpan;
import com.door43.translationstudio.spannables.Span;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is the link rendering engine.
 */
public class LinkRenderer extends RenderingEngine {

    private final Span.OnClickListener mLinkListener;
    private final OnPreprocessLink mPreprocessor;

    /**
     * Creates a new link rendering engine with some custom click listeners
     * @param linkListener
     */
    public LinkRenderer(OnPreprocessLink preprocessor, Span.OnClickListener linkListener) {
        mLinkListener = linkListener;
        mPreprocessor = preprocessor;
    }

    @Override
    public CharSequence render(CharSequence in) {
        CharSequence out = in;

        out = renderPassageLink(out);

        return out;
    }

    /**
     * Renders links to other passages
     * @param in
     * @return
     */
    private CharSequence renderPassageLink(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = PassageLinkSpan.PATTERN;
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
            if(isStopped()) return in;
            PassageLinkSpan link = new PassageLinkSpan(matcher.group(3), matcher.group(2));
            link.setOnClickListener(mLinkListener);

            // check if the link should be rendered
            if(mPreprocessor.onPreprocess(link)) {
                out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), link.toCharSequence());
            } else {
                out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), link.getTitle());
            }
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * Used to identify which links to render
     */
    public static interface OnPreprocessLink {
        public boolean onPreprocess(PassageLinkSpan span);
    }
}
