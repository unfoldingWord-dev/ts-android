package com.door43.translationstudio.targettranslations;

import android.content.Context;

/**
 * Created by joel on 9/8/2015.
 */
public class ReadModeFragment extends ViewModeFragment {

    @Override
    ViewModeAdapter generateAdapter(Context context, String targetTranslationId, String sourceTranslationId) {
        return new ReadModeAdapter(context, targetTranslationId, sourceTranslationId);
    }
}
