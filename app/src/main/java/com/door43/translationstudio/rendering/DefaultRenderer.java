package com.door43.translationstudio.rendering;

import com.door43.translationstudio.ui.spannables.Span;

/**
 * This is the default rendering engine.
 */
public class DefaultRenderer extends RenderingEngine {

    private Span.OnClickListener mNoteListener;
    private String mSearch;
    private int mHighlightColor = 0;

    /**
     * Creates a new default rendering engine without any listeners
     */
    public DefaultRenderer() {

    }

    /**
     * If set to not null matched strings will be highlighted.
     *
     * @param searchString - null is disable
     * @param highlightColor
     */
    public void setSearchString(CharSequence searchString, int highlightColor) {
        mHighlightColor = highlightColor;
        if((searchString != null) && (searchString.length() > 0) ) {
            mSearch = searchString.toString().toLowerCase();
        } else {
            mSearch = null;
        }
    }

    /**
     * Creates a new default rendering engine with some custom click listeners
     * @param noteListener
     */
    public DefaultRenderer(Span.OnClickListener noteListener) {
        mNoteListener = noteListener;
    }

    /**
     * Renders the input into a readable format
     * @param in the raw input string
     * @return
     */
    @Override
    public CharSequence render(CharSequence in) {
        CharSequence out = in;

        USXRenderer renderer = new USXRenderer(null, mNoteListener);
        renderer.setSearchString(mSearch, mHighlightColor);

        out = renderer.renderNote(out);
        out = renderer.renderHighlightSearch(out);

        return out;
    }
}
