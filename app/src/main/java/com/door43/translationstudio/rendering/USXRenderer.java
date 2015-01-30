package com.door43.translationstudio.rendering;

import android.graphics.Typeface;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.View;

import com.door43.translationstudio.R;
import com.door43.translationstudio.spannables.RelativeLineHeightSpan;
import com.door43.translationstudio.spannables.Span;
import com.door43.translationstudio.spannables.VerseSpan;
import com.door43.translationstudio.util.MainContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by joel on 1/26/2015.
 */
public class USXRenderer extends RenderingEngine {

    private Span.OnClickListener mVerseListener;

    /**
     * Creates a new usx rendering engine without any listeners
     */
    public USXRenderer() {

    }

    /**
     * Creates a new usx rendering engine with some custom click listeners
     * @param verseListener
     */
    public USXRenderer(VerseSpan.OnClickListener verseListener) {
        mVerseListener = verseListener;
    }

    /**
     * Renders the usx input into a readable form
     * @param in the raw input string
     * @return
     */
    @Override
    public CharSequence render(CharSequence in) {
        CharSequence out = in;

        out = renderParagraph(out);
        out = renderVerse(out);

        return out;
    }

    private CharSequence renderVerse(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = Pattern.compile(VerseSpan.PATTERN);
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
            if(isStopped()) return in;
            VerseSpan verse = new VerseSpan(matcher.group(1));
            verse.setOnClickListener(mVerseListener);

            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), verse.toCharSequence());
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    private CharSequence renderParagraph(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = Pattern.compile("<para\\s+style=\"p\"\\s*>(((?!</para>).)*)</para>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
            if(isStopped()) return in;
            SpannableString span = new SpannableString(matcher.group(1));
            // TODO: are we supposed to style paragraphs in a particular way?
            // leading margin span
            // line break span
            String lineBreak = "";
            if(out.length() > 0) {
                lineBreak = "\n";
            }
            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), lineBreak, "    ", span, "\n");
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }
}
