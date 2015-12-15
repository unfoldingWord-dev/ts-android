package com.door43.translationstudio.rendering;

import android.text.TextUtils;

import com.door43.translationstudio.spannables.ArticleLinkSpan;
import com.door43.translationstudio.spannables.PassageLinkSpan;
import com.door43.translationstudio.spannables.Span;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is the link rendering engine.
 */
public class LinkRenderer extends RenderingEngine {

    private final Span.OnClickListener mLinkListener;
    private final OnPreprocessLink preprocessCallback;
    private boolean renderHtml = false;

    /**
     * Creates a new link rendering engine with some custom click listeners
     * @param linkListener
     */
    public LinkRenderer(OnPreprocessLink preprocessor, Span.OnClickListener linkListener) {
        mLinkListener = linkListener;
        preprocessCallback = preprocessor;
    }

    @Override
    public CharSequence render(CharSequence in) {
        this.renderHtml = false;
        CharSequence out = in;

        out = renderPassageLink(out);
        out = renderTranslationAcademyLink(out);

        return out;
    }

    public String renderHtml(CharSequence in) {
        this.renderHtml = true;

        CharSequence out = in;

        out = renderPassageLink(out);
        out = renderTranslationAcademyLink(out);

        return out.toString();
    }

    /**
     * Renders links to other passages
     * @param in
     * @return
     */
    private CharSequence renderPassageLink(CharSequence in) {
        return renderLink(in, PassageLinkSpan.PATTERN, new OnCreateLink() {
            @Override
            public Span onCreate(Matcher matcher) {
                return new PassageLinkSpan(matcher.group(3), matcher.group(2));
            }
        });
    }

    /**
     * Renders links to translation academy pages
     * @param in
     * @return
     */
    private CharSequence renderTranslationAcademyLink(CharSequence in) {
        return renderLink(in, ArticleLinkSpan.PATTERN, new OnCreateLink() {
            @Override
            public Span onCreate(Matcher matcher) {
                return ArticleLinkSpan.parse(matcher.group(3), matcher.group(2));
            }
        });
    }

    /**
     * A generic rendering method for rendering span links
     *
     * @param in
     * @param pattern
     * @param callback
     * @return
     */
    private CharSequence renderLink(CharSequence in, Pattern pattern, OnCreateLink callback) {
        CharSequence out = "";
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
            if(isStopped()) return in;
            Span link = callback.onCreate(matcher);
            if(link != null) {
                link.setOnClickListener(mLinkListener);
                if (preprocessCallback == null || preprocessCallback.onPreprocess(link)) {
                    // render clickable link
                    if(this.renderHtml) {
                        String htmlLink = "<app-link href=\"" + link.getMachineReadable() + "\">" + link.getHumanReadable() + "</app-link>";
                        out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), htmlLink);
                    } else {
                        out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), link.toCharSequence());
                    }
                } else {
                    // render non-clickable link
                    out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), link.getHumanReadable());
                }
            } else {
                // ignore link
                out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.end()));
            }
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    private interface OnCreateLink {
        Span onCreate(Matcher matcher);
    }

    /**
     * Used to identify which links to render
     */
    public interface OnPreprocessLink {
        boolean onPreprocess(Span span);
    }
}
