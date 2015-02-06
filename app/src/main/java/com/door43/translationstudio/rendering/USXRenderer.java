package com.door43.translationstudio.rendering;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;

import com.door43.translationstudio.spannables.NoteSpan;
import com.door43.translationstudio.spannables.Span;
import com.door43.translationstudio.spannables.VerseSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is the USX rendering engine. This handles all of the rendering for USX formatted source and translation
 */
public class USXRenderer extends RenderingEngine {

    private Span.OnClickListener mNoteListener;
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
    public USXRenderer(Span.OnClickListener verseListener, Span.OnClickListener noteListener) {
        mVerseListener = verseListener;
        mNoteListener = noteListener;
    }

    /**
     * Renders the usx input into a readable form
     * @param in the raw input string
     * @return
     */
    @Override
    public CharSequence render(CharSequence in) {
        CharSequence out = in.toString().trim();

        out = renderLineBreaks(out);
        out = renderWhiteSpace(out);
        out = renderParagraph(out);
        out = renderPoeticLine(out);
        out = renderVerse(out);
        out = renderNote(out);

        return out;
    }

    public CharSequence renderWhiteSpace(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = Pattern.compile("(\\s+)");
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
            if(isStopped()) return in;
            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), " ");
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }
    
    /**
     * Strips out new lines and replaces them with a single space
     * @param in
     * @return
     */
    public CharSequence renderLineBreaks(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = Pattern.compile("(\\s*\\n+\\s*)");
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
            if(isStopped()) return in;
            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), " ");
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * Renders all note tags
     * @param in
     * @return
     */
    public CharSequence renderNote(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = Pattern.compile(NoteSpan.PATTERN);
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
            if(isStopped()) return in;
            NoteSpan verse = NoteSpan.parseNote(matcher.group());
            verse.setOnClickListener(mNoteListener);

            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), verse.toCharSequence());
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * Renders all verse tags
     * @param in
     * @return
     */
    public CharSequence renderVerse(CharSequence in) {
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

    /**
     * Renders all paragraph tgs
     * @param in
     * @return
     */
    public CharSequence renderParagraph(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = Pattern.compile("<para\\s+style=\"p\"\\s*>\\s*(((?!</para>).)*)</para>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
            if(isStopped()) return in;
            SpannableString span = new SpannableString(matcher.group(1));
            String lineBreak = "";
            if(matcher.start() > 0) {
                lineBreak = "\n";
            }
            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), lineBreak, "    ", span, "\n");
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * Renders all paragraph tgs
     * @param in
     * @return
     */
    public CharSequence renderPoeticLine(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = Pattern.compile("<para\\s+style=\"q(\\d+)\"\\s*>\\s*(((?!</para>).)*)</para>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
            if(isStopped()) return in;
            int level = Integer.parseInt(matcher.group(1));
            SpannableString span = new SpannableString(matcher.group(2));
            span.setSpan(new StyleSpan(Typeface.ITALIC), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            String padding = "";
            for(int i = 0; i < level; i ++) {
                padding += "    ";
            }

            // don't stack new lines
            String leadingLineBreak = "";
            String trailingLineBreak = "";

            // leading
            if(in.subSequence(0, matcher.start()) != null) {
                String previous = in.subSequence(0, matcher.start()).toString().replace(" ", "");
                int lastLineBreak = previous.lastIndexOf("\n");
                if (lastLineBreak < previous.length()) {
                    leadingLineBreak = "\n";
                }
            }

            // trailing
            if(in.subSequence(matcher.end(), in.length()) != null) {
                String next = in.subSequence(matcher.end(), in.length()).toString().replace(" ", "");
                int nextLineBreak = next.indexOf("\n");
                if (nextLineBreak > 0) {
                    trailingLineBreak = "\n";
                }
            }

            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), leadingLineBreak, padding, span, trailingLineBreak);
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }
}
