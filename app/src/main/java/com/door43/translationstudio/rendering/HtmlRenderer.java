package com.door43.translationstudio.rendering;

import android.text.Html;
import android.text.TextUtils;

import com.door43.translationstudio.ui.spannables.ArticleLinkSpan;
import com.door43.translationstudio.ui.spannables.MarkdownLinkSpan;
import com.door43.translationstudio.ui.spannables.MarkdownTitledLinkSpan;
import com.door43.translationstudio.ui.spannables.PassageLinkSpan;
import com.door43.translationstudio.ui.spannables.ShortReferenceSpan;
import com.door43.translationstudio.ui.spannables.Span;
import com.door43.translationstudio.ui.spannables.TranslationWordLinkSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by joel on 12/2/2015.
 */
public class HtmlRenderer extends RenderingEngine {

    private final Span.OnClickListener mLinkListener;
    private final OnPreprocessLink preprocessCallback;

    public HtmlRenderer(OnPreprocessLink preprocessor, Span.OnClickListener linkListener) {
        mLinkListener = linkListener;
        preprocessCallback = preprocessor;
    }

    @Override
    public CharSequence render(CharSequence in) {
        CharSequence out = in;
        out = renderTranslationAcademyAddress(out);
        if(isStopped()) return in;
        out = renderPassageLink(out);
        if(isStopped()) return in;
        out = renderShortReferenceLink(out);
        if(isStopped()) return in;
        out = renderMarkdownLink(out);
        if(isStopped()) return in;
        out = renderTranslationWordLink(out);
        if(isStopped()) return in;
        // TODO: 12/15/2015 it would be nice if we could pass in a private click listener and interpret the link types before calling the supplied listener.
        // this will allow calling code to use instance of rather than comparing strings.
        out = Html.fromHtml(out.toString(), null, new HtmlTagHandler(mLinkListener));
        if(isStopped()) return in;
        return out;
    }

    private CharSequence renderTranslationWordLink(CharSequence in) {
        return renderLink(in, MarkdownLinkSpan.PATTERN, "tw", new OnCreateLink() {
            @Override
            public Span onCreate(Matcher matcher) {
                String address = matcher.group(1).replaceAll("^:", "").trim().toLowerCase();

                // cut off title e.g. en:obe:other:stuff|title
                String[] addressName = address.split("\\|");
                address = addressName[0];

                String[] chunks = address.split(":");
                if(chunks.length > 2) {
                    String id = null;
                    // check for tw links
                    if(chunks[1].equals("obe")) {
                        id = chunks[chunks.length-1];
                    }
                    // TODO: if there are other forms of tw links we can check for them here.

                    if(id != null) {
                        return new TranslationWordLinkSpan(id, id);
                    }
                }
                return null;
            }
        });
    }

    private CharSequence renderMarkdownLink(CharSequence in) {
        return renderLink(in, MarkdownTitledLinkSpan.PATTERN, "m", new OnCreateLink() {
            @Override
            public Span onCreate(Matcher matcher) {
                return new MarkdownTitledLinkSpan(matcher.group(1), matcher.group(3));
            }
        });
    }

    /**
     * Renders links to other passages in the project
     * @param in
     * @return
     */
    private CharSequence renderPassageLink(CharSequence in) {
        return renderLink(in, PassageLinkSpan.PATTERN, "p", new OnCreateLink() {
            @Override
            public Span onCreate(Matcher matcher) {
                return new PassageLinkSpan(matcher.group(3), matcher.group(1));
            }
        });
    }

    /**
     * Renders short references. that is references without a book label.
     * e.g. 1:1 indicates chapter 1 verse 1 of the current book.
     *
     * @param in
     * @return
     */
    private CharSequence renderShortReferenceLink(CharSequence in) {
        return renderLink(in, ShortReferenceSpan.PATTERN, "sr", new OnCreateLink() {
            @Override
            public Span onCreate(Matcher matcher) {
                return new ShortReferenceSpan(matcher.group(0));
            }
        });
    }

    /**
     * Renders addresses to translation academy pages as html
     * Example [[en:ta:vol1:translate:translate_unknown|How to Translate Unknowns]]
     * @param in
     * @return
     */
    public CharSequence renderTranslationAcademyAddress(CharSequence in) {
        return renderLink(in, ArticleLinkSpan.ADDRESS_PATTERN, "ta", new OnCreateLink() {
            @Override
            public Span onCreate(Matcher matcher) {
                String title = matcher.group(4);
                if(title == null) {
                    title = matcher.group(0);
                }
                return ArticleLinkSpan.parse(title, matcher.group(2));
            }
        });
    }

    /**
     * Renders links to translation academy pages as html
     * Example <a href="/en/ta/vol1/translate/figs_intro" title="en:ta:vol1:translate:figs_intro">Figures of Speech</a>
     * @param in
     * @return
     */
    public CharSequence renderTranslationAcademyLink(CharSequence in) {
        return renderLink(in, ArticleLinkSpan.LINK_PATTERN, "ta", new OnCreateLink() {
            @Override
            public Span onCreate(Matcher matcher) {
                String title = matcher.group(6);
                if(title == null) {
                    title = matcher.group(0);
                }
                return ArticleLinkSpan.parse(title, matcher.group(3).replace("/", ":"));
            }
        });
    }

    /**
     * A generic rendering method for rendering content links as html
     *
     * @param in
     *@param pattern
     * @param callback   @return
     */
    private CharSequence renderLink(CharSequence in, Pattern pattern, String linkType, OnCreateLink callback) {
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
                    CharSequence title = link.getHumanReadable();
                    if(title == null || title.toString().isEmpty()) {
                        title = link.getMachineReadable();
                    }
                    String htmlLink = "<app-link href=\"" + link.getMachineReadable() + "\" type=\"" + linkType + "\" >" + title + "</app-link>";
                    out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), htmlLink);
                } else {
                    // render as plain text
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
