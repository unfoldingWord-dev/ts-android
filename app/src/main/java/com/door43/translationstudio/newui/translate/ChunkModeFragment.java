package com.door43.translationstudio.newui.translate;

import android.app.Activity;
import android.view.MotionEvent;

/**
 * Displays translations in chunks
 */
public class ChunkModeFragment extends ViewModeFragment {

    @Override
    ViewModeAdapter generateAdapter(Activity activity, String targetTranslationId, String sourceTranslationId, String chapterId, String frameId) {
        return new ChunkModeAdapter(activity, targetTranslationId, sourceTranslationId, chapterId, frameId);
    }

    @Override
    protected void onRightSwipe(MotionEvent e1, MotionEvent e2) {
        if(getAdapter() != null) {
            ((ChunkModeAdapter) getAdapter()).closeTargetTranslationCard();
        }
    }

    @Override
    protected void onLeftSwipe(MotionEvent e1, MotionEvent e2) {
        if(getAdapter() != null) {
            ((ChunkModeAdapter) getAdapter()).openTargetTranslationCard();
        }
    }
}
