package com.door43.translationstudio.newui;

import android.app.Activity;

/**
 * Displays translations in chunks
 */
public class ChunkModeFragment extends ViewModeFragment {

    @Override
    ViewModeAdapter generateAdapter(Activity activity, String targetTranslationId, String sourceTranslationId) {
        return new ChunkModeAdapter(activity, targetTranslationId, sourceTranslationId);
    }
}
