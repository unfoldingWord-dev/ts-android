package com.door43.translationstudio.ui.translate.review;

import com.door43.translationstudio.ui.translate.TranslationHelp;

import org.unfoldingword.resourcecontainer.Link;

/**
 * Interface for events on the resource card
 */
public interface OnResourceClickListener {
    void onNoteClick(TranslationHelp note, int resourceCardWidth);
    void onWordClick(String resourceContainerSlug, Link word, int resourceCardWidth);
    void onQuestionClick(TranslationHelp question, int resourceCardWidth);
    void onResourceTabNotesSelected(ReviewHolder holder, ReviewListItem item);
    void onResourceTabWordsSelected(ReviewHolder holder, ReviewListItem item);
    void onResourceTabQuestionsSelected(ReviewHolder holder, ReviewListItem item);
    void onSourceTabSelected(String sourceTranslationId);
    void onChooseSourceButtonSelected();
    void onTapResourceCard();
}
