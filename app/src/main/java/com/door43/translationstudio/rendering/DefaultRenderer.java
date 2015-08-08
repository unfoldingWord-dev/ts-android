package com.door43.translationstudio.rendering;

import com.door43.translationstudio.spannables.Span;

/**
 * This is the default rendering engine.
 */
public class DefaultRenderer extends RenderingEngine {

    private Span.OnClickListener mNoteListener;

    /**
     * Creates a new default rendering engine without any listeners
     */
    public DefaultRenderer() {

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

        out = new USXRenderer(null, mNoteListener).renderNote(out);

        return out;
    }
}
