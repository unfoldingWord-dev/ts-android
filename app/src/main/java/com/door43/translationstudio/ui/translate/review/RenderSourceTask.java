package com.door43.translationstudio.ui.translate.review;

import android.graphics.Color;
import android.view.View;

import com.door43.translationstudio.core.TranslationFormat;
import com.door43.translationstudio.rendering.Clickables;
import com.door43.translationstudio.rendering.DefaultRenderer;
import com.door43.translationstudio.rendering.RenderingGroup;
import com.door43.translationstudio.ui.spannables.NoteSpan;
import com.door43.translationstudio.ui.spannables.Span;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.tools.taskmanager.ManagedTask;

/**
 * Renders the source text
 */
public class RenderSourceTask extends ManagedTask {

    private static final int HIGHLIGHT_COLOR = Color.YELLOW;
    private final Door43Client library;
    private final ReviewListItem item;
    private OnSourceClickListener listener;
    private final CharSequence searchQuery;
    private final SearchSubject searchSubject;

    public RenderSourceTask(Door43Client library, ReviewListItem item, OnSourceClickListener listener, CharSequence searchQuery, SearchSubject searchSubject) {
        this.library = library;
        this.item = item;
        this.listener = listener;
        this.searchQuery = searchQuery;
        this.searchSubject = searchSubject;
    }

    @Override
    public void start() {
        setThreadPriority(Thread.MIN_PRIORITY);
        if(isCanceled()) return;
        CharSequence text = renderSourceText(item.sourceText, item.sourceTranslationFormat, item);
        setResult(text);
    }

    /**
     * generate spannable for source text.  Will add click listener for notes if supported
     * @param text
     * @param format
     * @param item
     * @return
     */
    private CharSequence renderSourceText(String text, TranslationFormat format, final ReviewListItem item) {
        RenderingGroup renderingGroup = new RenderingGroup();
        boolean enableSearch = this.searchQuery != null
                && searchSubject == SearchSubject.SOURCE;
        if (Clickables.isClickableFormat(format)) {
            // TODO: add click listeners for verses
            Span.OnClickListener noteClickListener = new Span.OnClickListener() {
                @Override
                public void onClick(View view, Span span, int start, int end) {
                    if(span instanceof NoteSpan) {
                        if(listener != null) listener.onSourceFootnoteClick(item, (NoteSpan)span, start, end);
                    }
                }

                @Override
                public void onLongClick(View view, Span span, int start, int end) {

                }
            };

            Clickables.setupRenderingGroup(format, renderingGroup, null, noteClickListener, false);
            if( enableSearch ) {
                renderingGroup.setSearchString(this.searchQuery, HIGHLIGHT_COLOR);
            }
        } else {
            // TODO: add note click listener
            renderingGroup.addEngine(new DefaultRenderer(null));
            if( enableSearch ) {
                renderingGroup.setSearchString(this.searchQuery, HIGHLIGHT_COLOR);
            }
        }
        renderingGroup.init(text);
        CharSequence results = renderingGroup.start();
        item.hasMissingVerses = renderingGroup.isAddedMissingVerse();
        return results;
    }

    /**
     * Generates a tag for this task
     *
     * @param chapter
     * @param chunk
     * @return
     */
    public static String makeTag(String chapter, String chunk) {
        return "render_source_" + chapter + "_" + chunk + "_task";
    }

    public ReviewListItem getItem() {
        return item;
    }
}
