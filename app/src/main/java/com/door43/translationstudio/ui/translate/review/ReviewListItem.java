package com.door43.translationstudio.ui.translate.review;

import com.door43.translationstudio.ui.translate.ListItem;

import java.util.List;

/**
 * Represents a single item in the review list
 */
public class ReviewListItem extends ListItem {
    public boolean hasSearchText = false;
    public List<CharSequence> mergeItems;
    public int mergeItemSelected = -1;
    public int selectItemNum = -1;
    public boolean refreshSearchHighlightSource = false;
    public boolean refreshSearchHighlightTarget = false;
    public int currentTargetTaskId = -1;
    public int currentSourceTaskId = -1;
    public boolean hasMissingVerses = false;

    public ReviewListItem(String chapterSlug, String chunkSlug) {
        super(chapterSlug, chunkSlug);
    }


}
