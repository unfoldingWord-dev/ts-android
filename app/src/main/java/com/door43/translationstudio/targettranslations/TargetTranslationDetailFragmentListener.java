package com.door43.translationstudio.targettranslations;

/**
 * Created by joel on 9/11/2015.
 */
public interface TargetTranslationDetailFragmentListener {
    void onScrollProgress(int progress);

    void onItemCountChanged(int itemCount, int progress);

    void onNoSourceTranslations(String targetTranslationId);

    /**
     * causes the activity to re-evaluate the view mode navigation
     */
    void invalidateViewMode();
}
