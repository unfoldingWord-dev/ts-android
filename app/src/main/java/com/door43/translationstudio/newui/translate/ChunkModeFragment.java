package com.door43.translationstudio.newui.translate;

import android.app.Activity;

/**
 * Displays translations in chunks
 */
public class ChunkModeFragment extends ViewModeFragment {

    @Override
    ViewModeAdapter generateAdapter(Activity activity, String targetTranslationId, String sourceTranslationId, String chapterId, String frameId) {
        return new ChunkModeAdapter(activity, targetTranslationId, sourceTranslationId);
    }
}
