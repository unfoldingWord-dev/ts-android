package com.door43.translationstudio.rendering;

import android.os.Build;
import android.text.TextUtils;

import com.door43.translationstudio.ui.spannables.USFMChar;
import com.door43.translationstudio.ui.spannables.USFMNoteSpan;
import com.door43.translationstudio.ui.spannables.USFMVerseSpan;
import com.door43.translationstudio.ui.spannables.USXChar;
import com.door43.translationstudio.ui.spannables.USXNoteSpan;
import com.door43.translationstudio.ui.spannables.Span;
import com.door43.translationstudio.ui.spannables.USXVerseSpan;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts USX to USFM
 */
public class USXtoUSFMConverter  {


    /**
     * Creates a new converter
     */
    public USXtoUSFMConverter() {

    }


    public static CharSequence doConversion(CharSequence in) {
        USXtoUSFMConverter converter = new USXtoUSFMConverter();
        return converter.convert(in);
    }

    /**
     * Renders the usx input into usfm
     * @param in the raw input string
     * @return
     */
    public CharSequence convert(CharSequence in) {
        CharSequence out = in;

        out = trimWhitespace(out);
        out = renderLineBreaks(out);
        // TODO: this will strip out new lines. Eventually we may want to convert these to paragraphs.
        out = renderWhiteSpace(out);
        out = renderMajorSectionHeading(out);
        out = renderSectionHeading(out);
        out = renderParagraph(out);
        out = renderBlankLine(out);
        out = renderPoeticLine(out);
        out = renderRightAlignedPoeticLine(out);
        out = renderVerse(out);
        out = renderNote(out);
        out = renderChapterLabel(out);
        out = renderSelah(out);

        return out;
    }

    /**
     * Renders all the Selah tags
     * @param in
     * @return
     */
    private CharSequence renderSelah(CharSequence in) {
        // TODO: 3/3/16 need to add
        return in;
    }

    /**
     * Strips out new lines and replaces them with a single space
     * @param in
     * @return
     */
    public CharSequence trimWhitespace(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = Pattern.compile("(^\\s*|\\s*$)");
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), "");
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * Renders section headings.
     * @param in
     * @return
     */
    public CharSequence renderSectionHeading(CharSequence in) {
        // TODO: 3/3/16 need to add
        return in;
    }
    /**
     * Renders major section headings.
     * @param in
     * @return
     */
    public CharSequence renderMajorSectionHeading(CharSequence in) {
        // TODO: 3/3/16 need to add
        return in;
    }

    /**
     * Strips out extra whitespace from the text
     * @param in
     * @return
     */
    public CharSequence renderWhiteSpace(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = Pattern.compile("(\\s+)");
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
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
        Pattern pattern = Pattern.compile(USXNoteSpan.PATTERN);
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
            USXNoteSpan note = USXNoteSpan.parseNote(matcher.group());
            if(note != null) {
                USFMNoteSpan usfmNote = convertNoteSpanUSXtoUSFM(note);
                out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), usfmNote.getMachineReadable());
            } else {
                // failed to parse the note
                out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.end()));
            }

            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * convert USXNoteSpan to USFMNoteSpan
     * @param note
     * @return
     */
    protected USFMNoteSpan convertNoteSpanUSXtoUSFM(USXNoteSpan note) {
        List<USFMChar> usfmChars = convertUsxChars(note);
        return new USFMNoteSpan(note.getStyle(), note.getCaller(), usfmChars);
    }

    /**
     * convert USXChar list in note to USFMChar list
     * @param note
     * @return
     */
    private List<USFMChar> convertUsxChars(USXNoteSpan note) {
        List<USFMChar> usfmChars = new ArrayList<>();
        List<USXChar> chars = note.getChars();
        for( USXChar c: chars) {
            String style = c.style;
            switch(style) {
                case USXChar.STYLE_PASSAGE_TEXT:
                    style = USFMChar.STYLE_PASSAGE_TEXT;
                    break;
                case USXChar.STYLE_FOOTNOTE_REFERENCE:
                    style = USFMChar.STYLE_FOOTNOTE_REFERENCE;
                    break;
                case USXChar.STYLE_FOOTNOTE_TEXT:
                    style = USFMChar.STYLE_FOOTNOTE_TEXT;
                    break;
                case USXChar.STYLE_FOOTNOTE_KEYWORD:
                    style = USFMChar.STYLE_FOOTNOTE_KEYWORD;
                    break;
                case USXChar.STYLE_FOOTNOTE_QUOTATION:
                    style = USFMChar.STYLE_FOOTNOTE_QUOTATION;
                    break;
                case USXChar.STYLE_FOOTNOTE_ALT_QUOTATION:
                    style = USFMChar.STYLE_FOOTNOTE_ALT_QUOTATION;
                    break;
                case USXChar.STYLE_FOOTNOTE_LABEL:
                    style = USFMChar.STYLE_FOOTNOTE_LABEL;
                    break;
                case USXChar.STYLE_FOOTNOTE_PARAGRAPH:
                    style = USFMChar.STYLE_FOOTNOTE_PARAGRAPH;
                    break;
                case USXChar.STYLE_FOOTNOTE_VERSE:
                    style = USFMChar.STYLE_FOOTNOTE_VERSE;
                    break;
                case USXChar.STYLE_FOOTNOTE_DEUTEROCANONICAL_APOCRYPHA:
                    style = USFMChar.STYLE_FOOTNOTE_DEUTEROCANONICAL_APOCRYPHA;
                    break;
            }
            usfmChars.add(new USFMChar(style, c.value));
        }
        return usfmChars;
    }

    /**
     * Renders all verse tags
     * @param in
     * @return
     */
    public CharSequence renderVerse(CharSequence in) {
        CharSequence out = "";

        CharSequence insert = "";
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN) {
            insert = "\n"; // this is a hack to get around bug in JellyBean in rendering multiple
            // verses on a long line.  This hack messes up the paragraph formatting,
            // but at least JellyBean becomes usable and doesn't crash.
        }

        Pattern pattern = Pattern.compile(USXVerseSpan.PATTERN);
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        List<Integer> foundVerses = new ArrayList<>();
        while(matcher.find()) {
            Span verse = new USFMVerseSpan(matcher.group(1)); // convert verse number to USFM verse span
            if (verse != null) {
                // record found verses
                int startVerse = ((USFMVerseSpan)verse).getStartVerseNumber();
                int endVerse = ((USFMVerseSpan)verse).getEndVerseNumber();
                boolean alreadyRendered = false;
                if(endVerse > startVerse) {
                    // range of verses
                    for(int i = startVerse; i <= endVerse; i ++) {
                        if(!foundVerses.contains(i)) {
                            foundVerses.add(i);
                        } else {
                            alreadyRendered = true;
                        }
                    }
                } else {
                    if(!foundVerses.contains(startVerse)) {
                        foundVerses.add(startVerse);
                    } else {
                        alreadyRendered = true;
                    }
                }
                // render verses not already found
                if(!alreadyRendered) {
                    // exclude verses not within the range
                    boolean invalidVerse = false;

                    if(!invalidVerse) {
                        out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), insert, verse.getMachineReadable());
                    } else {
                        // exclude invalid verse
                        out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()));
                    }
                } else {
                    // exclude duplicate verse
                    out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()));
                }
            } else {
                // failed to parse the verse
                out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.end()));
            }

            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));

        return out;
    }

    /**
     * Renders all paragraph tags
     * @param in
     * @return
     */
    public CharSequence renderParagraph(CharSequence in) {
        // TODO: 3/3/16 need to add
        return in;
    }

    /**
     * Renders all blank line tags
     * @param in
     * @return
     */
    public CharSequence renderBlankLine(CharSequence in) {
        // TODO: 3/3/16 need to add
        return in;
    }

    /**
     * Renders a chapter label
     * @param in
     * @return
     */
    public CharSequence renderChapterLabel(CharSequence in) {
        // TODO: 3/3/16 need to add
        return in;
    }

    /**
     * Renders all poetic line tags
     * @param in
     * @return
     */
    public CharSequence renderPoeticLine(CharSequence in) {
        // TODO: 3/3/16 need to add
        return in;
    }

    /**
     * Renders all right-aligned poetic line tags
     * @param in
     * @return
     */
    public CharSequence renderRightAlignedPoeticLine(CharSequence in) {
        // TODO: 3/3/16 need to add
        return in;
    }

    /**
     * Return the leading section heading, if any. Non-leading major section headings, and leading
     * headings of other types, are not included.
     *
     * <p>As this is a static helper method, behavior is unaffected by the value of
     * {@link mSuppressLeadingMajorSectionHeadings}.</p>
     *
     * @see http://digitalbiblelibrary.org/static/docs/usx/parastyles.html
     * @param in The string to examine for a leading major section heading.
     * @return The leading major section heading; or the empty string if there is none.
     */
    public static CharSequence getLeadingMajorSectionHeading(CharSequence in) {
        // TODO: 3/3/16 need to add
        return in;
    }

    /**
     * Returns a pattern that matches a para tag pair e.g. <para style=""></para>
     * @param style a string or regular expression to identify the style
     * @return
     */
    private static Pattern paraPattern(String style) {
        return Pattern.compile("<para\\s+style=\""+style+"\"\\s*>\\s*(((?!</para>).)*)</para>", Pattern.DOTALL);
    }

    /**
     * Returns a pattern that matches a single para tag e.g. <para style=""/>
     * @param style a string or regular expression to identify the style
     * @return
     */
    private static Pattern paraShortPattern(String style) {
        return Pattern.compile("<para\\s+style=\""+style+"\"\\s*/>", Pattern.DOTALL);
    }
}

