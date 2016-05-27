package com.door43.translationstudio.rendering;

import com.door43.translationstudio.core.TranslationFormat;
import com.door43.translationstudio.spannables.Span;
import com.door43.translationstudio.spannables.USFMNoteSpan;
import com.door43.translationstudio.spannables.USXNoteSpan;

/**
 * Class to support clickable spans
 */
public class Clickables {


    /**
     * test if this is a clickable format
     * @param format
     * @return
     */
    public static boolean isClickableFormat(TranslationFormat format) {
        return (format == TranslationFormat.USX) || (format == TranslationFormat.USFM);
    }

    /**
     * setup rendering group for translation format
     * @param format
     * @param renderingGroup
     * @param verseClickListener
     * @param noteClickListener
     * @param target - true if rendering target translations, false if source text
     * @return
     */
    static public ClickableRenderingEngine setupRenderingGroup(TranslationFormat format, RenderingGroup renderingGroup, Span.OnClickListener verseClickListener, Span.OnClickListener noteClickListener, boolean target) {

        TranslationFormat defaultFormat = target ? TranslationFormat.USFM : TranslationFormat.USX;
        ClickableRenderingEngine renderer = ClickableRenderingEngineFactory.create(format, defaultFormat, verseClickListener, noteClickListener);
        renderingGroup.addEngine(renderer);
        return renderer;
    }
}
