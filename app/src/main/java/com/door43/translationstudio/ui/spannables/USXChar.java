package com.door43.translationstudio.ui.spannables;

import java.util.regex.Pattern;

/**
 * Represents a char element according to the usx specification
 * See http://dbl.ubs-icap.org:8090/display/DBLDOCS/USX#USX-char
 */
public class USXChar {
    // passage styles (custom tag not defined in USX)
    public static final String STYLE_PASSAGE_TEXT = "pt";

    // footnote styles
    public static final String STYLE_FOOTNOTE_REFERENCE = "fr";
    public static final String STYLE_FOOTNOTE_TEXT = "ft";
    public static final String STYLE_FOOTNOTE_KEYWORD = "fk";
    public static final String STYLE_FOOTNOTE_QUOTATION = "fq";
    public static final String STYLE_FOOTNOTE_ALT_QUOTATION = "fqa";
    public static final String STYLE_FOOTNOTE_LABEL = "fl";
    public static final String STYLE_FOOTNOTE_PARAGRAPH = "fp";
    public static final String STYLE_FOOTNOTE_VERSE = "fv";
    public static final String STYLE_FOOTNOTE_DEUTEROCANONICAL_APOCRYPHA = "fdc";

    // selah styles
    public static final String STYLE_SELAH = "qs";

    public final String style;
    public final CharSequence value;

    public USXChar(String style, CharSequence value) {
        this.style = style.trim().toLowerCase();
        this.value = value;
    }

    /**
     * Returns the compiled pattern to match this char
     * @return
     */
    public static Pattern getPattern(String style) {
        return Pattern.compile("<char\\s+style=\"" + style + "\"\\s*>\\s*(((?!</char>).)*)</char>", Pattern.DOTALL);
    }
}
