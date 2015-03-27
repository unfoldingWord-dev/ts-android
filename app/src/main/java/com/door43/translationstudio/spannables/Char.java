package com.door43.translationstudio.spannables;

/**
 * Represents a char element according to the usx specification
 * See http://dbl.ubs-icap.org:8090/display/DBLDOCS/USX#USX-char
 */
public class Char {
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

    public final String style;
    public final String value;

    public Char(String style, String value) {
        this.style = style.trim().toLowerCase();
        this.value = value;
    }
}
