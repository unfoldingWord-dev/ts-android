package com.door43.translationstudio.rendering;

/**
 * This is an abstract base class for clickable rendering engine. This handles all of the rendering for USX formatted source and translation
 * NOTE: when rendering large chunks of text it is important to always keep things as a CharSequence and not string
 * so that spans generated by prior rendering methods are not lost.
 */
public abstract class ClickableRenderingEngine extends RenderingEngine {

    /**
     * if set to false verses will not be displayed in the output.
     *
     * @param enable default is true
     */
    public abstract void setVersesEnabled(boolean enable);

    /**
     * if set to true, then line breaks will be shown in the output.
     *
     * @param enable default is false
     */
    public abstract void setLinebreaksEnabled(boolean enable);

    /**
     * If set to not null matched strings will be highlighted.
     *
     * @param searchString - null is disable
     * @param highlightColor
     */
    public abstract void setSearchString(CharSequence searchString, int highlightColor);

    /**
     * Specifies an inclusive range of verses expected in the input.
     * If a verse is not found it will be inserted at the front of the input.
     * @param verseRange
     */
    public abstract void setPopulateVerseMarkers(int[] verseRange);

    /**
     * Set whether to suppress display of major section headers.
     *
     * <p>The intent behind this is that major section headers prior to chapter markers will be
     * displayed above chapter markers, but only in read mode.</p>
     *
     * @param suppressLeadingMajorSectionHeadings The value to set
     */
    public abstract void setSuppressLeadingMajorSectionHeadings(boolean suppressLeadingMajorSectionHeadings);

     /**
     * Renders all verse tags
     * @param in
     * @return
     */
    public abstract CharSequence renderVerse(CharSequence in);

    public abstract CharSequence getLeadingMajorSectionHeading(CharSequence in);

    public abstract boolean isAddedMissingVerse();
}

