package com.door43.translationstudio.rendering;

import com.door43.translationstudio.core.TranslationFormat;
import com.door43.translationstudio.ui.spannables.Span;

/**
 * ClickableRenderingEngineFactory for creating ClickableRenderingEngine based on format
 */
public class ClickableRenderingEngineFactory {

    /**
     * create appropriate rendering engine for format and add click listeners
     * @param format
     * @param defaultFormat
     * @param verseClickListener
     * @param noteClickListener
     * @return
     */
    public static ClickableRenderingEngine create(TranslationFormat format, TranslationFormat defaultFormat, Span.OnClickListener verseClickListener, Span.OnClickListener noteClickListener) {

        ClickableRenderingEngine renderer = null;

        if( (format != TranslationFormat.USFM) && (format != TranslationFormat.USX) ) {
            format = defaultFormat;
        }

        if(format == TranslationFormat.USFM) {
            renderer = new USFMRenderer(verseClickListener, noteClickListener);
        } if(format == TranslationFormat.USX)  {
            renderer = new USXRenderer(verseClickListener, noteClickListener);
        }

        return renderer;
    }

}
