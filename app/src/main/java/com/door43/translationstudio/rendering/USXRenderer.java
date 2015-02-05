package com.door43.translationstudio.rendering;

import android.text.SpannableString;
import android.text.TextUtils;

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
}
