package com.door43.translationstudio.ui.translate.review;

import com.door43.translationstudio.ui.spannables.NoteSpan;

/**
 * Created by joel on 3/6/17.
 */

public interface OnSourceClickListener {
    void onSourceFootnoteClick(ReviewListItem item, NoteSpan span, int start, int end);
}
